package hack.echo.client;

import hack.echo.client.auth.AuthManager;
import hack.echo.client.chunk.BlockEspManager;
import hack.echo.client.command.CommandManager;
import hack.echo.client.config.FeatureConfig;
import hack.echo.client.event.EventManager;
import hack.echo.client.vulkan.api.VulkanDevice;
import hack.echo.client.particle.ParticleManager;
import hack.echo.client.features.FeatureManager;
import hack.echo.client.friends.FriendManager;
import hack.echo.client.handlers.HandlerManager;
import hack.echo.client.render.Shaders;
import hack.echo.client.render2.api.Draw2D;
import hack.echo.client.render2.impl.opengl.font.Fonts;
import hack.echo.client.render2.impl.vulkan.api.VkDraw2D;
import hack.echo.client.render3.api.Draw3D;
import hack.echo.client.render2.impl.opengl.api.GlDraw2D;
import hack.echo.client.render3.impl.gl.api.GlDraw3D;
import hack.echo.client.vulkan.utils.VkContext;
import hack.echo.client.render3.impl.vk.api.VkDraw3D;
import hack.echo.client.chunk.ChunkManager;
import hack.echo.client.screens.ScreenManager;
import hack.echo.client.spotify.SpotifyManager;
import hack.echo.client.utils.VulkanUtil;
import hack.echo.client.utils.audio.EchoAudio;
//? if <= 26.1.2 {
import hack.echo.client.vulkan.utils.VkContext_1_21_11;
//?} else {
/*import hack.echo.client.vulkan.utils.VkContext_26_2;
 *///?}
import hack.echo.client.vulkan.memory.VkImage;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//? if auth {
import java.net.http.WebSocket;
 //?}

//TODO: Auth dependency on rotation utils
public class Echo implements ModInitializer, SimpleSynchronousResourceReloadListener {
    public static final String MOD_ID = "echo";
    public static Echo INSTANCE;
    public static FeatureManager featureManager;
    public static FriendManager friendManager;
    public static ScreenManager screenManager;
    public static FeatureConfig featureConfig;
    public static HandlerManager handlerManager;
    public static BlockEspManager blockEspManager;
    public static CommandManager commandManager;
    public static AuthManager authManager;
    public static volatile boolean isDestroyed = false;
    public static Draw2D draw2D;
    public static Draw3D draw3d;
    public static VkContext vkContext;
    public static ChunkManager chunkManager;
    public static SpotifyManager spotifyManager;
    public static ParticleManager particleManager;
    public static VulkanDevice vulkanDevice;

    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    // "+auth-related"
    @Override
    public void onInitialize() {
        //? if !auth {
        /*commandManager = new CommandManager();
        particleManager = new ParticleManager();
        chunkManager = new ChunkManager();
        blockEspManager = new BlockEspManager();
        friendManager = new FriendManager();
        featureManager = new FeatureManager();
        //? if debug
        //LOGGER.info("FeatureManager initialized");
        screenManager = new ScreenManager();
        //? if debug
        //LOGGER.info("ScreenManager initialized");
        handlerManager = new HandlerManager();
        //? if debug
        //LOGGER.info("HandlerManager initialized");

        featureConfig = new FeatureConfig();

        spotifyManager = new SpotifyManager();
        //? if debug
        //LOGGER.info("FeatureConfig initialized");
        //? if debug
        //LOGGER.info("Fonts initialized");
        Shaders.init();
        //? if debug
        //LOGGER.info("Shaders initialized");
        EchoAudio.preload();


        //? if debug
        //LOGGER.info("If you crash here it is probably a mismatch in server features.json");

        featureConfig.loading = true;
        featureManager.initialize();
        //? if debug
        //LOGGER.info("FeatureManager initialized");
        handlerManager.initialize();
        LOGGER.info("HandlerManager initialized");
        screenManager.initialize();
        //? if debug
        //LOGGER.info("ScreenManager initialized");

        //? if debug {
        /^LOGGER.info("Initialized :3");
        LOGGER.info("Inside debug mode");
        ^///?}


        EventManager.register(this);
        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(this);
        *///?}
        //? if auth {
		if (authManager == null) {
			return;
		}
		authManager.requestPacket = 1;
		Shaders.init();
        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(this);
		EventManager.register(this);
		//?}
    }
    //? if auth {

	 private boolean authFingerprintMatchesInline(AuthManager manager) {
	 int fp = manager.getAuthFingerprint();
	 int tgt = manager.getAuthFingerprintTarget();
	 int delta = fp ^ tgt;
	 delta |= delta >> 3;
	 delta ^= (delta << 2);
	 delta |= delta >> 5;
	 return (delta & 0x01) == 0;
	 }
	 //?}

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        if (draw2D == null) {
            boolean isVulkan = VulkanUtil.isVulkanLoaded();
            if (isVulkan) {


                //? if > 26.1.2 {
                /*vkContext = new VkContext_26_2();
                 *///?} else {
                vkContext = new VkContext_1_21_11();
                //?}

                vulkanDevice = new VulkanDevice(
                        vkContext.getVma(),
                        vkContext.getDevice(),
                        () -> vkContext.getCurrentFrame(),
                        vkContext.getFramesNum()
                );
                VkImage.createDefaultTexture();

                Fonts.init();
                draw2D = new VkDraw2D();
                draw3d = new VkDraw3D();
            } else {
                Fonts.init();
                draw2D = new GlDraw2D();
                draw3d = new GlDraw3D();
            }

            particleManager.buildAtlas();


            // some module's need the renderers to be loaded for their onEnable to function properly. I.e : Search.java
            featureConfig.loadProfile("_autosave");
            featureConfig.loading = false;

            //? if debug
            //LOGGER.info("AutoSave loaded");

        }
    }
    @Override
    public Identifier getFabricId() {
        return Identifier.fromNamespaceAndPath(MOD_ID, "echo");
    }
}