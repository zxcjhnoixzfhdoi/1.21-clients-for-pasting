package hack.echo.client.render3.api;

import hack.echo.client.Echo;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public abstract class Draw3D  {
    public Vec3 origin = Vec3.ZERO;
    public Matrix4f mvp;
    public Matrix4f view;
    public Matrix4f proj;

    public abstract void updateCamera(Matrix4f view, Matrix4f proj);

    public abstract FramebufferTarget getMinecraftTarget();

    public abstract FramebufferTarget getCustomTarget();

    public static Draw3D getInstance() {
        return Echo.draw3d;
    }

    public abstract void beginFrame();

    public abstract void endFrame();

    public abstract void cleanup();
}
