package hack.echo.client.utils.rotation;

import hack.echo.client.auth.MathProt;
import hack.echo.client.event.EventSubscribe;
import hack.echo.client.features.Feature;
import hack.echo.client.handlers.RotationHandler;
import hack.echo.client.utils.Imports;
import hack.echo.client.utils.MathUtil;
import hack.echo.client.utils.player.CustomGetVelocity;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/*
 * Credit for the closest-point math: https://github.com/sootysplash/optimal-aim
 */
public final class RotationUtils implements Imports {

    private RotationUtils() {}

    // -- Enums --

    public enum EntityPoints {
        STRAIGHT,
        CLOSEST,
        RANDOM,
        CENTER,
        FEET
    }

    public enum AimType {
        REGULAR,
        BLATANT,
        WINDMOUSE
    }

    // -- State --

    private static float[] lastRotations = new float[]{0f, 0f};
    private static float[] lastSentRotations = new float[]{0f, 0f};

    private static double windYawVel = 0;
    private static double windPitchVel = 0;
    private static double windYaw = 0;
    private static double windPitch = 0;
    private static float gcdYawRemainder = 0f;
    private static float gcdPitchRemainder = 0f;

    private static long lastNanoTime = 0;

    @Getter
    private static boolean isTracking = false;
    @Getter
    private static LivingEntity currentTarget = null;

    private static Feature currentController = null;
    private static EventSubscribe.Priority currentPriority = EventSubscribe.Priority.LOWEST;

    // Sticky alt-point: prevents jitter when the primary candidate is blocked.
    // Stored as offset from box.minCorner so it tracks the entity as it moves.
    private static LivingEntity stickyAltTarget = null;
    private static Vec3 stickyAltOffset = null;

    // -- Delta time --

    private static float getDeltaSeconds() {
        float delta = MathUtil.getDeltaSeconds(lastNanoTime);
        lastNanoTime = MathUtil.nanoTime();
        return delta;
    }

    // -- State management --

    public static void resetState() {
        if (mc.player != null) {
            lastRotations[0] = mc.player.getYRot();
            lastRotations[1] = mc.player.getXRot();
            lastSentRotations[0] = mc.player.getYRot();
            lastSentRotations[1] = mc.player.getXRot();
            RotationHandler.syncToPlayer(mc.player.getYRot(), mc.player.getXRot());
        } else {
            lastRotations = new float[]{0f, 0f};
            lastSentRotations = new float[]{0f, 0f};
            RotationHandler.reset();
        }
        windYawVel = 0;
        windPitchVel = 0;
        windYaw = 0;
        windPitch = 0;
        gcdYawRemainder = 0f;
        gcdPitchRemainder = 0f;
        lastNanoTime = 0;
        isTracking = false;
        currentTarget = null;
        currentController = null;
        currentPriority = EventSubscribe.Priority.LOWEST;
        clearStickyAlt();
    }

    public static void initFromPlayer() {
        if (mc.player == null) return;
        lastRotations[0] = mc.player.getYRot();
        lastRotations[1] = mc.player.getXRot();
        lastSentRotations[0] = mc.player.getYRot();
        lastSentRotations[1] = mc.player.getXRot();
        gcdYawRemainder = 0f;
        gcdPitchRemainder = 0f;
        RotationHandler.updateServerRotations(mc.player.getYRot(), mc.player.getXRot());
    }

    public static void stopTracking() {
        if (mc.player != null) {
            RotationHandler.syncToPlayer(mc.player.getYRot(), mc.player.getXRot());
            lastSentRotations[0] = mc.player.getYRot();
            lastSentRotations[1] = mc.player.getXRot();
        } else {
            RotationHandler.reset();
        }
        lastNanoTime = 0;
        isTracking = false;
        currentTarget = null;
        currentController = null;
        currentPriority = EventSubscribe.Priority.LOWEST;
        clearStickyAlt();
    }

    public static float[] getLastRotations() {
        return lastRotations.clone();
    }

    // -- Core rotation: entity target --

    public static boolean rotateTo(LivingEntity target, float yawSpeed, float pitchSpeed,
                                   boolean silent, AimType aimType, EntityPoints pointsMode, float multipoint) {
        return rotateTo(target, yawSpeed, pitchSpeed, silent, aimType, pointsMode, multipoint, false, false, null, EventSubscribe.Priority.NORMAL);
    }

    public static boolean rotateTo(LivingEntity target, float yawSpeed, float pitchSpeed,
                                   boolean silent, AimType aimType, EntityPoints pointsMode, float multipoint,
                                   boolean useRandom, Feature requester, EventSubscribe.Priority priority) {
        return rotateTo(target, yawSpeed, pitchSpeed, silent, aimType, pointsMode, multipoint, useRandom, false, requester, priority);
    }

