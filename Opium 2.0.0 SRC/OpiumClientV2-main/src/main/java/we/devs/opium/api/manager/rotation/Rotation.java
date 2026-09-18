package we.devs.opium.api.manager.rotation;

public class Rotation {
    private final int priority;
    private float yaw, pitch;
    private boolean block;

    public Rotation(int priority, float yaw, float pitch, boolean block) {
        this.priority = priority;
        this.yaw = yaw;
        this.pitch = pitch;
        this.block = block;
    }

    public int getPriority() {
        return priority;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
    }

    public float getYaw() {
        return yaw;
    }

    public float getPitch() {
        return pitch;
    }

    public boolean isBlock() {
        return block;
    }
}