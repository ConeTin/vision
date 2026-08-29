package ru.conetin.vision.client.ui;

public interface UiCanvas {

    int width();

    int height();

    float hoverX();

    float hoverY();

    boolean isHovered();

    boolean isFocused();

    float alpha();

    void fillRect(float x, float y, float w, float h, int argb);

    void roundedRect(float x, float y, float w, float h, float radius, int argb);

    void roundedRectOutline(float x, float y, float w, float h, float radius, float thickness, int argb);

    void gradientRect(float x, float y, float w, float h, int argbTop, int argbBottom);

    void text(String text, float x, float y, int argb);

    void textCentered(String text, float cx, float y, int argb);

    int textWidth(String text);

    int fontHeight();

    default boolean hovering(float x, float y, float w, float h) {
        if (!isHovered()) return false;
        float hx = hoverX();
        float hy = hoverY();
        return hx >= x && hx <= x + w && hy >= y && hy <= y + h;
    }
}
