package ru.conetin.vision.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import ru.conetin.vision.client.os.Window;
import ru.conetin.vision.client.os.WindowManager;
import ru.conetin.vision.client.os.apps.DemoApp;
import ru.conetin.vision.client.os.apps.WebContent;
import ru.conetin.vision.client.render.WindowRenderer;

public class VisionClient implements ClientModInitializer {

    private static final float SPAWN_DISTANCE = 2.5f;

    private static final String DEFAULT_URL = "https://www.google.com";

    private static KeyBinding openWindowKey;
    private static KeyBinding openWebKey;

    @Override
    public void onInitializeClient() {
        openWindowKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.vision.open_window",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_V,
                "key.categories.vision"
        ));
        openWebKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.vision.open_web",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_B,
                "key.categories.vision"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            WindowManager.INSTANCE.tick(client);
            while (openWindowKey.wasPressed()) {
                openDemoWindow(client);
            }
            while (openWebKey.wasPressed()) {
                openWebWindow(client);
            }
        });

        WorldRenderEvents.AFTER_TRANSLUCENT.register(WindowRenderer::render);
    }

    private void openDemoWindow(MinecraftClient client) {
        if (client.player == null) {
            return;
        }
        Window window = new Window(new DemoApp(), 480, 320, 3.0f, 2.0f);
        WindowManager.INSTANCE.open(window, SPAWN_DISTANCE);
    }

    private void openWebWindow(MinecraftClient client) {
        if (client.player == null) {
            return;
        }
        int resW = 1280;
        int resH = 800;
        Window window = new Window(new WebContent(DEFAULT_URL, resW, resH), resW, resH, 3.2f, 2.0f);
        WindowManager.INSTANCE.open(window, SPAWN_DISTANCE);
    }
}
