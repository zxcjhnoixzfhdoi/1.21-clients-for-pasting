package onl.luka.grizzly.module.modules.world.scaffold

import com.mojang.blaze3d.platform.InputConstants
import onl.luka.grizzly.module.Module
import onl.luka.grizzly.config.entry.ItemListEntry
import onl.luka.grizzly.gui.helpers.itemCategories
import onl.luka.grizzly.mixin.client.LocalPlayerAccessor
import onl.luka.grizzly.module.modules.movement.InvMove
import onl.luka.grizzly.util.InputUtil
import onl.luka.grizzly.util.RotationManager
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.player.LocalPlayer
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.util.Mth
import net.minecraft.world.InteractionHand
import net.minecraft.world.item.BlockItem
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.sqrt

object Scaffold : Module("Scaffold", "Automatically places blocks under you while walking", Category.WORLD) {

    internal const val ROTATION_OWNER = "scaffold"
    private const val CONTEXT_BREAK_DISTANCE_SQR = 9.0

    enum class BridgeMode {
        NINJA, BREEZILY, NORMAL, TELLY, NCP
    }

    enum class ScaffoldState {
        TOWERING, UPSTACKING, DIAGONAL, STRAIGHT, NONE
    }

    enum class TellyMode {
        NORMAL, NON_UPWARDS
    }

    enum class TellyRotationMode {
        MATH, SIMPLE, BACKWARDS
    }

    val blockWhitelist = itemList("Block Whitelist", listOf("wool_category"), defaultMode = ItemListEntry.Mode.WHITELIST, filter = ItemListEntry.Filter.BLOCKS_ONLY)
    val bridgeMode = enum("mode", BridgeMode.NINJA)


    private val tellyOnly: () -> Boolean = { bridgeMode.value == BridgeMode.TELLY }
    private val ncpOnly: () -> Boolean = { bridgeMode.value == BridgeMode.NCP }

    val ncpExtend = int("ncp extend", 0, 0, 5).also { it.visibleWhen = ncpOnly }
    val ncpPlaceDelay = int("ncp place delay", 30, 0, 200).also { it.visibleWhen = ncpOnly }
    val ncpSwing = boolean("ncp swing", true).also { it.visibleWhen = ncpOnly }
    val ncpSafety = boolean("ncp safety", true).also { it.visibleWhen = ncpOnly }
    val ncpRestoreSlot = boolean("ncp restore slot", true).also { it.visibleWhen = ncpOnly }

    val rotSpeed = float("rot speed", 60f, 0f, 180f).also { it.visibleWhen = tellyOnly }
    val minRotSpeed = float("min rot speed", 40f, 0f, 180f).also { it.visibleWhen = tellyOnly }
    val tellyMode = enum("telly mode", TellyMode.NORMAL).also { it.visibleWhen = tellyOnly }
    val tellyRotationMode = enum("rotation mode", TellyRotationMode.MATH).also { it.visibleWhen = tellyOnly }
    val searchRange = int("search range", 2, 0, 5).also { it.visibleWhen = tellyOnly }
    val angleTolerance = float("angle tolerance", 12f, 0f, 45f).also { it.visibleWhen = tellyOnly }
    val strafeCorrect = boolean("strafe correct", true).also { it.visibleWhen = tellyOnly }
    val keepY = boolean("keep y", true).also { it.visibleWhen = tellyOnly }
    val tellyDown = boolean("down", true).also { it.visibleWhen = tellyOnly }
    val noSprint = boolean("no sprint", false).also { it.visibleWhen = tellyOnly }
    val intelligentPicker = boolean("intelligent picker", true).also {
        it.visibleWhen = { tellyOnly() || ncpOnly() }
    }

    private val crouchDelay = intRange("crouch delay", 45 to 55, 0, 500).also {
        it.visibleWhen = { bridgeMode.value == BridgeMode.NINJA }
    }
    private val autoclickCps = intRange("autoclick cps", 4 to 6, 1, 6)
    val disableOnDeath = boolean("disable on death", true)
    val disableOnWorldChange = boolean("disable on world change", true)

    private var isCrouching = false
    private var crouchWaitTicks = 0

    private var autoclickAccum = 0.0f
    private var autoclickTargetCps = 0
    var ownsRotation = false
    private var ninjaStrafeRight = true
    private var breezilyStrafeRight = true
    private var diagonalBreezilyStrafeTicks = 0
    private var diagonalBreezilyStrafeRight = true
    private var diagonalBreezilyAligned = false
    private var normalMovementLine: MovementLine? = null
    private var normalLineYaw = Float.NaN
    private var normalUpstackCycle = false
    private val normalPlacedBlocks = ArrayDeque<BlockPos>(4)
    private var trackedPlayer: LocalPlayer? = null
    private var trackedLevel: ClientLevel? = null
    private var trackedPosition: Vec3? = null

    private var telly: Telly? = null
    private var lastBridgeMode = bridgeMode.value

    private data class Placement(
        val placePos: BlockPos,
        val neighbor: BlockPos,
        val face: Direction,
        val score: Double,
        val hitVec: Vec3? = null,
        val rotation: Pair<Float, Float>? = null,
    )

    private data class MovementCandidate(
        val forward: Float,
        val strafe: Float,
    )

    private data class DirectionalKeys(
        val forward: Boolean,
        val backward: Boolean,
        val left: Boolean,
        val right: Boolean,
    )

    private data class MovementLine(
        val anchor: Vec3,
        val direction: Vec3,
    ) {
        fun nearestHorizontalPoint(point: Vec3): Vec3 {
            val dx = point.x - anchor.x
            val dz = point.z - anchor.z
            val denominator = direction.x * direction.x + direction.z * direction.z
            if (denominator < 1.0e-8) return Vec3(anchor.x, point.y, anchor.z)
            val distance = (dx * direction.x + dz * direction.z) / denominator
            return Vec3(
                anchor.x + direction.x * distance,
                point.y,
                anchor.z + direction.z * distance,
            )
        }

        fun horizontalDistanceToSqr(point: Vec3): Double =
            nearestHorizontalPoint(point).let { nearest ->
                val dx = point.x - nearest.x
                val dz = point.z - nearest.z
                dx * dx + dz * dz
            }
    }

    private fun findBlockSlot(player: LocalPlayer): Int {
        val world = Minecraft.getInstance().level ?: return -1
        for (i in 0..8) {
            val stack = player.inventory.getItem(i)
            if (stack.isEmpty || stack.item !is BlockItem) continue
            val block = (stack.item as BlockItem).block
            if (!block.defaultBlockState().isCollisionShapeFullBlock(world, BlockPos.ZERO)) continue
            if (blockWhitelist.value.isNotEmpty()) {
                val blockId = BuiltInRegistries.BLOCK.getKey(block).toString().lowercase()
                if (!blockMatchesWhitelist(blockId, blockWhitelist.value)) continue
            }
            return i
        }
        return -1
    }

    private fun hasBlocks(player: LocalPlayer): Boolean = findBlockSlot(player) != -1

    private fun activeScaffoldPlayer(client: Minecraft): LocalPlayer? {
        val player = client.player ?: return null
        if (client.level == null || client.gameMode == null) return null
        if (!player.isAlive || player.isRemoved || !player.abilities.mayBuild) return null
        if (!hasBlocks(player)) return null
        return player
    }

    private fun scaffoldContextChanged(client: Minecraft, player: LocalPlayer): Boolean {
        val level = client.level ?: return true
        val position = player.position()
        val previousPosition = trackedPosition
        val changed = (trackedPlayer != null && trackedPlayer !== player) ||
                (trackedLevel != null && trackedLevel !== level) ||
                (previousPosition != null &&
                        previousPosition.distanceToSqr(position) > CONTEXT_BREAK_DISTANCE_SQR)

        trackedPlayer = player
        trackedLevel = level
        trackedPosition = position
        return changed
    }

