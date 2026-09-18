package we.devs.opium.client.events;

import we.devs.opium.api.manager.event.EventArgument;
import we.devs.opium.api.manager.event.EventListener;
import net.minecraft.client.util.math.MatrixStack;

public class EventRender3D extends EventArgument {
    private final float tick;
    private final MatrixStack matrices;

    public EventRender3D(float tick, MatrixStack matrices) {
        this.tick = tick;
        this.matrices = matrices;
    }

    public float getTick() {
        return this.tick;
    }

    public MatrixStack getMatrices() {
        return this.matrices;
    }

    @Override
    public void call(EventListener listener) {
        listener.onRender3D(this);
    }
}
