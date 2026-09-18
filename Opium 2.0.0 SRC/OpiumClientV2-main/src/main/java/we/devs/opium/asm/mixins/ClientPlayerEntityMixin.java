package we.devs.opium.asm.mixins;

import com.mojang.authlib.GameProfile;
import net.minecraft.entity.MovementType;
import org.spongepowered.asm.mixin.injection.Redirect;
import we.devs.opium.Opium;
import we.devs.opium.api.utilities.IMinecraft;
import we.devs.opium.client.events.EventMotion;
import we.devs.opium.client.events.EventPush;
import we.devs.opium.client.events.EventTick;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import we.devs.opium.client.events.PlayerMoveEvent;
import we.devs.opium.client.modules.movement.ModuleNoSlow;

@Mixin(ClientPlayerEntity.class)
public abstract class ClientPlayerEntityMixin extends AbstractClientPlayerEntity implements IMinecraft {
    @Shadow
    private double lastX;
    @Shadow
    private double lastBaseY;
    @Shadow
    private double lastZ;
    @Shadow
    private float lastYaw;
    @Shadow
    private float lastPitch;
    @Shadow
    private boolean lastOnGround;
    @Shadow
    protected abstract void sendSprintingPacket();
    @Shadow
    public abstract boolean isSneaking();
    @Shadow
    private boolean lastSneaking;
    @Shadow @Final
    public ClientPlayNetworkHandler networkHandler;
    @Shadow
    protected abstract boolean isCamera();
    @Shadow
    private int ticksSinceLastPositionPacketSent;
    @Shadow
    private boolean autoJumpEnabled;

    @Shadow
    protected void autoJump(float dx, float dz) {}

    public ClientPlayerEntityMixin(ClientWorld world, GameProfile profile) {
        super(world, profile);
    }

    @Inject(method = "move", at = @At("HEAD"), cancellable = true)
    private void move(MovementType type, Vec3d movement, CallbackInfo info) {
        PlayerMoveEvent event = new PlayerMoveEvent(type, movement);
        Opium.EVENT_MANAGER.call(event);
        if (event.isCanceled()) {
            info.cancel();
        } else if (!type.equals(event.getType()) || !movement.equals(event.getMovement())) {
            double double_1 = this.getX();
            double double_2 = this.getZ();
            super.move(event.getType(), event.getMovement());
            this.autoJump((float) (this.getX() - double_1), (float) (this.getZ() - double_2));
            info.cancel();
        }
    }

    @Inject(method = "sendMovementPackets", at = @At("HEAD"), cancellable = true)
    public void injectSendMovementPacketsPre(CallbackInfo ci) {
        EventTick event = new EventTick();
        Opium.EVENT_MANAGER.call(event);
        if (event.isCanceled()) {
            ci.cancel();
        }
    }

    @Inject(method = "sendMovementPackets", at = @At(value = "HEAD"), cancellable = true)
    private void onSendMovementPackets(CallbackInfo ci) {
        //thanks to mojang
        EventMotion event = new EventMotion(mc.player.getX(), mc.player.getY(), mc.player.getZ(), mc.player.getYaw(), mc.player.getPitch(), mc.player.isOnGround());
        Opium.EVENT_MANAGER.call(event);
        double x = event.getX();
        double y = event.getY();
        double z = event.getZ();
        float yaw = event.getRotationYaw();
        float pitch = event.getRotationPitch();
        boolean onGround = event.isOnGround();
        if (event.isCanceled()) {
            ci.cancel();
            sendSprintingPacket();
            boolean sneak = isSneaking();
            if (sneak != lastSneaking) {
                ClientCommandC2SPacket.Mode packet = sneak ? ClientCommandC2SPacket.Mode.PRESS_SHIFT_KEY : ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY;
                networkHandler.sendPacket(new ClientCommandC2SPacket(this, packet));
                lastSneaking = sneak;
            }
            if (isCamera()) {
                double d = x - lastX;
                double e = y - lastBaseY;
                double f = z - lastZ;
                double g = yaw - lastYaw;
                double h = pitch - lastPitch;
                ++ticksSinceLastPositionPacketSent;
                boolean bl2 = MathHelper.squaredMagnitude(d, e, f) > MathHelper.square(2.0E-4) || ticksSinceLastPositionPacketSent >= 20;
                boolean bl3 = g != 0.0 || h != 0.0;
                if (hasVehicle()) {
                    Vec3d vec3d = getVelocity();
                    networkHandler.sendPacket(new PlayerMoveC2SPacket.Full(vec3d.x, -999.0, vec3d.z, getYaw(), getPitch(), onGround));
                    bl2 = false;
                } else if (bl2 && bl3) {
                    networkHandler.sendPacket(new PlayerMoveC2SPacket.Full(x, y, z, yaw, pitch, onGround));
                } else if (bl2) {
                    networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y, z, onGround));
                } else if (bl3) {
                    networkHandler.sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(yaw, pitch, onGround));
                } else if (lastOnGround != isOnGround()) {
                    networkHandler.sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(onGround));
                }
                if (bl2) {
                    lastX = x;
                    lastBaseY = y;
                    lastZ = z;
                    ticksSinceLastPositionPacketSent = 0;
                }
                if (bl3) {
                    lastYaw = yaw;
                    lastPitch = pitch;
                }
                lastOnGround = onGround;
                autoJumpEnabled = mc.options.getAutoJump().getValue();
            }
        }
    }

    @Inject(method = "pushOutOfBlocks", at = @At("HEAD"), cancellable = true)
    private void onPushOutOfBlocks(double x, double d, CallbackInfo ci) {
        EventPush event = new EventPush();
        Opium.EVENT_MANAGER.call(event);
        if (event.isCanceled()) {
            ci.cancel();
        }
    }

    @Redirect(method = "tickMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;isUsingItem()Z"), require = 0)
    private boolean onTickMovement(ClientPlayerEntity player) {
        if (ModuleNoSlow.INSTANCE.getItems()) {
            return false;
        }

        return player.isUsingItem();
    }
}