    private fun clearTrackedContext() {
        trackedPlayer = null
        trackedLevel = null
        trackedPosition = null
    }

    private fun suspendScaffold(client: Minecraft, restoreMovement: Boolean = true) {
        if (bridgeMode.value == BridgeMode.TELLY) telly?.onDisabled()
        if (bridgeMode.value == BridgeMode.NCP) Ncp.onDisabled()
        releaseRotation()
        if (restoreMovement) {
            restorePhysicalKeys(client)
        } else if (!InvMove.controlsCurrentScreen(client)) {
            releaseMovementKeys(client)
        }
        resetTechniqueState()
        clearTrackedContext()
    }

    internal fun suspendForOpenScreen(client: Minecraft) {
        suspendScaffold(client, restoreMovement = false)
    }

    init {
        telly = Telly
        bridgeMode.onChange { newMode ->
            if (lastBridgeMode == BridgeMode.TELLY && newMode != BridgeMode.TELLY) {
                telly?.onDisabled()
            }
            if (lastBridgeMode == BridgeMode.NCP && newMode != BridgeMode.NCP) {
                Ncp.onDisabled()
            }
            releaseRotation()
            clearTrackedContext()
            resetTechniqueState()
            lastBridgeMode = newMode
        }

        ClientTickEvents.START_CLIENT_TICK.register { client ->
            if (!isEnabled()) {
                releaseRotation()
                clearTrackedContext()
                return@register
            }
            if (client.gui.screen() != null && !InvMove.allowsMovementInCurrentScreen(client)) {
                suspendForOpenScreen(client)
                return@register
            }
            // Telly runs its own tick and owns its rotation for the whole bridge. Letting the
            // shared context tracking below run would release that rotation mid-jump.
            if (bridgeMode.value == BridgeMode.TELLY || bridgeMode.value == BridgeMode.NCP) return@register

            val player = activeScaffoldPlayer(client)
            if (player == null || scaffoldContextChanged(client, player)) {
                suspendScaffold(client)
                return@register
            }

            if (bridgeMode.value == BridgeMode.NINJA || bridgeMode.value == BridgeMode.BREEZILY) {
                player.isSprinting = false
                player.setSprinting(false)
                (player as LocalPlayerAccessor).setSprintTriggerTime(0)
                client.options.keySprint.setDown(false)
            } else {
                client.options.keySprint.setDown(InputUtil.isPhysicalKeyDown(client.options.keySprint))
            }

            val W = InputUtil.isPhysicalKeyDown(client.options.keyUp)
            val S = InputUtil.isPhysicalKeyDown(client.options.keyDown)
            val A = InputUtil.isPhysicalKeyDown(client.options.keyLeft)
            val D = InputUtil.isPhysicalKeyDown(client.options.keyRight)
            val isJumping = InputUtil.isPhysicalKeyDown(client.options.keyJump)
            client.options.keyJump.setDown(isJumping)
            val movingHoriz = W || S || A || D

            var targetCard = RotationManager.getClientYaw()
            var rawMovementYaw = targetCard
            if (movingHoriz) {
                val camRad = Math.toRadians(targetCard.toDouble())
                var mx = 0.0; var mz = 0.0
                if (W) { mx -= sin(camRad); mz += cos(camRad)
                }
                if (S) { mx += sin(camRad); mz -= cos(camRad)
                }
                if (D) { mx -= cos(camRad); mz -= sin(camRad)
                }
                if (A) { mx += cos(camRad); mz += sin(camRad)
                }

                rawMovementYaw = Math.toDegrees(atan2(-mx, mz)).toFloat()
                targetCard = Math.round(rawMovementYaw / 45.0f) * 45.0f
            } else {
                targetCard = Math.round(RotationManager.getClientYaw() / 45.0f) * 45.0f
            }

            val isDiagonal = (targetCard % 90.0f) != 0.0f

            val scaffoldState = when {
                !movingHoriz && isJumping -> ScaffoldState.TOWERING
                movingHoriz && isJumping -> ScaffoldState.UPSTACKING
                movingHoriz && isDiagonal -> ScaffoldState.DIAGONAL
                movingHoriz && !isDiagonal -> ScaffoldState.STRAIGHT
                else -> ScaffoldState.NONE
            }
            normalUpstackCycle = when {
                bridgeMode.value != BridgeMode.NORMAL -> false
                scaffoldState == ScaffoldState.UPSTACKING -> true
                player.onGround() -> false
                else -> normalUpstackCycle
            }

            var pressRight = false
            var pressLeft  = false

            fun applyBreezilyBridge() {
                val camRad = Math.toRadians(RotationManager.getClientYaw().toDouble())
                if (D && !A) breezilyStrafeRight = true
                if (A && !D) breezilyStrafeRight = false
                breezilyStrafeRight = chooseBreezilyStrafeRight(player, camRad, invertCorrection = W && !S)
                pressRight = breezilyStrafeRight
                pressLeft  = !breezilyStrafeRight
                client.options.keyUp.setDown(false)
                client.options.keyDown.setDown(true)
                client.options.keyRight.setDown(pressRight)
                client.options.keyLeft.setDown(pressLeft)
                client.options.keyRight.setDown(pressRight)
            }

            fun restorePhysicalMovement() {
                pressRight = D
                pressLeft = A
                client.options.keyUp.setDown(W)
                client.options.keyDown.setDown(S)
                client.options.keyRight.setDown(D)
                client.options.keyLeft.setDown(A)
            }

            fun sidewaysBridge() {
                if (W && !S && !A && !D) {
                    val camRad = Math.toRadians(RotationManager.getClientYaw().toDouble())
                    ninjaStrafeRight = chooseNinjaStrafeRight(player, camRad)
                    var mx = -sin(camRad)
                    var mz = cos(camRad)
                    if (ninjaStrafeRight) {
                        mx -= cos(camRad)
                        mz -= sin(camRad)
                    } else {
                        mx += cos(camRad)
                        mz += sin(camRad)
                    }
                    val rawMoveYaw = Math.toDegrees(atan2(-mx, mz)).toFloat()
                    targetCard = Math.round(rawMoveYaw / 45.0f) * 45.0f

                    pressRight = ninjaStrafeRight
                    pressLeft = !ninjaStrafeRight
                    client.options.keyUp.setDown(false)
                    client.options.keyDown.setDown(true)
                    client.options.keyRight.setDown(pressRight)
                    client.options.keyLeft.setDown(pressLeft)
                } else {
                    pressRight = false
                    pressLeft = false
                    client.options.keyUp.setDown(false)
                    client.options.keyDown.setDown(true)
                    client.options.keyRight.setDown(false)
                    client.options.keyLeft.setDown(false)
                }
            }

            when (scaffoldState) {
                ScaffoldState.STRAIGHT -> {
                    resetDiagonalBreezilyCorrection()
                    when (bridgeMode.value) {
                        BridgeMode.NINJA -> {
                            sidewaysBridge()
                        }
                        BridgeMode.BREEZILY -> {
                            applyBreezilyBridge()
                        }
                        BridgeMode.NORMAL -> restorePhysicalMovement()

                        else -> {}
                    }
                }


                ScaffoldState.DIAGONAL -> {
                    when (bridgeMode.value) {
                        BridgeMode.NINJA -> {
                            resetDiagonalBreezilyCorrection()
                            pressRight = false
                            pressLeft = false
                            client.options.keyUp.setDown(false)
                            client.options.keyDown.setDown(true)
                            client.options.keyRight.setDown(false)
                            client.options.keyLeft.setDown(false)
                        }
                        BridgeMode.BREEZILY -> {
                            val correction = diagonalBreezilyCorrection(player, Mth.wrapDegrees(targetCard + 180f))
                            pressRight = correction == true
                            pressLeft = correction == false
                            client.options.keyUp.setDown(false)
                            client.options.keyDown.setDown(true)
                            client.options.keyRight.setDown(pressRight)
                            client.options.keyLeft.setDown(pressLeft)
                        }
                        BridgeMode.NORMAL -> restorePhysicalMovement()

                        else -> {}
                    }
                }


                ScaffoldState.UPSTACKING, ScaffoldState.TOWERING, ScaffoldState.NONE -> {
                    resetDiagonalBreezilyCorrection()
                    pressRight = false
                    pressLeft = false
                    if (bridgeMode.value == BridgeMode.NORMAL) {
                        restorePhysicalMovement()
                    } else if (scaffoldState == ScaffoldState.UPSTACKING) {
                        if (!isDiagonal) {
                            sidewaysBridge()
                        }
                        else {
                            client.options.keyUp.setDown(false)
                            client.options.keyDown.setDown(true)
                            client.options.keyRight.setDown(false)
                            client.options.keyLeft.setDown(false)
                        }
                    } else {
                        client.options.keyUp.setDown(false)
                        client.options.keyDown.setDown(false)
                        client.options.keyRight.setDown(false)
                        client.options.keyLeft.setDown(false)
                    }
                }
            }

            val level = client.level ?: return@register
            val hasSideKey = pressRight || pressLeft
            val basePlacementYaw = Mth.wrapDegrees(targetCard + 180f)
            val fastNormalUpstack = bridgeMode.value == BridgeMode.NORMAL &&
                    scaffoldState == ScaffoldState.UPSTACKING
            val normalLine = if (bridgeMode.value == BridgeMode.NORMAL && movingHoriz && !normalUpstackCycle) {
                updateNormalMovementLine(player, rawMovementYaw)
            } else {
                null
            }
            val techniquePlacement = when (bridgeMode.value) {
                BridgeMode.NORMAL -> if (fastNormalUpstack) {
                    findUpstackPlacement(player, level)
                } else {
                    findTechniquePlacement(player, level, stabilized = true, optimalLine = normalLine)
                }
                else -> null
            }
            val correctedNormalInput = if (bridgeMode.value == BridgeMode.NORMAL) {
                if (normalUpstackCycle) {
                    DirectionalKeys(W, S, A, D)
                } else {
                    stabilizeNormalMovement(
                        player,
                        normalLine,
                        DirectionalKeys(W, S, A, D),
                        isJumping,
                    )
                }
            } else {
                null
            }

            if (bridgeMode.value == BridgeMode.NORMAL && techniquePlacement == null) {
                releaseRotation()
                applyDirectionalKeys(client, correctedNormalInput ?: DirectionalKeys(W, S, A, D))
                return@register
            }

            val normalRotation = techniquePlacement
                ?.takeIf { bridgeMode.value == BridgeMode.NORMAL && !fastNormalUpstack }
                ?.let { rotationToPlacement(player, it) }
            val idleTechniqueRotation = techniquePlacement
                ?.takeIf {
                    !movingHoriz &&
                            bridgeMode.value == BridgeMode.BREEZILY
                }
                ?.let { placement ->
                    val targetYaw = rotationToPlacement(player, placement).first
                    Mth.wrapDegrees(floor(targetYaw / 90f) * 90f + 45f) to 75f
                }
            val aimYaw = normalRotation?.first ?: idleTechniqueRotation?.first ?: basePlacementYaw
            val aimPitch = normalRotation?.second ?: idleTechniqueRotation?.second ?: when (scaffoldState) {
                ScaffoldState.TOWERING -> 90.0f
                ScaffoldState.UPSTACKING -> correctedUpstackPitch(player, W, S, A, D)
                    //if (isDiagonal || hasSideKey) 75.6f else 78.5f

                ScaffoldState.DIAGONAL -> 75.6f
                ScaffoldState.STRAIGHT -> when (bridgeMode.value) {
                    BridgeMode.NINJA -> 78.0f
                    BridgeMode.BREEZILY -> 79.9f
                    else -> 0f
                }
                else -> if (isDiagonal || hasSideKey) 78.0f else 80f
            }

            RotationManager.movementMode = RotationManager.MovementMode.CLIENT
            RotationManager.rotationMode = RotationManager.RotationMode.CLIENT
            RotationManager.perspective = true
            RotationManager.setTargetRotation(
                aimYaw,
                aimPitch,
                ROTATION_OWNER,
                handlesMovementCorrection = true,
            )
            ownsRotation = true
            RotationManager.quickTick(
                if (bridgeMode.value == BridgeMode.BREEZILY) 180f else 60f,
            )

            if (bridgeMode.value == BridgeMode.NORMAL) {
                val movementKeys = correctedNormalInput ?: DirectionalKeys(W, S, A, D)
                applyMovementCorrection(
                    client,
                    movementKeys.forward,
                    movementKeys.backward,
                    movementKeys.left,
                    movementKeys.right,
                )
                RotationManager.physicsYawOverride = RotationManager.getCurrentYaw()
            }

            val stack = player.mainHandItem
            if (stack.isEmpty || stack.item !is BlockItem) {
                val slot = findBlockSlot(player)
                if (slot != -1) player.inventory.setSelectedSlot(slot)
            } else if (blockWhitelist.value.isNotEmpty()) {
                val blockId = BuiltInRegistries.BLOCK.getKey((stack.item as BlockItem).block).toString().lowercase()
                if (!blockMatchesWhitelist(blockId, blockWhitelist.value)) {
                    val slot = findBlockSlot(player)
                    if (slot != -1) player.inventory.setSelectedSlot(slot)
                }
            }

            val rayHit = currentPlacementHit(player, level)
            val placementHit = when (bridgeMode.value) {
                BridgeMode.NORMAL -> if (fastNormalUpstack) {
                    techniquePlacement?.toHitResult()
                } else {
                    rayHit?.takeIf { techniquePlacement?.matches(it) == true }
                }
                BridgeMode.BREEZILY ->
                    rayHit?.takeIf { isUsefulScaffoldHit(player, it) }
                BridgeMode.NINJA -> {
                    if (scaffoldState == ScaffoldState.UPSTACKING) {
                        findUpstackPlacement(player, level)?.toHitResult()
                    } else if (player.onGround() && (isDiagonal || rayHit?.direction?.axis?.isHorizontal == true)) {
                        rayHit
                    } else {
                        null
                    }
                }
                BridgeMode.TELLY, BridgeMode.NCP -> null
            }

            if (placementHit != null) {
                val result = client.gameMode?.useItemOn(player, InteractionHand.MAIN_HAND, placementHit)
                if (result?.consumesAction() == true) {
                    if (bridgeMode.value == BridgeMode.NORMAL) {
                        trackNormalPlacedBlock(placementHit.blockPos.relative(placementHit.direction))
                    }
                    player.swing(InteractionHand.MAIN_HAND)
                }
            } else if (bridgeMode.value == BridgeMode.NINJA) {
                if (autoclickTargetCps == 0) {
                    val (lo, hi) = autoclickCps.value
                    autoclickTargetCps = if (hi > lo) (lo..hi).random() else lo
                }
                autoclickAccum += autoclickTargetCps / 20.0f
                while (autoclickAccum >= 1.0f) {
                    KeyMapping.click(InputConstants.getKey(client.options.keyUse.saveString()))
                    autoclickAccum -= 1.0f
                    val (lo, hi) = autoclickCps.value
                    autoclickTargetCps = if (hi > lo) (lo..hi).random() else lo
                }
            }

            // crouch walk
            if (player.onGround()
                && (
                        bridgeMode.value == BridgeMode.NINJA &&
                                (scaffoldState == ScaffoldState.STRAIGHT ||
                                        scaffoldState == ScaffoldState.DIAGONAL ||
                                        scaffoldState == ScaffoldState.UPSTACKING)
                )) {
                val nearEdge = if (movingHoriz) {
                    isMovingTowardEdge(player, client.level!!, W, S, A, D)
                } else {
                    isNearEdge(player, client.level!!)
                }

                if (nearEdge && !isCrouching) {
                    isCrouching = true
                    val (lo, hi) = crouchDelay.value
                    val delayMs = if (hi > lo) (lo..hi).random() else lo
                    crouchWaitTicks = (delayMs / 50).coerceAtLeast(1)
                }

                if (isCrouching) {
                    client.options.keyShift.setDown(true)
                    player.setShiftKeyDown(true)
                    if (!nearEdge) crouchWaitTicks--
                    if (crouchWaitTicks <= 0 && !nearEdge) isCrouching = false
                } else {
                    val physicalShift = InputUtil.isPhysicalKeyDown(client.options.keyShift)
                    client.options.keyShift.setDown(physicalShift)
                    player.setShiftKeyDown(physicalShift)
                }
            }
            else {
                isCrouching = false
                crouchWaitTicks = 0
                val physicalShift = InputUtil.isPhysicalKeyDown(client.options.keyShift)
                client.options.keyShift.setDown(physicalShift)
                player.setShiftKeyDown(physicalShift)
            }
        }
    }

