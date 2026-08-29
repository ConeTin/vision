package ru.conetin.vision.client.os;

import net.minecraft.util.math.Vec3d;

public class Window {

    public static final Vec3d WORLD_UP = new Vec3d(0, 1, 0);

    private final WindowContent content;

    private Vec3d center;
    private Vec3d right = new Vec3d(1, 0, 0);
    private Vec3d up = WORLD_UP;

    private float physWidth;
    private float physHeight;

    private final float density;
    private final int refResWidth;
    private final int refResHeight;
    private int resWidth;
    private int resHeight;

    private float openProgress = 0f;
    private boolean closing = false;
    private boolean dead = false;
    private boolean barHovered = false;
    private float barHover = 0f;
    private boolean closeHovered = false;
    private float closeHover = 0f;
    private int cornerSide = 0;
    private boolean cornerActive = false;
    private boolean cornerHovered = false;
    private float cornerShow = 0f;
    private float cornerHover = 0f;
    private float cornerExitDelay = 0f;

    public Window(WindowContent content, int resWidth, int resHeight, float physWidth, float physHeight) {
        this.content = content;
        this.resWidth = resWidth;
        this.resHeight = resHeight;
        this.refResWidth = resWidth;
        this.refResHeight = resHeight;
        this.physWidth = physWidth;
        this.physHeight = physHeight;
        this.density = resWidth / physWidth;
        this.center = Vec3d.ZERO;
    }

    public WindowContent content() {
        return content;
    }

    public void placeFacing(Vec3d at, Vec3d viewer) {
        this.center = at;
        faceTowards(viewer);
    }

    public void faceTowards(Vec3d viewer) {
        Vec3d normal = viewer.subtract(center);
        if (normal.lengthSquared() < 1.0e-6) {
            return;
        }
        normal = normal.normalize();
        Vec3d r = WORLD_UP.crossProduct(normal);
        if (r.lengthSquared() < 1.0e-6) {
            return;
        }
        this.right = r.normalize();
        this.up = normal.crossProduct(this.right).normalize();
    }

    public void faceTowardsSmooth(Vec3d viewer, float k) {
        Vec3d tgt = viewer.subtract(center);
        if (tgt.lengthSquared() < 1.0e-6) {
            return;
        }
        tgt = tgt.normalize();
        Vec3d newNormal = normal().add(tgt.subtract(normal()).multiply(k));
        if (newNormal.lengthSquared() < 1.0e-6) {
            return;
        }
        newNormal = newNormal.normalize();
        Vec3d r = WORLD_UP.crossProduct(newNormal);
        if (r.lengthSquared() < 1.0e-6) {
            return;
        }
        this.right = r.normalize();
        this.up = newNormal.crossProduct(this.right).normalize();
    }

    public Vec3d center() {
        return center;
    }

    public void setCenter(Vec3d center) {
        this.center = center;
    }

    public Vec3d right() {
        return right;
    }

    public Vec3d up() {
        return up;
    }

    public Vec3d normal() {
        return right.crossProduct(up);
    }

    public float physWidth() {
        return physWidth;
    }

    public float physHeight() {
        return physHeight;
    }

    public int resWidth() {
        return resWidth;
    }

    public int resHeight() {
        return resHeight;
    }

    public float[] rayHitLocal(Vec3d origin, Vec3d dir) {
        Vec3d n = normal();
        double denom = dir.dotProduct(n);
        if (Math.abs(denom) < 1.0e-6) {
            return null;
        }
        double t = center.subtract(origin).dotProduct(n) / denom;
        if (t <= 0) {
            return null;
        }
        Vec3d rel = origin.add(dir.multiply(t)).subtract(center);
        double lx = rel.dotProduct(right);
        double ly = rel.dotProduct(up);
        float px = (float) ((lx / physWidth + 0.5) * resWidth);
        float py = (float) ((0.5 - ly / physHeight) * resHeight);
        return new float[]{px, py, (float) t};
    }

    public boolean inWindow(float px, float py) {
        return px >= 0 && px <= resWidth && py >= 0 && py <= resHeight;
    }

    public float barWidth() {
        return refResWidth * 0.16f;
    }

    public float barHeight() {
        return 7f;
    }

    public float barX() {
        return (resWidth - barWidth()) / 2f;
    }

    public float barY() {
        return resHeight + 10f;
    }

