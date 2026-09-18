package hack.echo.client.render3.api;

import hack.echo.client.particle.ParticleManager;
import hack.echo.client.render2.api.CrossTexture;
import org.joml.Matrix4f;

public abstract class FramebufferTarget {


    public abstract void beginFrame();

    public abstract void endFrame();

    public abstract void cleanup();

    public abstract void box(double x, double y, double z, double sx, double sy, double sz, int color);

    public abstract void tracer(double x, double y, double z, int color);

    public abstract void line(double fromX, double fromY, double fromZ, double toX, double toY, double toZ, int color);

    public abstract void particle(double x, double y, double z, float size, int color, ParticleManager.UV uv, float rotation);

}
