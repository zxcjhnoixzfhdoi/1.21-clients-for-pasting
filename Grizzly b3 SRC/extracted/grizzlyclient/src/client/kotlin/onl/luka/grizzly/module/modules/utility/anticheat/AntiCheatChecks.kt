package onl.luka.grizzly.module.modules.utility.anticheat

import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.player.Player
import onl.luka.grizzly.module.modules.utility.CheatDetector
import java.util.UUID

internal enum class EvidenceConfidence { POSSIBLE, SUSPICIOUS, STRONG }

internal data class Evidence(
    val checkId: String,
    val checkName: String,
    val amount: Double,
    val confidence: EvidenceConfidence,
    val detail: String,
)

internal data class CheckTick(
    val client: Minecraft,
    val player: Player,
    val tracked: TrackedPlayer,
    val snapshot: PlayerSnapshot,
    val tick: Long,
    val globallyLagging: Boolean,
) {
    val exempt: Boolean get() = globallyLagging || tracked.isInGrace(tick, CheatDetector.joinGraceTicks.value)
}

internal data class AttackEvent(
    val attacker: TrackedPlayer,
    val victim: TrackedPlayer,
    val attackerEntity: Player,
    val victimEntity: Player,
    val tick: Long,
    val actionTick: Long,
    val directSource: Boolean,
    val hadSwing: Boolean,
)

internal data class PlacementEvent(
    val player: TrackedPlayer,
    val entity: Player,
    val position: BlockPos,
    val tick: Long,
    val actionTick: Long,
    val attributionGap: Double,
    val hadSwing: Boolean,
)

internal interface AntiCheatCheck {
    val id: String
    val displayName: String
    fun onTick(input: CheckTick): Evidence? = null
    fun onAttack(client: Minecraft, event: AttackEvent, globallyLagging: Boolean): Evidence? = null
    fun onPlacement(client: Minecraft, event: PlacementEvent, globallyLagging: Boolean): Evidence? = null
    fun remove(uuid: UUID) {}
    fun reset() {}
}