    public static boolean rotateTo(LivingEntity target, float yawSpeed, float pitchSpeed,
                                   boolean silent, AimType aimType, EntityPoints pointsMode, float multipoint,
                                   boolean useRandom, boolean throughWalls, Feature requester, EventSubscribe.Priority priority) {
        if (target == null || mc.player == null || mc.level == null) return false;
        if (requester != null && !tryTakeControl(requester, priority)) return false;

        float pt = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        float shrink = multipoint * 0.01f;

        Optional<Vec3> opt = getPossiblePoints(target, shrink, pointsMode, useRandom, throughWalls, pt);
        if (opt.isEmpty()) return false;

        if (currentTarget != target) {
            currentTarget = target;
            windYawVel = 0;
            windPitchVel = 0;
            windYaw = 0;
            windPitch = 0;
            if (!isTracking) initFromPlayer();
        } else if (!isTracking) {
            initFromPlayer();
        }
        isTracking = true;

        float[] targetRot = calculate(mc.player.getEyePosition(pt), opt.get());
        float[] baseRotation = silent ? lastRotations : new float[]{mc.player.getYRot(), mc.player.getXRot()};
        targetRot = ensureContinuity(baseRotation, targetRot);

        lastRotations = getFixedRotations(lastRotations,
                getTargetRotations(lastRotations, targetRot, yawSpeed, pitchSpeed, silent, aimType));

        applyRotations(lastRotations, silent);
        return true;
    }

    // -- Core rotation: position target --

    public static boolean rotateTo(Vec3 position, float yawSpeed, float pitchSpeed,
                                   boolean silent, AimType aimType) {
        return rotateTo(position, yawSpeed, pitchSpeed, silent, aimType, null, EventSubscribe.Priority.NORMAL);
    }

    public static boolean rotateTo(Vec3 position, float yawSpeed, float pitchSpeed,
                                   boolean silent, AimType aimType, Feature requester, EventSubscribe.Priority priority) {
        if (position == null || mc.player == null) return false;
        if (requester != null && !tryTakeControl(requester, priority)) return false;

        currentTarget = null;
        windYawVel = 0;
        windPitchVel = 0;
        windYaw = 0;
        windPitch = 0;

        if (!isTracking) initFromPlayer();
        isTracking = true;

        float pt = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        float[] targetRot = calculate(mc.player.getEyePosition(pt), position);
        float[] baseRotation = silent ? lastRotations : new float[]{mc.player.getYRot(), mc.player.getXRot()};
        targetRot = ensureContinuity(baseRotation, targetRot);

        lastRotations = getFixedRotations(lastRotations,
                getTargetRotations(lastRotations, targetRot, yawSpeed, pitchSpeed, silent, aimType));

        applyRotations(lastRotations, silent);
        return true;
    }

    // -- Rotate back to client view --

    public static boolean rotateBack(boolean silent) {
        return rotateBack(silent, 150f, 150f, AimType.REGULAR);
    }

    public static boolean rotateBack(boolean silent, float yawSpeed, float pitchSpeed, AimType aimType) {
        return rotateBack(silent, yawSpeed, pitchSpeed, aimType, true);
    }

    public static boolean rotateBack(boolean silent, float yawSpeed, float pitchSpeed, AimType aimType, boolean scaleSpeed) {
        if (mc.player == null) {
            isTracking = false;
            currentTarget = null;
            currentController = null;
            currentPriority = EventSubscribe.Priority.LOWEST;
            return false;
        }

        if (!isTracking) {
            RotationHandler.syncToPlayer(mc.player.getYRot(), mc.player.getXRot());
            lastSentRotations[0] = mc.player.getYRot();
            lastSentRotations[1] = mc.player.getXRot();
            currentTarget = null;
            currentController = null;
            currentPriority = EventSubscribe.Priority.LOWEST;
            return false;
        }

        float[] clientRot = new float[]{mc.player.getYRot(), mc.player.getXRot()};

        if (aimType == AimType.BLATANT) {
            RotationHandler.syncToPlayer(mc.player.getYRot(), mc.player.getXRot());
            lastSentRotations[0] = mc.player.getYRot();
            lastSentRotations[1] = mc.player.getXRot();
            lastRotations[0] = mc.player.getYRot();
            lastRotations[1] = mc.player.getXRot();
            isTracking = false;
            currentTarget = null;
            currentController = null;
            currentPriority = EventSubscribe.Priority.LOWEST;
            return false;
        }

        float delta = getDeltaSeconds();
        float speedMultiplier = scaleSpeed ? (silent ? 2.5f : 0.5f) : 1.0f;

        lastRotations[0] = lerpAngle(lastRotations[0], clientRot[0], delta, yawSpeed * speedMultiplier);
        lastRotations[1] = lerpAngle(lastRotations[1], clientRot[1], delta, pitchSpeed * speedMultiplier);

        float yawDiff = Math.abs(Mth.wrapDegrees(lastRotations[0] - clientRot[0]));
        float pitchDiff = Math.abs(lastRotations[1] - clientRot[1]);

        if (yawDiff < 1.0f && pitchDiff < 1.0f) {
            RotationHandler.syncToPlayer(mc.player.getYRot(), mc.player.getXRot());
            lastSentRotations[0] = mc.player.getYRot();
            lastSentRotations[1] = mc.player.getXRot();
            isTracking = false;
            currentTarget = null;
            currentController = null;
            currentPriority = EventSubscribe.Priority.LOWEST;
            return false;
        }

        if (silent) applySilentRotations(lastRotations);
        return true;
    }

