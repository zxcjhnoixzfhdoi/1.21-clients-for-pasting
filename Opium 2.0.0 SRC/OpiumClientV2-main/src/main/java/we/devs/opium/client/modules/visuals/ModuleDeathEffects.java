package we.devs.opium.client.modules.visuals;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundEvents;
import we.devs.opium.client.events.DeathEvent;
import we.devs.opium.api.manager.module.Module;
import we.devs.opium.api.manager.module.RegisterModule;
import we.devs.opium.client.values.impl.ValueBoolean;

@RegisterModule(name = "DeathEffects", description = "Makes an effect when someone dies.", category = Module.Category.VISUALS)
public class ModuleDeathEffects extends Module {
    ValueBoolean sound = new ValueBoolean("Sound", "Sound", "Plays a thunder sound when a player dies.", false);

    @Override
    public void onDeath(DeathEvent event) {
        if (event.getEntity() instanceof PlayerEntity player) {
            // Spawn a lightning bolt at the player's location
            LightningEntity bolt = new LightningEntity(EntityType.LIGHTNING_BOLT, mc.world);
            bolt.updatePositionAndAngles(player.getX(), player.getY(), player.getZ(), player.getYaw(), player.getPitch());
            mc.world.addEntity(bolt);

            // Play a thunder sound if the sound option is enabled
            if (this.sound.getValue()) {
                mc.player.playSound(SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 1.0f);
            }
        }
    }
}
