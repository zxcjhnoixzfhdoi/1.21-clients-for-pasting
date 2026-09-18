package hack.echo.client.event.impl;

import hack.echo.client.render3.api.Draw3D;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Matrix4f;

import hack.echo.client.event.Event;

@Getter
@AllArgsConstructor
public class EventRender3D extends Event {
    private final PoseStack matrices;
    private final float tickDelta;
    private final float deltaTicks;
    private final Camera camera;
    private final GameRenderer gameRenderer;
    private final Matrix4f projectionMatrix;
    private final Matrix4f modelViewMatrix;
    private final LevelRenderer worldRenderer;
    private final Draw3D draw3D;
}