    // -- Sync silent rotation to client view, then run action --

    /**
     * @param requester The feature requesting rotation control
     * @param priority Priority level for rotation control
     * @param aimType BLATANT, REGULAR
     * @param yawSpeed Non blatant yaw speed
     * @param pitchSpeed Non blatant pitch speed
     * @param action The action to execute (e.g., item use) - will be run after rotation sync
     */
    public static void syncToClientAndRun(Feature requester, EventSubscribe.Priority priority,
                                               AimType aimType, float yawSpeed, float pitchSpeed,
                                               Runnable action) {
        if (mc.player == null || mc.level == null) {
            action.run();
            return;
        }

        if (!RotationHandler.isHasSilentRotation()) {
            action.run();
            return;
        }

        boolean tookControl = (requester == null || tryTakeControl(requester, priority));
        if (!tookControl) {
            action.run();
            return;
        }

        if (!isTracking) initFromPlayer();
        isTracking = true;
        currentTarget = null;

        float[] clientRot = new float[]{mc.player.getYRot(), mc.player.getXRot()};

        if (aimType == AimType.BLATANT) {
            lastRotations[0] = clientRot[0];
            lastRotations[1] = clientRot[1];
            applySilentRotations(lastRotations);
            action.run();
        } else {
            // Intentionally uses FPS-based delta for rapid convergence loop
            float delta = 1.0f / Math.max(1f, mc.getFps());

            while (true) {
                lastRotations[0] = lerpAngle(lastRotations[0], clientRot[0], delta, yawSpeed);
                lastRotations[1] = lerpAngle(lastRotations[1], clientRot[1], delta, pitchSpeed);
                applySilentRotations(lastRotations);

                float yawDiff = Math.abs(Mth.wrapDegrees(lastRotations[0] - clientRot[0]));
                float pitchDiff = Math.abs(lastRotations[1] - clientRot[1]);

                if (yawDiff < 1.0f && pitchDiff < 1.0f) {
                    action.run();
                    break;
                }
            }
        }
        releaseControl(requester);

        if (currentController == null && mc.player != null) {
            RotationHandler.syncToPlayer(mc.player.getYRot(), mc.player.getXRot());
            lastSentRotations[0] = mc.player.getYRot();
            lastSentRotations[1] = mc.player.getXRot();
        }
    }

    // -- Rotation application --

    public static void applySilentRotations(float[] rotations) {
        float yawDiff = Math.abs(Mth.wrapDegrees(rotations[0] - lastSentRotations[0]));
        float enforcedPitch = MathProt.getEnforcedPitch(rotations[1]);
        float pitchDiff = Math.abs(enforcedPitch - lastSentRotations[1]);

        if (yawDiff <= 0.01f && pitchDiff <= 0.01f) return;

        float normalizedYaw = lastSentRotations[0] + Mth.wrapDegrees(rotations[0] - lastSentRotations[0]);
        float normalizedPitch = Mth.clamp(enforcedPitch, -90f, 90f);

        RotationHandler.setServerRotationsWithDelta(normalizedYaw, normalizedPitch);
        RotationHandler.setHasSilentRotation(true);
        lastSentRotations[0] = normalizedYaw;
        lastSentRotations[1] = normalizedPitch;
    }