    private fun isNearEdge(player: LocalPlayer, world: ClientLevel): Boolean {
        val px = player.x
        val by = floor(player.y - 1.0).toInt()
        val pz = player.z

        val margin = 0.3
        val offsets = arrayOf(
            doubleArrayOf(0.0, 0.0),
            doubleArrayOf(-margin, 0.0),
            doubleArrayOf(margin, 0.0),
            doubleArrayOf(0.0, -margin),
            doubleArrayOf(0.0, margin)
        )

        for (offset in offsets) {
            val bx = floor(px + offset[0]).toInt()
            val bz = floor(pz + offset[1]).toInt()
            val state = world.getBlockState(BlockPos(bx, by, bz))
            if (state.isAir || !state.fluidState.isEmpty || !state.isCollisionShapeFullBlock(world,
                    BlockPos(bx, by, bz)
                )) {
                return true
            }
        }
        return false
    }

    private fun chooseNinjaStrafeRight(player: LocalPlayer, camRad: Double): Boolean {
        val sideOffset = lateralBlockOffset(player, camRad)

        return when {
            sideOffset > 0.08 -> true
            sideOffset < -0.08 -> false
            else -> ninjaStrafeRight
        }
    }

    private fun chooseBreezilyStrafeRight(
        player: LocalPlayer,
        camRad: Double,
        invertCorrection: Boolean,
    ): Boolean {
        val sideOffset = if (invertCorrection) {
            -lateralBlockOffset(player, camRad)
        } else {
            lateralBlockOffset(player, camRad)
        }

        return when {
            sideOffset > 0.18 -> false
            sideOffset < -0.18 -> true
            else -> breezilyStrafeRight
        }
    }

