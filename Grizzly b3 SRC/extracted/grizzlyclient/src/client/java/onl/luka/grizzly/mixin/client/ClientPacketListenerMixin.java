package onl.luka.grizzly.mixin.client;

import onl.luka.grizzly.module.modules.combat.Backtrack;
import onl.luka.grizzly.module.modules.combat.JumpReset;
import onl.luka.grizzly.module.modules.combat.Misplace;
import onl.luka.grizzly.module.modules.combat.KnockbackDelay;
import onl.luka.grizzly.module.modules.combat.Velocity;
import onl.luka.grizzly.module.modules.skyblock.DanceRoomHelper;
import onl.luka.grizzly.module.modules.skyblock.DojoHelper;
import onl.luka.grizzly.module.modules.skyblock.PowderChestSolver;
import onl.luka.grizzly.module.modules.minigames.Bedwars;
import onl.luka.grizzly.module.modules.utility.anticheat.AntiCheatEngine;
import onl.luka.grizzly.module.modules.utility.anticheat.AntiCheatPacketObserver;
import onl.luka.grizzly.module.modules.world.ChestStealer;
import onl.luka.grizzly.module.modules.world.scaffold.Scaffold;
import onl.luka.grizzly.util.RotationManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {

    @Inject(method = "handleOpenScreen", at = @At("HEAD"))
    private void medved$recordContainerOpen(ClientboundOpenScreenPacket packet, CallbackInfo ci) {
        ChestStealer.onContainerOpened(packet.getContainerId(), packet.getTitle());
    }


    @Inject(method = "handleAddEntity", at = @At("HEAD"))
    private void medved$observeEntitySpawn(ClientboundAddEntityPacket packet, CallbackInfo ci) {
        AntiCheatPacketObserver.onSpawn(
                packet.getId(), packet.getX(), packet.getY(), packet.getZ(), packet.getYRot(), packet.getXRot()
        );
    }

    @Inject(method = "handleAnimate", at = @At("HEAD"))
    private void medved$observeAnimation(ClientboundAnimatePacket packet, CallbackInfo ci) {
        AntiCheatPacketObserver.onSwing(packet.getId(), packet.getAction());
    }

    @Inject(method = "handleHurtAnimation", at = @At("HEAD"))
    private void medved$observeHurtAnimation(ClientboundHurtAnimationPacket packet, CallbackInfo ci) {
        AntiCheatPacketObserver.onDamage(packet.id(), -1, -1);
    }

    @Inject(method = "handleEntityEvent", at = @At("HEAD"))
    private void medved$observeLegacyHurtEvent(ClientboundEntityEventPacket packet, CallbackInfo ci) {
        if (packet.getEventId() != 2) return;
        Minecraft observedClient = Minecraft.getInstance();
        if (observedClient.level == null) return;
        Entity entity = packet.getEntity(observedClient.level);
        if (entity instanceof Player) {
            AntiCheatPacketObserver.onDamage(entity.getId(), -1, -1);
        }
    }

    @Inject(method = "handleParticleEvent", at = @At("HEAD"))
    private void medved$onParticle(ClientboundLevelParticlesPacket packet, CallbackInfo ci) {
        PowderChestSolver.onParticle(packet);
    }

    @Inject(method = "handleSystemChat", at = @At("HEAD"))
    private void medved$onSystemChat(ClientboundSystemChatPacket packet, CallbackInfo ci) {
        PowderChestSolver.onSystemChat(packet.content().getString());
        if (!packet.overlay()) {
            Bedwars.onSystemChat(packet.content().getString());
        }
    }

    @Inject(method = "handleTakeItemEntity", at = @At("HEAD"))
    private void medved$onTakeItem(ClientboundTakeItemEntityPacket packet, CallbackInfo ci) {
        Bedwars.onTakeItem(packet);
    }

    @Inject(method = "handleSoundEvent", at = @At("HEAD"))
    private void medved$onSound(ClientboundSoundPacket packet, CallbackInfo ci) {
        DanceRoomHelper.onSound(
                packet.getSound().value().location().toString(),
                packet.getVolume(),
                packet.getPitch()
        );
    }

    @Inject(method = "handleSoundEntityEvent", at = @At("HEAD"))
    private void medved$onEntitySound(ClientboundSoundEntityPacket packet, CallbackInfo ci) {
        DanceRoomHelper.onSound(
                packet.getSound().value().location().toString(),
                packet.getVolume(),
                packet.getPitch()
        );
    }

    @Inject(method = "handleRotatePlayer", at = @At("RETURN"))
    private void medved$afterRotatePlayer(ClientboundPlayerRotationPacket packet, CallbackInfo ci) {
        if (!RotationManager.perspective && RotationManager.isActive()) {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null) {
                RotationManager.restoreClientCamera(player);
            }
        }
    }

    @Inject(method = "handleMovePlayer", at = @At("RETURN"))
    private void medved$afterMovePlayer(ClientboundPlayerPositionPacket packet, CallbackInfo ci) {
        DojoHelper.onPlayerPositioned(Minecraft.getInstance());
    }

    @Inject(method = "handlePlayerCombatKill", at = @At("HEAD"))
    private void medved$onPlayerDeath(ClientboundPlayerCombatKillPacket packet, CallbackInfo ci) {
        AntiCheatPacketObserver.onRemove(new int[]{packet.playerId()});
        if (Scaffold.INSTANCE.isEnabled() && Scaffold.INSTANCE.getDisableOnDeath().getValue()) {
            Scaffold.INSTANCE.disable();
        }
    }

    @Inject(method = "handleRespawn", at = @At("HEAD"))
    private void medved$onRespawn(ClientboundRespawnPacket packet, CallbackInfo ci) {
        AntiCheatEngine.reset();
        if (Scaffold.INSTANCE.isEnabled() && Scaffold.INSTANCE.getDisableOnWorldChange().getValue()) {
            Scaffold.INSTANCE.disable();
        }
        Backtrack.INSTANCE.clearBuffer();
        Misplace.clearTracking();
    }

    @Inject(method = "clearLevel", at = @At("HEAD"))
    private void medved$onClearLevel(CallbackInfo ci) {
        AntiCheatEngine.reset();
        Backtrack.INSTANCE.clearBuffer();
        Misplace.clearTracking();
    }

    @Inject(method = "handleTeleportEntity", at = @At("RETURN"))
    private void medved$observeEntityTeleport(ClientboundTeleportEntityPacket packet, CallbackInfo ci) {
        Minecraft observedClient = Minecraft.getInstance();
        if (observedClient.level == null) return;
        Entity entity = observedClient.level.getEntity(packet.id());
        if (entity == null) return;
        AntiCheatPacketObserver.onAbsoluteMove(
                entity.getId(), entity.getX(), entity.getY(), entity.getZ(),
                entity.getYRot(), entity.getXRot(), entity.onGround(), true
        );
        Misplace.onServerPosition(entity.getId(), entity.position());
    }

    @Inject(method = "handleRemoveEntities", at = @At("HEAD"))
    private void medved$observeRemovedEntities(ClientboundRemoveEntitiesPacket packet, CallbackInfo ci) {
        AntiCheatPacketObserver.onRemove(packet.getEntityIds().toIntArray());
    }

    @Inject(method = "handleMoveEntity", at = @At("HEAD"), cancellable = true)
    private void medved$onMoveEntity(ClientboundMoveEntityPacket packet, CallbackInfo ci) {
        Minecraft observedClient = Minecraft.getInstance();
        if (!Backtrack.INSTANCE.getFlushing() && observedClient.level != null) {
            Entity observedEntity = packet.getEntity(observedClient.level);
            if (observedEntity != null) {
                Vec3 decodedPosition = packet.hasPosition()
                        ? observedEntity.getPositionCodec().decode(packet.getXa(), packet.getYa(), packet.getZa())
                        : Vec3.ZERO;
                AntiCheatPacketObserver.onRelativeMove(
                        observedEntity.getId(),
                        decodedPosition.x,
                        decodedPosition.y,
                        decodedPosition.z,
                        packet.hasPosition(),
                        packet.hasRotation(),
                        packet.getYRot(),
                        packet.getXRot(),
                        packet.isOnGround()
                );
            }
        }
        if (observedClient.level != null && !Backtrack.INSTANCE.getFlushing()) {
            Entity moved = packet.getEntity(observedClient.level);
            if (moved != null && packet.hasPosition()) {
                Misplace.onServerPosition(
                        moved.getId(),
                        moved.getPositionCodec().decode(packet.getXa(), packet.getYa(), packet.getZa())
                );
            }
        }

        if (!Backtrack.INSTANCE.isEnabled() || Backtrack.INSTANCE.getFlushing()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        Entity entity = packet.getEntity(mc.level);
        if (entity == null || entity instanceof LocalPlayer) return;
        if (Backtrack.INSTANCE.getOnlyPlayers().getValue() && !(entity instanceof Player)) return;
        if (!Backtrack.INSTANCE.shouldDelay()) return;
        ClientPacketListener connection = (ClientPacketListener)(Object) this;
        Vec3 realPos = packet.hasPosition()
                ? entity.getPositionCodec().decode(packet.getXa(), packet.getYa(), packet.getZa())
                : entity.position();
        ci.cancel();
        Backtrack.INSTANCE.enqueue(entity.getId(), realPos, () -> connection.handleMoveEntity(packet));
    }

    @Inject(method = "handleEntityPositionSync", at = @At("HEAD"), cancellable = true)
    private void medved$onEntityPositionSync(ClientboundEntityPositionSyncPacket packet, CallbackInfo ci) {
        if (!Backtrack.INSTANCE.getFlushing()) {
            AntiCheatPacketObserver.onAbsoluteMove(
                    packet.id(),
                    packet.values().position().x,
                    packet.values().position().y,
                    packet.values().position().z,
                    packet.values().yRot(),
                    packet.values().xRot(),
                    packet.onGround(),
                    false
            );
        }
        if (!Backtrack.INSTANCE.getFlushing()) {
            Misplace.onServerPosition(packet.id(), packet.values().position());
        }

        if (!Backtrack.INSTANCE.isEnabled() || Backtrack.INSTANCE.getFlushing()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        Entity entity = mc.level.getEntity(packet.id());
        if (entity == null || entity instanceof LocalPlayer) return;
        if (Backtrack.INSTANCE.getOnlyPlayers().getValue() && !(entity instanceof Player)) return;
        if (!Backtrack.INSTANCE.shouldDelay()) return;
        ClientPacketListener connection = (ClientPacketListener)(Object) this;
        Vec3 realPos = packet.values().position();
        ci.cancel();
        Backtrack.INSTANCE.enqueue(packet.id(), realPos, () -> connection.handleEntityPositionSync(packet));
    }

    @Inject(method = "handleSetEntityMotion", at = @At("HEAD"), cancellable = true)
    private void medved$onSetEntityMotion(ClientboundSetEntityMotionPacket packet, CallbackInfo ci) {
        AntiCheatPacketObserver.onVelocity(
                packet.id(), packet.movement().x, packet.movement().y, packet.movement().z
        );
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || packet.id() != player.getId()) return;

        JumpReset.onVelocity(player, packet.movement());

        if (!Velocity.INSTANCE.isEnabled()) return;

        Vec3 motion = packet.movement();
        double mx = motion.x;
        double my = motion.y;
        double mz = motion.z;

        Velocity.Mode mode = Velocity.INSTANCE.getMode().getValue();

        if (mode == Velocity.Mode.MODIFY) {
            float factorXZ = 1f - (Velocity.INSTANCE.getModifyPercent().getValue() / 100f);
            float factorY  = 1f - (Velocity.INSTANCE.getModifyYPercent().getValue() / 100f);
            player.lerpMotion(new Vec3(mx * factorXZ, my * factorY, mz * factorXZ));
            ci.cancel();

        } else if (mode == Velocity.Mode.CANCEL) {
            ci.cancel();

        } else if (mode == Velocity.Mode.REVERSE) {
            Velocity.INSTANCE.scheduleReverse(mx, my, mz);
            //ci.cancel();

        } else if (mode == Velocity.Mode.REDUCE) {
            if (!Velocity.INSTANCE.getReceivedDamage()) return;
            if (Math.abs(mx) < 0.01 && Math.abs(mz) < 0.01) return;
            Velocity.INSTANCE.triggerReduce(mc);

        } else if (mode == Velocity.Mode.FREEZE) {
            if (Math.abs(mx) < 0.01 && Math.abs(mz) < 0.01) return;
            if (Velocity.INSTANCE.tryConsumeFreezeVelocity()) {
                ci.cancel();
            }

        } else if (mode == Velocity.Mode.DELAY) {
            if (Math.abs(mx) < 0.1 && Math.abs(mz) < 0.1) return;
            Velocity.INSTANCE.startPacketDelay();
        }

        if (KnockbackDelay.INSTANCE.isEnabled() && !KnockbackDelay.INSTANCE.isHolding()) {
            int chance = KnockbackDelay.INSTANCE.getChance().getValue();
            if (chance >= 100 || (int)(Math.random() * 100) < chance) {
                KnockbackDelay.INSTANCE.triggerDelay(KnockbackDelay.cachedOnGround);
            }
        }
    }

    @Inject(method = "handleDamageEvent", at = @At("HEAD"))
    private void medved$onDamageEvent(ClientboundDamageEventPacket packet, CallbackInfo ci) {
        AntiCheatPacketObserver.onDamage(packet.entityId(), packet.sourceCauseId(), packet.sourceDirectId());
        if (!Velocity.INSTANCE.isEnabled()) return;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || packet.entityId() != player.getId()) return;

        Velocity.Mode mode = Velocity.INSTANCE.getMode().getValue();
        if (mode == Velocity.Mode.REDUCE) {
            Velocity.INSTANCE.setReceivedDamage(true);
        } else if (mode == Velocity.Mode.FREEZE) {
            Velocity.INSTANCE.onFreezeDamage();
        }
    }

    @Inject(method = "handleBlockUpdate", at = @At("HEAD"))
    private void medved$onBlockUpdate(ClientboundBlockUpdatePacket packet, CallbackInfo ci) {
        Minecraft observedClient = Minecraft.getInstance();
        if (observedClient.level != null) {
            AntiCheatPacketObserver.onBlockChange(
                    packet.getPos(),
                    observedClient.level.getBlockState(packet.getPos()).isAir(),
                    packet.getBlockState().isAir()
            );
        }
        if (!Velocity.INSTANCE.isEnabled()) return;
        if (Velocity.INSTANCE.getMode().getValue() != Velocity.Mode.FREEZE) return;
        if (!Velocity.freezeWaitForUpdate) return;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;
        if (packet.getPos().equals(player.blockPosition())) {
            Velocity.INSTANCE.onFreezeBlockUpdate();
        }
    }

    @Inject(method = "handleChunkBlocksUpdate", at = @At("HEAD"))
    private void medved$observeSectionBlockUpdates(ClientboundSectionBlocksUpdatePacket packet, CallbackInfo ci) {
        Minecraft observedClient = Minecraft.getInstance();
        if (observedClient.level == null) return;
        packet.runUpdates((pos, state) -> {
            boolean wasAir = observedClient.level.getBlockState(pos).isAir();
            if (wasAir && !state.isAir()) {
                AntiCheatPacketObserver.onBlockChange(pos, true, false);
            }
        });
    }

    @Inject(method = "handleExplosion", at = @At("RETURN"), cancellable = true)
    private void medved$onExplode(ClientboundExplodePacket packet, CallbackInfo ci) {
        if (!Velocity.INSTANCE.isEnabled()) return;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        Optional<Vec3> knockback = packet.playerKnockback();
        if (knockback.isEmpty()) return;

        Vec3 motion = knockback.get();
        double mx = motion.x;
        double my = motion.y;
        double mz = motion.z;

        Velocity.Mode mode = Velocity.INSTANCE.getMode().getValue();

        if (mode == Velocity.Mode.MODIFY) {
            float factorXZ = 1f - (Velocity.INSTANCE.getModifyPercent().getValue() / 100f);
            float factorY  = 1f - (Velocity.INSTANCE.getModifyYPercent().getValue() / 100f);
            player.setDeltaMovement(player.getDeltaMovement().subtract(motion)
                    .add(mx * factorXZ, my * factorY, mz * factorXZ));

        } else if (mode == Velocity.Mode.CANCEL) {
            player.setDeltaMovement(player.getDeltaMovement().subtract(motion));

        } else if (mode == Velocity.Mode.REVERSE) {
            float factor = Velocity.INSTANCE.getReversePercent().getValue() / 100f;
            player.setDeltaMovement(player.getDeltaMovement().subtract(motion)
                    .add(-mx * factor, my, -mz * factor));
        }

        if (KnockbackDelay.INSTANCE.isEnabled() && !KnockbackDelay.INSTANCE.isHolding()) {
            int chance = KnockbackDelay.INSTANCE.getChance().getValue();
            if (chance >= 100 || (int)(Math.random() * 100) < chance) {
                KnockbackDelay.INSTANCE.triggerDelay(KnockbackDelay.cachedOnGround);
            }
        }
    }
}
