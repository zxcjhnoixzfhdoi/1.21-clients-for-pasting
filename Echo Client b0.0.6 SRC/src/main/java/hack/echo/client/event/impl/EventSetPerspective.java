package hack.echo.client.event.impl;

import hack.echo.client.event.Event;
import lombok.Getter;
import net.minecraft.client.CameraType;

@Getter
public class EventSetPerspective extends Event {
    private final CameraType cameraType;

    public EventSetPerspective(CameraType cameraType) {
        this.cameraType = cameraType;
    }
}
