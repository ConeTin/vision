package ru.conetin.vision.client.os;

import ru.conetin.vision.client.ui.UiCanvas;

public interface WindowContent {

    String title();

    void render(UiCanvas g);

    default boolean mouseClicked(float x, float y, int button) {
        return false;
    }

    default void mouseMoved(float x, float y) {
    }

    default boolean mousePressed(float x, float y, int button) {
        return mouseClicked(x, y, button);
    }

    default boolean mouseReleased(float x, float y, int button) {
        return false;
    }

    default boolean mouseScrolled(float x, float y, double amount) {
        return false;
    }

    default boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return false;
    }

    default boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        return false;
    }

    default boolean charTyped(char chr, int modifiers) {
        return false;
    }

    default boolean wantsKeyboard() {
        return false;
    }

    default void onFocus(boolean focused) {
    }

    default void onClosed() {
    }

    default void onResize(int w, int h) {
    }

    default void tick() {
    }
}