    private fun lateralBlockOffset(player: LocalPlayer, camRad: Double): Double {
        val localX = player.x - floor(player.x) - 0.5
        val localZ = player.z - floor(player.z) - 0.5
        val rightX = -cos(camRad)
        val rightZ = -sin(camRad)
        return localX * rightX + localZ * rightZ
    }

    private fun diagonalBreezilyCorrection(player: LocalPlayer, aimYaw: Float): Boolean? {
        if (diagonalBreezilyAligned) {
            return null
        }

        if (diagonalBreezilyStrafeTicks > 0) {
            diagonalBreezilyStrafeTicks--
            if (diagonalBreezilyStrafeTicks == 0) {
                diagonalBreezilyAligned = true
            }
            return diagonalBreezilyStrafeRight
        }

        val sideOffset = lateralBlockOffset(player, Math.toRadians(aimYaw.toDouble()))
        diagonalBreezilyStrafeRight = when {
            sideOffset > 0.08 -> false
            sideOffset < -0.08 -> true
            else -> {
                diagonalBreezilyAligned = true
                return null
            }
        }
        diagonalBreezilyStrafeTicks = 1
        return diagonalBreezilyStrafeRight
    }

    private fun resetDiagonalBreezilyCorrection() {
        diagonalBreezilyStrafeTicks = 0
        diagonalBreezilyAligned = false
    }

    private fun findTechniquePlacement(
        player: LocalPlayer,
        world: ClientLevel,
        stabilized: Boolean,
        optimalLine: MovementLine?,
    ): Placement? {
        val predictedPos = predictPlacementPosition(player, world, optimalLine)
        val predictedEye = predictedPos.add(0.0, player.eyeHeight.toDouble(), 0.0)
        val targetPos = BlockPos(
            floor(predictedPos.x).toInt(),
            floor(predictedPos.y).toInt() - 1,
            floor(predictedPos.z).toInt(),
        )

        // LiquidBounce does not investigate offsets while the directly targeted
        // position already has a supporting block.
        if (!world.getBlockState(targetPos).canBeReplaced()) return null

        val offsets = buildList {
            for (x in intArrayOf(0, -1, 1)) {
                for (z in intArrayOf(0, -1, 1)) {
                    add(BlockPos(x, 0, z))
                    add(BlockPos(x, -1, z))
                }
            }
        }.sortedBy { offset ->
            val pos = targetPos.offset(offset)
            val center = Vec3(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5)
            if (optimalLine != null) {
                optimalLine.horizontalDistanceToSqr(center)
            } else {
                center.distanceToSqr(predictedPos)
            }
        }

        for (offset in offsets) {
            val placePos = targetPos.offset(offset)
            if (!world.getBlockState(placePos).canBeReplaced()) continue

            var bestFace: Direction? = null
            var bestNeighbor: BlockPos? = null
            var bestPoint: Vec3? = null
            var bestAngle = Double.POSITIVE_INFINITY
            for (face in Direction.entries) {
                val neighbor = placePos.relative(face.opposite)
                if (world.getBlockState(neighbor).canBeReplaced()) continue

                val faceCenter = centerOfFace(neighbor, face)
                val toEye = predictedEye.subtract(faceCenter)
                val facingDot = toEye.x * face.stepX + toEye.y * face.stepY + toEye.z * face.stepZ
                if (facingDot < 0.0 || predictedEye.distanceToSqr(faceCenter) > 20.25) continue

                val point = if (stabilized) {
                    stabilizedPointOnFace(predictedEye, neighbor, face, optimalLine, player.position())
                } else {
                    faceCenter
                }
                val candidateRotation = rotationFrom(predictedEye, point)
                val yawDifference = Mth.wrapDegrees(candidateRotation.first - RotationManager.getCurrentYaw())
                val pitchDifference = candidateRotation.second - RotationManager.getCurrentPitch()
                val angle = yawDifference * yawDifference + pitchDifference * pitchDifference
                if (angle < bestAngle) {
                    bestAngle = angle.toDouble()
                    bestFace = face
                    bestNeighbor = neighbor
                    bestPoint = point
                }
            }

            val face = bestFace ?: continue
            val neighbor = bestNeighbor ?: continue
            val point = bestPoint ?: continue
            return Placement(
                placePos = placePos,
                neighbor = neighbor,
                face = face,
                score = bestAngle,
                hitVec = point,
                rotation = rotationFrom(predictedEye, point),
            )
        }
        return null
    }

