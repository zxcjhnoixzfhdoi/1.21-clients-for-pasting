package hack.echo.client.features.impl.misc;

import hack.echo.client.event.EventManager;
import hack.echo.client.event.EventSubscribe;
import hack.echo.client.event.impl.EventHandleInput;
import hack.echo.client.event.impl.EventOnAttackEntity;
import hack.echo.client.features.Category;
import hack.echo.client.features.Feature;
import hack.echo.client.features.FeatureInfo;
import hack.echo.client.features.settings.impl.BoolSetting;
import hack.echo.client.features.settings.impl.FloatSetting;
import hack.echo.client.features.settings.impl.KeybindSetting;
import hack.echo.client.features.settings.impl.MultiModeSetting;
import hack.echo.client.handlers.InputHandler;
import hack.echo.client.utils.combat.TargetUtils;
import hack.echo.client.utils.player.PlayerUtils;
import hack.echo.client.utils.strings.Concat;
import hack.echo.client.utils.world.WorldUtils;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.phys.EntityHitResult;
import org.jspecify.annotations.Nullable;

public class TargetControlModule extends Feature {

    public TargetControlModule() {
        super(new FeatureInfo(
                Concat.of("Targets"),
                Concat.of("Global target settings for combat modules"),
                Category.INTERNALS
        ));

        antiBotChecks.setDependency(obj -> antiBot.getValue());
        antiBotChecks.setOption(Concat.of("Name"), true);
        antiBotChecks.setOption(Concat.of("Movement"), true);
        antiBotChecks.setOption(Concat.of("Tab List"), true);
        antiBotChecks.setOption(Concat.of("Scale"), true);
        antiBotChecks.setOption(Concat.of("Bliss"), false);
        lastAttacked.setDependency(obj -> keepTarget.getValue());
        keepTarget.onChanged(() -> {
            if (!keepTarget.getValue()) {
                TargetUtils.clearTrackedTargets();
            }
        });

        EventManager.register(this);
    }

    @Override
    public void toggle() {
        
    }

    @Override
    public void setEnabled(boolean enabled) {

    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    public final MultiModeSetting entityTypes = new MultiModeSetting(
            Concat.of("Entity Types"),
            Concat.of("Players"),
            Concat.of("Hostile"),
            Concat.of("Passive"),
            Concat.of("Neutral"),
            Concat.of("Invisible"),
            Concat.of("Naked")
    );

    public final BoolSetting ignoreTeams = new BoolSetting(Concat.of("Ignore Teams"), true);
    public final BoolSetting ignoreFriends = new BoolSetting(Concat.of("Ignore Friends"), true);
    public final BoolSetting antiBot = new BoolSetting(Concat.of("AntiBot"), true);
    public final MultiModeSetting antiBotChecks = new MultiModeSetting(
            Concat.of("AntiBot Checks"),
            Concat.of("Name"),
            Concat.of("Movement"),
            Concat.of("Tab List"),
            Concat.of("Scale"),
            Concat.of("Bliss")
    );
    public final BoolSetting keepTarget = new BoolSetting(Concat.of("Keep Target"), true);
    public final BoolSetting lastAttacked = new BoolSetting(Concat.of("Last Attacked"), false);
    public final FloatSetting keepTargetRange = new FloatSetting(Concat.of("Keep Target Range"), 16.0f, 1.0f, 50.0f, 0.5f);
    public final KeybindSetting manualTargetSelect = new KeybindSetting(Concat.of("Manual Target Select"), -1);

    @Nullable
    public static LivingEntity pendingShieldBreakTarget = null;

    private boolean manualTargetSelectWasDown;

    @SuppressWarnings("unused")
    @EventSubscribe
    public void onHandleInput(EventHandleInput.Early event) {
        if (isNull()) return;
        if (hack.echo.client.api.MinecraftCompat.getScreen() != null) return;

        boolean keyDown = InputHandler.isBindDown(manualTargetSelect.getKey());
        if (!keyDown) {
            manualTargetSelectWasDown = false;
            return;
        }

        if (manualTargetSelectWasDown) return;
        manualTargetSelectWasDown = true;

        if (!keepTarget.getValue()) return;

        LivingEntity target = PlayerUtils.getManualSelectTarget();
        if (target == null) return;
        if (!TargetUtils.isTargetAllowed(target)) return;

        TargetUtils.setKeptTarget(target);
    }

    @SuppressWarnings("unused")
    @EventSubscribe
    public void onDetectShieldBreak(EventOnAttackEntity.Pre event) {
        if (isNull()) return;

        if (!(mc.hitResult instanceof EntityHitResult entityHit)) return;
        if (!(entityHit.getEntity() instanceof Player target)) return;

        if (!target.isUsingItem()) return;
        if (!(target.getUseItem().getItem() instanceof ShieldItem)) return;

        if (WorldUtils.isShieldFacingAway(target)) return;
        if (!TargetUtils.isTargetAllowed(target)) return;


        ItemStack weapon = mc.player.getMainHandItem();
        if (!(weapon.getItem() instanceof AxeItem)) return;


        pendingShieldBreakTarget = target;
    }

}
