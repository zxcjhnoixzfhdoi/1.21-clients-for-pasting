package hack.echo.client.features.impl.movement;

import java.util.Optional;

import hack.echo.client.event.EventSubscribe;
import hack.echo.client.event.impl.EventPacketReceive;
import hack.echo.client.features.Category;
import hack.echo.client.features.Feature;
import hack.echo.client.features.FeatureInfo;
import hack.echo.client.features.settings.impl.FloatSetting;
import hack.echo.client.features.settings.impl.ModeSetting;
import hack.echo.client.features.settings.impl.MultiModeSetting;
import hack.echo.client.api.EntityMotionPacketCompat;
import hack.echo.client.utils.player.PlayerUtils;
import hack.echo.client.utils.strings.Concat;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.world.phys.Vec3;

public class VelocityModule extends Feature {

	public VelocityModule() {
		super(new FeatureInfo(
			Concat.of("Velocity"),
			Concat.of("Modifies knockback received"),
			Category.MOVEMENT
		));
	}
    private final ModeSetting mode = new ModeSetting(
            Concat.of("Mode"),
            Concat.of("Cancel"),
            Concat.of("Cancel"),
            Concat.of("MinemenClub")
    );
    private final FloatSetting amount = new FloatSetting(
            Concat.of("Amount"),
            0.0f,
            0.0f,
            1.0f,
            0.01f,
            p -> mode.is(Concat.of("Cancel"))
    );
    private final MultiModeSetting sources = new MultiModeSetting(
            Concat.of("Sources"),
            Concat.of("Hits"),
            Concat.of("Explosions")
    );
    private boolean justProcessedExplosion = false;

    @EventSubscribe
    public void onVelocity(EventPacketReceive event) {
        if (isNull()) return;
        if (event.getPacket() instanceof ClientboundSetEntityMotionPacket velocityPacket) {
            if (mc.player == null || EntityMotionPacketCompat.id(velocityPacket) != mc.player.getId()) return;

            boolean isExplosionRelated = justProcessedExplosion && sources.isEnabled(Concat.of("Explosions"));
            justProcessedExplosion = false;
            
            if (isExplosionRelated && !sources.isEnabled(Concat.of("Hits"))) {
                if (mode.is(Concat.of("Cancel"))) {
                    event.cancel();
                }
                return;
            }
            
            if (!sources.isEnabled(Concat.of("Hits"))) {
                return;
            }

            if (mode.is(Concat.of("Cancel"))) {
                if (amount.getValue() == 1.0f) return;
                float amountValue = amount.getValue();
                if (amountValue == 0.0f) {
                    event.cancel();
                } else {
                    Vec3 packetVelocity = EntityMotionPacketCompat.movement(velocityPacket);
                    event.cancel();
                    mc.player.setDeltaMovement(
                        packetVelocity.x * amountValue,
                        packetVelocity.y * amountValue,
                        packetVelocity.z * amountValue
                    );
                }
            } else if (mode.is(Concat.of("MinemenClub"))) {
                if (mc.player.hurtTime == 7) {
                    PlayerUtils.setMotionX(mc.player.getDeltaMovement().x() * 0.25);
                    PlayerUtils.setMotionY(-0.0535f);
                    PlayerUtils.setMotionZ(mc.player.getDeltaMovement().z() * 0.25);
                }
            }
        }
    }

    @EventSubscribe
    public void onExplosionVelocity(EventPacketReceive event) {
        if (isNull()) return;
        if (event.getPacket() instanceof ClientboundExplodePacket explodePacket) {
            if (mc.player == null) return;

            if (!sources.isEnabled(Concat.of("Explosions"))) {
                return;
            }

            if (!mode.is(Concat.of("Cancel"))) return;
            if (amount.getValue() == 1.0f) return;
            Optional<Vec3> knockbackOpt = explodePacket.playerKnockback();
            if (knockbackOpt.isEmpty()) return;
            
            float amountValue = amount.getValue();
            event.cancel();
            if (amountValue == 0.0f) {
                justProcessedExplosion = true;
                return;
            }
            Vec3 explosionPacketVelocity = knockbackOpt.get();
            Vec3 modifiedKnockback = new Vec3(
                explosionPacketVelocity.x * amountValue,
                explosionPacketVelocity.y * amountValue,
                explosionPacketVelocity.z * amountValue
            );
            mc.player.addDeltaMovement(modifiedKnockback);
            justProcessedExplosion = true;
        }
        
    }

    @Override
    public void onDisable() {
        super.onDisable();
        justProcessedExplosion = false;
    }

    @Override
    public void onEnable() {
        super.onEnable();
        justProcessedExplosion = false;
    }
}