    private fun predictPlacementPosition(
        player: LocalPlayer,
        world: ClientLevel,
        optimalLine: MovementLine?,
    ): Vec3 {
        if (optimalLine != null) {
            val directionLength = sqrt(
                optimalLine.direction.x * optimalLine.direction.x +
                        optimalLine.direction.z * optimalLine.direction.z,
            )
            if (directionLength > 1.0e-6) {
                val direction = Vec3(
                    optimalLine.direction.x / directionLength,
                    0.0,
                    optimalLine.direction.z / directionLength,
                )
                val origin = player.position()
                val fallOff = findSupportFallOffPosition(player, world, origin, direction)
                if (fallOff != null) {
                    val dx = fallOff.x - origin.x
                    val dz = fallOff.z - origin.z
                    val distanceToEdge = sqrt(dx * dx + dz * dz)
                    if (distanceToEdge > 0.05) {
                        val predictedDistance = (distanceToEdge - 0.2).coerceAtLeast(0.0)
                        return Vec3(
                            origin.x + direction.x * predictedDistance,
                            origin.y,
                            origin.z + direction.z * predictedDistance,
                        )
                    }
                }
            }
            return player.position()
        }

        val velocity = player.deltaMovement
        val horizontalSpeed = sqrt(velocity.x * velocity.x + velocity.z * velocity.z)
        if (horizontalSpeed < 0.01) return player.position()

        val directionX = velocity.x / horizontalSpeed
        val directionZ = velocity.z / horizontalSpeed
        val supportY = floor(player.y - 1.0).toInt()
        val lookAhead = (horizontalSpeed * 3.0 + 0.35).coerceIn(0.55, 1.25)
        var distance = 0.05
        while (distance <= lookAhead) {
            val x = player.x + directionX * distance
            val z = player.z + directionZ * distance
            val support = BlockPos(floor(x).toInt(), supportY, floor(z).toInt())
            if (world.getBlockState(support).canBeReplaced()) {
                val predictedDistance = distance + 0.05
                return Vec3(
                    player.x + directionX * predictedDistance,
                    player.y,
                    player.z + directionZ * predictedDistance,
                )
            }
            distance += 0.05
        }
        return player.position()
    }

    private fun findSupportFallOffPosition(
        player: LocalPlayer,
        world: ClientLevel,
        origin: Vec3,
        direction: Vec3,
    ): Vec3? {
        val maxDistance = 2.5
        val endX = origin.x + direction.x * maxDistance
        val endZ = origin.z + direction.z * maxDistance
        val halfWidth = player.bbWidth / 2.0 + 0.001
        val minSupportY = player.y - 1.25
        val maxSupportY = player.y + 0.05
        val supportBoxes = ArrayList<AABB>()

        val minX = floor(minOf(origin.x, endX) - 1.0).toInt()
        val maxX = floor(maxOf(origin.x, endX) + 1.0).toInt()
        val minY = floor(player.y - 2.0).toInt()
        val maxY = floor(player.y).toInt()
        val minZ = floor(minOf(origin.z, endZ) - 1.0).toInt()
        val maxZ = floor(maxOf(origin.z, endZ) + 1.0).toInt()

        for (x in minX..maxX) {
            for (y in minY..maxY) {
                for (z in minZ..maxZ) {
                    val pos = BlockPos(x, y, z)
                    val shape = world.getBlockState(pos).getCollisionShape(world, pos)
                    if (shape.isEmpty) continue

                    for (localBox in shape.toAabbs()) {
                        val box = localBox.move(x.toDouble(), y.toDouble(), z.toDouble())
                        if (box.maxY < minSupportY || box.maxY > maxSupportY) continue
                        supportBoxes += AABB(
                            box.minX - halfWidth,
                            box.minY,
                            box.minZ - halfWidth,
                            box.maxX + halfWidth,
                            box.maxY,
                            box.maxZ + halfWidth,
                        )
                    }
                }
            }
        }
        if (supportBoxes.isEmpty()) return null

        var foundSupport = false
        var distance = 0.0
        while (distance <= maxDistance) {
            val x = origin.x + direction.x * distance
            val z = origin.z + direction.z * distance
            val supported = supportBoxes.any { box ->
                x >= box.minX && x <= box.maxX && z >= box.minZ && z <= box.maxZ
            }
            if (supported) {
                foundSupport = true
            } else if (foundSupport) {
                return Vec3(x, origin.y, z)
            } else {
                return null
            }
            distance += 0.01
        }
        return null
    }

    private fun rotationToPlacement(player: LocalPlayer, placement: Placement): Pair<Float, Float> {
        placement.rotation?.let { return it }
        val hit = placement.toHitResult().location
        val dx = hit.x - player.x
        val dy = hit.y - (player.y + player.eyeHeight)
        val dz = hit.z - player.z
        val horizontal = sqrt(dx * dx + dz * dz)
        val yaw = Mth.wrapDegrees(Math.toDegrees(atan2(-dx, dz)).toFloat())
        val pitch = Mth.wrapDegrees(-Math.toDegrees(atan2(dy, horizontal)).toFloat())
        return yaw to pitch.coerceIn(-90f, 90f)
    }

    private fun centerOfFace(block: BlockPos, face: Direction): Vec3 = Vec3(
        block.x + 0.5 + face.stepX * 0.5,
        block.y + 0.5 + face.stepY * 0.5,
        block.z + 0.5 + face.stepZ * 0.5,
    )

    private fun rotationFrom(eye: Vec3, point: Vec3): Pair<Float, Float> {
        val dx = point.x - eye.x
        val dy = point.y - eye.y
        val dz = point.z - eye.z
        val horizontal = sqrt(dx * dx + dz * dz)
        return Mth.wrapDegrees(Math.toDegrees(atan2(-dx, dz)).toFloat()) to
                Mth.wrapDegrees(-Math.toDegrees(atan2(dy, horizontal)).toFloat()).coerceIn(-90f, 90f)
    }

