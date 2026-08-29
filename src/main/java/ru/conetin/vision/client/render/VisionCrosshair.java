package ru.conetin.vision.client.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Defines;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;

public final class VisionCrosshair {

    private static final ShaderProgramKey KEY = new ShaderProgramKey(
            Identifier.of("vision", "core/crosshair"), VertexFormats.POSITION_TEXTURE, Defines.EMPTY);
    private static final float SIZE = 17f;

    private VisionCrosshair() {
    }

    public static void render(DrawContext context, float morph) {
        ShaderProgram prog = MinecraftClient.getInstance().getShaderLoader().getOrCreateProgram(KEY);
        if (prog == null) {
            return;
        }
        float cx = context.getScaledWindowWidth() / 2f - 0.5f;
        float cy = context.getScaledWindowHeight() / 2f;
        float h = SIZE / 2f;
        Matrix4f mat = context.getMatrices().peek().getPositionMatrix();

        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(
                GlStateManager.SrcFactor.ONE_MINUS_DST_COLOR, GlStateManager.DstFactor.ONE_MINUS_SRC_COLOR,
                GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ZERO);
        RenderSystem.setShader(prog);
        GlUniform mu = prog.getUniform("Morph");
        if (mu != null) {
            mu.set(morph);
        }
        GlUniform qu = prog.getUniform("QuadSize");
        if (qu != null) {
            qu.set(SIZE);
        }

        BufferBuilder bb = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
        bb.vertex(mat, cx - h, cy - h, 0f).texture(0f, 0f);
        bb.vertex(mat, cx - h, cy + h, 0f).texture(0f, 1f);
        bb.vertex(mat, cx + h, cy + h, 0f).texture(1f, 1f);
        bb.vertex(mat, cx + h, cy - h, 0f).texture(1f, 0f);
        BuiltBuffer built = bb.endNullable();
        if (built != null) {
            BufferRenderer.drawWithGlobalProgram(built);
        }
        RenderSystem.defaultBlendFunc();
    }
}
