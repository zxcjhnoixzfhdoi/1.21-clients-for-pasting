package hack.echo.client.event.impl;

import com.google.common.eventbus.AllowConcurrentEvents;
import hack.echo.client.event.Event;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

@AllArgsConstructor
@Getter
public class EventBlockUpdate extends Event {
    private final BlockPos pos;
    private final BlockState state;
}
