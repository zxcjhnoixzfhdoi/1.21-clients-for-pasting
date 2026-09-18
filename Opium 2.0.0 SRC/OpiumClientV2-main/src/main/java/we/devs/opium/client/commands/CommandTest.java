package we.devs.opium.client.commands;

import we.devs.opium.Opium;
import we.devs.opium.api.manager.command.Command;
import we.devs.opium.api.manager.command.RegisterCommand;
import we.devs.opium.api.utilities.ChatUtils;
import we.devs.opium.client.gui.GamblingScreen;
import we.devs.opium.client.gui.HwidBlockerScreen;

import static we.devs.opium.api.manager.CapeManager.setCape;
import static we.devs.opium.api.manager.CapeManager.setFullAnimatedCape;

@RegisterCommand(name="Test", description="Let's you change your cape", syntax="", aliases={"test"})
public class CommandTest extends Command {
    @Override
    public void onCommand(String[] args) {
        mc.setScreen(Opium.GAMBLING_SCREEN);
        ChatUtils.sendMessage("Hello World!");
    }

}
