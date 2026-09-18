package hack.echo.client.utils.combat.antibot;

import hack.echo.client.features.impl.misc.TargetControlModule;
import hack.echo.client.utils.strings.Concat;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class BlissAntiBotCheck implements AntiBotCheck {

    private static final int BLISS_AIR_TICKS = 2;

    private final Map<UUID, Integer> airTicks = new HashMap<>();
    private final Set<UUID> seenFalling = new HashSet<>();
    private final Set<UUID> seenGliding = new HashSet<>();

    @Override
    public CharSequence settingName() {
        return Concat.of("Bliss");
    }

    @Override
    public boolean isBot(Player player, TargetControlModule targets) {
        if (!targets.antiBotChecks.isEnabled(settingName())) {
            clearState(player);
            return false;
        }

        if (player.isFallFlying()) {
            seenGliding.add(player.getUUID());
        }

        if (seenGliding.contains(player.getUUID())) {
            clearAirState(player);
            return false;
        }

        if (!player.isAlive() || player.isRemoved()) {
            clearState(player);
            return false;
        }

        if (!hasArmor(player)) {
            clearState(player);
            return false;
        }

        if (player.onGround()) {
            clearState(player);
            return false;
        }

        UUID uuid = player.getUUID();
        if (player.fallDistance > 0.0) {
            seenFalling.add(uuid);
        }

        int currentAirTicks = airTicks.getOrDefault(uuid, 0) + 1;
        airTicks.put(uuid, currentAirTicks);

        if (currentAirTicks < BLISS_AIR_TICKS) {
            return false;
        }

        return !seenFalling.contains(uuid);
    }

    private boolean hasArmor(Player player) {
        if (!player.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) {
            return true;
        }

        if (!player.getItemBySlot(EquipmentSlot.CHEST).isEmpty()) {
            return true;
        }

        if (!player.getItemBySlot(EquipmentSlot.LEGS).isEmpty()) {
            return true;
        }

        if (!player.getItemBySlot(EquipmentSlot.FEET).isEmpty()) {
            return true;
        }

        return false;
    }

    private void clearState(Player player) {
        UUID uuid = player.getUUID();
        clearAirState(player);
        seenGliding.remove(uuid);
    }

    private void clearAirState(Player player) {
        UUID uuid = player.getUUID();
        airTicks.remove(uuid);
        seenFalling.remove(uuid);
    }
}
