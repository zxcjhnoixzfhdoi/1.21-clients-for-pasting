package we.devs.opium;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWImage;
import we.devs.opium.api.manager.command.CommandManager;
import we.devs.opium.api.manager.element.ElementManager;
import we.devs.opium.api.manager.event.EventListener;
import we.devs.opium.api.manager.event.EventManager;
import we.devs.opium.api.manager.friend.FriendManager;
import we.devs.opium.api.manager.miscellaneous.ConfigManager;
import we.devs.opium.api.manager.miscellaneous.PlayerManager;
import we.devs.opium.api.manager.module.ModuleManager;
import we.devs.opium.api.manager.music.CustomSoundEngineManager;
import we.devs.opium.api.manager.music.MusicDownloader;
import we.devs.opium.api.manager.rotation.RotationManager;
import we.devs.opium.api.utilities.TPSUtils;
import we.devs.opium.api.utilities.dump.AntiDump;
import we.devs.opium.client.events.EventTick;
import we.devs.opium.client.gui.GamblingScreen;
import we.devs.opium.client.gui.click.ClickGuiScreen;
import we.devs.opium.client.gui.config.ConfigManagerScreen;
import we.devs.opium.client.gui.hud.HudEditorScreen;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import we.devs.opium.api.manager.miscellaneous.FontManager;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.Timer;


public class Opium implements ModInitializer {

    public static final String NAME = "Opium";
    public static final String VERSION = "2.0.0";
    public static final Logger LOGGER = LoggerFactory.getLogger("Opium");

    public static Color COLOR_CLIPBOARD;
    public static CommandManager COMMAND_MANAGER;
    public static EventManager EVENT_MANAGER;
    public static FriendManager FRIEND_MANAGER;
    public static ModuleManager MODULE_MANAGER;
    public static ElementManager ELEMENT_MANAGER;
    public static PlayerManager PLAYER_MANAGER;
    public static ClickGuiScreen CLICK_GUI;
    public static HudEditorScreen HUD_EDITOR;
    public static ConfigManagerScreen CONFIG_MANAGER_SCREEN;
    public static GamblingScreen GAMBLING_SCREEN;
    public static ConfigManager CONFIG_MANAGER;
    public static FontManager FONT_MANAGER;
    public static RotationManager ROTATION_MANAGER;
    public static final Timer TIMER = new Timer("Timer", true);
    public static MinecraftClient mc;
    boolean iconSet = false;
    public static boolean devEnv;

    public static final boolean NO_TELEMETRY = System.getenv("NO_TELEMETRY") != null;

    private static void onPlayDisconnect(ClientPlayNetworkHandler handler, MinecraftClient client) {
        try {
            CONFIG_MANAGER.saveConfig("OpiumCfg");
        } catch (IOException e) {
            LOGGER.error("Failed to save config: {}", e.getMessage());
        }
        LOGGER.info("Player Left World -> Saved Configs");
    }

    @Override
    public void onInitialize() {
        mc = MinecraftClient.getInstance();
        long startTime = System.currentTimeMillis();
        LOGGER.info("Initialization process for Opium has started!");

        devEnv = mc.getSession().getAccessToken().equals("FabricMC");

        if (devEnv) {
            Opium.LOGGER.warn("Dev env detected, ignoring UUID update step.");
        }

        MusicDownloader.downloadMusicFiles();

        //HWIDValidator.isHWIDValid(devEnv);

        EVENT_MANAGER = new EventManager();
        COMMAND_MANAGER = new CommandManager();
        FRIEND_MANAGER = new FriendManager();
        MODULE_MANAGER = new ModuleManager();
        ELEMENT_MANAGER = new ElementManager();
        PLAYER_MANAGER = new PlayerManager();
        CONFIG_MANAGER = new ConfigManager();
        GAMBLING_SCREEN = new GamblingScreen();
        ROTATION_MANAGER = new RotationManager();

        LOGGER.info("Managers loaded successfully!");

        CLICK_GUI = new ClickGuiScreen();
        HUD_EDITOR = new HudEditorScreen();
        CONFIG_MANAGER_SCREEN = new ConfigManagerScreen();

        LOGGER.info("GUI screens loaded successfully!");

        new TPSUtils();

        EVENT_MANAGER.register(new EventListener() {
            private boolean fontsInitialized = false;

            @Override
            public void onTick(EventTick event) {
                if (!fontsInitialized && MinecraftClient.getInstance().getWindow() != null) {
                    FONT_MANAGER = new FontManager();
                    FONT_MANAGER.registerFonts();
                    fontsInitialized = true;
                    LOGGER.info("FontManager initialized successfully!");
                }
            }
        });

        TIMER.scheduleAtFixedRate(AntiDump.get(), 50, 50);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!iconSet && MinecraftClient.getInstance().getWindow() != null) {
                setWindowIcon();
                iconSet = true;
            }
        });

        Runtime.getRuntime().addShutdownHook(new Thread(CustomSoundEngineManager::cleanup));

        long endTime = System.currentTimeMillis();
        LOGGER.info("Initialization process for Opium has finished! Took {} ms", endTime - startTime);

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if (client.player != null) {
                try {
                    CONFIG_MANAGER.loadConfig("OpiumCfg");
                } catch (IOException e) {
                    LOGGER.error("Failed to load config: {}", e.getMessage());
                }
                LOGGER.info("Player {} joined the game. -> Loaded Configs", client.player.getName().getString());
            }
        });
        ClientPlayConnectionEvents.DISCONNECT.register(Opium::onPlayDisconnect);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> onPlayDisconnect(null, MinecraftClient.getInstance())));
    }

    private void setWindowIcon() {
        try {
            long windowHandle = MinecraftClient.getInstance().getWindow().getHandle();
            // Load the icon using an Identifier
            Identifier iconPath = Identifier.of("opium", "icons/opium-small.png");
            InputStream iconStream = MinecraftClient.getInstance()
                    .getResourceManager()
                    .getResource(iconPath)
                    .orElseThrow(() -> new IllegalArgumentException("Icon not found: " + iconPath))
                    .getInputStream();

            // Read the image
            BufferedImage image = ImageIO.read(iconStream);
            ByteBuffer iconBuffer = convertToByteBuffer(image);

            // Create GLFW image
            GLFWImage.Buffer imageBuffer = GLFWImage.malloc(1);
            GLFWImage icon = imageBuffer.get(0);
            icon.set(image.getWidth(), image.getHeight(), iconBuffer);

            // Set the icon
            GLFW.glfwSetWindowIcon(windowHandle, imageBuffer);

            // Free resources
            imageBuffer.free();
        } catch (Exception e) {
            Opium.LOGGER.error("Failed to load icon: {}", e.getMessage());
        }
    }

    private ByteBuffer convertToByteBuffer(BufferedImage image) {
        int[] pixels = image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
        ByteBuffer buffer = ByteBuffer.allocateDirect(image.getWidth() * image.getHeight() * 4);

        for (int pixel : pixels) {
            buffer.put((byte) ((pixel >> 16) & 0xFF)); // Red
            buffer.put((byte) ((pixel >> 8) & 0xFF));  // Green
            buffer.put((byte) (pixel & 0xFF));         // Blue
            buffer.put((byte) ((pixel >> 24) & 0xFF)); // Alpha
        }

        buffer.flip();
        return buffer;
    }


}