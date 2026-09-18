package hack.echo.client.event.impl;

import hack.echo.client.event.Event;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class EventKey extends Event {
    private int key;
    private int action;
}
