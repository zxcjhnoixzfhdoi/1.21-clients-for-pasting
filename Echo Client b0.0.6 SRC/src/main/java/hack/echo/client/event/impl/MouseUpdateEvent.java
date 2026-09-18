package hack.echo.client.event.impl;

import hack.echo.client.event.Event;

public class MouseUpdateEvent extends Event {

    public double deltaX;
    public double deltaY;

    public MouseUpdateEvent(double deltaX, double deltaY) {
        this.deltaX = deltaX;
        this.deltaY = deltaY;
    }
}