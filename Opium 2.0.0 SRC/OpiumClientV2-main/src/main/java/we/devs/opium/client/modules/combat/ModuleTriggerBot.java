package we.devs.opium.client.modules.combat;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Hand;
import we.devs.opium.api.manager.module.Module;
import we.devs.opium.api.manager.module.RegisterModule;
import we.devs.opium.client.values.impl.ValueBoolean;

@RegisterModule(name = "Triggerbot", description = "Automatically attacks when an entity is in range.", category = Module.Category.COMBAT)
public class ModuleTriggerBot extends Module {
    ValueBoolean swing = new ValueBoolean("Swing Hand", "Swing Hand", "Swings You Hand On Attack", true);
    private final MinecraftClient client = MinecraftClient.getInstance();


    @Override
    public void onEnable() {
        super.onEnable();
        if (client.player == null || client.world == null) {
            this.disable(false);
            return;
        }
    }
    @Override
    public void onDisable() {
        super.onDisable();
    }

    @Override
    public void onTick() {
        super.onTick();
        if (client.player == null || client.world == null) return;
        // Check if enough time has passed since the last attack
        if (delayCheck()) triggerAction();
    }

    private void triggerAction() {
        if (client.targetedEntity != null) {
            assert mc.interactionManager != null;
            mc.interactionManager.attackEntity(mc.player, client.targetedEntity);
            if (swing.getValue()) {
                assert mc.player != null;
                mc.player.swingHand(Hand.MAIN_HAND);
            }
        }
    }

    private boolean delayCheck() {
        float delay = 0.5f;
        assert mc.player != null;
        return mc.player.getAttackCooldownProgress(delay) >= 1;
    }
}