package onl.luka.grizzly.module.modules.world

import onl.luka.grizzly.module.Module
import onl.luka.grizzly.config.entry.ItemListEntry
import onl.luka.grizzly.module.modules.world.clutch.FallTracer
import onl.luka.grizzly.module.modules.world.clutch.LadderRescue
import onl.luka.grizzly.module.modules.world.clutch.LadderRescuePlanner
import onl.luka.grizzly.module.modules.world.clutch.PlanRehearsal
import onl.luka.grizzly.module.modules.world.clutch.PlacementGeometry
import onl.luka.grizzly.module.modules.world.clutch.BridgeRules
import onl.luka.grizzly.module.modules.world.clutch.MotionSim
import onl.luka.grizzly.module.modules.world.clutch.BridgePlanner
import onl.luka.grizzly.module.modules.world.clutch.FallTrace
import onl.luka.grizzly.module.modules.world.clutch.BlockPlacement
import onl.luka.grizzly.module.modules.world.clutch.MotionInput
import onl.luka.grizzly.util.NotificationManager
import onl.luka.grizzly.util.RotationManager
import onl.luka.grizzly.util.interactBlock
import onl.luka.grizzly.gui.helpers.itemCategories
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.player.LocalPlayer
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.CactusBlock
import net.minecraft.world.level.block.MagmaBlock
import net.minecraft.world.level.block.PowderSnowBlock
import net.minecraft.world.level.block.SweetBerryBushBlock
import net.minecraft.world.level.block.WitherRoseBlock
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3
import onl.luka.grizzly.util.InputUtil.isPhysicalKeyDown
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt

object Clutch : Module("Clutch", "Saves yourself from falling", Category.WORLD) {

    private const val ROTATION_OWNER = "clutch"
    private const val FALL_TRACE_TICKS = 50

    // Blocks preferred when several are available, best first
    private val PREFERRED_BLOCK_NAMES = listOf(
        "wool", "terracotta", "concrete", "planks", "sandstone", "stone", "dirt", "cobblestone",
    )

    enum class Trigger { ALWAYS, ON_VOID, ON_LETHAL_FALL, FALL_DISTANCE }

    private val triggersGroup = group("Triggers")
    private val trigger = enum("trigger", Trigger.ALWAYS).inGroup(triggersGroup)
    private val minFallBlocks = float("blocks", 4f, 1f, 50f).also {
        it.visibleWhen = { trigger.value == Trigger.FALL_DISTANCE }
    }.inGroup(triggersGroup)
    private val onlyOnDamage = boolean("Only on combat damage", false).also {
        it.aliases("Only on damage", "Only on player damage")
    }.inGroup(triggersGroup)
    private val damageWindowTicks = int("Damage window (ticks)", 12, 1, 40).also {
        it.visibleWhen = { onlyOnDamage.value }
    }.inGroup(triggersGroup)

    private val behaviourGroup = group("Behaviour")
    private val autoLadder = boolean("Auto ladder", true).inGroup(behaviourGroup)
    private val allowStaircaseUp = boolean("Allow staircase up", true).inGroup(behaviourGroup)
    private val searchDepth = int("Search depth", 4, 1, 8).inGroup(behaviourGroup)
    private val searchRadius = int("Search radius", 4, 1, 6).also {
        it.aliases("Anchor radius")
    }.inGroup(behaviourGroup)
    private val validatePaths = boolean("Validate paths", true).inGroup(behaviourGroup)
    private val limitBlocks = boolean("Limit blocks", false).inGroup(behaviourGroup)
    private val maxBlocks = int("Max blocks", 5, 1, 10).also {
        it.visibleWhen = { limitBlocks.value }
    }.inGroup(behaviourGroup)
    private val failDelay = int("Fail delay (ms)", 100, 0, 500).inGroup(behaviourGroup)
    private val clutchMoveDelay = intRange("Clutch move delay", 3 to 6, 0, 10).inGroup(behaviourGroup)

    private val rotationGroup = group("Rotations")
    private val speed = float("Speed", 60f, 10f, 120f).also {
        it.aliases("rotation speed")
    }.inGroup(rotationGroup)
    private val resetAngle = boolean("Reset angle", true).inGroup(rotationGroup)
    private val resetAngleDelay = intRange("Reset angle delay", 3 to 6, 0, 10).also {
        it.visibleWhen = { resetAngle.value }
    }.inGroup(rotationGroup)

    private val itemsGroup = group("Items")
    private val returnToLastSlot = boolean("Return to last slot", true).also {
        it.aliases("return to slot")
    }.inGroup(itemsGroup)
    private val returnDelay = intRange("Return delay", 3 to 6, 0, 10).also {
        it.visibleWhen = { returnToLastSlot.value }
    }.inGroup(itemsGroup)
    private val blacklist = boolean("Blacklist", true).inGroup(itemsGroup)
    private val blacklistBlocks = itemList(
        "Block blacklist",
        emptyList(),
        defaultMode = ItemListEntry.Mode.BLACKLIST,
        filter = ItemListEntry.Filter.BLOCKS_ONLY,
    ).also { it.visibleWhen = { blacklist.value } }.inGroup(itemsGroup)
    private val heldWhitelist = boolean("Held whitelist", false).inGroup(itemsGroup)
    private val whitelistBlocks = itemList(
        "Held block whitelist",
        listOf("wool_category"),
        defaultMode = ItemListEntry.Mode.WHITELIST,
        filter = ItemListEntry.Filter.BLOCKS_ONLY,
    ).also {
        it.aliases("Block Whitelist")
        it.visibleWhen = { heldWhitelist.value }
    }.inGroup(itemsGroup)

