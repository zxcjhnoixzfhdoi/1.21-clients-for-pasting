package we.devs.opium.client.commands;

import we.devs.opium.api.manager.command.Command;
import we.devs.opium.api.manager.command.RegisterCommand;
import we.devs.opium.api.utilities.FakePlayerEntity;
import we.devs.opium.client.modules.miscellaneous.ModuleFakePlayer;

@RegisterCommand(name = "fakeplayer", description = "Spawns A Fake Player", syntax = "fakeplayer", aliases = {"fp"})
public class CommandFakePlayer extends Command {
    @Override
    public void onCommand(String[] args) {
        ModuleFakePlayer module = ModuleFakePlayer.getInstance();
        if (module != null && module.fakePlayer == null) {
            module.fakePlayer = new FakePlayerEntity(mc.player, "Opium-Fake-Player", 20.0f, true);
            module.fakePlayer.spawn();
        } else if(module != null) {
            module.fakePlayer.despawn();
            module.fakePlayer = null;
        }
    }
}
