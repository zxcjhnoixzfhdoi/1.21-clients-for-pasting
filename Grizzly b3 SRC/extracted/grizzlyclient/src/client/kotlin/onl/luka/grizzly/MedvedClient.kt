package onl.luka.grizzly

import onl.luka.grizzly.alt.AltManager
import onl.luka.grizzly.config.ConfigManager
import onl.luka.grizzly.module.ModuleManager
import onl.luka.grizzly.module.modules.exploits.*
import onl.luka.grizzly.module.modules.combat.AutoAnchor
import onl.luka.grizzly.module.modules.anarchy.AutoHvHGapple
import onl.luka.grizzly.module.modules.anarchy.CrystalAura
import onl.luka.grizzly.module.modules.anarchy.KillAura
import onl.luka.grizzly.module.modules.combat.*
import onl.luka.grizzly.module.modules.hud.*
import onl.luka.grizzly.module.modules.minigames.*
import onl.luka.grizzly.module.modules.movement.*
import onl.luka.grizzly.module.modules.movement.Parkour
import onl.luka.grizzly.module.modules.other.*
import onl.luka.grizzly.module.modules.player.*
import onl.luka.grizzly.module.modules.render.*
import onl.luka.grizzly.module.modules.skyblock.*
import onl.luka.grizzly.module.modules.utility.AutoEat
import onl.luka.grizzly.module.modules.utility.*
import onl.luka.grizzly.module.modules.utility.anticheat.CheatDatabase
import onl.luka.grizzly.module.modules.world.*
import onl.luka.grizzly.module.modules.world.scaffold.Scaffold
import onl.luka.grizzly.util.LagManager
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.loader.api.FabricLoader
import onl.luka.grizzly.module.modules.player.ClientBrand
import org.lwjgl.glfw.GLFW

object MedvedClient : ClientModInitializer {

    private var insertWasDown = false