    private val showBlockCount = boolean("Show block count", false)

    private var previousSlot = -1
    private var clutching = false
    private var ownsRotation = false
    private var controlsSuppressed = false
    private var damageTicksRemaining = 0
    private var previousHurtTime = 0
    private var previousFallDistance = 0.0
    private var pendingFallDamageTicks = 0

    private var savedYaw = Float.NaN
    private var savedPitch = Float.NaN
    private var settledTicks = 0

    // Height of the ground the player last stood on; the clutch aims to get back to it
    private var groundLevelY = 0.0

    private var returnDelayTicks = -1
    private var resetAngleDelayTicks = -1
    private var restoringViewTicks = -1
    private var moveDelayTicks = -1
    private var failUntilMs = 0L

    private var pendingFailMessage: String? = null
    private var pendingFailTicks = 0
    private var notifiedThisFall = false
    private var lastPlanFailure: String? = null

    private var staircaseArmed = false
    private var previousJumpHeld = false
    private var lastJumpReleaseMs = 0L

    private var ladderPlan: LadderRescue? = null
    private var ladderAssistActive = false
    private var ladderAssistTicks = 0
    private var ladderFallDistanceBeforeTick = 0.0

    private var lastPlayer: LocalPlayer? = null
    private var lastPosition: Vec3? = null

    @JvmField var isActivelyPlacing = false

    // Placements still to make, in the order the search planned them
    private val pendingTargets = ArrayDeque<BlockPlacement>()

    // Positions this clutch has already filled, so the search stops re-proposing them
    private val placedBlocks = HashSet<BlockPos>()

    init {
        ClientTickEvents.START_CLIENT_TICK.register { client -> onClientTick(client) }
    }

    override fun onTick(client: Minecraft) {}

    private fun onClientTick(client: Minecraft) {
        if (!isEnabled()) return
        val player = client.player ?: return
        val world = client.level ?: return

        if (playerJumped(player)) {
            resetState(client, restoreSlot = false)
            return
        }

        updateDamageWindow(player, world)
        updateFallTarget(player)
        tickAftermath(client, player)
        tickStaircase(client)
        tickFailNotice()

        if (ladderAssistActive) {
            tickLadderCatch(player, world)
            return
        }

        if (player.onGround()) {
            armFailNotice()
            if (!clutching) {
                isActivelyPlacing = false
                releaseClutchControl(client)
                return
            }
            // Landing does not end a clutch. Only settling does - if the player
            // is still walking off the block they just placed, keep bridging.
            settledTicks++
            if (settledTicks >= GROUND_SETTLE_TICKS || !wouldStepOff(player, world)) {
                finishClutch(client)
                return
            }
        } else {
            settledTicks = 0
        }

        // The trigger only decides whether to *start*. Re-testing it every tick
        // would end the clutch the moment the player touches down, because a
        // grounded player is never falling.
        if (!clutching && !shouldEngage(player, world)) {
            releaseClutchControl(client)
            return
        }
        if (findBlockSlot(player) == -1 && (!autoLadder.value || findLadderSlot(player) == -1)) {
            if (clutching) finishClutch(client) else releaseClutchControl(client)
            return
        }

        val reach = if (player.isCreative) 5.0 else 4.5
        val eye = eyePosition(player)
        val target = resolveTarget(player, world, reach)
        if (target == null) {
            holdOrRelease(client)
            return
        }

        // Vanilla refuses a placement that would intersect us, so hold the
        // target and let the fall carry us clear of it.
        if (player.boundingBox.intersects(PlacementGeometry.blockBounds(world, target.fills))) {
            holdOrRelease(client)
            return
        }

        val hitPoint = PlacementGeometry.aimPointWithinReach(
            player, world, eye, target,
            RotationManager.getCurrentYaw(), RotationManager.getCurrentPitch(), reach,
        )
        if (hitPoint == null) {
            holdOrRelease(client)
            return
        }
        target.hitPoint = hitPoint

        if (!clutching) beginClutch(player)
        isActivelyPlacing = true
        applyMovementCorrection(client)

        if (!selectSlotFor(player, target)) return

        val (aimYaw, aimPitch) = PlacementGeometry.anglesTo(eye, hitPoint)
        primePlacementRotation(aimYaw, aimPitch)

        val hit = rayTraceAt(
            player, reach, RotationManager.getCurrentYaw(), RotationManager.getCurrentPitch(),
        ) ?: return
        if (!hitMatchesTarget(hit, target)) return

        interactBlock(hit)
        placedBlocks += target.fills
        pendingTargets.removeFirstOrNull()

        val plan = ladderPlan
        if (plan != null && pendingTargets.isEmpty()) {
            ladderAssistActive = true
            ladderAssistTicks = 0
            ladderFallDistanceBeforeTick = player.fallDistance
        }
    }

    // Whether the fall is one of the kinds the module is configured to catch
    private fun shouldEngage(player: LocalPlayer, world: ClientLevel): Boolean {
        if (player.isInWater || player.isInLava || player.onClimbable()) return false
        if (player.abilities.flying) return false
        if (onlyOnDamage.value && damageTicksRemaining <= 0) return false
        if (findBlockSlot(player) == -1 && (!autoLadder.value || findLadderSlot(player) == -1)) return false
        if (System.currentTimeMillis() < failUntilMs) return false
        if (player.onGround() && player.deltaMovement.y < 0.0) return false
        return isFallTriggered(player, world)
    }

