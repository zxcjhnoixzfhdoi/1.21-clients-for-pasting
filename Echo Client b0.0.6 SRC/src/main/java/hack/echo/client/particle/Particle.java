package hack.echo.client.particle;

public abstract class Particle {
    public int age;
    public final int lifetime;
    public double x, y, z;
    public int color;
    public float rotation;

    public Particle(int lifetime) {
        this.lifetime = lifetime;
    }

    public boolean isDead() {
        return age >= lifetime;
    }

    public boolean isOverlay() {
        return false;
    }

    public abstract float getSize();
    public abstract void update(float tickDelta);
}
