package hack.echo.client.utils.combat;

import hack.echo.client.Echo;
import hack.echo.client.features.impl.misc.TargetControlModule;
import hack.echo.client.features.impl.misc.Teams;
import hack.echo.client.utils.Imports;
import hack.echo.client.utils.combat.antibot.AntiBotChecks;
import hack.echo.client.utils.player.PlayerUtils;
import hack.echo.client.utils.strings.Concat;
import hack.echo.client.utils.world.WorldUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public class TargetUtils implements Imports {

    private static LivingEntity keptTarget;
    private static LivingEntity lastAttackedTarget;
    private static int lastAttackedTick;

    private TargetUtils() {
    }

    public static void onAttack(Entity entity) {
        if (mc.player == null) {
            return;
        }

        if (!(entity instanceof LivingEntity living)) {
            return;
        }

        lastAttackedTarget = living;
        lastAttackedTick = mc.player.tickCount;

        if (!isTargetAllowed(living)) {
            return;
        }

        keptTarget = living;
    }

    public static boolean isTargetAllowed(LivingEntity entity) {
        if (entity == null) {
            return false;
        }

        if (!entity.isAlive() || entity.isRemoved() || entity.isDeadOrDying()) {
            return false;
        }

        if (mc.player == null) {
            return false;
        }

        TargetControlModule targets = getTargets();
        if (targets == null) {
            return true;
        }

        var entityTypes = targets.entityTypes;
        boolean anyEnabled = false;
        for (Boolean enabled : entityTypes.getOptionsSequenceView().values()) {
            if (!enabled) {
                continue;
            }

            anyEnabled = true;
            break;
        }

        if (!anyEnabled) {
            return true;
        }

        if (WorldUtils.isNaked(entity) && !entityTypes.isEnabled(Concat.of("Naked"))) {
            return false;
        }

        if (WorldUtils.isPlayer(entity)) {
            return isPlayerTargetAllowed(entity, targets);
        }

        if (entity.isInvisible() && !entityTypes.isEnabled(Concat.of("Invisible"))) {
            return false;
        }

        if (WorldUtils.isHostile(entity)) {
            return entityTypes.isEnabled(Concat.of("Hostile"));
        }

        if (WorldUtils.isPassive(entity)) {
            return entityTypes.isEnabled(Concat.of("Passive"));
        }

        if (WorldUtils.isNeutral(entity)) {
            return entityTypes.isEnabled(Concat.of("Neutral"));
        }

        return false;
    }

    public static LivingEntity resolveTarget(LivingEntity candidate) {
        if (!isKeepTargetEnabled()) {
            return candidate;
        }

        if (isLastAttackedModeEnabled()) {
            return resolveLastAttackedTarget();
        }

        if (isKeptTargetValid(keptTarget)) {
            return keptTarget;
        }

        if (!isKeptTargetValid(candidate)) {
            keptTarget = null;
            return null;
        }

        keptTarget = candidate;
        return keptTarget;
    }

    public static LivingEntity findClosestResolvedTarget(Vec3 origin, float partialTick) {
        if (origin == null || mc.level == null || mc.player == null) {
            return null;
        }

        LivingEntity bestTarget = null;
        double bestDistSq = Double.MAX_VALUE;

        for (var entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity living)) continue;
            if (entity == mc.player) continue;
            if (!isTargetAllowed(living)) continue;

            living = resolveTarget(living);
            if (living == null) continue;

            double distSq = origin.distanceToSqr(living.getPosition(partialTick));
            if (distSq < bestDistSq) {
                bestDistSq = distSq;
                bestTarget = living;
            }
        }

        return bestTarget;
    }

    public static void setKeptTarget(LivingEntity target) {
        if (!isKeptTargetValid(target)) {
            return;
        }

        keptTarget = target;
    }

    public static void clearKeptTarget() {
        keptTarget = null;
    }

    public static void clearTrackedTargets() {
        keptTarget = null;
        lastAttackedTarget = null;
        lastAttackedTick = 0;
    }

    public static LivingEntity getLastAttackedTarget(int ticks) {
        if (mc.player == null) {
            return null;
        }

        if (!isLastAttackedStateValid()) {
            return null;
        }

        if (mc.player.tickCount - lastAttackedTick > ticks) {
            return null;
        }

        return lastAttackedTarget;
    }

    public static boolean isBot(LivingEntity entity) {
        TargetControlModule targets = getTargets();
        if (targets == null) {
            return false;
        }

        return isBot(entity, targets);
    }

    private static LivingEntity resolveLastAttackedTarget() {
        LivingEntity attackedTarget = getStoredLastAttackedTarget();
        if (attackedTarget != null) {
            keptTarget = attackedTarget;
        }

        if (isKeptTargetValid(keptTarget)) {
            return keptTarget;
        }

        keptTarget = null;
        return null;
    }

    private static LivingEntity getStoredLastAttackedTarget() {
        if (!isLastAttackedStateValid()) {
            return null;
        }

        return lastAttackedTarget;
    }

    private static boolean isLastAttackedStateValid() {
        if (lastAttackedTarget == null) {
            return false;
        }

        if (mc.player == null) {
            return false;
        }

        if (lastAttackedTarget.level() != mc.level) {
            return false;
        }

        if (!lastAttackedTarget.isAlive() || lastAttackedTarget.isRemoved()) {
            return false;
        }

        return isTargetAllowed(lastAttackedTarget);
    }

    private static boolean isKeptTargetValid(LivingEntity target) {
        if (target == null || mc.player == null) {
            return false;
        }

        if (!target.isAlive() || target.isRemoved()) {
            return false;
        }

        if (target.level() != mc.level) {
            return false;
        }

        if (mc.player.distanceTo(target) > getKeepTargetRange()) {
            return false;
        }

        return isTargetAllowed(target);
    }

    private static double getKeepTargetRange() {
        TargetControlModule targets = getTargets();
        if (targets == null) {
            return 16.0;
        }

        return targets.keepTargetRange.getValue();
    }

    private static boolean isKeepTargetEnabled() {
        TargetControlModule targets = getTargets();
        if (targets == null) {
            return false;
        }

        return targets.keepTarget.getValue();
    }

    private static boolean isLastAttackedModeEnabled() {
        TargetControlModule targets = getTargets();
        if (targets == null) {
            return false;
        }

        if (!targets.keepTarget.getValue()) {
            return false;
        }

        return targets.lastAttacked.getValue();
    }

    private static boolean isPlayerTargetAllowed(LivingEntity entity, TargetControlModule targets) {
        if (!targets.entityTypes.isEnabled(Concat.of("Players"))) {
            return false;
        }

        if (targets.antiBot.getValue() && isBot(entity, targets)) {
            return false;
        }

        if (targets.ignoreTeams.getValue()) {
            Teams teams = Echo.featureManager.getFeatureByClass(Teams.class);
            if (teams != null && teams.isOnSameTeam(entity)) {
                return false;
            }
        }

        if (targets.ignoreFriends.getValue() && PlayerUtils.isFriend(entity.getName().getString())) {
            return false;
        }

        return true;
    }

    private static boolean isBot(LivingEntity entity, TargetControlModule targets) {
        if (!(entity instanceof Player player)) {
            return false;
        }

        return AntiBotChecks.isBot(player, targets);
    }

    private static TargetControlModule getTargets() {
        if (Echo.featureManager == null) {
            return null;
        }

        return Echo.featureManager.getFeatureByClass(TargetControlModule.class);
    }
}