    private fun isFallTriggered(player: LocalPlayer, world: ClientLevel): Boolean {
        if (trigger.value == Trigger.ALWAYS) return true
        // No landing block means the simulation ran out of world: a void fall.
        val landing = currentPrediction(player, world).landingBlock
        return when (trigger.value) {
            Trigger.ALWAYS -> true
            Trigger.ON_VOID -> landing == null
            Trigger.ON_LETHAL_FALL -> landing == null || player.y - landing.y - 3.0 > player.health
            Trigger.FALL_DISTANCE -> landing == null || player.y - (landing.y + 1) >= minFallBlocks.value
        }
    }

    private var cachedPrediction: FallTrace? = null
    private var cachedPredictionTick = -1L
    private var predictionTick = 0L

    // One fall simulation per tick, shared by the trigger, the search and the ladder planner
    private fun currentPrediction(player: LocalPlayer, world: ClientLevel): FallTrace {
        if (cachedPredictionTick != predictionTick || cachedPrediction == null) {
            val options = Minecraft.getInstance().options
            cachedPrediction = FallTracer.predict(
                player, world, FALL_TRACE_TICKS,
                forward = isPhysicalKeyDown(options.keyUp),
                backward = isPhysicalKeyDown(options.keyDown),
                left = isPhysicalKeyDown(options.keyLeft),
                right = isPhysicalKeyDown(options.keyRight),
                jump = isPhysicalKeyDown(options.keyJump),
                movementYaw = referenceYaw(),
            )
            cachedPredictionTick = predictionTick
        }
        return cachedPrediction!!
    }

    private fun updateDamageWindow(player: LocalPlayer, world: ClientLevel) {
        predictionTick++
        trackExpectedFallDamage(player)

        if (player.hurtTime > previousHurtTime && !isEnvironmentalDamage(player, world)) {
            damageTicksRemaining = damageWindowTicks.value
        } else if (damageTicksRemaining > 0) {
            damageTicksRemaining--
        }
        previousHurtTime = player.hurtTime
    }

    /**
     * A landing is known a good while before its damage arrives, since the hurt animation has to
     * come back from the server. Arming the expectation on touchdown and spending it on the next
     * hurt tick is what keeps a long drop from being read as a hit.
     */
    private fun trackExpectedFallDamage(player: LocalPlayer) {
        if (player.onGround() && previousFallDistance > FALL_DAMAGE_THRESHOLD) {
            pendingFallDamageTicks = FALL_DAMAGE_GRACE_TICKS
        } else if (pendingFallDamageTicks > 0) {
            pendingFallDamageTicks--
        }
        previousFallDistance = player.fallDistance
    }

    /**
     * There is no damage source on the client for a plain hurt animation, so the causes we can
     * still see for ourselves are ruled out and everything left is treated as a hit.
     */
    private fun isEnvironmentalDamage(player: LocalPlayer, world: ClientLevel): Boolean {
        if (pendingFallDamageTicks > 0) {
            // Spent, so a real hit landing right after the drop is not swallowed too.
            pendingFallDamageTicks = 0
            return true
        }
        if (player.isOnFire || player.isInLava) return true
        if (player.airSupply <= 0) return true
        if (player.isFullyFrozen) return true
        if (player.foodData.foodLevel <= 0) return true
        if (player.y < world.minY) return true
        return touchingHurtingBlock(player, world)
    }

    private fun touchingHurtingBlock(player: LocalPlayer, world: ClientLevel): Boolean {
        val box = player.boundingBox.inflate(0.01)
        val minX = floor(box.minX).toInt()
        val minY = floor(box.minY - 0.1).toInt()
        val minZ = floor(box.minZ).toInt()
        val maxX = floor(box.maxX).toInt()
        val maxY = floor(box.maxY).toInt()
        val maxZ = floor(box.maxZ).toInt()

        for (x in minX..maxX) {
            for (y in minY..maxY) {
                for (z in minZ..maxZ) {
                    val block = world.getBlockState(BlockPos(x, y, z)).block
                    if (block is CactusBlock ||
                        block is SweetBerryBushBlock ||
                        block is MagmaBlock ||
                        block is PowderSnowBlock ||
                        block is WitherRoseBlock
                    ) {
                        return true
                    }
                }
            }
        }
        return false
    }

    // Repeated jumps queue a staircase, letting the search catch the player a level higher.
    private fun tickStaircase(client: Minecraft) {
        if (!allowStaircaseUp.value) {
            previousJumpHeld = false
            staircaseArmed = false
            return
        }
        val jumpHeld = isPhysicalKeyDown(client.options.keyJump)
        val now = System.currentTimeMillis()
        if (jumpHeld && !previousJumpHeld) {
            staircaseArmed = now - lastJumpReleaseMs <= STAIRCASE_WINDOW_MS
        } else if (!jumpHeld && previousJumpHeld) {
            lastJumpReleaseMs = now
        }
        previousJumpHeld = jumpHeld
    }

