package ru.conetin.vision.client.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Defines;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

final class BlurRenderer {

    private static final int START = 1;
    private static final int LEVELS = 7;

    private static final ShaderProgramKey DOWN_KEY = new ShaderProgramKey(
            Identifier.of("vision", "core/kawase_down"), VertexFormats.POSITION, Defines.EMPTY);
    private static final ShaderProgramKey UP_KEY = new ShaderProgramKey(
            Identifier.of("vision", "core/kawase_up"), VertexFormats.POSITION, Defines.EMPTY);

    private static SimpleFramebuffer[] fbs;
    private static SimpleFramebuffer worldCopy;
    private static SimpleFramebuffer scratch;
    private static int baseW = -1;
    private static int baseH = -1;

    private BlurRenderer() {
    }

    static void captureWorld(int mainFbo, int sw, int sh) {
        ensure(sw, sh);
        GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, mainFbo);
        GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, worldCopy.fbo);
        GlStateManager._glBlitFrameBuffer(
                0, 0, sw, sh, 0, 0, sw, sh, GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);
        GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, mainFbo);
    }

    static void beginScratch(int sw, int sh) {
        GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, worldCopy.fbo);
        GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, scratch.fbo);
        GlStateManager._glBlitFrameBuffer(
                0, 0, sw, sh, 0, 0, sw, sh, GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);
        GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, scratch.fbo);
        GlStateManager._viewport(0, 0, sw, sh);
    }

    static int blurScratch(int sw, int sh, int rebindTo) {
        return runPyramid(scratch.fbo, sw, sh, rebindTo);
    }

    private static int runPyramid(int srcFbo, int sw, int sh, int rebindTo) {
        MinecraftClient client = MinecraftClient.getInstance();
        ShaderProgram down = client.getShaderLoader().getOrCreateProgram(DOWN_KEY);
        ShaderProgram up = client.getShaderLoader().getOrCreateProgram(UP_KEY);

        RenderSystem.disableBlend();
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);

        GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, srcFbo);
        GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, fbs[0].fbo);
        GlStateManager._glBlitFrameBuffer(
                0, 0, sw, sh,
                0, 0, fbs[0].viewportWidth, fbs[0].viewportHeight,
                GL11.GL_COLOR_BUFFER_BIT, GL11.GL_LINEAR);

        if (down != null && up != null) {
            for (int i = 1; i < LEVELS; i++) {
                pass(down, fbs[i - 1], fbs[i]);
            }
            for (int i = LEVELS - 2; i >= 0; i--) {
                pass(up, fbs[i + 1], fbs[i]);
            }
        }

        GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, rebindTo);
        GlStateManager._viewport(0, 0, sw, sh);
        return fbs[0].getColorAttachment();
    }

    private static void pass(ShaderProgram prog, SimpleFramebuffer src, SimpleFramebuffer dst) {
        dst.beginWrite(true);
        RenderSystem.setShader(prog);
        RenderSystem.setShaderTexture(0, src.getColorAttachment());
        GlUniform hp = prog.getUniform("HalfPixel");
        if (hp != null) {
            hp.set(0.5f / src.viewportWidth, 0.5f / src.viewportHeight);
        }
        BufferBuilder bb = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION);
        bb.vertex(0f, 0f, 0f);
        bb.vertex(1f, 0f, 0f);
        bb.vertex(1f, 1f, 0f);
        bb.vertex(0f, 1f, 0f);
        BuiltBuffer built = bb.endNullable();
        if (built != null) {
            BufferRenderer.drawWithGlobalProgram(built);
        }
    }

    private static void ensure(int w, int h) {
        if (fbs != null && baseW == w && baseH == h) {
            return;
        }
        if (fbs != null) {
            for (SimpleFramebuffer fb : fbs) {
                fb.delete();
            }
            worldCopy.delete();
            scratch.delete();
        }
        fbs = new SimpleFramebuffer[LEVELS];
        int lw = Math.max(1, w / START);
        int lh = Math.max(1, h / START);
        for (int i = 0; i < LEVELS; i++) {
            SimpleFramebuffer fb = new SimpleFramebuffer(Math.max(1, lw), Math.max(1, lh), false);
            fb.setTexFilter(GL11.GL_LINEAR);
            fbs[i] = fb;
            lw /= 2;
            lh /= 2;
        }
        worldCopy = new SimpleFramebuffer(Math.max(1, w), Math.max(1, h), false);
        worldCopy.setTexFilter(GL11.GL_LINEAR);
        scratch = new SimpleFramebuffer(Math.max(1, w), Math.max(1, h), false);
        scratch.setTexFilter(GL11.GL_LINEAR);
        baseW = w;
        baseH = h;
    }
}