    private fun stabilizedPointOnFace(
        eye: Vec3,
        block: BlockPos,
        face: Direction,
        optimalLine: MovementLine?,
        playerPosition: Vec3,
    ): Vec3 {
        val center = centerOfFace(block, face)
        val baseMinX = if (face.axis == Direction.Axis.X) center.x else block.x + 0.15
        val baseMaxX = if (face.axis == Direction.Axis.X) center.x else block.x + 0.85
        val baseMinY = if (face.axis == Direction.Axis.Y) center.y else block.y + 0.15
        val baseMaxY = if (face.axis == Direction.Axis.Y) center.y else block.y + 0.85
        val baseMinZ = if (face.axis == Direction.Axis.Z) center.z else block.z + 0.15
        val baseMaxZ = if (face.axis == Direction.Axis.Z) center.z else block.z + 0.85
        var minX = baseMinX
        var maxX = baseMaxX
        var minY = baseMinY
        var maxY = baseMaxY
        var minZ = baseMinZ
        var maxZ = baseMaxZ

        if (optimalLine != null) {
            val lineDenominator = when (face.axis) {
                Direction.Axis.X -> optimalLine.direction.x
                Direction.Axis.Y -> optimalLine.direction.y
                Direction.Axis.Z -> optimalLine.direction.z
            }
            val lineNumerator = when (face.axis) {
                Direction.Axis.X -> center.x - eye.x
                Direction.Axis.Y -> center.y - eye.y
                Direction.Axis.Z -> center.z - eye.z
            }
            if (abs(lineDenominator) > 1.0e-6) {
                val collision = eye.add(optimalLine.direction.scale(lineNumerator / lineDenominator))
                val nearestOnLine = optimalLine.nearestHorizontalPoint(playerPosition)
                val awayFromLine = playerPosition.subtract(nearestOnLine)
                val awayLength = sqrt(
                    awayFromLine.x * awayFromLine.x + awayFromLine.z * awayFromLine.z,
                )
                val cropEnd = if (awayLength > 1.0e-6) {
                    playerPosition.add(
                        awayFromLine.x / awayLength * 2.0,
                        0.0,
                        awayFromLine.z / awayLength * 2.0,
                    )
                } else {
                    playerPosition
                }

                minX = maxOf(minX, minOf(collision.x, cropEnd.x))
                maxX = minOf(maxX, maxOf(collision.x, cropEnd.x))
                minY = maxOf(minY, playerPosition.y - 2.0)
                maxY = minOf(maxY, playerPosition.y + 1.0)
                minZ = maxOf(minZ, minOf(collision.z, cropEnd.z))
                maxZ = minOf(maxZ, maxOf(collision.z, cropEnd.z))

                val croppedArea = when (face.axis) {
                    Direction.Axis.X -> (maxY - minY) * (maxZ - minZ)
                    Direction.Axis.Y -> (maxX - minX) * (maxZ - minZ)
                    Direction.Axis.Z -> (maxX - minX) * (maxY - minY)
                }
                if (minX > maxX || minY > maxY || minZ > maxZ || croppedArea < 0.0001) {
                    minX = baseMinX
                    maxX = baseMaxX
                    minY = baseMinY
                    maxY = baseMaxY
                    minZ = baseMinZ
                    maxZ = baseMaxZ
                }
            }
        }

        val yaw = Math.toRadians(RotationManager.getCurrentYaw().toDouble())
        val pitch = Math.toRadians(RotationManager.getCurrentPitch().toDouble())
        val cosPitch = cos(pitch)
        val direction = Vec3(-sin(yaw) * cosPitch, -sin(pitch), cos(yaw) * cosPitch)
        val denominator = when (face.axis) {
            Direction.Axis.X -> direction.x
            Direction.Axis.Y -> direction.y
            Direction.Axis.Z -> direction.z
        }
        val numerator = when (face.axis) {
            Direction.Axis.X -> center.x - eye.x
            Direction.Axis.Y -> center.y - eye.y
            Direction.Axis.Z -> center.z - eye.z
        }
        val distance = if (abs(denominator) > 1.0e-6) numerator / denominator else -1.0
        val projected = if (distance >= 0.0) eye.add(direction.scale(distance)) else center
        return Vec3(
            if (face.axis == Direction.Axis.X) center.x else projected.x.coerceIn(minX, maxX),
            if (face.axis == Direction.Axis.Y) center.y else projected.y.coerceIn(minY, maxY),
            if (face.axis == Direction.Axis.Z) center.z else projected.z.coerceIn(minZ, maxZ),
        )
    }

    private fun currentPlacementHit(player: LocalPlayer, world: ClientLevel): BlockHitResult? {
        val savedYaw = player.yRot
        val savedPitch = player.xRot
        player.yRot = RotationManager.getCurrentYaw()
        player.xRot = RotationManager.getCurrentPitch()
        val reach = if (player.isCreative) 5.0 else 4.5
        val hit = player.pick(reach, 1.0f, false) as? BlockHitResult
        player.yRot = savedYaw
        player.xRot = savedPitch

        hit ?: return null
        if (world.getBlockState(hit.blockPos).canBeReplaced()) return null
        val placePos = hit.blockPos.relative(hit.direction)
        if (!world.getBlockState(placePos).canBeReplaced()) return null
        return hit
    }

    private fun updateNormalMovementLine(player: LocalPlayer, rawMovementYaw: Float): MovementLine {
        val movementYaw = if (!normalLineYaw.isNaN() &&
            Mth.degreesDifferenceAbs(rawMovementYaw, normalLineYaw) <= 30f
        ) {
            normalLineYaw
        } else {
            Math.round(rawMovementYaw / 45.0f) * 45.0f
        }
        val directionRadians = Math.toRadians(movementYaw.toDouble())
        val direction = Vec3(-sin(directionRadians), 0.0, cos(directionRadians))
        val directionChanged = normalLineYaw.isNaN() ||
                Mth.degreesDifferenceAbs(normalLineYaw, movementYaw) > 30f

        val fittedAnchor = fittedNormalLineAnchor(player.position(), direction)
        val nextLine = when {
            directionChanged -> MovementLine(fittedAnchor ?: player.position(), direction)
            fittedAnchor != null -> MovementLine(fittedAnchor, direction)
            normalMovementLine != null -> normalMovementLine!!
            else -> MovementLine(player.position(), direction)
        }
        normalLineYaw = movementYaw
        normalMovementLine = nextLine
        return nextLine
    }

    private fun fittedNormalLineAnchor(playerPosition: Vec3, intendedDirection: Vec3): Vec3? {
        if (normalPlacedBlocks.size < 2) return null
        val previous = normalPlacedBlocks[normalPlacedBlocks.size - 2]
        val latest = normalPlacedBlocks.last()
        val previousCenter = Vec3(previous.x + 0.5, playerPosition.y, previous.z + 0.5)
        val latestCenter = Vec3(latest.x + 0.5, playerPosition.y, latest.z + 0.5)
        var placedDirection = latestCenter.subtract(previousCenter)
        val length = sqrt(
            placedDirection.x * placedDirection.x + placedDirection.z * placedDirection.z,
        )
        if (length < 1.0e-6) return null
        placedDirection = Vec3(placedDirection.x / length, 0.0, placedDirection.z / length)
        if (placedDirection.dot(intendedDirection) < 0.5) return null

        val fittedLine = MovementLine(previousCenter.add(latestCenter).scale(0.5), placedDirection)
        return fittedLine.nearestHorizontalPoint(playerPosition)
    }

    private fun trackNormalPlacedBlock(block: BlockPos) {
        if (normalPlacedBlocks.lastOrNull() == block) return
        while (normalPlacedBlocks.size >= 4) normalPlacedBlocks.removeFirst()
        normalPlacedBlocks.addLast(block)
    }

    private fun stabilizeNormalMovement(
        player: LocalPlayer,
        optimalLine: MovementLine?,
        input: DirectionalKeys,
        jumping: Boolean,
    ): DirectionalKeys {
        optimalLine ?: return input
        if (jumping && player.onGround()) return input

        val position = player.position()
        val nearest = optimalLine.nearestHorizontalPoint(position)
        val toLine = nearest.subtract(position)
        val movingTowardLine = toLine.x * player.deltaMovement.x + toLine.z * player.deltaMovement.z > 0.0
        val maxDeviation = if (movingTowardLine) 0.075 else 0.2
        if (toLine.x * toLine.x + toLine.z * toLine.z < maxDeviation * maxDeviation) return input

        val correctionLength = sqrt(toLine.x * toLine.x + toLine.z * toLine.z)
        if (correctionLength < 1.0e-6) return input
        val correction = closestMovementCandidate(
            toLine.x / correctionLength,
            toLine.z / correctionLength,
            RotationManager.getClientYaw(),
        )
        val frontalAxisBlocked = input.forward || input.backward
        val sagittalAxisBlocked = input.left || input.right
        return DirectionalKeys(
            forward = if (frontalAxisBlocked) input.forward else correction.forward > 0f,
            backward = if (frontalAxisBlocked) input.backward else correction.forward < 0f,
            left = if (sagittalAxisBlocked) input.left else correction.strafe > 0f,
            right = if (sagittalAxisBlocked) input.right else correction.strafe < 0f,
        )
    }

