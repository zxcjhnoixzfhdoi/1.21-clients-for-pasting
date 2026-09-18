package hack.echo.client.features.impl.movement;

import hack.echo.client.event.EventSubscribe;
import hack.echo.client.event.impl.EventPacketReceive;
import hack.echo.client.features.Category;
import hack.echo.client.features.Feature;
import hack.echo.client.features.FeatureInfo;
import hack.echo.client.features.settings.impl.BoolSetting;
import hack.echo.client.features.settings.impl.FloatSetting;
import hack.echo.client.api.EntityMotionPacketCompat;
import hack.echo.client.utils.strings.Concat;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;

// Flags on high%
public class JumpReset extends Feature {

    private final FloatSetting chance = new FloatSetting("Chance", 100, 0, 100, 1);
    private final BoolSetting liquidCheck = new BoolSetting("Liquid Check", true);
    private final BoolSetting webCheck = new BoolSetting("Web Check", true);

    public JumpReset() {
        super(new FeatureInfo(
                Concat.of("Jump Reset"),
                Concat.of("Automatically jumps on knockback to reduce incoming velocity"),
                Category.MOVEMENT));
    }

    @EventSubscribe
    public void onPacketReceive(EventPacketReceive event) {
        if (isNull())
            return;

        if (!(event.getPacket() instanceof ClientboundSetEntityMotionPacket velo))
            return;

        if (mc.player == null || EntityMotionPacketCompat.id(velo) != mc.player.getId())
            return;
        if (mc.player.hurtTime <= 0)
            return;
        if (!mc.player.onGround())
            return;
        if (!shouldJump())
            return;

        mc.player.jumpFromGround();
    }

    private boolean shouldJump() {
        if (isNull())
            return false;

        if (Math.random() * 100 > chance.getValue())
            return false;
        if (liquidCheck.getValue() && (mc.player.isInWater() || mc.player.isInLava()))
            return false;
        return !webCheck.getValue() || !mc.level.getBlockState(mc.player.blockPosition())
                .getBlock().getDescriptionId().contains("web");
    }
}
