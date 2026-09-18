package onl.luka.grizzly.util

import net.minecraft.client.Minecraft
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.InteractionResult.SwingSource
import net.minecraft.world.entity.Entity
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.EntityHitResult

fun InteractionResult.shouldSwingHand(): Boolean =
    this is InteractionResult.Success && swingSource === SwingSource.CLIENT

data class StrictInteractionResult(
    val hand: InteractionHand,
    val source: StrictInteractionSource,
    val result: InteractionResult,
) {
    val isUseItemSuccess: Boolean
        get() = source == StrictInteractionSource.USE_ITEM &&
            result is InteractionResult.Success
}

enum class StrictInteractionSource {
    INTERACT,
    USE_ITEM_ON,
    USE_ITEM,
}

fun useItem(hand: InteractionHand): InteractionResult {
    val mc = Minecraft.getInstance()
    val player = mc.player ?: return InteractionResult.FAIL
    val gameMode = mc.gameMode ?: return InteractionResult.FAIL

    val result = gameMode.useItem(player, hand)
    if (result is InteractionResult.Success) {
        if (result.shouldSwingHand()) {
            player.swing(hand)
        }
        mc.gameRenderer.itemInHandRenderer.itemUsed(hand)
    }
    return result
}

fun useItemStrict(hand: InteractionHand): StrictInteractionResult? {
    val result = useItem(hand)
    if (result !is InteractionResult.Success) return null
    return StrictInteractionResult(hand, StrictInteractionSource.USE_ITEM, result)
}

fun useItemStrict(): StrictInteractionResult? =
    InteractionHand.entries.firstNotNullOfOrNull(::useItemStrict)

fun interactEntityLikeVanilla(
    entity: Entity,
    hitResult: EntityHitResult = EntityHitResult(entity),
): StrictInteractionResult? {
    fun interactOrUse(hand: InteractionHand): StrictInteractionResult? {
        val interactResult = interactEntity(entity, hitResult, hand) ?: return null
        if (interactResult is InteractionResult.Success) {
            return StrictInteractionResult(
                hand,
                StrictInteractionSource.INTERACT,
                interactResult,
            )
        }

        val useResult = useItem(hand)
        if (useResult !is InteractionResult.Success) return null
        return StrictInteractionResult(
            hand,
            StrictInteractionSource.USE_ITEM,
            useResult,
        )
    }

    return InteractionHand.entries.firstNotNullOfOrNull(::interactOrUse)
}

fun interactEntity(
    entity: Entity,
    hitResult: EntityHitResult = EntityHitResult(entity),
    hand: InteractionHand = InteractionHand.MAIN_HAND,
): InteractionResult? {
    val mc = Minecraft.getInstance()
    val player = mc.player ?: return null
    val gameMode = mc.gameMode ?: return null

    if (!entity.level().worldBorder.isWithinBounds(entity.blockPosition())) return null

    val result = gameMode.interact(player, entity, hitResult, hand)
    if (result.shouldSwingHand()) {
        player.swing(hand)
    }

    return result
}

fun interactBlock(
    hitResult: BlockHitResult,
    hand: InteractionHand = InteractionHand.MAIN_HAND,
): InteractionResult {
    val mc = Minecraft.getInstance()
    val player = mc.player ?: return InteractionResult.FAIL
    val gameMode = mc.gameMode ?: return InteractionResult.FAIL

    val itemStack = player.getItemInHand(hand)
    val oldCount = itemStack.count
    val result = gameMode.useItemOn(player, hand, hitResult)

    if (result.shouldSwingHand()) {
        player.swing(hand)
        if (!itemStack.isEmpty && (itemStack.count != oldCount || player.hasInfiniteMaterials())) {
            mc.gameRenderer.itemInHandRenderer.itemUsed(hand)
        }
    }

    return result
}

fun interactBlockLikeVanilla(
    hitResult: BlockHitResult,
): StrictInteractionResult? {
    fun interactOrUse(hand: InteractionHand): StrictInteractionResult? {
        val interactResult = interactBlock(hitResult, hand)
        if (
            interactResult is InteractionResult.Success ||
            interactResult is InteractionResult.Fail
        ) {
            return StrictInteractionResult(
                hand,
                StrictInteractionSource.USE_ITEM_ON,
                interactResult,
            )
        }

        val useResult = useItem(hand)
        if (useResult !is InteractionResult.Success) return null
        return StrictInteractionResult(
            hand,
            StrictInteractionSource.USE_ITEM,
            useResult,
        )
    }

    return InteractionHand.entries.firstNotNullOfOrNull(::interactOrUse)
}