    public boolean isBarHit(float px, float py) {
        float pad = 16f;
        return px >= barX() - pad && px <= barX() + barWidth() + pad
                && py >= barY() - pad && py <= barY() + barHeight() + pad;
    }

    public void setBarHovered(boolean hovered) {
        this.barHovered = hovered;
    }

    public float barHover() {
        return barHover;
    }

    public float closeCx() {
        return barX() - 16f;
    }

    public float closeCy() {
        return barY() + barHeight() / 2f;
    }

    public boolean isCloseHit(float px, float py) {
        float dx = px - closeCx();
        float dy = py - closeCy();
        return dx * dx + dy * dy <= 13f * 13f;
    }

    public void setCloseHovered(boolean hovered) {
        this.closeHovered = hovered;
    }

    public float closeHover() {
        return closeHover;
    }

    public float controlsShow() {
        return Math.max(barHover, closeHover);
    }

    public int cornerSide() {
        return cornerSide;
    }

    public boolean isCornerActive() {
        return cornerActive;
    }

    public float cornerShow() {
        return cornerShow;
    }

    public float cornerHover() {
        return cornerHover;
    }

    public int cornerHit(float px, float py) {
        float reach = 46f;
        float out = 22f;
        if (py < resHeight - reach || py > resHeight + out) {
            return 0;
        }
        if (px >= -out && px <= reach) {
            return -1;
        }
        if (px >= resWidth - reach && px <= resWidth + out) {
            return 1;
        }
        return 0;
    }

    public void setCornerState(int side, boolean onHandle) {
        if (side != 0) {
            this.cornerSide = side;
            this.cornerActive = true;
        } else {
            this.cornerActive = false;
        }
        this.cornerHovered = onHandle;
    }

    public void setPhysSize(float w, float h) {
        this.physWidth = w;
        this.physHeight = h;
        int newResW = Math.round(w * density);
        int newResH = Math.round(h * density);
        if (newResW != resWidth || newResH != resHeight) {
            this.resWidth = newResW;
            this.resHeight = newResH;
            content.onResize(newResW, newResH);
        }
    }

    public double[] rayHitMeters(Vec3d origin, Vec3d dir) {
        Vec3d n = normal();
        double denom = dir.dotProduct(n);
        if (Math.abs(denom) < 1.0e-6) {
            return null;
        }
        double t = center.subtract(origin).dotProduct(n) / denom;
        if (t <= 0) {
            return null;
        }
        Vec3d rel = origin.add(dir.multiply(t)).subtract(center);
        return new double[]{rel.dotProduct(right), rel.dotProduct(up)};
    }

    public void close() {
        this.closing = true;
    }

    public boolean isDead() {
        return dead;
    }

    public float eased() {
        float p = openProgress;
        if (p <= 0f) return 0f;
        if (p >= 1f) return 1f;
        float inv = 1f - p;
        return 1f - inv * inv * inv;
    }

    public float visualScale() {
        float p = openProgress;
        if (p >= 1f) {
            return 1f;
        }
        float c1 = 1.70158f;
        float c3 = c1 + 1f;
        float t = p - 1f;
        float back = 1f + c3 * t * t * t + c1 * t * t;
        return 0.82f + 0.18f * back;
    }

    public void animate(float dt) {
        float kHover = 1f - (float) Math.exp(-16.0 * dt);
        float kIn = 1f - (float) Math.exp(-11.0 * dt);
        float kOut = 1f - (float) Math.exp(-8.0 * dt);
        barHover += ((barHovered ? 1f : 0f) - barHover) * kHover;
        closeHover += ((closeHovered ? 1f : 0f) - closeHover) * kHover;
        cornerHover += ((cornerHovered ? 1f : 0f) - cornerHover) * kHover;
        if (cornerActive) {
            cornerExitDelay = 0.15f;
            cornerShow += (1f - cornerShow) * kIn;
        } else {
            if (cornerExitDelay > 0f) {
                cornerExitDelay -= dt;
            } else {
                cornerShow += (0f - cornerShow) * kOut;
            }
            if (cornerShow < 0.01f) {
                cornerSide = 0;
            }
        }

        float open = 5.0f * dt;
        if (closing) {
            openProgress -= open * 1.5f;
            if (openProgress <= 0f) {
                openProgress = 0f;
                dead = true;
            }
        } else if (openProgress < 1f) {
            openProgress = Math.min(1f, openProgress + open);
        }
    }
}
