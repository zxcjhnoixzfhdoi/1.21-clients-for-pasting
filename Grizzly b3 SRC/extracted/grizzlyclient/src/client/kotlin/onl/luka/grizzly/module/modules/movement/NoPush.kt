package onl.luka.grizzly.module.modules.combat

import onl.luka.grizzly.module.Module

object NoPush : Module(
    name = "No Push",
    description = "Prevents other entities from displacing you on collision",
    category = Category.MOVEMENT
)