    override fun onInitializeClient() {
        val gameDir = FabricLoader.getInstance().gameDir

        ConfigManager.init(gameDir.resolve("config/medved"))
        AltManager.init(gameDir.resolve("config/medved"))

        // combat
        ModuleManager.register(SilentAura)
        ModuleManager.register(TriggerBot)
        ModuleManager.register(AimAssist)
        ModuleManager.register(LeftClicker)
        ModuleManager.register(RightClicker)
        ModuleManager.register(NoHitDelay)
        ModuleManager.register(AutoBlock)
        ModuleManager.register(HitSelect)
        ModuleManager.register(Velocity)
        ModuleManager.register(JumpReset)
        ModuleManager.register(Refill)
        ModuleManager.register(ComboTap)
        ModuleManager.register(KnockbackDelay)
        ModuleManager.register(Reach)
        ModuleManager.register(KnockbackDisplacement)
        ModuleManager.register(Backtrack)
        ModuleManager.register(Misplace)
        ModuleManager.register(Criticals)
        ModuleManager.register(AutoRod)
        ModuleManager.register(HitSwap)
        ModuleManager.register(AutoCrystal)
        ModuleManager.register(KeepSprint)
        ModuleManager.register(AutoMace)
        ModuleManager.register(AutoAnchor)
        // exploits
        ModuleManager.register(Disabler)
        ModuleManager.register(Regen)
        ModuleManager.register(AntiCactus)
        ModuleManager.register(AntiHunger)
        ModuleManager.register(PortalGodmode)
        ModuleManager.register(NoSwing)
        ModuleManager.register(AntiAura)
        ModuleManager.register(ComboOneHit)
        ModuleManager.register(ResetVL)
        ModuleManager.register(ChatBypass)
        // anarchy
        ModuleManager.register(AutoHvHGapple)
        ModuleManager.register(KillAura)
        ModuleManager.register(CrystalAura)
        // movement
        ModuleManager.register(Sprint)
        ModuleManager.register(Speed)
        ModuleManager.register(Step)
        ModuleManager.register(Phase)
        ModuleManager.register(Spin)
        ModuleManager.register(MoveFix)
        ModuleManager.register(Stasis)
        ModuleManager.register(InvMove)
        ModuleManager.register(Flight)
        ModuleManager.register(Timer)
        ModuleManager.register(NoFall)
        ModuleManager.register(AntiVoid)
        ModuleManager.register(NoJumpDelay)
        ModuleManager.register(NoPush)
        ModuleManager.register(Parkour)
        // render
        ModuleManager.register(Animations)
        ModuleManager.register(ESP3D)
        ModuleManager.register(ESP2D)
        ModuleManager.register(TargetESP)
        ModuleManager.register(Chams)
        ModuleManager.register(Nametags)
        ModuleManager.register(RiceFarmer)
        ModuleManager.register(Skeletons)
        ModuleManager.register(Ambience)
        // player
        ModuleManager.register(FastMine)
        ModuleManager.register(FastEat)
        ModuleManager.register(BlockIn)
        ModuleManager.register(TimerRange)
        ModuleManager.register(FakeLag)
        ModuleManager.register(Blink)
        ModuleManager.register(ClientBrand)
        // world
        ModuleManager.register(Scaffold)
        ModuleManager.register(Clutch)
        ModuleManager.register(FastPlace)
        ModuleManager.register(AutoPlace)
        ModuleManager.register(ChestStealer)
        ModuleManager.register(BedBreaker)
        ModuleManager.register(ChestAura)
        ModuleManager.register(Nuker)
        // other
        ModuleManager.register(ClickGui)
        ModuleManager.register(Font)
        ModuleManager.register(Colour)
        ModuleManager.register(Rotations)
        ModuleManager.register(TargetFilter)
        ModuleManager.register(Commands)
        ModuleManager.register(AutoConfig)
        ModuleManager.register(Notifications)
        ModuleManager.register(AutoUpdate)
        // hud
        ModuleManager.register(ModulesList)
        ModuleManager.register(Watermark)
        ModuleManager.register(ScoreboardHud)
        ModuleManager.register(TargetHud)
        ModuleManager.register(ScaffoldInfo)
        ModuleManager.register(BedPlates)
        // skyblock
        ModuleManager.register(CorpseFinder)
        ModuleManager.register(FrozenTreasuresESP)
        ModuleManager.register(MineshaftOreGuide)
        ModuleManager.register(EfficientMinerOverlay)
        ModuleManager.register(ClickLock)
        ModuleManager.register(AutoPickaxeAbility)
        ModuleManager.register(MiningAssist)
        ModuleManager.register(PowderChestSolver)
        ModuleManager.register(DojoHelper)
        ModuleManager.register(DanceRoomHelper)
        ModuleManager.register(AutoReel)
        // utility
        ModuleManager.register(AntiFireball)
        ModuleManager.register(AutoEat)
        ModuleManager.register(AutoTotem)
        ModuleManager.register(InventoryManager)
        ModuleManager.register(CheatDetector)
        // minigames
        ModuleManager.register(PartyGames)
        ModuleManager.register(QuakecraftRage)
        ModuleManager.register(Bedwars)
        Bedwars.initializeContext()

        ModuleManager.init()
        AutoConfig.init()
        CheatDatabase.init(gameDir.resolve("config/medved"))
        AutoUpdate.init()

        ClientTickEvents.END_CLIENT_TICK.register { _ ->
            LagManager.onTick()
        }
        ClientPlayConnectionEvents.DISCONNECT.register { _, _ ->
            Bedwars.onWorldLeave()
        }
    }

    @JvmStatic
    fun pollFrameInput() {
        val mc = net.minecraft.client.Minecraft.getInstance()
        val handle = mc.window.handle()
        val down = GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_INSERT) == GLFW.GLFW_PRESS
        if (mc.gui.screen() == null && down && !insertWasDown) ClickGui.toggle()
        insertWasDown = down
    }
}