    // Only used for KB displacement. Would remove othewise
    public static void forceSilentRotations(float yaw, float pitch) {
        if (mc.player == null) return;

        float baseYaw = RotationHandler.isHasSilentRotation() ? RotationHandler.getServerYaw() : mc.player.getYRot();
        float basePitch = RotationHandler.isHasSilentRotation() ? RotationHandler.getServerPitch() : mc.player.getXRot();

        lastRotations[0] = baseYaw;
        lastRotations[1] = basePitch;
        lastSentRotations[0] = baseYaw;
        lastSentRotations[1] = basePitch;

        float[] targetRotations = ensureContinuity(
            new float[]{baseYaw, basePitch},
            new float[]{yaw, pitch}
        );

        applySilentRotations(targetRotations);
        lastRotations[0] = targetRotations[0];
        lastRotations[1] = targetRotations[1];
    }

    public static void applyClientRotations(float[] rotations) {
        if (mc.player == null) return;
        float enforcedPitch = MathProt.getEnforcedPitch(rotations[1]);
        mc.player.setYRot(rotations[0]);
        mc.player.setXRot(enforcedPitch);
        RotationHandler.reset();
    }

    private static void applyRotations(float[] rotations, boolean silent) {
        if (silent) {
            applySilentRotations(rotations);
        } else {
            applyClientRotations(rotations);
        }
    }

    // -- Angle calculation --

    public static float[] calculate(Vec3 from, Vec3 to) {
        Vec3 diff = to.subtract(from);
        double distance = Math.hypot(diff.x, diff.z);
        float yaw = Mth.wrapDegrees((float) (Math.toDegrees(Math.atan2(diff.z, diff.x))) - 90.0F);
        float pitch = Mth.wrapDegrees((float) (-Math.toDegrees(Math.atan2(diff.y, distance))));
        return new float[]{yaw, pitch};
    }

    public static Vec3 predictAimPoint(LivingEntity entity, int ticks, EntityPoints mode) {
        if (entity == null || ticks <= 0) return getEntityPoint(entity, mode, 1.0f);
        Vec3 offset = getEntityPoint(entity, mode, 1.0f).subtract(entity.position());
        return entity.position().add(CustomGetVelocity.get(entity).scale(ticks)).add(offset);
    }

    // -- Interpolation helpers --

    public static AABB getInterpolatedAABB(Entity entity, float partialTick) {
        Vec3 pos = entity.getPosition(partialTick);
        return entity.getDimensions(entity.getPose()).makeBoundingBox(pos);
    }

    // -- Point / box utilities --

    public static Vec3 closestPointToBox(AABB box, float partialTick) {
        if (mc.player == null) return box.getCenter();
        Vec3 eye = mc.player.getEyePosition(partialTick);
        return clampToBox(eye, box);
    }

    private static Vec3 clampToBox(Vec3 point, AABB box) {
        double cx = Math.min(Math.max(point.x, box.minX), box.maxX);
        double cy = Math.min(Math.max(point.y, box.minY), box.maxY);
        double cz = Math.min(Math.max(point.z, box.minZ), box.maxZ);
        return new Vec3(cx, cy, cz);
    }

    public static Vec3 closestPointToBoxFromRay(AABB box, Vec3 rayOrigin, Vec3 rayDirection) {
        if (box == null || rayOrigin == null || rayDirection == null) return box.getCenter();

        Vec3 toBox = box.getCenter().subtract(rayOrigin);
        double t = Math.max(0, toBox.dot(rayDirection));
        Vec3 rayPoint = rayOrigin.add(rayDirection.scale(t));

        double cx = Math.min(Math.max(rayPoint.x, box.minX), box.maxX);
        double cy = Math.min(Math.max(rayPoint.y, box.minY), box.maxY);
        double cz = Math.min(Math.max(rayPoint.z, box.minZ), box.maxZ);

        double minY = box.minY + 0.51;
        if (cy < minY) cy = minY;

        return new Vec3(cx, cy, cz);
    }

    public static Vec3 randomPointInBox(AABB box) {
        double rx = box.minX + ThreadLocalRandom.current().nextDouble() * (box.maxX - box.minX);
        double ry = box.minY + ThreadLocalRandom.current().nextDouble() * (box.maxY - box.minY);
        double rz = box.minZ + ThreadLocalRandom.current().nextDouble() * (box.maxZ - box.minZ);
        return new Vec3(rx, ry, rz);
    }

    public static Vec3 getEntityPoint(LivingEntity entity, EntityPoints mode, float partialTick) {
        if (entity == null) return Vec3.ZERO;
        AABB box = getInterpolatedAABB(entity, partialTick);
        Vec3 center = box.getCenter();

        return switch (mode) {
            case STRAIGHT -> new Vec3(center.x, box.maxY - 0.1f, center.z);
            case CLOSEST -> closestPointToBox(box, partialTick);
            case RANDOM -> randomPointInBox(box);
            case CENTER -> center;
            case FEET -> new Vec3(center.x, box.minY + 0.1, center.z);
        };
    }

