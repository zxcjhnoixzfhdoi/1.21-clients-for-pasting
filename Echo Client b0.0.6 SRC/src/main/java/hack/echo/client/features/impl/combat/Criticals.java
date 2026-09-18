package hack.echo.client.features.impl.combat;

import hack.echo.client.event.EventSubscribe;
import hack.echo.client.event.impl.EventStartAttack;
import hack.echo.client.event.impl.EventTick;
import hack.echo.client.features.Category;
import hack.echo.client.features.Feature;
import hack.echo.client.features.FeatureInfo;
import hack.echo.client.features.settings.impl.ModeSetting;
import hack.echo.client.handlers.impl.SprintController;
import hack.echo.client.utils.combat.CombatUtils;
import hack.echo.client.utils.strings.Concat;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.entity.LivingEntity;

public class Criticals extends Feature {

        public Criticals() {
                super(new FeatureInfo(
                                Concat.of("Criticals"),
                                Concat.of("Ensures critical hits"),
                                Category.COMBAT));
        }

        private final ModeSetting mode = new ModeSetting(
                        Concat.of("Mode"),
                        Concat.of("Legit"),
                        Concat.of("Vanilla"),
                        Concat.of("Mospixel"),
                        Concat.of("Legit"));

        private boolean stoppedSprint = false;

        @Override
        public void onDisable() {
                if (stoppedSprint) {
                        SprintController.setForceStopSprint(false);
                        stoppedSprint = false;
                }
                super.onDisable();
        }

        @Override
        public CharSequence concat() {
                return mode.getValueSequence();
        }

        @EventSubscribe
        private void onTick(EventTick event) {
                if (isNull()) return;
                if (!mode.is(Concat.of("Legit"))) {
                        if (stoppedSprint) {
                                SprintController.setForceStopSprint(false);
                                stoppedSprint = false;
                        }
                        return;
                }
                boolean shouldStop = !mc.player.onGround() && mc.player.getDeltaMovement().y <= 0.0;
                if (shouldStop) {
                        SprintController.setForceStopSprint(true);
                        stoppedSprint = true;
                } else if (stoppedSprint) {
                        SprintController.setForceStopSprint(false);
                        stoppedSprint = false;
                }
        }

        @EventSubscribe
        public void onAttack(EventStartAttack event) {
                if (isNull()) return;
                if (!(event.getTarget() instanceof LivingEntity)) return;

                boolean legitCrit = CombatUtils.canCrit();
                if (legitCrit) return;

                if (mode.is(Concat.of("Vanilla"))) {
                        mc.player.connection.send(
                                        new ServerboundMovePlayerPacket.Pos(
                                                        mc.player.getX(), mc.player.getY() + 0.20, mc.player.getZ(),
                                                        false, false));
                        mc.player.connection.send(
                                        new ServerboundMovePlayerPacket.Pos(
                                                        mc.player.getX(), mc.player.getY() + 0.10, mc.player.getZ(),
                                                        false, false));
                } else if (mode.is(Concat.of("Mospixel"))) {
                        mc.player.connection.send(
                                        new ServerboundMovePlayerPacket.Pos(
                                                        mc.player.getX(), mc.player.getY() + 0.000000271875,
                                                        mc.player.getZ(), false, false));
                        mc.player.connection.send(
                                        new ServerboundMovePlayerPacket.Pos(
                                                        mc.player.getX(), mc.player.getY(), mc.player.getZ(), false,
                                                        false));
                }
        }
}
