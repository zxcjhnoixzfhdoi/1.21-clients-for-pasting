package onl.luka.grizzly.module.modules.combat

import com.mojang.blaze3d.platform.InputConstants
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.event.player.AttackEntityCallback
import onl.luka.grizzly.module.Module
import onl.luka.grizzly.module.modules.player.Blink
import onl.luka.grizzly.module.modules.player.FakeLag
import onl.luka.grizzly.util.LagManager
import onl.luka.grizzly.util.interactBlockLikeVanilla
import onl.luka.grizzly.util.interactEntityLikeVanilla
import onl.luka.grizzly.util.useItemStrict
import net.minecraft.client.Minecraft
import net.minecraft.client.KeyMapping
import net.minecraft.core.component.DataComponents
import net.minecraft.core.component.DataComponents.BLOCKS_ATTACKS
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.attribute.modifier.AttributeModifier
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.ItemUseAnimation
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult
import kotlin.random.Random

object AutoBlock : Module(
    name = "Auto Block",
    description = "Holds block with your sword to reduce incoming damage",
    category = Category.COMBAT
) {
    private const val MAX_QUEUED_ATTACKS = 1

    enum class BlockMode { MANUAL, AUTOMATIC }

    private val mode = enum("mode", BlockMode.AUTOMATIC)

    private val blockChance = float("block chance (%)", 80f, 0f, 100f).also {
        it.visibleWhen = { mode.value == BlockMode.MANUAL }
    }
    private val blockCps = float("block cps", 6f, 1f, 8f).also {
        it.visibleWhen = { mode.value == BlockMode.AUTOMATIC }
    }

    private val blockRange = float("range", 6.5f, 1.0f, 20.0f)

    private val maxHoldMs = int("max hold (ms)", 100, 50, 200)

    private val condLmb    = boolean("check left click",  true)
    private val condRmb    = boolean("check right click", false)
    private val condDamage = boolean("check took damage", false)
    private val damageWindowMs = int("damage window (ms)", 3000, 250, 10000).also {
        it.visibleWhen = { condDamage.value }
    }

    private val releaseMs = intRange("release (ms)", 10 to 30, 1, 40)

    private val lagEnabled = boolean("lag", false)
    private val lagChance = float("lag chance (%)", 100f, 0f, 100f).also {
        it.visibleWhen = { lagEnabled.value }
    }
    private val lagDurationMs = intRange("lag duration (ms)", 150 to 250, 50, 1000).also {
        it.visibleWhen = { lagEnabled.value }
    }
    private val preventDelayAttacks = boolean("prevent delaying attacks", false).also {
        it.visibleWhen = { lagEnabled.value }
    }
    private val blockAgainImmediately = boolean("block again immediately", true).also {
        it.visibleWhen = { lagEnabled.value }
    }

    @JvmField
    val slowDown = boolean("slow down", true)

    @Volatile var isLagging  = false
        private set
    @JvmField
    var isBlocking            = false

    private var releaseUntilMs    = 0L
    private var lagUntilMs        = 0L
    private var manualBlockActive = false
    private var postAttackBlockActive = false
    private var nextAutoCycleMs   = 0L
    private var blockStartMs      = 0L
    private var blockStartTick    = Long.MIN_VALUE
    private var lastDamageTakenMs = 0L
    private var prevHurtTime      = 0

    private var lastNearbyEntityMs = 0L
    private var lastAttackActivityMs = 0L
    private var clientTick = 0L
    private var lastReleaseTick = Long.MIN_VALUE
    private var lastAttackTick = Long.MIN_VALUE
    private var suppressBlockThroughTick = Long.MIN_VALUE
    private var preparedAuraAttackTick = Long.MIN_VALUE
    private var preparedManualAttackTick = Long.MIN_VALUE
    private var pendingAuraAttackTick = Long.MIN_VALUE
    private var pendingAttackReadyTick = Long.MIN_VALUE
    private var replayingAttacksTick = Long.MIN_VALUE
    private var pendingAttackClicks = 0

    private fun isBehaviorActive(): Boolean =
        enabled.value ||
            (
                SilentAura.isEnabled() &&
                    SilentAura.autoBlock.value &&
                    SilentAura.target != null
            )

    init {
        AttackEntityCallback.EVENT.register { player, _, _, _, _ ->
            if (player === Minecraft.getInstance().player && isBehaviorActive()) {
                val now = System.currentTimeMillis()
                lastAttackActivityMs = now
                lastAttackTick = clientTick
                suppressBlockThroughTick =
                    maxOf(suppressBlockThroughTick, clientTick)

                val auraAttack = preparedAuraAttackTick == clientTick
                preparedAuraAttackTick = Long.MIN_VALUE
                val preparedManualAttack =
                    preparedManualAttackTick == clientTick
                if (auraAttack || (enabled.value && !preparedManualAttack)) {
                    armPostAttackBlock()
                }

                val stoppedLag = lagEnabled.value && preventDelayAttacks.value && isLagging
                if (stoppedLag) {
                    stopLag(reblock = false)
                    releaseUntilMs = 0L
                }
                lastNearbyEntityMs = now
            }
            InteractionResult.PASS
        }

        ClientTickEvents.START_CLIENT_TICK.register { client ->
            clientTick++
            val now = System.currentTimeMillis()
            val player = client.player

            if (player != null) {
                if (player.hurtTime > prevHurtTime) lastDamageTakenMs = now
                prevHurtTime = player.hurtTime
            }

            if (
                isBlocking &&
                pendingAttackClicks == 0 &&
                player != null &&
                !player.isUsingItem
            ) {
                isBlocking = false
                blockStartMs = 0L
                blockStartTick = Long.MIN_VALUE
                client.options.keyUse.setDown(
                    isPhysicalKeyDown(client.options.keyUse),
                )
                removeSlowdown(player)
            }

            if (isLagging && now >= lagUntilMs) {
                val reblock = isBehaviorActive() && lagEnabled.value && blockAgainImmediately.value
                stopLag(reblock = reblock)
                if (reblock) releaseUntilMs = 0L
            }

            val silentAuraAutoBlock =
                SilentAura.isEnabled() &&
                    SilentAura.autoBlock.value &&
                    SilentAura.target != null
            val active = enabled.value || silentAuraAutoBlock

            if (!active && !silentAuraAutoBlock) {
                forceRelease(client)
                clearPostAttackBlock()
                clearPendingAttacks()
                return@register
            }

            if (player == null || client.gui.screen() != null) {
                forceRelease(client)
                clearPostAttackBlock()
                clearPendingAttacks()
                return@register
            }

            if (KnockbackDisplacement.hasPendingAttack()) {
                prepareHitFlick(client)
                return@register
            }

            if (pendingAttackClicks > 0) {
                drainAttackClicks(client)
                if (isBlocking) {
                    releaseUntilMs = now + randomRelease()
                    release(client, allowLag = false)
                    pendingAttackReadyTick = clientTick + 2
                    return@register
                }
                if (player.isUsingItem) {
                    return@register
                }

                if (
                    clientTick < pendingAttackReadyTick ||
                    now < releaseUntilMs
                ) {
                    return@register
                }

                replayPendingAttacks(client)
                return@register
            }

            if (!silentAuraAutoBlock) {
                pendingAuraAttackTick = Long.MIN_VALUE
            } else if (pendingAuraAttackTick != Long.MIN_VALUE) {
                return@register
            }

            if (
                clientTick <= lastAttackTick ||
                clientTick <= lastReleaseTick ||
                clientTick <= suppressBlockThroughTick
            ) {
                return@register
            }

            if (active && !conditionsMet(client, now)) {
                forceRelease(client)
                clearPostAttackBlock()
                return@register
            }

            val hasBlockItem = InteractionHand.entries.any { hand ->
                player.getItemInHand(hand).canUseAsBlock()
            }

            if (!hasBlockItem) {
                forceRelease(client)
                clearPostAttackBlock()
                return@register
            }

            val level = client.level
            val entityNearby = level != null && level.entitiesForRendering()
                .filterIsInstance<LivingEntity>()
                .any { e -> e !== player && !e.isDeadOrDying && player.distanceTo(e) <= blockRange.value }

            if (entityNearby) {
                lastNearbyEntityMs = now
            } else {
                if (condDamage.value && (now - lastNearbyEntityMs) >= 250L) {
                    lastDamageTakenMs = 0L
                }

                release(client)
                clearPostAttackBlock()
                return@register
            }

            if (
                enabled.value &&
                mode.value == BlockMode.MANUAL &&
                postAttackBlockActive &&
                !isBlocking
            ) {
                pendingAttackClicks =
                    (pendingAttackClicks + drainAttackClicks(client))
                        .coerceAtMost(MAX_QUEUED_ATTACKS)
            }

            if (isBlocking && blockStartMs > 0 && (now - blockStartMs) >= maxHoldMs.value) {
                //println("max hold timeout reached, releasing")
                releaseUntilMs = now + randomRelease()
                release(client)
                if (mode.value == BlockMode.MANUAL) {
                    clearPostAttackBlock()
                }
                return@register
            }

            if (now < releaseUntilMs) {
                release(client)
                return@register
            }

            when (mode.value) {
                BlockMode.MANUAL -> {
                    if (manualBlockActive) {
                        if (!doBlock(now) && pendingAttackClicks > 0) {
                            replayPendingAttacks(client)
                        }
                    } else {
                        release(client)
                        postAttackBlockActive = false
                    }
                }

                BlockMode.AUTOMATIC -> {
                    if (nextAutoCycleMs == 0L) {
                        nextAutoCycleMs = now + cycleDurationMs()
                    }
                    val attackQueued =
                        LeftClicker.isEnabled() && LeftClicker.clickedThisTick
                    val attackedThisTick =
                        now - lastAttackActivityMs <= ATTACK_PACKET_GUARD_MS
                    if (
                        !isBlocking &&
                        now >= nextAutoCycleMs &&
                        !attackQueued &&
                        !attackedThisTick &&
                        doBlock(now)
                    ) {
                        nextAutoCycleMs = now + cycleDurationMs()
                    }
                }
            }
        }
    }

    private fun randomRelease(): Long {
        val (lo, hi) = releaseMs.value
        return if (hi > lo) (lo..hi).random().toLong() else lo.toLong()
    }

    private fun cycleDurationMs(): Long = (1000f / blockCps.value).toLong()

    private fun conditionsMet(client: Minecraft, now: Long): Boolean {
        if (!condLmb.value && !condRmb.value && !condDamage.value) return true
        val leftClickActive = when (mode.value) {
            BlockMode.MANUAL -> postAttackBlockActive
            BlockMode.AUTOMATIC ->
                isPhysicalKeyDown(client.options.keyAttack) ||
                    now - lastAttackActivityMs <= ATTACK_ACTIVITY_WINDOW_MS
        }
        return (condLmb.value    && leftClickActive) ||
                (condRmb.value    && isUseHeld(client))   ||
                (condDamage.value && lastDamageTakenMs > 0L && (now - lastDamageTakenMs) <= damageWindowMs.value)
    }

    private fun drainAttackClicks(client: Minecraft): Int {
        var clicks = 0
        while (client.options.keyAttack.consumeClick()) {
            if (clicks < MAX_QUEUED_ATTACKS) {
                clicks++
            }
        }
        return clicks
    }

    private fun deferBlockedAttack(client: Minecraft): Boolean {
        if (
            !enabled.value ||
            (
                !isBlocking &&
                    lastReleaseTick != clientTick &&
                    client.player?.isUsingItem != true
            )
        ) {
            return false
        }

        val clicks = drainAttackClicks(client)
        if (clicks <= 0) return false

        pendingAttackClicks =
            (pendingAttackClicks + clicks).coerceAtMost(MAX_QUEUED_ATTACKS)
        if (isBlocking && blockStartTick != clientTick) {
            releaseUntilMs = System.currentTimeMillis() + randomRelease()
            release(client, allowLag = false)
        }

        pendingAttackReadyTick =
            maxOf(pendingAttackReadyTick, clientTick + 2)
        return true
    }

    private fun replayPendingAttacks(client: Minecraft) {
        if (pendingAttackClicks <= 0) return
        val attackKey = InputConstants.getKey(client.options.keyAttack.saveString())
        repeat(pendingAttackClicks.coerceAtMost(MAX_QUEUED_ATTACKS)) {
            KeyMapping.click(attackKey)
        }
        pendingAttackClicks = 0
        pendingAttackReadyTick = Long.MIN_VALUE
        replayingAttacksTick = clientTick
        preparedManualAttackTick = clientTick
        suppressBlockThroughTick = maxOf(suppressBlockThroughTick, clientTick)
    }

    private fun clearPendingAttacks() {
        pendingAttackClicks = 0
        pendingAttackReadyTick = Long.MIN_VALUE
        replayingAttacksTick = Long.MIN_VALUE
        pendingAuraAttackTick = Long.MIN_VALUE
    }

    private fun clearPostAttackBlock() {
        manualBlockActive = false
        postAttackBlockActive = false
        if (mode.value == BlockMode.AUTOMATIC) {
            nextAutoCycleMs = 0L
        }
    }

    private fun armPostAttackBlock() {
        postAttackBlockActive = when (mode.value) {
            BlockMode.MANUAL -> {
                manualBlockActive =
                    Random.nextFloat() * 100f < blockChance.value
                manualBlockActive
            }
            BlockMode.AUTOMATIC -> true
        }
    }

    override fun onEnabled() {
        isBlocking         = false
        isLagging          = false
        releaseUntilMs     = 0L
        lagUntilMs         = 0L
        manualBlockActive  = false
        postAttackBlockActive = false
        nextAutoCycleMs    = 0L
        blockStartMs       = 0L
        blockStartTick     = Long.MIN_VALUE
        lastDamageTakenMs  = 0L
        lastNearbyEntityMs = 0L
        lastAttackActivityMs = 0L
        lastReleaseTick = Long.MIN_VALUE
        lastAttackTick = Long.MIN_VALUE
        suppressBlockThroughTick = Long.MIN_VALUE
        preparedAuraAttackTick = Long.MIN_VALUE
        preparedManualAttackTick = Long.MIN_VALUE
        clearPendingAttacks()
        prevHurtTime       = 0
    }

    override fun onDisabled() {
        if (isLagging) stopLag(reblock = false)
        Minecraft.getInstance().player?.let { removeSlowdown(it) }
        forceRelease(Minecraft.getInstance())
        Minecraft.getInstance().options.keyUse.setDown(isPhysicalKeyDown(Minecraft.getInstance().options.keyUse))
        releaseUntilMs     = 0L
        clearPostAttackBlock()
        nextAutoCycleMs    = 0L
        blockStartMs       = 0L
        blockStartTick     = Long.MIN_VALUE
        lastNearbyEntityMs = 0L
        lastAttackActivityMs = 0L
        lastReleaseTick = Long.MIN_VALUE
        lastAttackTick = Long.MIN_VALUE
        suppressBlockThroughTick = Long.MIN_VALUE
        preparedAuraAttackTick = Long.MIN_VALUE
        preparedManualAttackTick = Long.MIN_VALUE
        clearPendingAttacks()
    }

    private fun startLag() {
        isLagging  = true
        val (lo, hi) = lagDurationMs.value
        lagUntilMs = System.currentTimeMillis() + (if (hi > lo) (lo..hi).random().toLong() else lo.toLong())
    }

    private fun stopLag(reblock: Boolean) {
        if (!isLagging) return
        isLagging  = false
        lagUntilMs = 0L

        val otherLagging =
            (Blink.isEnabled()   && Blink.holding)                              ||
                    (FakeLag.isEnabled() && FakeLag.isCurrentlyLagging)                 ||
                    (Velocity.isEnabled()
                            && Velocity.mode.value == Velocity.Mode.DELAY
                            && Velocity.isDelayWindowActive())

        if (!otherLagging) LagManager.flushAllOutgoing()
        if (reblock) releaseUntilMs = 0L
    }

    private fun doBlock(now: Long): Boolean {
        if (isBlocking) {
            return true
        }
        val mc = Minecraft.getInstance()
        val player = mc.player
        if (player?.isUsingItem == true) {
            return false
        }
        if (
            player == null ||
            clientTick <= lastReleaseTick ||
            clientTick <= lastAttackTick ||
            clientTick <= suppressBlockThroughTick ||
            now < releaseUntilMs
        ) return false

        if (slowDown.value) applySlowdown(player)
        block()
        if (isBlocking) {
            mc.options.keyUse.setDown(true)
            blockStartMs = now
            blockStartTick = clientTick
        } else {
            removeSlowdown(player)
        }
        return isBlocking
    }
    
    private fun release(client: Minecraft, allowLag: Boolean = true): Boolean {
        if (!isBlocking) {
            client.player?.let { removeSlowdown(it) }
            return false
        }

        val player = client.player
        if (player == null) {
            isBlocking   = false
            blockStartMs = 0L
            blockStartTick = Long.MIN_VALUE
            client.options.keyUse.setDown(
                isPhysicalKeyDown(client.options.keyUse),
            )
            slowingDown = false
            return false
        }

        if (allowLag && lagEnabled.value && !isLagging && client.gui.screen() == null) {
            if (Random.nextFloat() * 100f < lagChance.value) startLag()
        }

        isBlocking   = false
        blockStartMs = 0L
        blockStartTick = Long.MIN_VALUE
        client.options.keyUse.setDown(
            isPhysicalKeyDown(client.options.keyUse),
        )
        val gameMode = client.gameMode
        if (gameMode != null) {
            gameMode.releaseUsingItem(player)
        } else {
            player.stopUsingItem()
        }
        lastReleaseTick = clientTick
        suppressBlockThroughTick =
            maxOf(suppressBlockThroughTick, clientTick)
        client.player?.let { removeSlowdown(it) }
        return true
    }

    fun forceRelease(client: Minecraft) {
        release(client, allowLag = false)
    }

    @JvmStatic
    fun prepareHitFlick(client: Minecraft): Boolean {
        suppressBlockThroughTick = maxOf(suppressBlockThroughTick, clientTick + 1)
        if (isBlocking) release(client, allowLag = false)

        val player = client.player ?: return false
        return !player.isUsingItem && lastReleaseTick < clientTick
    }

    @JvmStatic
    fun beforeHandleKeybinds(client: Minecraft) {
        if (client.gui.screen() != null) return
        deferBlockedAttack(client)
    }

    @JvmStatic
    fun beforeVanillaAttack(client: Minecraft): Boolean {
        if (!enabled.value || client.gui.screen() != null) return true
        client.player ?: return true

        lastAttackActivityMs = System.currentTimeMillis()
        armPostAttackBlock()
        preparedManualAttackTick = clientTick
        suppressBlockThroughTick = maxOf(suppressBlockThroughTick, clientTick)

        if (replayingAttacksTick == clientTick) {
            lastAttackTick = clientTick
            return true
        }

        if (lastReleaseTick == clientTick || client.player?.isUsingItem == true) {
            pendingAttackClicks =
                (pendingAttackClicks + 1).coerceAtMost(MAX_QUEUED_ATTACKS)
            pendingAttackReadyTick =
                maxOf(pendingAttackReadyTick, clientTick + 2)
            return false
        }

        if (!isBlocking) {
            lastAttackTick = clientTick
            return true
        }

        pendingAttackClicks =
            (pendingAttackClicks + 1).coerceAtMost(MAX_QUEUED_ATTACKS)
        releaseUntilMs = System.currentTimeMillis() + randomRelease()
        release(client, allowLag = false)
        pendingAttackReadyTick = clientTick + 2
        return false
    }

    fun prepareAuraSwing(client: Minecraft): Boolean {
        val player = client.player ?: return false
        val now = System.currentTimeMillis()

        if (isBlocking) {
            releaseUntilMs = now + randomRelease()
            release(client, allowLag = false)
            pendingAuraAttackTick = clientTick + 1
            suppressBlockThroughTick =
                maxOf(suppressBlockThroughTick, clientTick + 1)
            return false
        }

        if (
            player.isUsingItem ||
            clientTick < pendingAuraAttackTick ||
            clientTick <= lastReleaseTick ||
            clientTick <= lastAttackTick ||
            now < releaseUntilMs
        ) {
            return false
        }

        pendingAuraAttackTick = Long.MIN_VALUE
        preparedAuraAttackTick = clientTick
        lastAttackTick = clientTick
        suppressBlockThroughTick =
            maxOf(suppressBlockThroughTick, clientTick)
        return true
    }

    private fun block() {
        val mc     = Minecraft.getInstance()
        val player = mc.player     ?: return

        val expectedBlockHand = InteractionHand.entries.find { hand ->
            player.getItemInHand(hand).canUseAsBlock()
        } ?: return

        val hitResult = mc.hitResult
        val entityHit = hitResult as? net.minecraft.world.phys.EntityHitResult

        val useItemResult = if (entityHit != null) {
            interactEntityLikeVanilla(entityHit.entity, entityHit)
        } else if (hitResult != null && hitResult.type == HitResult.Type.BLOCK) {
            interactBlockLikeVanilla(hitResult as BlockHitResult)
        } else {
            useItemStrict()
        }

        isBlocking =
            useItemResult?.isUseItemSuccess == true &&
                useItemResult.hand == expectedBlockHand
    }

    fun ItemStack.canUseAsBlock(): Boolean =
        has(BLOCKS_ATTACKS) ||
                get(DataComponents.CONSUMABLE)?.animation == ItemUseAnimation.BLOCK

    private fun isUseHeld(client: Minecraft): Boolean {
        return isPhysicalKeyDown(client.options.keyUse)
    }

    private fun isPhysicalKeyDown(mapping: KeyMapping): Boolean {
        val key = InputConstants.getKey(mapping.saveString())
        return if (key.type == InputConstants.Type.MOUSE) {
            org.lwjgl.glfw.GLFW.glfwGetMouseButton(Minecraft.getInstance().window.handle(), key.value) == org.lwjgl.glfw.GLFW.GLFW_PRESS
        } else {
            InputConstants.isKeyDown(Minecraft.getInstance().window, key.value)
        }
    }

    private fun isHoldingBlockable(client: Minecraft): Boolean {
        val player = client.player ?: return false
        return InteractionHand.entries.any { hand ->
            player.getItemInHand(hand).canUseAsBlock()
        }
    }

    @JvmStatic
    var slowingDown: Boolean = false
    private fun applySlowdown(player: net.minecraft.client.player.LocalPlayer) {
        slowingDown = true
    }

    private fun removeSlowdown(player: net.minecraft.client.player.LocalPlayer) {
        slowingDown = false
    }

    private const val ATTACK_ACTIVITY_WINDOW_MS = 250L
    private const val ATTACK_PACKET_GUARD_MS = 25L
}