    // -- Aim point resolution --

    public static Optional<Vec3> getPossiblePoints(LivingEntity entity, float shrink, EntityPoints entityPoints,
                                                   boolean useRandom, boolean throughWalls, float partialTick) {
        if (entity == null || mc.player == null || mc.level == null) return Optional.empty();

        AABB bBox = getInterpolatedAABB(entity, partialTick);
        Vec3 center = bBox.getCenter();
        Vec3 candidate = getEntityPoint(entity, entityPoints, partialTick);

        if (useRandom) {
            candidate = blendWithRandom(candidate, bBox, entityPoints);
        }

        Vec3 finalCandidate = center.lerp(candidate, shrink);

        if (isPointVisible(finalCandidate, partialTick)) {
            clearStickyAlt();
            return Optional.of(finalCandidate);
        }

        // Reuse last frame's visible alt if it is still visible — prevents per-frame
        // flipping between adjacent grid samples as the player moves.
        if (stickyAltTarget == entity && stickyAltOffset != null) {
            Vec3 sticky = new Vec3(
                    bBox.minX + stickyAltOffset.x,
                    bBox.minY + stickyAltOffset.y,
                    bBox.minZ + stickyAltOffset.z
            );
            if (isInsideBox(sticky, bBox) && isPointVisible(sticky, partialTick)) {
                return Optional.of(sticky);
            }
        }

        Vec3 visibleAlt = findVisiblePointInBox(bBox, finalCandidate, partialTick);
        if (visibleAlt != null) {
            stickyAltTarget = entity;
            stickyAltOffset = new Vec3(
                    visibleAlt.x - bBox.minX,
                    visibleAlt.y - bBox.minY,
                    visibleAlt.z - bBox.minZ
            );
            return Optional.of(visibleAlt);
        }

        clearStickyAlt();
        return throughWalls ? Optional.of(finalCandidate) : Optional.empty();
    }

    private static void clearStickyAlt() {
        stickyAltTarget = null;
        stickyAltOffset = null;
    }

    private static boolean isInsideBox(Vec3 p, AABB box) {
        return p.x >= box.minX && p.x <= box.maxX
                && p.y >= box.minY && p.y <= box.maxY
                && p.z >= box.minZ && p.z <= box.maxZ;
    }

    private static boolean isPointVisible(Vec3 point, float partialTick) {
        HitResult r = rayTraceTo(point, partialTick);
        return r == null
                || r.getType() == HitResult.Type.MISS
                || r.getLocation().distanceToSqr(point) < 1.0e-6d;
    }

    // Grid-samples the entity AABB and returns the visible sample closest to {@code preferred}.
    // Density is tuned to reliably resolve slab-sized (0.5b) gaps in line of sight.
    private static Vec3 findVisiblePointInBox(AABB box, Vec3 preferred, float partialTick) {
        if (mc.player == null || mc.level == null) return null;

        final int xSteps = 3;
        final int ySteps = 5;
        final int zSteps = 3;
        final double width = box.maxX - box.minX;
        final double height = box.maxY - box.minY;
        final double depth = box.maxZ - box.minZ;

        Vec3 best = null;
        double bestDistSq = Double.MAX_VALUE;

        for (int xi = 0; xi < xSteps; xi++) {
            double x = box.minX + (xi / (double) (xSteps - 1)) * width;
            for (int yi = 0; yi < ySteps; yi++) {
                double y = box.minY + (yi / (double) (ySteps - 1)) * height;
                for (int zi = 0; zi < zSteps; zi++) {
                    double z = box.minZ + (zi / (double) (zSteps - 1)) * depth;
                    Vec3 sample = new Vec3(x, y, z);

                    if (!isPointVisible(sample, partialTick)) continue;

                    double distSq = sample.distanceToSqr(preferred);
                    if (distSq < bestDistSq) {
                        bestDistSq = distSq;
                        best = sample;
                    }
                }
            }
        }

        return best;
    }