    private fun resolveTarget(player: LocalPlayer, world: ClientLevel, reach: Double): BlockPlacement? {
        dropStaleTargets(player, world)
        if (pendingTargets.isEmpty()) {
            val path = planPath(player, world, reach) ?: return null
            if (limitBlocks.value && path.size > maxBlocks.value) {
                fail("Needs ${path.size} blocks, limit is ${maxBlocks.value}")
                return null
            }
            pendingTargets.addAll(path)
            dropStaleTargets(player, world)
        }
        return pendingTargets.firstOrNull()
    }

    private fun dropStaleTargets(player: LocalPlayer, world: ClientLevel) {
        while (true) {
            val target = pendingTargets.firstOrNull() ?: return
            val placed = target.fills
            if (!world.getBlockState(placed).canBeReplaced()) {
                pendingTargets.removeFirst()
                continue
            }
            if (!PlacementGeometry.spaceIsClear(world, player, placed)) {
                pendingTargets.removeFirst()
                placedBlocks += placed
                continue
            }
            // The block we are meant to click must exist, or nothing can be aimed at.
            if (!PlacementGeometry.isSolid(world, target.against)) {
                clearPath()
                return
            }
            return
        }
    }

    // Plans blocks first, and falls back to a ladder rescue when blocks cannot reach
    private fun planPath(player: LocalPlayer, world: ClientLevel, reach: Double): List<BlockPlacement>? {
        val prediction = currentPrediction(player, world)
        planBlockPath(player, world, reach, prediction)?.let {
            ladderPlan = null
            return it
        }
        if (!autoLadder.value || findLadderSlot(player) == -1) {
            fail(lastPlanFailure ?: "No route to a safe block")
            return null
        }

        val planner = LadderRescuePlanner(world, player, reach, placedBlocks)
        val plan = planner.plan(prediction)
        if (plan == null) {
            fail(planner.failureReason ?: lastPlanFailure ?: "No route to a safe block")
            return null
        }
        if (limitBlocks.value && plan.pendingPlacementCount > maxBlocks.value) {
            fail("Ladder needs ${plan.pendingPlacementCount} blocks, limit is ${maxBlocks.value}")
            return null
        }
        ladderPlan = plan
        return plan.targets
    }

    // Finds a support to bridge from and a block to land on
    private fun planBlockPath(
        player: LocalPlayer,
        world: ClientLevel,
        reach: Double,
        prediction: FallTrace,
    ): List<BlockPlacement>? {
        val simulation = MotionSim(world, player, referenceYaw())
        val input = currentInput()
        simulation.setInput(input.forward, input.backward, input.left, input.right, input.jump)

        val estimatedTicks = ticksBackToGround(player)
        val motionX = player.deltaMovement.x
        val motionZ = player.deltaMovement.z
        val standingAboveBlock = PlacementGeometry.isSolid(
            world,
            BlockPos(floor(player.x).toInt(), floor(player.y - 0.015625).toInt(), floor(player.z).toInt()),
        )
        val initialRadius = if (standingAboveBlock) 1 else searchRadius.value
        val lowestYOffset = when {
            player.onGround() -> -2
            player.deltaMovement.y > 0.0 -> -3
            else -> -1
        }

        val supports = LinkedHashMap<BlockPos, MutableSet<BlockPos>>()
        val rejected = HashSet<BlockPos>()
        var simulatedTicks = 0

        for (tick in 0..maxOf(estimatedTicks, MIN_SEARCH_TICKS)) {
            simulatedTicks++
            val feetY = simulation.y
            val baseX = floor(simulation.x).toInt()
            val baseY = floor(simulation.y).toInt()
            val baseZ = floor(simulation.z).toInt()

            for (yOffset in 0 downTo lowestYOffset) {
                for (radius in 0 until searchRadius.value) {
                    // The ring is biased along the way the player is moving.
                    val minX = if (motionX >= 0.0) -initialRadius else -radius
                    val maxX = if (motionX >= 0.0) radius else initialRadius
                    val minZ = if (motionZ >= 0.0) -initialRadius else -radius
                    val maxZ = if (motionZ >= 0.0) radius else initialRadius
                    for (xOffset in minX..maxX) {
                        for (zOffset in minZ..maxZ) {
                            if (abs(xOffset) != radius && abs(zOffset) != radius) continue
                            val candidate = BlockPos(baseX + xOffset, baseY + yOffset, baseZ + zOffset)
                            if (candidate in rejected || candidate in supports) continue
                            // A support has to sit at or below the feet, and has to
                            // be something that already exists and can be clicked.
                            if (candidate.y + 1 > feetY) continue
                            val state = world.getBlockState(candidate)
                            if (!PlacementGeometry.isSolid(world, candidate) ||
                                PlacementGeometry.isInteractive(state)
                            ) {
                                rejected += candidate
                                continue
                            }
                            supports[candidate] = HashSet()
                        }
                    }
                }
            }

            simulation.tick()

            if (simulation.motionY <= 0.0) {
                val landingY = floor(simulation.y).toInt() - 1
                val landing = BlockPos(floor(simulation.x).toInt(), landingY, floor(simulation.z).toInt())
                for ((support, landings) in supports) {
                    val validHeight = if (staircaseArmed) landingY >= support.y else landingY == support.y
                    val distance = abs(support.x - landing.x) + abs(support.z - landing.z) +
                        abs(support.y - landing.y) - 1
                    if (validHeight && support != landing && distance <= simulatedTicks) landings += landing
                }
            }
            if (simulation.y <= world.minY) break
        }

        val pairs = mutableListOf<Pair<BlockPos, BlockPos>>()
        for ((support, landings) in supports) {
            val landing = landings.firstOrNull { canStartBridge(world, support, it) } ?: continue
            pairs += support to landing
        }
        if (pairs.isEmpty()) return null
        pairs.sortBy { (support, landing) -> bridgeCost(player, support, landing) }

        val strategy = BridgeRules(
            level = world,
            player = player,
            candidatePositions = prediction.eyePositions,
            placedBlocks = placedBlocks,
            depthLimit = searchDepth.value,
        )
        val planner = BridgePlanner(strategy)
        var attempts = 0
        lastPlanFailure = null

        for ((support, landing) in pairs) {
            val path = planner.findPath(support, landing)
            if (path == null || path.isEmpty()) continue
            if (limitBlocks.value && path.size > maxBlocks.value) continue
            if (!validatePaths.value) return path

            // Only worth simulating a handful; each run is a full fall.
            if (attempts++ >= MAX_VALIDATION_ATTEMPTS) break
            val validation = PlanRehearsal.validate(
                world, player, path, landing, reach, FALL_TRACE_TICKS, input, referenceYaw(),
            )
            if (validation.landed) return path
            lastPlanFailure = validation.failureReason
        }
        return null
    }

