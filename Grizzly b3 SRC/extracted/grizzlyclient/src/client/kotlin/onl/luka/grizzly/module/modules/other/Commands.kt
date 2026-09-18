package onl.luka.grizzly.module.modules.other

import onl.luka.grizzly.module.Module

object Commands : Module(
    name = "Commands",
    description = "Client-side ',' commands and public '!' chat commands",
    category = Category.OTHER,
) {

    override val isProtected = true
    override val showInModulesList = false

    init { enabled.value = true }

    val clientPrefix = string("client prefix", ",")

    val publicCommands = boolean("public commands", true)

    val prefix = string("public prefix", "!")
        .also { it.visibleWhen = { publicCommands.value } }

    val partyGuildOnly = boolean("party/guild only", false)
        .also { it.visibleWhen = { publicCommands.value } }
}