    private static Vec3 blendWithRandom(Vec3 candidate, AABB bBox, EntityPoints entityPoints) {
        Vec3 randomPoint;

        if (entityPoints == EntityPoints.CLOSEST) {
            double regionWidth = (bBox.maxX - bBox.minX) * 0.9;
            double regionHeight = (bBox.maxY - bBox.minY) * 0.9;
            double regionDepth = (bBox.maxZ - bBox.minZ) * 0.9;

            double rx = candidate.x + (ThreadLocalRandom.current().nextDouble() - 0.5) * regionWidth;
            double ry = candidate.y + (ThreadLocalRandom.current().nextDouble() - 0.5) * regionHeight;
            double rz = candidate.z + (ThreadLocalRandom.current().nextDouble() - 0.5) * regionDepth;

            rx = Math.min(Math.max(rx, bBox.minX), bBox.maxX);
            ry = Math.min(Math.max(ry, bBox.minY), bBox.maxY);
            rz = Math.min(Math.max(rz, bBox.minZ), bBox.maxZ);

            randomPoint = new Vec3(rx, ry, rz);
        } else {
            randomPoint = randomPointInBox(bBox);
        }

        return candidate.lerp(randomPoint, 0.50);
    }

    // -- Ray tracing --

    public static HitResult rayTraceTo(Vec3 end, float partialTick) {
        if (mc.player == null || mc.level == null) return null;
        Vec3 start = mc.player.getEyePosition(partialTick);
        return mc.level.clip(new ClipContext(start, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, mc.player));
    }

    // -- Lerp helpers --

    public static float lerpAngle(float current, float target, float deltaSeconds, float speed) {
        float diff = Mth.wrapDegrees(target - current);
        float step = speed * deltaSeconds;
        if (Math.abs(diff) <= step) return current + diff;
        return current + Math.signum(diff) * step;
    }

    public static float smoothLerpAngle(float current, float target, float deltaSeconds, float speed) {
        float diff = Mth.wrapDegrees(target - current);
        float factor = 1.0f - (float) Math.exp(-Math.max(0.0f, speed) * deltaSeconds * 0.01f);
        return current + diff * factor;
    }

    // -- Aim math selection --

    public static float[] getTargetRotations(float[] lastRot, float[] targetRot,
                                              float yawSpeed, float pitchSpeed,
                                              boolean silent, AimType aimType) {
        if (mc.player == null || mc.level == null) return lastRot;

        float yaw = silent ? lastRot[0] : mc.player.getYRot();
        float pitch = silent ? lastRot[1] : mc.player.getXRot();
        float delta = getDeltaSeconds();
        float speedMultiplier = silent ? 2.5f : 0.5f;

        return switch (aimType) {
            case REGULAR -> new float[]{
                    lerpAngle(yaw, targetRot[0], delta, yawSpeed * speedMultiplier),
                    lerpAngle(pitch, targetRot[1], delta, pitchSpeed * speedMultiplier)
            };
            case BLATANT -> new float[]{targetRot[0], targetRot[1]};
            case WINDMOUSE -> getWindMouseRotations(
                    new float[]{yaw, pitch}, targetRot, yawSpeed, pitchSpeed, delta, silent);
        };
    }

    // -- WindMouse --

    private static float[] getWindMouseRotations(float[] currentRot, float[] targetRot,
                                                  float yawSpeed, float pitchSpeed,
                                                  float delta, boolean silent) {
        double currentYaw = currentRot[0];
        double currentPitch = currentRot[1];
        double targetYaw = targetRot[0];
        double targetPitch = targetRot[1];

        boolean yawEnabled = yawSpeed > 0.0f;
        boolean pitchEnabled = pitchSpeed > 0.0f;

        double yawScale = yawSpeed * (silent ? 2.5 : 0.5);
        double pitchScale = pitchSpeed * (silent ? 2.5 : 0.5);

        double M0Yaw = yawScale * delta;
        double M0Pitch = pitchScale * delta;
        double baseM0 = Math.max(M0Yaw, M0Pitch);

        double G_0 = baseM0 * 0.6;
        double W_0 = baseM0 * 0.2;
        double D_0 = 8.0;

        double diffYaw = Mth.wrapDegrees(targetYaw - currentYaw);
        double diffPitch = targetPitch - currentPitch;
        double dist = Math.hypot(diffYaw, diffPitch);

        double W_mag = Math.min(W_0, dist);

        // Update wind forces
        if (dist >= D_0) {
            windYaw = yawEnabled ? windYaw / Math.sqrt(3) + (ThreadLocalRandom.current().nextDouble() * 2 - 1) * W_mag / Math.sqrt(5) : 0;
            windPitch = pitchEnabled ? windPitch / Math.sqrt(3) + (ThreadLocalRandom.current().nextDouble() * 2 - 1) * W_mag / Math.sqrt(5) : 0;
        } else {
            windYaw = yawEnabled ? windYaw / Math.sqrt(3) : 0;
            windPitch = pitchEnabled ? windPitch / Math.sqrt(3) : 0;
        }

        double currM0Yaw = M0Yaw;
        double currM0Pitch = M0Pitch;
        if (dist < D_0) {
            currM0Yaw /= Math.sqrt(5);
            currM0Pitch /= Math.sqrt(5);
        }

        double gravity = Math.min(G_0, dist);

        // Update velocity
        if (yawEnabled) {
            windYawVel += windYaw + (dist == 0 ? 0 : gravity * diffYaw / dist);
        } else {
            windYawVel = 0;
        }
        if (pitchEnabled) {
            windPitchVel += windPitch + (dist == 0 ? 0 : gravity * diffPitch / dist);
        } else {
            windPitchVel = 0;
        }

        // Dampen when close
        if (dist < 0.5) {
            windYawVel *= 0.7;
            windPitchVel *= 0.7;
        }
        if (dist < 0.1) {
            windYawVel = 0;
            windPitchVel = 0;
        }

        // Clamp velocity
        if (yawEnabled && currM0Yaw > 0) {
            double yawLimit = currM0Yaw * (0.5 + ThreadLocalRandom.current().nextDouble() * 0.5);
            windYawVel = Mth.clamp(windYawVel, -yawLimit, yawLimit);
        }
        if (pitchEnabled && currM0Pitch > 0) {
            double pitchLimit = currM0Pitch * (0.5 + ThreadLocalRandom.current().nextDouble() * 0.5);
            windPitchVel = Mth.clamp(windPitchVel, -pitchLimit, pitchLimit);
        }

        return new float[]{
                (float) (currentYaw + windYawVel),
                (float) (currentPitch + windPitchVel)
        };
    }

