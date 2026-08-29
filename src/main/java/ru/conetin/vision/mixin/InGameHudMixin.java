package ru.conetin.vision.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.conetin.vision.client.os.WindowManager;
import ru.conetin.vision.client.render.VisionCrosshair;

@Mixin(InGameHud.class)
public class InGameHudMixin {

    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private void vision$crosshair(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (!MinecraftClient.getInstance().options.getPerspective().isFirstPerson()) {
            return;
        }
        float morph = WindowManager.INSTANCE.crosshairMorph();
        if (morph <= 0.01f) {
            return;
        }
        ci.cancel();
        VisionCrosshair.render(context, morph);
    }
}
