package we.devs.opium.client.modules.client;

import net.arikia.dev.drpc.DiscordRPC;
import net.arikia.dev.drpc.DiscordRichPresence;
import net.arikia.dev.drpc.DiscordEventHandlers;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerServerListWidget;
import net.minecraft.client.network.ServerInfo;
import we.devs.opium.Opium;
import we.devs.opium.api.manager.module.Module;
import we.devs.opium.api.manager.module.RegisterModule;
import we.devs.opium.api.utilities.ChatUtils;
import we.devs.opium.client.values.impl.*;

import java.awt.*;
import java.util.Objects;

@RegisterModule(name = "Discord RPC", description = "Displayes Opium As Your Dc-RPC", category = Module.Category.CLIENT)
public class ModuleRPC extends Module {
    ValueString getSetting(String name, String D, String value) {
        return new ValueString(name, name, D, CustomModeCategory, value);
    }


    private static ModuleRPC INSTANCE;
    private final ValueEnum ModeES = new ValueEnum("Mode", "Mode", "Lets You Change Between RPC Modes", modeE.Preset);
    private final ValueBoolean ShowServer = new ValueBoolean("Display Server", "Display Server", "Displayes the name of the server on rpc", true);
    private final ValueEnum BigImg = new ValueEnum("Big Image", "Big Image", "Sets The Large RPC Image", ImgE.Logo);
    private final ValueEnum SmallImg = new ValueEnum("Small Image", "Small Image", "Sets The Small RPC Image", ImgE.KenCarson);

    private final ValueCategory CustomModeCategory = new ValueCategory("Custom Mode", "Custom Mode Catagory");
    ValueString line1 = getSetting("Line 1", "Text For Line 1", "..");
    ValueString line2 = getSetting("Line 2", "Text For Line 2", "..");
    ValueString line3 = getSetting("Big Image Text", "Text For Big Image", ".");
    ValueString line4 = getSetting("Small Image Text", "Text For Small Image", "Cxiy");


    private boolean isRpcRunning = false;

    private void onInitialize() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> onClientTick());
    }
    public ModuleRPC() {
        INSTANCE = this;
    }


    @Override
    public void onEnable() {
        super.onEnable();
        startRPC();
        onInitialize();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        stopRPC();
        isRpcRunning = false;
    }

    public void onClientTick() {
        //System.out.println("RECEAVED TICK");
        if (isRpcRunning) {
            if (ModuleRPC.INSTANCE.ModeES.getValue().equals(modeE.CustomText)) updateRPC(line1.getValue(), line2.getValue(), line3.getValue(), line4.getValue());
            else {
                try {
                    ServerInfo serverEntry = mc.getCurrentServerEntry();
                    String serverName = "";
                    if (serverEntry == null) {
                        serverName = "Singleplayer";
                    } else serverName = serverEntry.name;
                    if (mc.player == null || mc.world == null) updateRPC("In Main Menu", "Using " + Opium.NAME + " " + Opium.VERSION, "","");
                    else if (ShowServer.getValue()) updateRPC("Playing On " + serverName, "Using " + Opium.NAME + " " + Opium.VERSION, "", "");
                    else updateRPC("No Peeking", "", "", "");
                } catch (Exception e) {
                    Opium.LOGGER.error("RPC Error - ", e);
                }
            }
        }
    }

    private void startRPC() {
        if (isRpcRunning) {
            return; // Prevent multiple initializations
        }

        isRpcRunning = true;

        // Create and configure DiscordEventHandlers
        DiscordEventHandlers handlers = new DiscordEventHandlers.Builder()
                .setReadyEventHandler(user -> {
                    assert mc.player != null;
                    ChatUtils.sendMessage("Discord RPC Connected To [" +  user.username + "]");
                })
                .build();

        // Initialize Discord RPC with handlers
        DiscordRPC.discordInitialize("1328117873276752006", handlers, true);

        // Create and set an initial presence
        DiscordRichPresence presence = new DiscordRichPresence.Builder("")
                .setDetails("")
                .setStartTimestamps(System.currentTimeMillis() / 1000L)
                .setBigImage("title", "")
                .setSmallImage("ken", "")
                .build();
        DiscordRPC.discordUpdatePresence(presence);
        ChatUtils.sendMessage("Discord RPC Started");
        // Start a new thread to handle Discord RPC events

        new Thread(() -> {
            while (isRpcRunning) {
                DiscordRPC.discordRunCallbacks();
                try {
                    Thread.sleep(2000); // Sleep for 2 seconds between callbacks
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, "Discord-RPC-Callback-Thread").start();
    }

    private void stopRPC() {
        if (!isRpcRunning) {
            return; // Prevent stopping if it's not running
        }

        isRpcRunning = false;
        DiscordRPC.discordShutdown();
        ChatUtils.sendMessage("Disconected From DiscordRPC");
        
    }


    private void updateRPC(String mainText, String DetailText, String BigImgText, String SmallImgText) {
        String BigImgShit = "";
        String SmallImgShit = "";
        if (ModuleRPC.INSTANCE.BigImg.getValue().equals(ImgE.Logo)) BigImgShit = "title";
        else if (ModuleRPC.INSTANCE.BigImg.getValue().equals(ImgE.KenCarson)) BigImgShit = "ken";
        else BigImgShit = "";

        if (ModuleRPC.INSTANCE.SmallImg.getValue().equals(ImgE.Logo)) SmallImgShit = "title";
        else if (ModuleRPC.INSTANCE.SmallImg.getValue().equals(ImgE.KenCarson)) SmallImgShit = "ken";
        else SmallImgShit = "";

        DiscordRichPresence presence = new DiscordRichPresence.Builder(mainText)
                .setDetails(DetailText)
                .setStartTimestamps(System.currentTimeMillis() / 1000L)
                .setBigImage(BigImgShit, BigImgText)
                .setSmallImage(SmallImgShit, SmallImgText)
                .build();
        DiscordRPC.discordUpdatePresence(presence);
    }

    public enum ImgE {
        Logo,
        KenCarson,
        None
    }

    public enum modeE {
        CustomText,
        Preset
    }


}