    // -- GCD fix --

    public static float[] getFixedRotations(float[] prev, float[] current) {
        float yawDiff = Mth.wrapDegrees(current[0] - prev[0]);
        float pitchDiff = Mth.wrapDegrees(current[1] - prev[1]);
        float[] capped = new float[]{prev[0] + yawDiff, prev[1] + pitchDiff};
        return applyGCD(capped, prev);
    }

    
    // -- GCD but works with hyperspeed --
    public static float[] applyGCD(float[] rotations, float[] lastRotations) {
        float f = (float) (mc.options.sensitivity().get() * 0.6F + 0.2F);
        float gcd = f * f * f * 1.2F;

        float accumulatedYaw = gcdYawRemainder + Mth.wrapDegrees(rotations[0] - lastRotations[0]);
        float accumulatedPitch = gcdPitchRemainder + Mth.wrapDegrees(rotations[1] - lastRotations[1]);
        float snappedYaw = gcd <= 0f ? accumulatedYaw : (int) (accumulatedYaw / gcd) * gcd;
        float snappedPitch = gcd <= 0f ? accumulatedPitch : (int) (accumulatedPitch / gcd) * gcd;

        gcdYawRemainder = Mth.wrapDegrees(accumulatedYaw - snappedYaw);
        gcdPitchRemainder = accumulatedPitch - snappedPitch;

        return new float[]{
                lastRotations[0] + snappedYaw,
                lastRotations[1] + snappedPitch
        };
    }

    public static float[] ensureContinuity(float[] lastRot, float[] targetRot) {
        float lastYaw = Mth.wrapDegrees(lastRot[0]);
        float diff = Mth.wrapDegrees(targetRot[0] - lastYaw);
        return new float[]{lastYaw + diff, Mth.clamp(targetRot[1], -90f, 90f)};
    }

    // -- Silent rotation accessors --

    public static float getSilentYaw() {
        return lastSentRotations[0];
    }

    public static float getSilentPitch() {
        return lastSentRotations[1];
    }

    public static boolean hasSilentRotation() {
        return RotationHandler.isHasSilentRotation();
    }

    // -- Priority system --

    public static boolean tryTakeControl(Feature requester, EventSubscribe.Priority priority) {
        if (currentController == null || currentController == requester) {
            currentController = requester;
            currentPriority = priority;
            return true;
        }
        if (getPriorityValue(priority) > getPriorityValue(currentPriority)) {
            currentController = requester;
            currentPriority = priority;
            return true;
        }
        return false;
    }

    private static int getPriorityValue(EventSubscribe.Priority priority) {
        return switch (priority) {
            case HIGHEST -> 5;
            case HIGH -> 4;
            case NORMAL -> 3;
            case LOW -> 2;
            case LOWEST -> 1;
        };
    }

    public static boolean isControlledBy(Feature feature) {
        return currentController == feature;
    }

    public static boolean canTakeControl(Feature requester, EventSubscribe.Priority priority) {
        if (currentController == null || currentController == requester) return true;
        return getPriorityValue(priority) > getPriorityValue(currentPriority);
    }

