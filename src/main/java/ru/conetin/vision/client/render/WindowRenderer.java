package ru.conetin.vision.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.Defines;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import ru.conetin.vision.client.os.Window;
import ru.conetin.vision.client.os.WindowContent;
import ru.conetin.vision.client.os.WindowManager;
import ru.conetin.vision.client.os.apps.WebContent;
import ru.conetin.vision.client.ui.UiCanvas;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class WindowRenderer {

    private static final int FULL_BRIGHT = 0x00F000F0;
    private static final float PZ = 0f;
    private static final int CORNER_SEG = 6;
    private static final int RADIUS = 18;

    private static final int GLASS_FILL = 0x59000000;

    private static final ShaderProgramKey GLASS_KEY = new ShaderProgramKey(
            Identifier.of("vision", "core/glass"), VertexFormats.POSITION_TEXTURE, Defines.EMPTY);
    private static final ShaderProgramKey BAR_KEY = new ShaderProgramKey(
            Identifier.of("vision", "core/bar"), VertexFormats.POSITION_TEXTURE, Defines.EMPTY);
    private static final ShaderProgramKey CLOSE_KEY = new ShaderProgramKey(
            Identifier.of("vision", "core/close"), VertexFormats.POSITION_TEXTURE, Defines.EMPTY);
    private static final ShaderProgramKey HANDLE_KEY = new ShaderProgramKey(
            Identifier.of("vision", "core/handle"), VertexFormats.POSITION_TEXTURE, Defines.EMPTY);
    private static final ShaderProgramKey WEB_KEY = new ShaderProgramKey(
            Identifier.of("vision", "core/web"), VertexFormats.POSITION_TEXTURE, Defines.EMPTY);

    private static long lastNanos = 0L;

    private WindowRenderer() {
    }

    private static float frameDt() {
        long now = System.nanoTime();
        double dt = lastNanos == 0L ? 1.0 / 60.0 : (now - lastNanos) / 1.0e9;
        lastNanos = now;
        if (dt > 0.1 || dt < 0.0) {
            dt = 1.0 / 60.0;
        }
        return (float) dt;
    }

    public static void render(WorldRenderContext context) {
        List<Window> windows = WindowManager.INSTANCE.windows();
        if (windows.isEmpty()) {
            return;
        }

        Vec3d camPos = context.camera().getPos();
        float dt = frameDt();
        for (Window w : windows) {
            w.animate(dt);
        }
        Vec3d lookDir = Vec3d.fromPolar(context.camera().getPitch(), context.camera().getYaw());
        WindowManager.INSTANCE.updateDrag(camPos, lookDir);

        VertexConsumerProvider consumers = context.consumers();
        VertexConsumerProvider.Immediate immediate =
                consumers instanceof VertexConsumerProvider.Immediate imm ? imm : null;
        TextRenderer font = MinecraftClient.getInstance().textRenderer;

        List<Window> sorted = new ArrayList<>(windows);
        sorted.sort(Comparator.comparingDouble(
                (Window w) -> w.center().squaredDistanceTo(camPos)).reversed());

        Matrix4f proj = context.projectionMatrix();
        Matrix4f view = context.positionMatrix();

        int mainFbo = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        int sw = MinecraftClient.getInstance().getWindow().getFramebufferWidth();
        int sh = MinecraftClient.getInstance().getWindow().getFramebufferHeight();

        RenderSystem.backupProjectionMatrix();
        RenderSystem.setProjectionMatrix(proj, RenderSystem.getProjectionType());
        Matrix4fStack mv = RenderSystem.getModelViewStack();
        mv.pushMatrix();
        mv.set(view);

        BlurRenderer.captureWorld(mainFbo, sw, sh);

        for (Window window : sorted) {
            if (window.eased() <= 0.01f) {
                continue;
            }
            int backdrop = buildBackdrop(window, sorted, camPos, sw, sh, mainFbo, font);
            renderWindow(window, camPos, backdrop, immediate, font);
        }

        mv.popMatrix();
        RenderSystem.restoreProjectionMatrix();
        restoreState();
    }

    private static int buildBackdrop(Window target, List<Window> sorted, Vec3d camPos,
                                     int sw, int sh, int mainFbo, TextRenderer font) {
        BlurRenderer.beginScratch(sw, sh);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        for (Window other : sorted) {
            if (other == target) {
                continue;
            }
            float oa = other.eased();
            if (oa <= 0.01f) {
                continue;
            }
            Matrix4f mo = buildMatrix(other, camPos);
            int ow = other.resWidth();
            int oh = other.resHeight();
            Canvas oc = new Canvas(mo, font, null, oa, ow, oh, -1f, -1f, false);
            oc.roundedRect(0, 0, ow, oh, RADIUS, GLASS_FILL);
        }

        return BlurRenderer.blurScratch(sw, sh, mainFbo);
    }

    private static void renderWindow(Window window, Vec3d camPos,
                                     int blurTex, VertexConsumerProvider.Immediate immediate,
                                     TextRenderer font) {
        float alpha = window.eased();
        if (alpha <= 0.01f) {
            return;
        }

        Matrix4f m = buildMatrix(window, camPos);

        setupState();

        int resW = window.resWidth();
        int resH = window.resHeight();

        WindowManager manager = WindowManager.INSTANCE;
        boolean hovered = manager.hoveredWindow() == window;
        Canvas base = new Canvas(m, font, immediate, alpha, resW, resH,
                hovered ? manager.hoverX() : -1f, hovered ? manager.hoverY() : -1f,
                manager.focusedWindow() == window);

        base.glass(blurTex, RADIUS, GLASS_FILL);

        WindowContent content = window.content();
        if (content instanceof WebContent web) {
            web.ensureBrowser();
            int tex = web.textureId();
            if (tex > 0) {
                base.web(tex, RADIUS);
            } else {
                content.render(base);
            }
        } else {
            content.render(base);
        }

        float M = window.cornerShow();
        boolean entering = window.isCornerActive();
        int side = window.cornerSide();

        if (M < 0.5f) {
            float a = M / 0.5f;
            float barAlpha = 1f - a;
            float barWScale = 1f - a;
            float slide = entering ? a * side * (resW * 0.22f) : 0f;
            if (barAlpha > 0.001f) {
                float fullW = window.barWidth();
                float barW = Math.max(1f, fullW * barWScale);
                float closeShrink = window.closeHover() * 5f;
                float anchor = side > 0 ? (fullW - barW) : 0f;
                float bx = window.barX() + anchor + closeShrink + slide;
                base.bar(bx, window.barY(), Math.max(1f, barW - closeShrink),
                        window.barHeight(), window.barHover(), barAlpha);
                base.closeButton(window.closeCx() + slide, window.closeCy(), 26f,
                        window.closeHover(), window.controlsShow() * barAlpha);
            }
        }
        if (M > 0.5f) {
            float b = (M - 0.5f) / 0.5f;
            final float REST_LO = -0.15f, REST_HI = 1.15f;
            final float SEG_U = REST_HI - REST_LO;
            float headU = REST_LO + b * (REST_HI - REST_LO);
            float tailU;
            float handleAlpha;
            if (entering) {
                tailU = REST_LO;
                handleAlpha = Math.min(1f, b * 3f);
            } else {
                tailU = headU - SEG_U;
                handleAlpha = Math.min(1f, b * 2f);
            }
            if (handleAlpha > 0.001f) {
                base.handle(resW, resH, RADIUS, side, handleAlpha, headU, tailU,
                        window.cornerHover(), 1f);
            }
        }

        if (immediate != null) {
            immediate.draw();
        }
    }

    private static Matrix4f buildMatrix(Window window, Vec3d camPos) {
        float scale = window.visualScale();
        float sx = window.physWidth() * scale / window.resWidth();
        float sy = window.physHeight() * scale / window.resHeight();

        Vec3d right = window.right();
        Vec3d up = window.up();
        Vec3d normal = window.normal();

        Vec3d colPx = right.multiply(sx);
        Vec3d colPy = up.multiply(-sy);
        Vec3d colPz = normal.multiply(sx);

        Vec3d rel = window.center().subtract(camPos);
        Vec3d origin = rel
                .subtract(right.multiply(0.5 * window.physWidth() * scale))
                .add(up.multiply(0.5 * window.physHeight() * scale));

        return new Matrix4f(
                (float) colPx.x, (float) colPx.y, (float) colPx.z, 0f,
                (float) colPy.x, (float) colPy.y, (float) colPy.z, 0f,
                (float) colPz.x, (float) colPz.y, (float) colPz.z, 0f,
                (float) origin.x, (float) origin.y, (float) origin.z, 1f
        );
    }

    private static void setupState() {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
    }

    private static void restoreState() {
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }

    private static final class Canvas implements UiCanvas {
        private final Matrix4f m;
        private final TextRenderer font;
        private final VertexConsumerProvider.Immediate consumers;
        private final float winAlpha;
        private final int logW;
        private final int logH;
        private final float hx;
        private final float hy;
        private final boolean focused;

        Canvas(Matrix4f m, TextRenderer font, VertexConsumerProvider.Immediate consumers,
               float winAlpha, int logW, int logH, float hx, float hy, boolean focused) {
            this.m = m;
            this.font = font;
            this.consumers = consumers;
            this.winAlpha = winAlpha;
            this.logW = logW;
            this.logH = logH;
            this.hx = hx;
            this.hy = hy;
            this.focused = focused;
        }

        @Override public int width() { return logW; }
        @Override public int height() { return logH; }
        @Override public float hoverX() { return isHovered() ? hx : -1f; }
        @Override public float hoverY() { return isHovered() ? hy : -1f; }
        @Override public boolean isHovered() {
            return hx >= 0 && hy >= 0 && hx <= logW && hy <= logH;
        }
        @Override public boolean isFocused() { return focused; }
        @Override public float alpha() { return winAlpha; }
        @Override public int textWidth(String text) { return font.getWidth(text); }
        @Override public int fontHeight() { return font.fontHeight; }

        @Override
        public void fillRect(float x, float y, float w, float h, int argb) {
            int c = applyAlpha(argb);
            BufferBuilder bb = begin(VertexFormat.DrawMode.QUADS);
            vert(bb, x, y, c);
            vert(bb, x, y + h, c);
            vert(bb, x + w, y + h, c);
            vert(bb, x + w, y, c);
            draw(bb);
        }

        @Override
        public void gradientRect(float x, float y, float w, float h, int argbTop, int argbBottom) {
            int top = applyAlpha(argbTop);
            int bot = applyAlpha(argbBottom);
            BufferBuilder bb = begin(VertexFormat.DrawMode.QUADS);
            vert(bb, x, y, top);
            vert(bb, x, y + h, bot);
            vert(bb, x + w, y + h, bot);
            vert(bb, x + w, y, top);
            draw(bb);
        }

        @Override
        public void roundedRect(float x, float y, float w, float h, float radius, int argb) {
            int c = applyAlpha(argb);
            float[] p = roundedPerimeter(x, y, w, h, radius);
            BufferBuilder bb = begin(VertexFormat.DrawMode.TRIANGLE_FAN);
            vert(bb, x + w / 2f, y + h / 2f, c);
            for (int i = 0; i < p.length; i += 2) {
                vert(bb, p[i], p[i + 1], c);
            }
            vert(bb, p[0], p[1], c);
            draw(bb);
        }

        @Override
        public void roundedRectOutline(float x, float y, float w, float h, float radius,
                                       float thickness, int argb) {
            int c = applyAlpha(argb);
            float[] outer = roundedPerimeter(x, y, w, h, radius);
            float[] inner = roundedPerimeter(x + thickness, y + thickness,
                    w - 2 * thickness, h - 2 * thickness, Math.max(0, radius - thickness));
            BufferBuilder bb = begin(VertexFormat.DrawMode.TRIANGLE_STRIP);
            for (int i = 0; i < outer.length; i += 2) {
                vert(bb, outer[i], outer[i + 1], c);
                vert(bb, inner[i], inner[i + 1], c);
            }
            vert(bb, outer[0], outer[1], c);
            vert(bb, inner[0], inner[1], c);
            draw(bb);
        }

        @Override
        public void text(String text, float x, float y, int argb) {
            font.draw(text, x, y, applyAlpha(argb), false, m, consumers,
                    TextRenderer.TextLayerType.NORMAL, 0, FULL_BRIGHT);
        }

        @Override
        public void textCentered(String text, float cx, float y, int argb) {
            text(text, cx - font.getWidth(text) / 2f, y, argb);
        }

        void glass(int blurTex, float radius, int tint) {
            ShaderProgram prog = MinecraftClient.getInstance().getShaderLoader().getOrCreateProgram(GLASS_KEY);
            if (prog == null) {
                roundedRect(0, 0, logW, logH, radius, tint);
                return;
            }
            RenderSystem.setShader(prog);
            RenderSystem.setShaderTexture(0, blurTex);
            RenderSystem.setShaderColor(1f, 1f, 1f, winAlpha);

            GlUniform ws = prog.getUniform("WinSize");
            if (ws != null) {
                ws.set((float) logW, (float) logH);
            }
            GlUniform rad = prog.getUniform("Radius");
            if (rad != null) {
                rad.set(radius);
            }
            GlUniform ti = prog.getUniform("Tint");
            if (ti != null) {
                ti.set(((tint >> 16) & 0xFF) / 255f, ((tint >> 8) & 0xFF) / 255f,
                        (tint & 0xFF) / 255f, ((tint >>> 24) & 0xFF) / 255f);
            }

            BufferBuilder bb = Tessellator.getInstance().begin(
                    VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
            quadVert(bb, 0, 0, 0f, 0f);
            quadVert(bb, 0, logH, 0f, 1f);
            quadVert(bb, logW, logH, 1f, 1f);
            quadVert(bb, logW, 0, 1f, 0f);
            BuiltBuffer built = bb.endNullable();
            if (built != null) {
                BufferRenderer.drawWithGlobalProgram(built);
            }
        }

        private void quadVert(BufferBuilder bb, float lx, float ly, float u, float v) {
            bb.vertex(m, lx, ly, PZ).texture(u, v);
        }

        void web(int texId, float radius) {
            ShaderProgram prog = MinecraftClient.getInstance().getShaderLoader().getOrCreateProgram(WEB_KEY);
            if (prog == null) {
                return;
            }
            RenderSystem.setShader(prog);
            RenderSystem.setShaderTexture(0, texId);
            RenderSystem.setShaderColor(1f, 1f, 1f, winAlpha);
            GlUniform u;
            if ((u = prog.getUniform("WinSize")) != null) {
                u.set((float) logW, (float) logH);
            }
            if ((u = prog.getUniform("Radius")) != null) {
                u.set(radius);
            }
            if ((u = prog.getUniform("FlipV")) != null) {
                u.set(0f);
            }
            BufferBuilder bb = Tessellator.getInstance().begin(
                    VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
            quadVert(bb, 0, 0, 0f, 0f);
            quadVert(bb, 0, logH, 0f, 1f);
            quadVert(bb, logW, logH, 1f, 1f);
            quadVert(bb, logW, 0, 1f, 0f);
            BuiltBuffer built = bb.endNullable();
            if (built != null) {
                BufferRenderer.drawWithGlobalProgram(built);
            }
            RenderSystem.setShaderTexture(0, 0);
        }

        void bar(float x, float y, float w, float h, float hover, float fade) {
            ShaderProgram prog = MinecraftClient.getInstance().getShaderLoader().getOrCreateProgram(BAR_KEY);
            if (prog == null) {
                roundedRect(x, y, w, h, h / 2f, 0x80FFFFFF);
                return;
            }
            RenderSystem.setShader(prog);
            RenderSystem.setShaderColor(1f, 1f, 1f, winAlpha * fade);
            GlUniform bs = prog.getUniform("BarSize");
            if (bs != null) {
                bs.set(w, h);
            }
            GlUniform hv = prog.getUniform("Hover");
            if (hv != null) {
                hv.set(hover);
            }
            BufferBuilder bb = Tessellator.getInstance().begin(
                    VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
            quadVert(bb, x, y, 0f, 0f);
            quadVert(bb, x, y + h, 0f, 1f);
            quadVert(bb, x + w, y + h, 1f, 1f);
            quadVert(bb, x + w, y, 1f, 0f);
            BuiltBuffer built = bb.endNullable();
            if (built != null) {
                BufferRenderer.drawWithGlobalProgram(built);
            }
        }

        void handle(int resW, int resH, float radius, int side, float alpha, float headU,
                    float tailU, float hover, float scale) {
            ShaderProgram prog = MinecraftClient.getInstance().getShaderLoader().getOrCreateProgram(HANDLE_KEY);
            if (prog == null) {
                return;
            }
            float margin = 30f;
            RenderSystem.setShader(prog);
            RenderSystem.setShaderColor(1f, 1f, 1f, winAlpha);
            GlUniform u;
            if ((u = prog.getUniform("WinSize")) != null) {
                u.set((float) resW, (float) resH);
            }
            if ((u = prog.getUniform("Radius")) != null) {
                u.set(radius);
            }
            if ((u = prog.getUniform("Side")) != null) {
                u.set((float) side);
            }
            if ((u = prog.getUniform("Margin")) != null) {
                u.set(margin);
            }
            if ((u = prog.getUniform("Alpha")) != null) {
                u.set(alpha);
            }
            if ((u = prog.getUniform("HeadU")) != null) {
                u.set(headU);
            }
            if ((u = prog.getUniform("TailU")) != null) {
                u.set(tailU);
            }
            if ((u = prog.getUniform("Hover")) != null) {
                u.set(hover);
            }
            if ((u = prog.getUniform("Scale")) != null) {
                u.set(scale);
            }
            BufferBuilder bb = Tessellator.getInstance().begin(
                    VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
            quadVert(bb, -margin, -margin, 0f, 0f);
            quadVert(bb, -margin, resH + margin, 0f, 1f);
            quadVert(bb, resW + margin, resH + margin, 1f, 1f);
            quadVert(bb, resW + margin, -margin, 1f, 0f);
            BuiltBuffer built = bb.endNullable();
            if (built != null) {
                BufferRenderer.drawWithGlobalProgram(built);
            }
        }

        void closeButton(float cx, float cy, float size, float hover, float show) {
            if (show <= 0.001f) {
                return;
            }
            ShaderProgram prog = MinecraftClient.getInstance().getShaderLoader().getOrCreateProgram(CLOSE_KEY);
            if (prog == null) {
                return;
            }
            RenderSystem.setShader(prog);
            RenderSystem.setShaderColor(1f, 1f, 1f, winAlpha);
            GlUniform sz = prog.getUniform("Size");
            if (sz != null) {
                sz.set(size, size);
            }
            GlUniform hv = prog.getUniform("Hover");
            if (hv != null) {
                hv.set(hover);
            }
            GlUniform sh = prog.getUniform("Show");
            if (sh != null) {
                sh.set(show);
            }
            float h = size / 2f;
            BufferBuilder bb = Tessellator.getInstance().begin(
                    VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
            quadVert(bb, cx - h, cy - h, 0f, 0f);
            quadVert(bb, cx - h, cy + h, 0f, 1f);
            quadVert(bb, cx + h, cy + h, 1f, 1f);
            quadVert(bb, cx + h, cy - h, 1f, 0f);
            BuiltBuffer built = bb.endNullable();
            if (built != null) {
                BufferRenderer.drawWithGlobalProgram(built);
            }
        }

        private int applyAlpha(int argb) {
            int baseA = (argb >>> 24) & 0xFF;
            int newA = Math.round(baseA * winAlpha);
            return (newA << 24) | (argb & 0x00FFFFFF);
        }

        private BufferBuilder begin(VertexFormat.DrawMode mode) {
            return Tessellator.getInstance().begin(mode, VertexFormats.POSITION_COLOR);
        }

        private void vert(BufferBuilder bb, float lx, float ly, int argb) {
            bb.vertex(m, lx, ly, PZ).color(argb);
        }

        private void draw(BufferBuilder bb) {
            BuiltBuffer built = bb.endNullable();
            if (built == null) {
                return;
            }
            RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            BufferRenderer.drawWithGlobalProgram(built);
        }

        private static float[] roundedPerimeter(float x, float y, float w, float h, float r) {
            r = Math.max(0, Math.min(r, Math.min(w, h) / 2f));
            int per = CORNER_SEG + 1;
            float[] out = new float[4 * per * 2];
            int idx = 0;
            idx = arc(out, idx, x + r, y + r, r, 180, 270, per);
            idx = arc(out, idx, x + w - r, y + r, r, 270, 360, per);
            idx = arc(out, idx, x + w - r, y + h - r, r, 0, 90, per);
            arc(out, idx, x + r, y + h - r, r, 90, 180, per);
            return out;
        }

        private static int arc(float[] out, int idx, float cx, float cy, float r,
                               float a0, float a1, int per) {
            for (int i = 0; i < per; i++) {
                float t = per == 1 ? 0f : (float) i / (per - 1);
                double a = Math.toRadians(a0 + (a1 - a0) * t);
                out[idx++] = cx + r * (float) Math.cos(a);
                out[idx++] = cy + r * (float) Math.sin(a);
            }
            return idx;
        }
    }
}