    // Remembers the height of the last ground the player was resting on
    private fun updateFallTarget(player: LocalPlayer) {
        if (player.onGround() && player.deltaMovement.y == RESTING_FALL_MOTION) {
            groundLevelY = player.y
        }
    }

    // How long until the player drops back to the height they set out from
    private fun ticksBackToGround(player: LocalPlayer): Int {
        var ticks = 0
        var estimatedY = player.y
        var motionY = player.deltaMovement.y
        val distantTarget = abs(groundLevelY - player.y) > 1.0
        while (ticks < 20) {
            estimatedY += motionY
            if (!distantTarget && estimatedY < groundLevelY) break
            motionY = (motionY - 0.08) * 0.98
            ticks++
        }
        return ticks
    }

    // Whether the first step out of the support toward the landing can be built
    private fun canStartBridge(world: ClientLevel, support: BlockPos, landing: BlockPos): Boolean {
        val deltaX = landing.x - support.x
        val deltaY = landing.y - support.y
        val deltaZ = landing.z - support.z
        val facings = listOfNotNull(
            if (deltaX > 0) Direction.EAST else if (deltaX < 0) Direction.WEST else null,
            if (deltaY > 0) Direction.UP else if (deltaY < 0) Direction.DOWN else null,
            if (deltaZ > 0) Direction.SOUTH else if (deltaZ < 0) Direction.NORTH else null,
        )
        return facings.any { facing ->
            val placed = support.relative(facing)
            placed !in placedBlocks && world.getBlockState(placed).canBeReplaced()
        }
    }

    // Short bridges win, height gain wins, and supports level with the ground left behind win heavily.
    private fun bridgeCost(player: LocalPlayer, support: BlockPos, landing: BlockPos): Double {
        val horizontalDistance = abs(support.x - landing.x) + abs(support.z - landing.z)
        var cost = horizontalDistance * 100.0
        if (landing.y > support.y) cost -= (landing.y - support.y) * 200.0
        if (clutching) {
            cost += hypot(support.x + 0.5 - player.x, support.z + 0.5 - player.z) * 1000.0
        }
        return cost + abs((support.y + 1) - groundLevelY) * 200.0
    }

    private fun wouldStepOff(player: LocalPlayer, world: ClientLevel): Boolean {
        val input = currentInput()
        return FallTracer.wouldStepOff(
            player, world, input.forward, input.backward, input.left, input.right, input.jump,
            referenceYaw(),
        )
    }

    // Yaw the player's keys are meant to move them along, which is not the yaw the clutch is aiming at.
    private fun referenceYaw(): Float =
        if (savedYaw.isNaN()) RotationManager.getClientYaw() else savedYaw

    private fun currentInput(): MotionInput {
        val options = Minecraft.getInstance().options
        return MotionInput(
            forward = isPhysicalKeyDown(options.keyUp),
            backward = isPhysicalKeyDown(options.keyDown),
            left = isPhysicalKeyDown(options.keyLeft),
            right = isPhysicalKeyDown(options.keyRight),
            jump = isPhysicalKeyDown(options.keyJump),
        )
    }

    // The block position we want filled so the player has something to land on
    private fun hitMatchesTarget(hit: BlockHitResult, target: BlockPlacement): Boolean {
        if (hit.blockPos == target.against) {
            val required = if (target.offsetFromFace) target.facing else null
            return required == null || required == hit.direction
        }
        return hit.blockPos.relative(hit.direction) == target.fills
    }

    private fun tickLadderCatch(player: LocalPlayer, world: ClientLevel) {
        val plan = ladderPlan
        if (plan == null) {
            ladderAssistActive = false
            return
        }
        if (++ladderAssistTicks > LADDER_ASSIST_TICKS) {
            fail("Ladder rescue timed out")
            finishClutch(Minecraft.getInstance())
            return
        }
        val onLadder = player.onClimbable()
        val fallDistance = player.fallDistance
        if (onLadder && (fallDistance <= 0.5 || fallDistance + 0.05 < ladderFallDistanceBeforeTick)) {
            finishClutch(Minecraft.getInstance())
            return
        }
        ladderFallDistanceBeforeTick = fallDistance

        if (!onLadder && (player.y < plan.ladderBlock.y - 0.5 || player.y > plan.ladderBlock.y + 3.0)) {
            fail("Missed the ladder")
            finishClutch(Minecraft.getInstance())
            return
        }
        if (player.y <= world.minY) {
            fail("Fell below the world")
            finishClutch(Minecraft.getInstance())
            return
        }
        steerToward(Minecraft.getInstance(), player, plan.catchX, plan.catchZ)
    }