    private fun isUsefulScaffoldHit(player: LocalPlayer, hit: BlockHitResult): Boolean {
        val placePos = hit.blockPos.relative(hit.direction)
        val supportY = floor(player.y - 1.0).toInt()
        if (placePos.y !in (supportY - 1)..supportY) return false

        val dx = placePos.x + 0.5 - player.x
        val dz = placePos.z + 0.5 - player.z
        return dx * dx + dz * dz <= 3.25
    }

    private fun Placement.matches(hit: BlockHitResult): Boolean =
        hit.blockPos == neighbor &&
                hit.direction == face &&
                hit.blockPos.relative(hit.direction) == placePos

    private fun restorePhysicalKeys(
        client: Minecraft,
        forward: Boolean,
        backward: Boolean,
        left: Boolean,
        right: Boolean,
    ) {
        client.options.keyUp.setDown(forward)
        client.options.keyDown.setDown(backward)
        client.options.keyLeft.setDown(left)
        client.options.keyRight.setDown(right)
        val shift = InputUtil.isPhysicalKeyDown(client.options.keyShift)
        client.options.keyShift.setDown(shift)
        client.player?.setShiftKeyDown(shift)
    }

    private fun applyDirectionalKeys(client: Minecraft, input: DirectionalKeys) {
        restorePhysicalKeys(
            client,
            input.forward,
            input.backward,
            input.left,
            input.right,
        )
    }

    private fun restorePhysicalKeys(client: Minecraft) {
        restorePhysicalKeys(
            client,
            InputUtil.isPhysicalKeyDown(client.options.keyUp),
            InputUtil.isPhysicalKeyDown(client.options.keyDown),
            InputUtil.isPhysicalKeyDown(client.options.keyLeft),
            InputUtil.isPhysicalKeyDown(client.options.keyRight),
        )
        client.options.keyJump.setDown(InputUtil.isPhysicalKeyDown(client.options.keyJump))
    }

    private fun releaseMovementKeys(client: Minecraft) {
        val options = client.options
        options.keyUp.setDown(false)
        options.keyDown.setDown(false)
        options.keyLeft.setDown(false)
        options.keyRight.setDown(false)
        options.keyJump.setDown(false)
        options.keyShift.setDown(false)
        options.keySprint.setDown(false)
        client.player?.setShiftKeyDown(false)
    }

    private fun applyMovementCorrection(
        client: Minecraft,
        forward: Boolean,
        backward: Boolean,
        left: Boolean,
        right: Boolean,
    ) {
        val input = DirectionalKeys(forward, backward, left, right)
        val intended = worldDirectionForInput(input, RotationManager.getClientYaw())
        if (intended == null) {
            restorePhysicalKeys(client, false, false, false, false)
            return
        }

        val best = closestMovementCandidate(
            intended.x,
            intended.z,
            RotationManager.getCurrentYaw(),
        )
        client.options.keyUp.setDown(best.forward > 0f)
        client.options.keyDown.setDown(best.forward < 0f)
        client.options.keyLeft.setDown(best.strafe > 0f)
        client.options.keyRight.setDown(best.strafe < 0f)
    }

    private fun worldDirectionForInput(input: DirectionalKeys, yaw: Float): Vec3? {
        var rawForward = 0f
        var rawStrafe = 0f
        if (input.forward) rawForward += 1f
        if (input.backward) rawForward -= 1f
        if (input.left) rawStrafe += 1f
        if (input.right) rawStrafe -= 1f

        if (rawForward == 0f && rawStrafe == 0f) return null

        val inputLength = sqrt(rawForward * rawForward + rawStrafe * rawStrafe)
        val normalizedForward = rawForward / inputLength
        val normalizedStrafe = rawStrafe / inputLength
        val yawRadians = Math.toRadians(yaw.toDouble())
        return Vec3(
            normalizedStrafe * cos(yawRadians) - normalizedForward * sin(yawRadians),
            0.0,
            normalizedForward * cos(yawRadians) + normalizedStrafe * sin(yawRadians),
        )
    }

    private fun closestMovementCandidate(worldX: Double, worldZ: Double, yaw: Float): MovementCandidate {
        val inverseSqrtTwo = (1.0 / sqrt(2.0)).toFloat()
        val candidates = arrayOf(
            MovementCandidate(1f, 0f),
            MovementCandidate(-1f, 0f),
            MovementCandidate(0f, 1f),
            MovementCandidate(0f, -1f),
            MovementCandidate(inverseSqrtTwo, inverseSqrtTwo),
            MovementCandidate(inverseSqrtTwo, -inverseSqrtTwo),
            MovementCandidate(-inverseSqrtTwo, inverseSqrtTwo),
            MovementCandidate(-inverseSqrtTwo, -inverseSqrtTwo),
        )
        val serverRadians = Math.toRadians(yaw.toDouble())
        var best = candidates[0]
        var bestDot = Double.NEGATIVE_INFINITY
        for (candidate in candidates) {
            val candidateX = candidate.strafe * cos(serverRadians) - candidate.forward * sin(serverRadians)
            val candidateZ = candidate.forward * cos(serverRadians) + candidate.strafe * sin(serverRadians)
            val dot = worldX * candidateX + worldZ * candidateZ
            if (dot > bestDot) {
                bestDot = dot
                best = candidate
            }
        }
        return best
    }

    private fun isMovingTowardEdge(
        player: LocalPlayer,
        world: ClientLevel,
        forward: Boolean,
        back: Boolean,
        left: Boolean,
        right: Boolean,
    ): Boolean {
        val yaw = Math.toRadians(RotationManager.getClientYaw().toDouble())
        var moveX = 0.0
        var moveZ = 0.0

        if (forward) {
            moveX -= sin(yaw)
            moveZ += cos(yaw)
        }
        if (back) {
            moveX += sin(yaw)
            moveZ -= cos(yaw)
        }
        if (right) {
            moveX -= cos(yaw)
            moveZ -= sin(yaw)
        }
        if (left) {
            moveX += cos(yaw)
            moveZ += sin(yaw)
        }

        val len = sqrt(moveX * moveX + moveZ * moveZ)
        if (len < 0.001) return isNearEdge(player, world)

        val margin = 0.42
        val checkX = player.x + moveX / len * margin
        val checkZ = player.z + moveZ / len * margin
        val checkY = floor(player.y - 1.0).toInt()
        val pos = BlockPos(floor(checkX).toInt(), checkY, floor(checkZ).toInt())
        val state = world.getBlockState(pos)
        return state.isAir || !state.fluidState.isEmpty || !state.isCollisionShapeFullBlock(world, pos)
    }

