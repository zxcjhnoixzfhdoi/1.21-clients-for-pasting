package onl.luka.grizzly.module.modules.skyblock

import com.google.gson.JsonParser
import com.mojang.blaze3d.platform.InputConstants
import onl.luka.grizzly.module.Module
import onl.luka.grizzly.util.HttpUtils
import onl.luka.grizzly.util.InputUtil
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.world.phys.AABB
import org.slf4j.LoggerFactory
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import kotlin.math.abs

object DanceRoomHelper : Module(
    "Dance Room Helper",
    "Performs the Mirrorverse dance actions in time with the room's sound cues",
    Category.SKYBLOCK,
) {
    private val earlyOffsetMs = int("early offset ms", 250, 0, 500)
    private val keyPressMs = int("key press ms", 250, 50, 250)

    private val scheduler = Executors.newSingleThreadScheduledExecutor { task ->
        Thread(task, "Medved-DanceRoomHelper").apply { isDaemon = true }
    }
    private val logger = LoggerFactory.getLogger("medved/dance-room-helper")

    @Volatile
    private var scheduleGeneration = 0L
    @Volatile
    private var instructions: List<String> = emptyList()
    @Volatile
    private var loadingInstructions = false
    @Volatile
    private var loadFailed = false
    private var scheduledAction: ScheduledFuture<*>? = null
    private var instructionIndex = 0
    private var inRoom = false
    private var shiftUntilMs = 0L
    private var jumpUntilMs = 0L
    private var forwardUntilMs = 0L
    private var lastSuccessSoundMs = 0L
    private var lastFailureSoundMs = 0L

    init {
        requestInstructions()
    }

    override fun onEnabled() {
        resetSequence(Minecraft.getInstance(), startImmediately = false)
        requestInstructions()
    }

    override fun onDisabled() {
        cancelScheduledAction()
        releaseInputs(Minecraft.getInstance())
        inRoom = false
        instructionIndex = 0
    }

    override fun hudInfo(): String = when {
        instructions.isNotEmpty() -> "${instructions.size} steps"
        loadingInstructions -> "Loading"
        loadFailed -> "Unavailable"
        else -> ""
    }

    override fun onTick(client: Minecraft) {
        val player = client.player
        val nowInRoom = player != null && DANCE_ROOM.contains(player.position())

        if (nowInRoom != inRoom) {
            inRoom = nowInRoom
            if (nowInRoom) {
                resetSequence(client, startImmediately = true)
            } else {
                cancelScheduledAction()
                releaseInputs(client)
            }
        }

        if (!inRoom) return
        applyInputOverrides(client, System.currentTimeMillis())
    }

    @JvmStatic
    fun onSound(soundId: String, volume: Float, pitch: Float) {
        if (!isEnabled() || !inRoom) return

        val now = System.currentTimeMillis()
        when {
            isSuccessSound(soundId, volume, pitch) -> {
                if (now - lastSuccessSoundMs < SOUND_DEBOUNCE_MS) return
                lastSuccessSoundMs = now
                instructionIndex++
                scheduleInstruction(instructionIndex, nextActionDelayMs())
            }

            isFailureSound(soundId, volume, pitch) -> {
                if (now - lastFailureSoundMs < SOUND_DEBOUNCE_MS) return
                lastFailureSoundMs = now
                resetSequence(Minecraft.getInstance(), startImmediately = true)
            }
        }
    }

    private fun resetSequence(client: Minecraft, startImmediately: Boolean) {
        cancelScheduledAction()
        releaseInputs(client)
        instructionIndex = 0
        lastSuccessSoundMs = 0L
        lastFailureSoundMs = 0L
        if (startImmediately) scheduleInstruction(0, 0L)
    }

    private fun requestInstructions() {
        if (loadingInstructions) return
        loadingInstructions = true
        loadFailed = false
        Thread {
            val result = runCatching {
                val json = HttpUtils.httpGet(
                    INSTRUCTIONS_URL,
                    mapOf(
                        "User-Agent" to "MedvedClient",
                        "Cache-Control" to "no-cache",
                    ),
                )
                JsonParser.parseString(json)
                    .asJsonObject
                    .getAsJsonArray("instructions")
                    .map { it.asString.trim() }
                    .filter(String::isNotEmpty)
                    .also { require(it.isNotEmpty()) { "The instruction list is empty" } }
            }

            result.onSuccess { loaded ->
                val recoveringFirstLoad = instructions.isEmpty()
                instructions = loaded
                loadingInstructions = false
                loadFailed = false
                logger.info("Loaded {} Dance Room instructions from SkyHanni", loaded.size)

                Minecraft.getInstance().execute {
                    if (recoveringFirstLoad && isEnabled() && inRoom) {
                        resumeCurrentInstruction()
                    }
                }
            }.onFailure { error ->
                loadingInstructions = false
                loadFailed = instructions.isEmpty()
                logger.warn("Failed to load Dance Room instructions from SkyHanni", error)

                Minecraft.getInstance().execute {
                    if (isEnabled() && instructions.isEmpty()) {
                        Minecraft.getInstance().player?.sendSystemMessage(
                            Component.literal(
                                "§c[Medved] Failed to load Dance Room instructions: " +
                                    (error.message ?: error.javaClass.simpleName),
                            ),
                        )
                        scheduleInstructionRetry()
                    }
                }
            }
        }.also {
            it.name = "Medved-DanceRoomInstructions"
            it.isDaemon = true
            it.start()
        }
    }

    private fun scheduleInstructionRetry() {
        scheduler.schedule({
            if (isEnabled() && instructions.isEmpty()) {
                requestInstructions()
            }
        }, LOAD_RETRY_SECONDS, TimeUnit.SECONDS)
    }

    private fun resumeCurrentInstruction() {
        val elapsedSinceCue = if (lastSuccessSoundMs == 0L) {
            0L
        } else {
            System.currentTimeMillis() - lastSuccessSoundMs
        }
        val delay = if (lastSuccessSoundMs == 0L) {
            0L
        } else {
            (nextActionDelayMs() - elapsedSinceCue).coerceAtLeast(0L)
        }
        scheduleInstruction(instructionIndex, delay)
    }

    private fun scheduleInstruction(index: Int, delayMs: Long) {
        cancelScheduledAction()
        val instruction = instructions.getOrNull(index) ?: return
        val generation = scheduleGeneration
        scheduledAction = scheduler.schedule({
            Minecraft.getInstance().execute {
                if (!isEnabled() || !inRoom || generation != scheduleGeneration || instructionIndex != index) {
                    return@execute
                }
                performInstruction(Minecraft.getInstance(), instruction)
            }
        }, delayMs, TimeUnit.MILLISECONDS)
    }

    private fun cancelScheduledAction() {
        scheduleGeneration++
        scheduledAction?.cancel(false)
        scheduledAction = null
    }

    private fun performInstruction(client: Minecraft, instruction: String) {
        if (client.player == null || client.gui.screen() != null) return

        val actions = instruction.lowercase().split(' ').toSet()

        val now = System.currentTimeMillis()
        if ("sneak" in actions) {
            shiftUntilMs = now + keyPressMs.value
        } else {
            shiftUntilMs = 0L
            restorePhysicalState(client.options.keyShift)
        }
        if ("move" in actions) {
            forwardUntilMs = now + keyPressMs.value
        }
        if ("jump" in actions) {
            jumpUntilMs = now + keyPressMs.value
        }
        if ("punch" in actions) {
            KeyMapping.click(InputConstants.getKey(client.options.keyAttack.saveString()))
        }
        applyInputOverrides(client, now)
    }

    private fun applyInputOverrides(client: Minecraft, now: Long) {
        if (shiftUntilMs > now) {
            client.options.keyShift.setDown(true)
        } else if (shiftUntilMs != 0L) {
            shiftUntilMs = 0L
            restorePhysicalState(client.options.keyShift)
        }

        if (jumpUntilMs > now) {
            client.options.keyJump.setDown(true)
        } else if (jumpUntilMs != 0L) {
            jumpUntilMs = 0L
            restorePhysicalState(client.options.keyJump)
        }

        if (forwardUntilMs > now) {
            //client.options.keyUp.setDown(true)
        } else if (forwardUntilMs != 0L) {
            forwardUntilMs = 0L
            //restorePhysicalState(client.options.keyUp)
        }
    }

    private fun releaseInputs(client: Minecraft) {
        shiftUntilMs = 0L
        jumpUntilMs = 0L
        forwardUntilMs = 0L
        restorePhysicalState(client.options.keyShift)
        restorePhysicalState(client.options.keyJump)
        restorePhysicalState(client.options.keyUp)
    }

    private fun restorePhysicalState(mapping: KeyMapping) {
        mapping.setDown(InputUtil.isPhysicalKeyDown(mapping))
    }

    private fun nextActionDelayMs(): Long =
        (DANCE_INTERVAL_MS - earlyOffsetMs.value).coerceAtLeast(0).toLong()

    private fun isSuccessSound(soundId: String, volume: Float, pitch: Float): Boolean =
        soundId.endsWith("block.note_block.bass") &&
            closeTo(volume, 1f) &&
            (closeTo(pitch, SUCCESS_PITCH_HIGH) || closeTo(pitch, SUCCESS_PITCH_LOW))

    private fun isFailureSound(soundId: String, volume: Float, pitch: Float): Boolean =
        (soundId.endsWith("entity.player.burp") && closeTo(volume, 0.8f)) ||
            (soundId.endsWith("entity.player.levelup") &&
                closeTo(volume, 1f) &&
                closeTo(pitch, FAILURE_PITCH))

    private fun closeTo(actual: Float, expected: Float): Boolean =
        abs(actual - expected) < FLOAT_EPSILON

    private const val INSTRUCTIONS_URL =
        "https://raw.githubusercontent.com/hannibal002/SkyHanni-REPO/refs/heads/main/" +
            "constants/DanceRoomInstructions.json"
    private const val DANCE_INTERVAL_MS = 1_000
    private const val LOAD_RETRY_SECONDS = 5L
    private const val SOUND_DEBOUNCE_MS = 100L
    private const val SUCCESS_PITCH_HIGH = 0.6984127f
    private const val SUCCESS_PITCH_LOW = 0.52380955f
    private const val FAILURE_PITCH = 1.8412699f
    private const val FLOAT_EPSILON = 0.0001f

    private val DANCE_ROOM = AABB(
        -267.0, 32.0, -110.0,
        -260.0, 40.0, -102.0,
    )
}