    // Presses whichever movement keys close the gap to the ladder cell
    private fun steerToward(client: Minecraft, player: LocalPlayer, targetX: Double, targetZ: Double) {
        val options = client.options
        val deltaX = targetX - player.x
        val deltaZ = targetZ - player.z
        if (abs(deltaX) < CENTERING_TOLERANCE && abs(deltaZ) < CENTERING_TOLERANCE) {
            releaseMovement(client)
            return
        }
        val yawRadians = Math.toRadians(player.yRot.toDouble())
        val forwardX = -Math.sin(yawRadians)
        val forwardZ = Math.cos(yawRadians)
        val forward = deltaX * forwardX + deltaZ * forwardZ
        val strafe = deltaX * forwardZ - deltaZ * forwardX

        options.keyUp.setDown(forward > CENTERING_TOLERANCE)
        options.keyDown.setDown(forward < -CENTERING_TOLERANCE)
        options.keyLeft.setDown(strafe > CENTERING_TOLERANCE)
        options.keyRight.setDown(strafe < -CENTERING_TOLERANCE)
        controlsSuppressed = true
    }

    private fun isValidBlockStack(stack: ItemStack): Boolean {
        if (stack.isEmpty || stack.item !is BlockItem) return false
        val block = (stack.item as BlockItem).block
        val world = Minecraft.getInstance().level ?: return false
        if (!block.defaultBlockState().isCollisionShapeFullBlock(world, BlockPos.ZERO)) return false
        if (blacklist.value && blacklistBlocks.value.isNotEmpty()) {
            if (blockMatches(blockId(stack), blacklistBlocks.value)) return false
        }
        return true
    }

    private fun isLadderStack(stack: ItemStack): Boolean =
        !stack.isEmpty && (stack.item as? BlockItem)?.block === Blocks.LADDER

    private fun blockId(stack: ItemStack): String =
        (stack.item as? BlockItem)?.let { BuiltInRegistries.BLOCK.getKey(it.block).toString().lowercase() } ?: ""

    // Best hotbar slot for clutch blocks, or -1 when nothing usable is held
    private fun findBlockSlot(player: LocalPlayer): Int {
        if (heldWhitelist.value) {
            val held = player.mainHandItem
            if (!isValidBlockStack(held)) return -1
            if (autoLadder.value && isLadderStack(held)) return -1
            if (!blockMatches(blockId(held), whitelistBlocks.value)) return -1
            return player.inventory.selectedSlot
        }
        var fallback = -1
        for (slot in 0..8) {
            val stack = player.inventory.getItem(slot)
            if (!isValidBlockStack(stack)) continue
            if (autoLadder.value && isLadderStack(stack)) continue
            val id = blockId(stack)
            if (PREFERRED_BLOCK_NAMES.any { id.contains(it) }) return slot
            if (fallback == -1) fallback = slot
        }
        return fallback
    }

    private fun findLadderSlot(player: LocalPlayer): Int =
        (0..8).firstOrNull { isLadderStack(player.inventory.getItem(it)) } ?: -1

    private fun selectSlotFor(player: LocalPlayer, target: BlockPlacement): Boolean {
        val slot = if (target.isLadder) findLadderSlot(player) else findBlockSlot(player)
        if (slot == -1) return false
        if (previousSlot == -1) previousSlot = player.inventory.selectedSlot
        if (player.inventory.selectedSlot != slot) player.inventory.setSelectedSlot(slot)
        return true
    }

    private fun countBlocks(player: LocalPlayer): Int {
        if (heldWhitelist.value) {
            val held = player.mainHandItem
            if (!isValidBlockStack(held) || !blockMatches(blockId(held), whitelistBlocks.value)) return 0
            return held.count
        }
        return (0..8).sumOf { slot ->
            val stack = player.inventory.getItem(slot)
            if (isValidBlockStack(stack) && !(autoLadder.value && isLadderStack(stack))) stack.count else 0
        }
    }

    private fun beginClutch(player: LocalPlayer) {
        clutching = true
        armFailNotice()
        if (savedYaw.isNaN()) {
            savedYaw = RotationManager.getClientYaw()
            savedPitch = player.xRot
        }
        resetAngleDelayTicks = -1
        restoringViewTicks = -1
        moveDelayTicks = -1
        returnDelayTicks = -1
    }

    // Starts the delayed slot restore, angle reset and movement freeze
    private fun finishClutch(client: Minecraft) {
        clutching = false
        settledTicks = 0
        isActivelyPlacing = false
        ladderPlan = null
        ladderAssistActive = false
        clearPath()
        returnDelayTicks = randomIn(returnDelay.value)
        resetAngleDelayTicks = randomIn(resetAngleDelay.value)
        moveDelayTicks = randomIn(clutchMoveDelay.value)
        releaseClutchControl(client)
    }