    private fun findUpstackPlacement(
        player: LocalPlayer,
        world: ClientLevel,
    ): Placement? {
        val velocity = player.deltaMovement
        val baseY = floor(player.y - 1.0).toInt()
        val candidates = mutableListOf<BlockPos>()

        fun addCandidate(x: Double, z: Double, yOffset: Int = 0) {
            val pos = BlockPos(floor(x).toInt(), baseY + yOffset, floor(z).toInt())
            if (pos !in candidates) candidates.add(pos)
        }

        addCandidate(player.x, player.z)
        addCandidate(player.x + velocity.x * 0.6, player.z + velocity.z * 0.6)
        addCandidate(player.x + velocity.x * 1.2, player.z + velocity.z * 1.2)
        addCandidate(player.x, player.z, -1)
        addCandidate(player.x + velocity.x * 0.8, player.z + velocity.z * 0.8, 1)
        addCandidate(player.x + velocity.x * 1.4, player.z + velocity.z * 1.4, 1)

        val placeFaces = arrayOf(
            Direction.NORTH,
            Direction.SOUTH,
            Direction.EAST,
            Direction.WEST,
            Direction.DOWN,
        )

        var best: Placement? = null
        for (placePos in candidates) {
            if (!world.getBlockState(placePos).isAir) continue

            for (dir in placeFaces) {
                val neighbor = placePos.relative(dir)
                val neighborState = world.getBlockState(neighbor)
                if (neighborState.isAir || !neighborState.fluidState.isEmpty) continue
                if (!neighborState.isCollisionShapeFullBlock(world, neighbor)) continue

                val face = dir.opposite
                val dist = squaredDistanceToFace(player, neighbor, face)
                if (dist > 20.25) continue

                val predictedX = player.x + velocity.x
                val predictedZ = player.z + velocity.z
                val horizontalError = sqrt(
                    (placePos.x + 0.5 - predictedX) * (placePos.x + 0.5 - predictedX) +
                            (placePos.z + 0.5 - predictedZ) * (placePos.z + 0.5 - predictedZ)
                )
                val verticalPenalty = abs(placePos.y - baseY) * 4.0
                val facePenalty = if (face == Direction.UP) 0.8 else 0.0
                val score = horizontalError * 4.0 + verticalPenalty + facePenalty + dist * 0.02

                if (best == null || score < best.score) {
                    best = Placement(placePos, neighbor, face, score)
                }
            }
        }

        return best
    }

    private fun Placement.toHitResult(): BlockHitResult {
        val location = hitVec ?: Vec3(
            neighbor.x + 0.5 + face.stepX * 0.45,
            neighbor.y + 0.5 + face.stepY * 0.45,
            neighbor.z + 0.5 + face.stepZ * 0.45,
        )
        return BlockHitResult(location, face, neighbor, false)
    }

    private fun squaredDistanceToFace(player: LocalPlayer, neighbor: BlockPos, face: Direction): Double {
        val eyeY = player.y + player.eyeHeight
        val x = neighbor.x + 0.5 + face.stepX * 0.45
        val y = neighbor.y + 0.5 + face.stepY * 0.45
        val z = neighbor.z + 0.5 + face.stepZ * 0.45
        return (x - player.x) * (x - player.x) +
                (y - eyeY) * (y - eyeY) +
                (z - player.z) * (z - player.z)
    }

    private fun correctedUpstackPitch(
        player: LocalPlayer,
        forward: Boolean,
        back: Boolean,
        left: Boolean,
        right: Boolean,
    ): Float {
        val yaw = Math.toRadians(RotationManager.getClientYaw().toDouble())
        var moveX = 0.0
        var moveZ = 0.0

        if (forward) {
            moveX -= sin(yaw)
            moveZ += cos(yaw)
        }
        if (back) {
            moveX += sin(yaw)
            moveZ -= cos(yaw)
        }
        if (right) {
            moveX -= cos(yaw)
            moveZ -= sin(yaw)
        }
        if (left) {
            moveX += cos(yaw)
            moveZ += sin(yaw)
        }

        val len = sqrt(moveX * moveX + moveZ * moveZ)
        if (len < 0.001) return 75.6f

        val localX = player.x - floor(player.x) - 0.5
        val localZ = player.z - floor(player.z) - 0.5
        val movementOffset = localX * (moveX / len) + localZ * (moveZ / len)
        val pitchCorrection = (-movementOffset * 0.35).coerceIn(-0.12, 0.12)
        return (75.6 + pitchCorrection).toFloat()
    }

    private fun isSolidSupportBlock(world: ClientLevel, x: Int, y: Int, z: Int): Boolean {
        val pos = BlockPos(x, y, z)
        val state = world.getBlockState(pos)
        return !state.isAir && state.fluidState.isEmpty && state.isCollisionShapeFullBlock(world, pos)
    }

    private fun resetTechniqueState() {
        resetDiagonalBreezilyCorrection()
        normalMovementLine = null
        normalLineYaw = Float.NaN
        normalUpstackCycle = false
        normalPlacedBlocks.clear()
        autoclickAccum = 0f
        autoclickTargetCps = 0
        isCrouching = false
        crouchWaitTicks = 0
    }

    internal fun tellyBlockSlot(player: LocalPlayer): Int {
        if (!intelligentPicker.value) return findBlockSlot(player)

        var bestSlot = -1
        var bestCount = 0
        for (slot in 0..8) {
            if (!isUsableScaffoldStack(player.inventory.getItem(slot))) continue
            val count = player.inventory.getItem(slot).count
            if (count > bestCount) {
                bestCount = count
                bestSlot = slot
            }
        }
        return bestSlot
    }

    internal fun isUsableScaffoldStack(stack: net.minecraft.world.item.ItemStack): Boolean {
        val item = stack.item
        if (stack.isEmpty || item !is BlockItem) return false
        if (blockWhitelist.value.isEmpty()) return true
        val blockId = BuiltInRegistries.BLOCK.getKey(item.block).toString().lowercase()
        return blockMatchesWhitelist(blockId, blockWhitelist.value)
    }

    internal fun scaffoldBlockSlot(player: LocalPlayer): Int = tellyBlockSlot(player)

    internal fun tellyRestoreMovementKeys(client: Minecraft) {
        restorePhysicalKeys(
            client,
            InputUtil.isPhysicalKeyDown(client.options.keyUp),
            InputUtil.isPhysicalKeyDown(client.options.keyDown),
            InputUtil.isPhysicalKeyDown(client.options.keyLeft),
            InputUtil.isPhysicalKeyDown(client.options.keyRight),
        )
    }

    internal fun releaseRotation() {
        if (RotationManager.ownsRotation(ROTATION_OWNER)) {
            RotationManager.clearRotation(ROTATION_OWNER)
        } else if (!RotationManager.isActive()) {
            // Normal can stop before claiming a target. Clear any perspective
            // state left by that idle transition without touching another owner.
            RotationManager.clearRotation()
        }
        ownsRotation = false
    }

    override fun onEnabled() {
        releaseRotation()
        clearTrackedContext()
        resetTechniqueState()
    }

    override fun onDisabled() {
        if (bridgeMode.value == BridgeMode.NCP) Ncp.onDisabled()
        if (telly != null && bridgeMode.value == BridgeMode.TELLY) {
            telly!!.onDisabled()
        }
        val opts = Minecraft.getInstance().options
        opts.keyUp.setDown(InputUtil.isPhysicalKeyDown(opts.keyUp))
        opts.keyDown.setDown(InputUtil.isPhysicalKeyDown(opts.keyDown))
        opts.keyLeft.setDown(InputUtil.isPhysicalKeyDown(opts.keyLeft))
        opts.keyRight.setDown(InputUtil.isPhysicalKeyDown(opts.keyRight))
        opts.keyShift.setDown(InputUtil.isPhysicalKeyDown(opts.keyShift))
        opts.keyJump.setDown(InputUtil.isPhysicalKeyDown(opts.keyJump))

        releaseRotation()
        RotationManager.allowStrafe = false
        RotationManager.allowForward = false
        RotationManager.freezeMovement = false
        RotationManager.suppressJump = false
        autoclickAccum = 0.0f
        autoclickTargetCps = 0
        isCrouching = false
        crouchWaitTicks = 0
        clearTrackedContext()
        resetTechniqueState()
    }

    private fun blockMatchesWhitelist(blockName: String, whitelist: List<String>): Boolean {
        for (entry in whitelist) {
            if (entry.endsWith("_category")) {
                val categoryId = entry.removeSuffix("_category")
                val category = itemCategories.firstOrNull { it.id == categoryId }
                if (category != null && category.matches(blockName)) return true
            } else {
                if (blockName.contains(entry.lowercase())) return true
            }
        }
        return false
    }

    override fun hudInfo(): String = bridgeMode.value.name.lowercase().replace("_", " ")
}
