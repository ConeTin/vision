package ru.conetin.vision.client.os.apps;

import com.cinemamod.mcef.MCEF;
import com.cinemamod.mcef.MCEFBrowser;
import net.minecraft.text.Text;
import ru.conetin.vision.client.os.WindowContent;
import ru.conetin.vision.client.ui.UiCanvas;

public class WebContent implements WindowContent {

    private final String initialUrl;
    private int width;
    private int height;

    private MCEFBrowser browser;

    public WebContent(String url, int width, int height) {
        this.initialUrl = url;
        this.width = Math.max(1, width);
        this.height = Math.max(1, height);
    }

    public void ensureBrowser() {
        if (browser != null || !MCEF.isInitialized()) {
            return;
        }
        browser = MCEF.createBrowser(initialUrl, true, width, height);
        browser.setCursorChangeListener(cursorType -> {});
    }

    public int textureId() {
        return browser != null ? browser.getRenderer().getTextureID() : -1;
    }

    @Override
    public String title() {
        return initialUrl;
    }

    @Override
    public void render(UiCanvas g) {
        if (browser == null) {
            String key = MCEF.isInitialized() ? "vision.web.loading" : "vision.web.starting";
            g.textCentered(Text.translatable(key).getString(),
                    g.width() / 2f, g.height() / 2f - g.fontHeight() / 2f, 0xFFFFFFFF);
        }
    }

    @Override
    public void mouseMoved(float x, float y) {
        if (browser != null) {
            browser.sendMouseMove((int) x, (int) y);
        }
    }

    @Override
    public boolean mousePressed(float x, float y, int button) {
        if (browser != null) {
            browser.sendMousePress((int) x, (int) y, button);
            browser.setFocus(true);
        }
        return true;
    }

    @Override
    public boolean mouseReleased(float x, float y, int button) {
        if (browser != null) {
            browser.sendMouseRelease((int) x, (int) y, button);
            browser.setFocus(true);
        }
        return true;
    }

    @Override
    public boolean mouseScrolled(float x, float y, double amount) {
        if (browser != null) {
            browser.sendMouseWheel((int) x, (int) y, amount, 0);
        }
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (browser != null) {
            browser.sendKeyPress(keyCode, scanCode, modifiers);
            browser.setFocus(true);
        }
        return true;
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (browser != null) {
            browser.sendKeyRelease(keyCode, scanCode, modifiers);
            browser.setFocus(true);
        }
        return true;
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (chr == (char) 0) {
            return false;
        }
        if (browser != null) {
            browser.sendKeyTyped(chr, modifiers);
            browser.setFocus(true);
        }
        return true;
    }

    @Override
    public boolean wantsKeyboard() {
        return browser != null;
    }

    @Override
    public void onFocus(boolean focused) {
        if (browser != null) {
            browser.setFocus(focused);
        }
    }

    @Override
    public void onResize(int w, int h) {
        this.width = Math.max(1, w);
        this.height = Math.max(1, h);
        if (browser != null) {
            browser.resize(this.width, this.height);
        }
    }

    @Override
    public void onClosed() {
        if (browser != null) {
            browser.close();
            browser = null;
        }
    }
}