    // Runs the post-clutch delays: movement freeze, slot restore, angle reset
    private fun tickAftermath(client: Minecraft, player: LocalPlayer) {
        if (clutching) return

        if (moveDelayTicks >= 0) {
            if (moveDelayTicks-- == 0) {
                releaseMovement(client)
            } else {
                suppressMovement(client)
            }
        }
        if (returnToLastSlot.value && previousSlot != -1 && returnDelayTicks-- <= 0) {
            player.inventory.setSelectedSlot(previousSlot)
            previousSlot = -1
        }
        if (resetAngleDelayTicks >= 0 && resetAngleDelayTicks-- == 0) {
            restoringViewTicks = RESTORE_VIEW_TICKS
        }
        if (restoringViewTicks >= 0) {
            // Keep turning until the angle is actually reached. Stepping once left the
            // claim and the perspective flags hanging when one step could not cover it.
            if (restoreView() || restoringViewTicks-- == 0) {
                restoringViewTicks = -1
                finishRestore(client)
            }
        }
    }

    // Turns back toward the angle held before the clutch. True once it has arrived.
    private fun restoreView(): Boolean {
        if (!resetAngle.value || savedYaw.isNaN()) return true
        RotationManager.movementMode = RotationManager.MovementMode.CLIENT
        RotationManager.rotationMode = RotationManager.RotationMode.CLIENT
        RotationManager.perspective = true
        RotationManager.setTargetRotation(
            savedYaw,
            savedPitch,
            ROTATION_OWNER,
            handlesMovementCorrection = true,
        )
        ownsRotation = true
        RotationManager.quickTick(speed.value)
        val yawGap = abs(net.minecraft.util.Mth.wrapDegrees(RotationManager.getCurrentYaw() - savedYaw))
        val pitchGap = abs(RotationManager.getCurrentPitch() - savedPitch)
        return yawGap < VIEW_RESTORED_DEGREES && pitchGap < VIEW_RESTORED_DEGREES
    }

    private fun finishRestore(client: Minecraft) {
        savedYaw = Float.NaN
        savedPitch = Float.NaN
        releaseClutchControl(client)
    }

    private fun primePlacementRotation(yaw: Float, pitch: Float) {
        RotationManager.movementMode = RotationManager.MovementMode.CLIENT
        RotationManager.rotationMode = RotationManager.RotationMode.CLIENT
        RotationManager.perspective = true
        RotationManager.setTargetRotation(
            yaw,
            pitch,
            ROTATION_OWNER,
            handlesMovementCorrection = true,
        )
        ownsRotation = true
        RotationManager.quickTick(speed.value)
        RotationManager.physicsYawOverride = RotationManager.getCurrentYaw()
        RotationManager.skipPositionSnap = true
    }

    private fun releaseClutchControl(client: Minecraft) {
        isActivelyPlacing = false
        if (ownsRotation || RotationManager.ownsRotation(ROTATION_OWNER)) {
            RotationManager.clearRotation(ROTATION_OWNER)
        }
        ownsRotation = false
        releaseMovement(client)
    }

    // A tick that cannot place anything must not drop the rotation
    private fun holdOrRelease(client: Minecraft) {
        if (!clutching) {
            releaseClutchControl(client)
            return
        }
        applyMovementCorrection(client)
        if (ownsRotation) RotationManager.quickTick(speed.value)
    }

    private fun suppressMovement(client: Minecraft) {
        val options = client.options
        options.keyUp.setDown(false)
        options.keyDown.setDown(false)
        options.keyLeft.setDown(false)
        options.keyRight.setDown(false)
        controlsSuppressed = true
    }

    // Remaps the held keys so the player keeps travelling the way they were before the view turned.
    private fun applyMovementCorrection(client: Minecraft) {
        val options = client.options
        val forwardHeld = isPhysicalKeyDown(options.keyUp)
        val backwardHeld = isPhysicalKeyDown(options.keyDown)
        val leftHeld = isPhysicalKeyDown(options.keyLeft)
        val rightHeld = isPhysicalKeyDown(options.keyRight)
        if (!forwardHeld && !backwardHeld && !leftHeld && !rightHeld) {
            suppressMovement(client)
            return
        }

        val forwardInput = (if (forwardHeld) 1f else 0f) - (if (backwardHeld) 1f else 0f)
        val leftInput = (if (leftHeld) 1f else 0f) - (if (rightHeld) 1f else 0f)
        val referenceYaw = if (savedYaw.isNaN()) RotationManager.getClientYaw() else savedYaw
        val referenceRadians = Math.toRadians(referenceYaw.toDouble())
        val worldX = leftInput * cos(referenceRadians) - forwardInput * sin(referenceRadians)
        val worldZ = forwardInput * cos(referenceRadians) + leftInput * sin(referenceRadians)
        val intendedYaw = Math.toDegrees(atan2(-worldX, worldZ)).toFloat()

        val delta = Math.toRadians(
            net.minecraft.util.Mth.wrapDegrees(intendedYaw - RotationManager.getCurrentYaw()).toDouble(),
        )
        val forwardImpulse = cos(delta)
        val leftImpulse = -sin(delta)

        options.keyUp.setDown(forwardImpulse >= MOVEMENT_CORRECTION_THRESHOLD)
        options.keyDown.setDown(forwardImpulse <= -MOVEMENT_CORRECTION_THRESHOLD)
        options.keyLeft.setDown(leftImpulse >= MOVEMENT_CORRECTION_THRESHOLD)
        options.keyRight.setDown(leftImpulse <= -MOVEMENT_CORRECTION_THRESHOLD)
        controlsSuppressed = true
    }

