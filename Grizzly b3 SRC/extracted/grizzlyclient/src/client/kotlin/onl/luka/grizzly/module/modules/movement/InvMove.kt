package onl.luka.grizzly.module.modules.movement

import onl.luka.grizzly.gui.ClickGui as GrizzlyClickGui
import onl.luka.grizzly.module.Module
import onl.luka.grizzly.util.InputUtil
import onl.luka.grizzly.util.SilentScreen
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.ChatScreen
import net.minecraft.client.gui.screens.PauseScreen
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.inventory.InventoryScreen
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket
import net.minecraft.util.Mth
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.phys.Vec3
import org.lwjgl.glfw.GLFW
import java.util.concurrent.ConcurrentLinkedQueue

object InvMove : Module(
    name = "Inv Move",
    description = "Allows movement while inventory and container screens are open",
    category = Category.MOVEMENT,
) {
    private const val CLICK_STOP_TICKS = 10

    enum class InventoryMode { DISABLED, VANILLA, BLINK, CLOSE, HYPIXEL }
    enum class ContainerMode { DISABLED, VANILLA, BLINK, HYPIXEL }

    private enum class ScreenKind { NONE, CLICK_GUI, INVENTORY, CONTAINER }

    private val inventoryMode = enum("inventory", InventoryMode.VANILLA)
    private val containerMode = enum("chests and others", ContainerMode.VANILLA)
    private val motion = float("motion", 1.0f, 0.05f, 1.0f)
    private val modifyMotionAfterClick = boolean("modify motion after click", false)
    private val slowWhenNecessary = boolean("slow when necessary", false)
    private val allowJumping = boolean("allow jumping", true)
    private val allowSprinting = boolean("allow sprinting", true)
    private val allowRotating = boolean("allow rotating", true)

    private val blinkedPackets = ConcurrentLinkedQueue<Packet<*>>()
    private var activeScreen: Screen? = null
    private var slowdownTicks = 0
    private var stopMovementTicks = 0
    private var closePacketSent = false
    private var controllingScreen = false

    @Volatile
    private var replayingPackets = false

    init {
        ClientTickEvents.START_CLIENT_TICK.register(::tick)
    }

    private fun tick(client: Minecraft) {
        if (!isEnabled()) return
        val player = client.player ?: run {
            resetState(clearPackets = true)
            return
        }
        val screen = client.gui.screen()
        val kind = screenKind(screen)

        if (screen !== activeScreen) {
            if (activeScreen != null && blinkedPackets.isNotEmpty()) releasePackets()
            activeScreen = screen
            closePacketSent = false
        }

        if (kind == ScreenKind.NONE || !modeAllows(kind)) {
            if (screen == null && blinkedPackets.isNotEmpty()) releasePackets()
            slowdownTicks = 0
            stopMovementTicks = 0
            if (controllingScreen) {
                releaseManagedKeys(client, restorePhysical = screen == null)
                controllingScreen = false
            }
            return
        }
        controllingScreen = true

        if (kind == ScreenKind.INVENTORY && inventoryMode.value == InventoryMode.CLOSE && !closePacketSent) {
            closePacketSent = true
            client.connection?.send(ServerboundContainerClosePacket(player.containerMenu.containerId))
        }

        if (stopMovementTicks > 0) {
            stopMovementTicks--
            releaseManagedKeys(client, restorePhysical = false)
            applyPostClickMotion(player)
            if (allowRotating.value) rotateWithArrows(client, 1.0f)
            return
        }

        applyMovementKeys(client)
        applyPostClickMotion(player)
        if (allowRotating.value) rotateWithArrows(client, 6.0f)
    }

    private fun applyMovementKeys(client: Minecraft) {
        val options = client.options
        val sprintRequested = InputUtil.isPhysicalKeyDown(options.keySprint) || Sprint.isEnabled()
        options.keyUp.setDown(InputUtil.isPhysicalKeyDown(options.keyUp))
        options.keyDown.setDown(InputUtil.isPhysicalKeyDown(options.keyDown))
        options.keyLeft.setDown(InputUtil.isPhysicalKeyDown(options.keyLeft))
        options.keyRight.setDown(InputUtil.isPhysicalKeyDown(options.keyRight))
        options.keyJump.setDown(allowJumping.value && InputUtil.isPhysicalKeyDown(options.keyJump))
        options.keySprint.setDown(allowSprinting.value && sprintRequested)

        val player = client.player ?: return
        if (!allowSprinting.value) {
            player.setSprinting(false)
        } else if (
            sprintRequested &&
            InputUtil.isPhysicalKeyDown(options.keyUp) &&
            (player.foodData.foodLevel > 6 || player.abilities.mayfly)
        ) {
            player.setSprinting(true)
        }
    }

    private fun applyPostClickMotion(player: net.minecraft.client.player.LocalPlayer) {
        if (slowdownTicks <= 0) return
        slowdownTicks--

        val scale = if (slowWhenNecessary.value) {
            when (player.getEffect(MobEffects.SPEED)?.amplifier ?: -1) {
                1 -> 0.615
                2 -> 0.3
                else -> 0.65
            }
        } else {
            motion.value.toDouble()
        }
        if (scale >= 0.999) return
        val velocity = player.deltaMovement
        player.deltaMovement = Vec3(velocity.x * scale, velocity.y, velocity.z * scale)
    }

    private fun rotateWithArrows(client: Minecraft, amount: Float) {
        val player = client.player ?: return
        val window = client.window.handle()
        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_DOWN) == GLFW.GLFW_PRESS) {
            player.xRot = Mth.clamp(player.xRot + amount, -90.0f, 90.0f)
        }
        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_UP) == GLFW.GLFW_PRESS) {
            player.xRot = Mth.clamp(player.xRot - amount, -90.0f, 90.0f)
        }
        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT) == GLFW.GLFW_PRESS) {
            player.yRot += amount
        }
        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT) == GLFW.GLFW_PRESS) {
            player.yRot -= amount
        }
    }

    private fun modeAllows(kind: ScreenKind): Boolean = when (kind) {
        ScreenKind.CLICK_GUI -> false
        ScreenKind.INVENTORY -> inventoryMode.value != InventoryMode.DISABLED
        ScreenKind.CONTAINER -> containerMode.value != ContainerMode.DISABLED
        ScreenKind.NONE -> false
    }

    private fun isBlinkMode(kind: ScreenKind): Boolean = when (kind) {
        ScreenKind.CLICK_GUI -> false
        ScreenKind.INVENTORY -> inventoryMode.value == InventoryMode.BLINK
        ScreenKind.CONTAINER -> containerMode.value == ContainerMode.BLINK
        ScreenKind.NONE -> false
    }

    private fun isHypixelMode(kind: ScreenKind): Boolean = when (kind) {
        ScreenKind.CLICK_GUI -> false
        ScreenKind.INVENTORY -> inventoryMode.value == InventoryMode.HYPIXEL
        ScreenKind.CONTAINER -> containerMode.value == ContainerMode.HYPIXEL
        ScreenKind.NONE -> false
    }

    @JvmStatic
    fun shouldKeepMouseGrabbed(): Boolean {
        if (!isEnabled() || !allowRotating.value) return false
        val screen = Minecraft.getInstance().gui.screen()
        if (screen !is SilentScreen) return false
        return modeAllows(screenKind(screen))
    }

    private fun screenKind(screen: Screen?): ScreenKind {
        val actual = if (screen is SilentScreen) screen.wrapped else screen
        return when (actual) {
            null, is ChatScreen, is PauseScreen -> ScreenKind.NONE
            is GrizzlyClickGui -> ScreenKind.CLICK_GUI
            is InventoryScreen -> ScreenKind.INVENTORY
            else -> ScreenKind.CONTAINER
        }
    }

    private fun releaseManagedKeys(client: Minecraft, restorePhysical: Boolean) {
        val options = client.options
        options.keyUp.setDown(restorePhysical && InputUtil.isPhysicalKeyDown(options.keyUp))
        options.keyDown.setDown(restorePhysical && InputUtil.isPhysicalKeyDown(options.keyDown))
        options.keyLeft.setDown(restorePhysical && InputUtil.isPhysicalKeyDown(options.keyLeft))
        options.keyRight.setDown(restorePhysical && InputUtil.isPhysicalKeyDown(options.keyRight))
        options.keyJump.setDown(restorePhysical && InputUtil.isPhysicalKeyDown(options.keyJump))
        options.keySprint.setDown(
            restorePhysical && (InputUtil.isPhysicalKeyDown(options.keySprint) || Sprint.isEnabled()),
        )
    }

    private fun resetState(clearPackets: Boolean) {
        slowdownTicks = 0
        stopMovementTicks = 0
        closePacketSent = false
        activeScreen = null
        controllingScreen = false
        if (clearPackets) blinkedPackets.clear()
    }

    private fun releasePackets() {
        if (blinkedPackets.isEmpty()) return
        val connection = Minecraft.getInstance().connection
        if (connection == null) {
            blinkedPackets.clear()
            return
        }

        replayingPackets = true
        try {
            while (true) {
                val packet = blinkedPackets.poll() ?: break
                connection.send(packet)
            }
        } finally {
            replayingPackets = false
        }
    }

    @JvmStatic
    fun handleOutgoingPacket(packet: Packet<*>): Boolean {
        if (!isEnabled() || replayingPackets) return false
        val kind = screenKind(Minecraft.getInstance().gui.screen())

        if (packet is ServerboundContainerClickPacket && modeAllows(kind)) {
            registerClickEffects(kind)
            if (isBlinkMode(kind)) {
                blinkedPackets.add(packet)
                return true
            }
        }

        if (packet is ServerboundContainerClosePacket && blinkedPackets.isNotEmpty()) {
            blinkedPackets.add(packet)
            releasePackets()
            return true
        }
        return false
    }

    @JvmStatic
    fun onContainerSlotClicked() {
        if (!isEnabled()) return
        val client = Minecraft.getInstance()
        val kind = screenKind(client.gui.screen())
        if (!modeAllows(kind)) return

        registerClickEffects(kind)
        if (stopMovementTicks > 0) {
            releaseManagedKeys(client, restorePhysical = false)
        }
    }

    private fun registerClickEffects(kind: ScreenKind) {
        if (isHypixelMode(kind) || modifyMotionAfterClick.value) {
            stopMovementTicks = maxOf(stopMovementTicks, CLICK_STOP_TICKS)
        }
        if (modifyMotionAfterClick.value || (slowWhenNecessary.value && !isBlinkMode(kind))) {
            slowdownTicks = maxOf(slowdownTicks, CLICK_STOP_TICKS)
        }
    }

    @JvmStatic
    fun controlsCurrentScreen(client: Minecraft = Minecraft.getInstance()): Boolean {
        if (!isEnabled()) return false
        val kind = screenKind(client.gui.screen())
        return kind != ScreenKind.NONE && modeAllows(kind)
    }

    @JvmStatic
    fun allowsClientKeybindsInCurrentScreen(client: Minecraft = Minecraft.getInstance()): Boolean {
        if (!isEnabled()) return false
        return when (val kind = screenKind(client.gui.screen())) {
            ScreenKind.INVENTORY, ScreenKind.CONTAINER -> modeAllows(kind)
            ScreenKind.NONE, ScreenKind.CLICK_GUI -> false
        }
    }

    @JvmStatic
    fun allowsMovementInCurrentScreen(client: Minecraft = Minecraft.getInstance()): Boolean =
        controlsCurrentScreen(client) && stopMovementTicks == 0

    override fun onDisabled() {
        releasePackets()
        val client = Minecraft.getInstance()
        releaseManagedKeys(client, restorePhysical = client.gui.screen() == null)
        resetState(clearPackets = true)
    }

    override fun hudInfo(): String {
        val inventory = inventoryMode.value.name.lowercase()
        val containers = containerMode.value.name.lowercase()
        if (inventory == "disabled") return containers
        if (containers == "disabled") return inventory
        return if (inventory == containers) inventory else "$inventory / $containers"
    }
}
