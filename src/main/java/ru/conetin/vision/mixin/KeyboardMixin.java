package ru.conetin.vision.mixin;

import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.conetin.vision.client.os.WindowManager;

@Mixin(Keyboard.class)
public class KeyboardMixin {

    @Inject(method = "onKey", at = @At("HEAD"), cancellable = true)
    private void vision$onKey(long window, int key, int scancode, int action, int modifiers, CallbackInfo ci) {
        if (MinecraftClient.getInstance().currentScreen != null) {
            return;
        }
        if (WindowManager.INSTANCE.onKey(key, scancode, action, modifiers)) {
            ci.cancel();
        }
    }

    @Inject(method = "onChar", at = @At("HEAD"), cancellable = true)
    private void vision$onChar(long window, int codePoint, int modifiers, CallbackInfo ci) {
        if (MinecraftClient.getInstance().currentScreen != null) {
            return;
        }
        if (WindowManager.INSTANCE.onChar(codePoint, modifiers)) {
            ci.cancel();
        }
    }
}
