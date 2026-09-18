package hack.echo.client.event.impl;

import hack.echo.client.event.Event;
import lombok.Getter;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvent;

@Getter
public class EventPlaySound extends Event {
    private final double x;
    private final double y;
    private final double z;
    private final SoundEvent soundEvent;
    private final SoundSource category;
    private final float volume;
    private final float pitch;

    public EventPlaySound(double x, double y, double z, SoundEvent soundEvent, SoundSource category, float volume, float pitch) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.soundEvent = soundEvent;
        this.category = category;
        this.volume = volume;
        this.pitch = pitch;
    }
}
