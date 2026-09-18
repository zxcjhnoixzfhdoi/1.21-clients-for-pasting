package onl.luka.grizzly.module.modules.other

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.network.chat.Component
import onl.luka.grizzly.module.Module
import onl.luka.grizzly.update.BuildStamp
import onl.luka.grizzly.update.RemoteBuild
import onl.luka.grizzly.update.UpdateInstaller
import onl.luka.grizzly.update.UpdateSource
import onl.luka.grizzly.util.NotificationManager

object AutoUpdate : Module(
    "Auto Update",
    "Keeps the client up to date with the latest build from your chosen channel",
    Category.OTHER,
) {
    override val includeInPresets = false
    override val showInModulesList = false

    init { enabled.value = true }

    val channel = enum("channel", UpdateSource.Channel.BETA)
    val branch = string("branch", "main").also {
        it.visibleWhen = { channel.value == UpdateSource.Channel.BETA }
    }
    val install = boolean("install automatically", true)
    val notify = boolean("notify", true)
    val checkNow = button("check now", "check") { check(manual = true) }

    private val seen = mutableSetOf<String>()
    @Volatile private var chatBacklog: List<String> = emptyList()
    private var ticksUntilCheck = STARTUP_DELAY_TICKS
    @Volatile private var busy = false
    private var registered = false

    fun init() {
        if (registered) return
        registered = true
        reportLastInstall()
        UpdateInstaller.cleanup()

        ClientTickEvents.END_CLIENT_TICK.register { client ->
            chatBacklog.takeIf { it.isNotEmpty() && client.player != null }?.let { lines ->
                lines.forEach { client.player?.sendSystemMessage(Component.literal(it)) }
                chatBacklog = emptyList()
            }

            if (ticksUntilCheck < 0) return@register
            if (--ticksUntilCheck >= 0) return@register
            if (isEnabled()) check(manual = false)
        }
    }

    private fun reportLastInstall() {
        val pending = UpdateInstaller.consumePending() ?: return
        if (!notify.value) return
        val installed = pending.commit == BuildStamp.local.commit &&
            pending.commitTime == BuildStamp.local.commitTime

        if (installed) {
            NotificationManager.showConfig(name, "Updated to ${pending.label}", 6000L)
            chatBacklog = listOf("$PREFIX§fUpdated to §a${pending.label}§f.")
        } else {
            NotificationManager.showError(name, "${pending.label} failed to install", 6000L)
            chatBacklog = listOf(
                "$PREFIX§fThe update to §a${pending.label}§f did not install.",
                "$PREFIX§7Still running ${BuildStamp.local.describe()}.",
            )
        }
    }

    private fun check(manual: Boolean) {
        if (busy) return
        if (!BuildStamp.canSelfUpdate) {
            if (manual) NotificationManager.showError(name, reasonCannotUpdate())
            return
        }

        busy = true
        Thread {
            val build = runCatching { resolve() }.getOrNull()
            val loud = notify.value || manual
            when {
                build == null -> if (manual) NotificationManager.show(name, staleMessage())
                !install.value -> if (loud) NotificationManager.show(name, "Update available: ${build.label}")
                else -> when (UpdateInstaller.stage(build, announce = notify.value)) {
                    UpdateInstaller.Result.STAGED -> if (loud) {
                        NotificationManager.showConfig(name, "${build.label} installs when you quit", 6000L)
                        chatBacklog = listOf("$PREFIX§f§a${build.label}§f downloaded, installs when you quit.")
                    }
                    UpdateInstaller.Result.ALREADY_CURRENT -> {
                        seen += build.label
                        if (manual) NotificationManager.show(name, "Already on the latest build")
                    }
                    UpdateInstaller.Result.INCOMPATIBLE -> {
                        seen += build.label
                        if (loud) NotificationManager.showError(
                            name,
                            "Skipped ${build.label}: ${UpdateInstaller.lastRejection.orEmpty()}",
                            6000L,
                        )
                    }
                    UpdateInstaller.Result.FAILED ->
                        if (loud) NotificationManager.showError(name, "Could not install ${build.label}")
                }
            }
            busy = false
        }.also { it.isDaemon = true }.start()
    }

    private fun resolve(): RemoteBuild? {
        if (UpdateInstaller.isArmed) return null
        val build = UpdateSource.latest(channel.value, branch.value) ?: return null
        if (build.label in seen) return null
        if (build.commit != null && build.commit == BuildStamp.local.commit) return null
        if (build.publishedAt <= BuildStamp.local.commitTime) return null
        return build
    }

    private fun staleMessage(): String =
        if (UpdateInstaller.isArmed) "An update is already waiting for you to quit"
        else "Already on the latest build"

    private fun reasonCannotUpdate(): String = when {
        BuildStamp.jar == null -> "Not running from a mods folder"
        !BuildStamp.local.isPresent -> "This build carries no version stamp"
        else -> "This is a local build, not one from CI"
    }

    override fun hudInfo(): String = channel.value.name.lowercase()

    private const val STARTUP_DELAY_TICKS = 100
    private const val PREFIX = "§8[§6Grizzly§8]§r "
}
