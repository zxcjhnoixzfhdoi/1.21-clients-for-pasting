package hack.echo.client.utils.trajectory;

import hack.echo.client.api.ChargedProjectilesCompat;
import hack.echo.client.utils.Imports;
import hack.echo.client.utils.simulation.CrossbowFireworkSimulation;
import hack.echo.client.utils.simulation.CrossbowSimulation;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class CrossbowTrajectoryUtils implements Imports {

    private CrossbowTrajectoryUtils() {
    }

    public static List<List<Vec3>> sample(Player player, ItemStack crossbowStack, float partialTick) {
        if (player == null || mc.level == null || crossbowStack == null || crossbowStack.isEmpty()) {
            return Collections.emptyList();
        }

        ChargedProjectiles chargedProjectiles = crossbowStack.get(DataComponents.CHARGED_PROJECTILES);

        List<ItemStack> loadedProjectiles = ChargedProjectilesCompat.itemCopies(chargedProjectiles);
        int projectileCount = loadedProjectiles.isEmpty() ? 1 : loadedProjectiles.size();

        float spread = 0.0F;
        var multishot = mc.level.registryAccess().get(Enchantments.MULTISHOT);
        if (multishot.isPresent()) {
            int level = EnchantmentHelper.getItemEnchantmentLevel(multishot.get(), crossbowStack);
            if (level > 0) {
                spread += level * 10.0F;
            }
        }

        float step = projectileCount == 1 ? 0.0F : 2.0F * spread / (projectileCount - 1);
        float startOffset = (projectileCount - 1) % 2 * step / 2.0F;
        float sign = 1.0F;

        List<List<Vec3>> trajectories = new ArrayList<>(projectileCount);
        for (int shotIndex = 0; shotIndex < projectileCount; shotIndex++) {
            float shotSpread = startOffset + sign * ((shotIndex + 1) / 2) * step;
            sign = -sign;

            ItemStack projectile = loadedProjectiles.isEmpty() ? ItemStack.EMPTY : loadedProjectiles.get(shotIndex);
            if (!projectile.isEmpty() && projectile.is(Items.FIREWORK_ROCKET)) {
                CrossbowFireworkSimulation simulation = new CrossbowFireworkSimulation(player, shotSpread, projectile, shotIndex, partialTick);
                trajectories.add(TrajectoryPathCollector.collect(
                    TrajectoryConstants.MAX_SIM_TICKS,
                    simulation::getPosition,
                    simulation::hasHit,
                    simulation::simulateTick
                ));
            } else {
                CrossbowSimulation simulation = new CrossbowSimulation(player, shotSpread, partialTick);
                trajectories.add(TrajectoryPathCollector.collect(
                    TrajectoryConstants.MAX_SIM_TICKS,
                    simulation::getPosition,
                    simulation::hasHit,
                    simulation::simulateTick
                ));
            }
        }

        return trajectories;
    }

    public static List<Vec3> sampleFirework(FireworkRocketEntity firework) {
        if (firework == null || mc.level == null) return Collections.emptyList();

        CrossbowFireworkSimulation simulation = new CrossbowFireworkSimulation(firework);
        return TrajectoryPathCollector.collect(
            TrajectoryConstants.MAX_SIM_TICKS,
            simulation::getPosition,
            simulation::hasHit,
            simulation::simulateTick
        );
    }
}
