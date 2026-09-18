package hack.echo.client.event.impl;

import hack.echo.client.event.Event;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.world.entity.MoverType;

@Getter
@Setter
public class EventMovePos extends Event {
    private MoverType moverType;
    private double x, y, z;

    public EventMovePos(MoverType moverType, double x, double y, double z) {
        super();
        this.moverType = moverType;
        this.x = x;
        this.y = y;
        this.z = z;
    }
}
