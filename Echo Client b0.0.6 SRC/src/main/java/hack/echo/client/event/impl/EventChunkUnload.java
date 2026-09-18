package hack.echo.client.event.impl;

import hack.echo.client.event.Event;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.world.level.ChunkPos;

@AllArgsConstructor
@Getter
public class EventChunkUnload extends Event {
    private final ChunkPos chunkPos;
}
