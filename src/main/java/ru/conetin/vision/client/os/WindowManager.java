package ru.conetin.vision.client.os;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public final class WindowManager {

    public static final WindowManager INSTANCE = new WindowManager();

    private final List<Window> windows = new ArrayList<>();

    private float crosshairMorph = 0f;
    private boolean looking = false;

    private Window dragged;
    private double dragDistance;
    private double grabLx;
    private double grabLy;
    private Window barWindow;
    private Window closeWindow;
    private Window cornerWindow;
    private Window resizing;
    private long lastNanos = 0L;

    private Window hoverWindow;
    private float hoverPx;
    private float hoverPy;
    private Window focused;
    private Window pressTarget;

    private WindowManager() {
    }

    public List<Window> windows() {
        return windows;
    }

    public float crosshairMorph() {
        return crosshairMorph;
    }

    public Window hoveredWindow() {
        return hoverWindow;
    }

    public float hoverX() {
        return hoverPx;
    }

    public float hoverY() {
        return hoverPy;
    }

    public Window focusedWindow() {
        return focused;
    }

    public void open(Window window, float distance) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player != null) {
            Vec3d eye = player.getEyePos();
            Vec3d look = player.getRotationVector();
            Vec3d horiz = new Vec3d(look.x, 0, look.z);
            if (horiz.lengthSquared() < 1.0e-6) {
                horiz = new Vec3d(0, 0, 1);
            }
            horiz = horiz.normalize();
            Vec3d at = eye.add(horiz.multiply(distance));
            at = new Vec3d(at.x, eye.y, at.z);
            window.placeFacing(at, eye);
        }
        windows.add(window);
    }

    public void tick(MinecraftClient client) {
        windows.forEach(w -> w.content().tick());
        windows.removeIf(w -> {
            if (w.isDead()) {
                w.content().onClosed();
                return true;
            }
            return false;
        });

        if (dragged != null && dragged.isDead()) {
            dragged = null;
        }
        if (resizing != null && resizing.isDead()) {
            resizing = null;
        }
        if (focused != null && focused.isDead()) {
            focused = null;
        }
        if (pressTarget != null && pressTarget.isDead()) {
            pressTarget = null;
        }

        boolean look = false;
        barWindow = null;
        closeWindow = null;
        cornerWindow = null;
        Window hoverW = null;
        float hpx = 0f;
        float hpy = 0f;
        ClientPlayerEntity player = client.player;
        if (player != null && !windows.isEmpty()) {
            Vec3d origin = player.getEyePos();
            Vec3d dir = player.getRotationVector();

            for (Window w : windows) {
                float[] h = w.rayHitLocal(origin, dir);
                boolean draggingThis = dragged == w;
                int corner = (h != null && !draggingThis) ? w.cornerHit(h[0], h[1]) : 0;
                boolean resizingThis = resizing == w;
                int side = resizingThis ? w.cornerSide() : corner;
                w.setCornerState(side, corner != 0 || resizingThis);
                boolean cornerMode = side != 0;

                boolean inClose = !cornerMode && h != null && w.isCloseHit(h[0], h[1]);
                boolean inBar = !cornerMode && !inClose && h != null && w.isBarHit(h[0], h[1]);
                boolean inWin = h != null && w.inWindow(h[0], h[1]);
                w.setCloseHovered(inClose);
                w.setBarHovered(inBar || dragged == w);
                if (inWin || inBar || inClose || corner != 0) {
                    look = true;
                }
                if (inBar && barWindow == null) {
                    barWindow = w;
                }
                if (inClose && closeWindow == null) {
                    closeWindow = w;
                }
                if (corner != 0 && cornerWindow == null) {
                    cornerWindow = w;
                }
                if (inWin && !cornerMode && hoverW == null) {
                    hoverW = w;
                    hpx = h[0];
                    hpy = h[1];
                }
            }
        } else {
            windows.forEach(w -> {
                w.setBarHovered(false);
                w.setCloseHovered(false);
                w.setCornerState(0, false);
            });
        }
        if (dragged != null || resizing != null) {
            look = true;
        }
        looking = look;

        hoverWindow = hoverW;
        hoverPx = hpx;
        hoverPy = hpy;
        if (hoverW != null) {
            hoverW.content().mouseMoved(hpx, hpy);
        }
        float target = look ? 1f : 0f;
        crosshairMorph += (target - crosshairMorph) * 0.25f;
    }

    public boolean onMouseButton(int button, boolean pressed) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.currentScreen != null) {
            return false;
        }
        if (button != 0) {
            return looking;
        }
        if (pressed) {
            if (cornerWindow != null) {
                beginResize(cornerWindow);
                return true;
            }
            if (closeWindow != null) {
                closeWindow.close();
                return true;
            }
            if (barWindow != null) {
                beginDrag(barWindow);
                return true;
            }
            if (hoverWindow != null) {
                pressTarget = hoverWindow;
                hoverWindow.content().mousePressed(hoverPx, hoverPy, button);
                setFocused(hoverWindow.content().wantsKeyboard() ? hoverWindow : null);
                return true;
            }
            setFocused(null);
            return looking;
        }
        if (dragged != null) {
            dragged = null;
            return true;
        }
        if (resizing != null) {
            resizing = null;
            return true;
        }
        if (pressTarget != null) {
            pressTarget.content().mouseReleased(hoverPx, hoverPy, button);
            pressTarget = null;
            return true;
        }
        return looking;
    }

    private void setFocused(Window w) {
        if (focused == w) {
            return;
        }
        if (focused != null) {
            focused.content().onFocus(false);
        }
        focused = w;
        if (focused != null) {
            focused.content().onFocus(true);
        }
    }

    public boolean onKey(int key, int scancode, int action, int mods) {
        if (focused == null) {
            return false;
        }
        if (key == GLFW.GLFW_KEY_ESCAPE && action == GLFW.GLFW_PRESS) {
            setFocused(null);
            return true;
        }
        if (action == GLFW.GLFW_RELEASE) {
            focused.content().keyReleased(key, scancode, mods);
        } else {
            focused.content().keyPressed(key, scancode, mods);
        }
        return true;
    }

    public boolean onChar(int codePoint, int mods) {
        if (focused == null) {
            return false;
        }
        focused.content().charTyped((char) codePoint, mods);
        return true;
    }

    public void updateDrag(Vec3d origin, Vec3d dir) {
        if (dragged == null && resizing == null) {
            return;
        }
        double dt = frameDt();

        if (dragged != null) {
            float kPos = (float) (1.0 - Math.exp(-13.0 * dt));
            float kRot = (float) (1.0 - Math.exp(-11.0 * dt));
            dragged.faceTowardsSmooth(origin, kRot);
            Vec3d rayPoint = origin.add(dir.multiply(dragDistance));
            Vec3d offset = dragged.right().multiply(grabLx).add(dragged.up().multiply(grabLy));
            Vec3d targetCenter = rayPoint.subtract(offset);
            Vec3d cur = dragged.center();
            dragged.setCenter(cur.add(targetCenter.subtract(cur).multiply(kPos)));
        }

        if (resizing != null) {
            double[] mm = resizing.rayHitMeters(origin, dir);
            if (mm != null) {
                float k = (float) (1.0 - Math.exp(-16.0 * dt));
                float tw = (float) Math.min(6.0, Math.max(0.8, 2.0 * Math.abs(mm[0])));
                float th = (float) Math.min(4.0, Math.max(0.5, 2.0 * Math.abs(mm[1])));
                float nw = resizing.physWidth() + (tw - resizing.physWidth()) * k;
                float nh = resizing.physHeight() + (th - resizing.physHeight()) * k;
                resizing.setPhysSize(nw, nh);
            }
        }
    }

    private double frameDt() {
        long now = System.nanoTime();
        double dt = lastNanos == 0L ? 1.0 / 60.0 : (now - lastNanos) / 1.0e9;
        lastNanos = now;
        if (dt > 0.1 || dt < 0.0) {
            dt = 1.0 / 60.0;
        }
        return dt;
    }

    public boolean onScroll(double vertical) {
        if (dragged != null) {
            dragDistance += vertical * 0.35;
            if (dragDistance < 1.6) {
                dragDistance = 1.6;
            } else if (dragDistance > 10.0) {
                dragDistance = 10.0;
            }
            return true;
        }
        if (hoverWindow != null && hoverWindow.content().mouseScrolled(hoverPx, hoverPy, vertical)) {
            return true;
        }
        return false;
    }

    private void beginResize(Window w) {
        resizing = w;
        lastNanos = 0L;
    }

    private void beginDrag(Window w) {
        ClientPlayerEntity p = MinecraftClient.getInstance().player;
        if (p == null) {
            return;
        }
        Vec3d origin = p.getEyePos();
        Vec3d dir = p.getRotationVector();
        float[] h = w.rayHitLocal(origin, dir);
        dragged = w;
        dragDistance = h != null ? h[2] : origin.distanceTo(w.center());
        lastNanos = 0L;
        Vec3d rel = origin.add(dir.multiply(dragDistance)).subtract(w.center());
        grabLx = rel.dotProduct(w.right());
        grabLy = rel.dotProduct(w.up());
    }
}
