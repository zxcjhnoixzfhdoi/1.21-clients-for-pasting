package hack.echo.client.features.impl.movement;

import hack.echo.client.event.EventSubscribe;
import hack.echo.client.event.impl.EventStartAttack;
import hack.echo.client.event.impl.EventMovementInput;
import hack.echo.client.features.Category;
import hack.echo.client.features.Feature;
import hack.echo.client.features.FeatureInfo;
import hack.echo.client.features.impl.movement.combohelpers.ComboMode;
import hack.echo.client.features.impl.movement.combohelpers.WTap;
import hack.echo.client.features.impl.movement.combohelpers.STap;
import hack.echo.client.features.settings.impl.BoolSetting;
import hack.echo.client.features.settings.impl.IntSetting;
import hack.echo.client.features.settings.impl.ModeSetting;
import hack.echo.client.utils.combat.TargetUtils;
import hack.echo.client.utils.strings.Concat;
import net.minecraft.world.entity.LivingEntity;

import java.util.LinkedHashMap;
import java.util.Map;

public class ComboHelper extends Feature {

    private static final CharSequence W_TAP_MODE = Concat.of("W-Tap");
    private static final CharSequence S_TAP_MODE = Concat.of("S-Tap");

    private static final ModeSetting modeSetting = new ModeSetting(
        Concat.of("Mode"),
        W_TAP_MODE,
        W_TAP_MODE,
        S_TAP_MODE
    );


    @SuppressWarnings("unused")
    private static final IntSetting wTapDurationSetting = WTap.durationSetting;
    @SuppressWarnings("unused")
    private static final IntSetting wTapReactionSetting = WTap.reactionSetting;
    @SuppressWarnings("unused")
    private static final BoolSetting wTapAllowInAirSetting = WTap.allowInAir;

    @SuppressWarnings("unused")
    private static final IntSetting sTapDurationSetting = STap.durationSetting;
    @SuppressWarnings("unused")
    private static final IntSetting sTapReactionSetting = STap.reactionSetting;
    @SuppressWarnings("unused")
    private static final BoolSetting sTapAllowInAirSetting = STap.allowInAir;

    static {

        WTap.durationSetting.setDependency(o -> modeSetting.is(W_TAP_MODE));
        WTap.reactionSetting.setDependency(o -> modeSetting.is(W_TAP_MODE));
        WTap.allowInAir.setDependency(o -> modeSetting.is(W_TAP_MODE));

        STap.durationSetting.setDependency(o -> modeSetting.is(S_TAP_MODE));
        STap.reactionSetting.setDependency(o -> modeSetting.is(S_TAP_MODE));
        STap.allowInAir.setDependency(o -> modeSetting.is(S_TAP_MODE));
    }

    private final Map<CharSequence, ComboMode> modes = new LinkedHashMap<>();

    public ComboHelper() {
        super(new FeatureInfo(
            Concat.of("Combo Helper"),
            Concat.of("Assists with combo attacks"),
            Category.MOVEMENT
        ));
        modes.put(W_TAP_MODE, new WTap());
        modes.put(S_TAP_MODE, new STap());
    }

    private ComboMode getCurrentMode() {
        return modes.get(modeSetting.getValue());
    }

    @Override
    public void onEnable() {
        super.onEnable();
        modes.values().forEach(ComboMode::onEnable);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        modes.values().forEach(ComboMode::onDisable);
    }

    @EventSubscribe
    public void onAttack(EventStartAttack e) {
        ComboMode mode = getCurrentMode();
        if (mode != null) {
            mode.onAttack(e);
        }
    }

    @EventSubscribe
    public void onMovement(EventMovementInput e) {
        ComboMode mode = getCurrentMode();
        if (mode != null) {
            mode.onMovement(e);
        }
    }

    public boolean isEntityAllowed(LivingEntity entity) {
        return TargetUtils.isTargetAllowed(entity);
    }
}