    private fun releaseMovement(client: Minecraft) {
        if (!controlsSuppressed) return
        val options = client.options
        options.keyUp.setDown(isPhysicalKeyDown(options.keyUp))
        options.keyDown.setDown(isPhysicalKeyDown(options.keyDown))
        options.keyLeft.setDown(isPhysicalKeyDown(options.keyLeft))
        options.keyRight.setDown(isPhysicalKeyDown(options.keyRight))
        controlsSuppressed = false
    }

    // Backs off for failDelay before planning again, and queues the reason
    private fun fail(reason: String) {
        failUntilMs = System.currentTimeMillis() + failDelay.value
        queueFail(reason)
        clearPath()
        ladderPlan = null
    }

    // Holds a failure back a couple of ticks
    private fun queueFail(message: String) {
        pendingFailMessage = message
        if (pendingFailTicks == 0) pendingFailTicks = (failDelay.value / 50).coerceIn(1, 3)
    }

    private fun clearQueuedFailure() {
        pendingFailMessage = null
        pendingFailTicks = 0
    }

    // A new fall is a new chance to report a failure
    private fun armFailNotice() {
        notifiedThisFall = false
        clearQueuedFailure()
    }

    // At most one notification per fall
    private fun tickFailNotice() {
        if (pendingFailTicks <= 0) return
        if (--pendingFailTicks > 0) return
        val message = pendingFailMessage ?: return
        pendingFailMessage = null
        if (notifiedThisFall) return
        notifiedThisFall = true
        NotificationManager.showError("Clutch failed", message, 3000L)
    }

    private fun clearPath() {
        pendingTargets.clear()
        placedBlocks.clear()
    }

    override fun hudInfo(): String {
        if (!showBlockCount.value) return ""
        val player = Minecraft.getInstance().player ?: return ""
        val remaining = if (pendingTargets.isEmpty()) countBlocks(player) else pendingTargets.size
        return remaining.toString()
    }

    override fun onDisabled() {
        resetState(Minecraft.getInstance(), restoreSlot = true)
    }

    // A respawn, dimension change or teleport leaves every in-flight plan pointing at
    // blocks that are no longer under us, so none of it is worth keeping.
    private fun playerJumped(player: LocalPlayer): Boolean {
        val samePlayer = lastPlayer === player
        val previous = lastPosition
        lastPlayer = player
        lastPosition = player.position()
        if (!samePlayer || previous == null) return true
        return previous.distanceToSqr(player.position()) > TELEPORT_DISTANCE_SQR
    }

    private fun resetState(client: Minecraft, restoreSlot: Boolean) {
        val player = client.player
        clutching = false
        ladderPlan = null
        ladderAssistActive = false
        ladderAssistTicks = 0
        damageTicksRemaining = 0
        previousHurtTime = 0
        previousFallDistance = 0.0
        pendingFallDamageTicks = 0
        isActivelyPlacing = false
        staircaseArmed = false
        settledTicks = 0
        failUntilMs = 0L
        armFailNotice()
        clearPath()
        if (restoreSlot && previousSlot != -1 && player != null && returnToLastSlot.value) {
            player.inventory.setSelectedSlot(previousSlot)
        }
        previousSlot = -1
        savedYaw = Float.NaN
        savedPitch = Float.NaN
        returnDelayTicks = -1
        resetAngleDelayTicks = -1
        restoringViewTicks = -1
        moveDelayTicks = -1
        groundLevelY = player?.y ?: 0.0
        releaseClutchControl(client)
    }

    private fun randomIn(range: Pair<Int, Int>): Int {
        val (low, high) = range
        return if (high > low) (low..high).random() else low
    }

    private fun eyePosition(player: LocalPlayer): Vec3 =
        Vec3(player.x, player.y + player.eyeHeight, player.z)

    private fun rayTraceAt(player: LocalPlayer, reach: Double, yaw: Float, pitch: Float): BlockHitResult? {
        val savedY = player.yRot
        val savedP = player.xRot
        player.yRot = yaw
        player.xRot = pitch
        val hitResult = player.pick(reach, 1.0f, false)
        player.yRot = savedY
        player.xRot = savedP
        return hitResult as? BlockHitResult
    }

    private fun blockMatches(blockName: String, entries: List<String>): Boolean {
        if (entries.isEmpty()) return false
        for (entry in entries) {
            if (entry.endsWith("_category")) {
                val categoryId = entry.removeSuffix("_category")
                val category = itemCategories.firstOrNull { it.id == categoryId }
                if (category != null && category.matches(blockName)) return true
            } else if (blockName.contains(entry.lowercase())) {
                return true
            }
        }
        return false
    }

    private const val MIN_DROP_TO_CATCH = 1.2
    private const val MAX_VALIDATION_ATTEMPTS = 4
    private const val GROUND_SETTLE_TICKS = 5
    private const val MOVEMENT_CORRECTION_THRESHOLD = 0.45
    private const val MIN_SEARCH_TICKS = 15
    private const val RESTING_FALL_MOTION = -0.0784000015258789
    private const val RESTORE_VIEW_TICKS = 40
    private const val LADDER_ASSIST_TICKS = 60
    private const val TELEPORT_DISTANCE_SQR = 64.0
    private const val VIEW_RESTORED_DEGREES = 1f
    private const val STAIRCASE_WINDOW_MS = 500L
    private const val CENTERING_TOLERANCE = 0.08
    private const val FALL_DAMAGE_THRESHOLD = 3.2
    private const val FALL_DAMAGE_GRACE_TICKS = 20
}