    public static void releaseControl(Feature requester) {
        if (currentController == requester) {
            currentController = null;
            currentPriority = EventSubscribe.Priority.LOWEST;
        }
    }

    /**
     * Cleanup for rotation tracking. Downgrades priority, rotates back, and releases control.
     * @return true if still rotating back (call again next tick), false when cleanup is complete
     */
    public static boolean cleanup(Feature requester, boolean silent) {
        return cleanup(requester, silent, 150f, 150f, AimType.REGULAR, true);
    }

    public static boolean cleanup(Feature requester, boolean silent, float yawSpeed, float pitchSpeed, AimType aimType, boolean scaleSpeed) {
        if (!isControlledBy(requester)) return false;
        tryTakeControl(requester, EventSubscribe.Priority.LOWEST);
        if (rotateBack(silent, yawSpeed, pitchSpeed, aimType, scaleSpeed)) return true;
        releaseControl(requester);
        return false;
    }

    // -- Builder --

    public static RotationBuilder aim(Feature requester) {
        return new RotationBuilder(requester);
    }

    public static class RotationBuilder {
        private final Feature requester;
        private EventSubscribe.Priority priority = EventSubscribe.Priority.NORMAL;
        private boolean silent = false;
        private AimType aimType = AimType.REGULAR;
        private float yawSpeed = 100f;
        private float pitchSpeed = 100f;
        private EntityPoints points = EntityPoints.STRAIGHT;
        private float multipoint = 50f;
        private boolean random = false;
        private boolean throughWalls = false;
        private int predictionTicks = 0;

        RotationBuilder(Feature requester) { this.requester = requester; }

        public RotationBuilder priority(EventSubscribe.Priority p) { this.priority = p; return this; }
        public RotationBuilder silent() { this.silent = true; return this; }
        public RotationBuilder silent(boolean s) { this.silent = s; return this; }
        public RotationBuilder windMouse() { this.aimType = AimType.WINDMOUSE; return this; }
        public RotationBuilder aimType(AimType t) { this.aimType = t; return this; }
        public RotationBuilder speed(float s) { this.yawSpeed = s; this.pitchSpeed = s; return this; }
        public RotationBuilder speed(float yaw, float pitch) { this.yawSpeed = yaw; this.pitchSpeed = pitch; return this; }
        public RotationBuilder points(EntityPoints p) { this.points = p; return this; }
        public RotationBuilder multipoint(float m) { this.multipoint = m; return this; }
        public RotationBuilder random(boolean r) { this.random = r; return this; }
        public RotationBuilder throughWalls(boolean t) { this.throughWalls = t; return this; }
        public RotationBuilder predict(int ticks) { this.predictionTicks = ticks; return this; }

        public boolean to(Vec3 pos) {
            return rotateTo(pos, yawSpeed, pitchSpeed, silent, aimType, requester, priority);
        }

        public boolean to(LivingEntity target) {
            if (predictionTicks > 0) {
                Vec3 predicted = predictAimPoint(target, predictionTicks, points);
                return rotateTo(predicted, yawSpeed, pitchSpeed, silent, aimType, requester, priority);
            }
            return rotateTo(target, yawSpeed, pitchSpeed, silent, aimType, points, multipoint, random, throughWalls, requester, priority);
        }
    }

    // -- Raycast helpers (used by interact-style modules to test what the player is currently aimed at) --

    /** Casts an OUTLINE ray from the player's eye in the given direction out to vanilla block-interaction range. */
    public static BlockHitResult raycastFromRotation(float yaw, float pitch, float partialTick) {
        if (mc.player == null || mc.level == null) return null;
        Vec3 eye = mc.player.getEyePosition(partialTick);
        Vec3 look = RotationHandler.getRotationVector(pitch, yaw);
        Vec3 end = eye.add(look.scale(mc.player.blockInteractionRange()));
        return mc.level.clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, mc.player));
    }

    /** True if a ray from the eye at the given rotation passes through (a slightly inflated) {@code clickedPos}. */
    public static boolean rayIntersectsBlock(float yaw, float pitch, BlockPos clickedPos, float partialTick) {
        if (mc.player == null || clickedPos == null) return false;
        Vec3 eye = mc.player.getEyePosition(partialTick);
        Vec3 look = RotationHandler.getRotationVector(pitch, yaw);
        Vec3 end = eye.add(look.scale(mc.player.blockInteractionRange()));
        return new AABB(clickedPos).inflate(1.0E-4).clip(eye, end).isPresent();
    }
}
