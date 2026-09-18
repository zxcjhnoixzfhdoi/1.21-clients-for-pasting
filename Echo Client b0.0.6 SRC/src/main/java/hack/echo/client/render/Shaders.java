package hack.echo.client.render;

import com.mojang.blaze3d.pipeline.BlendFunction;
//? if >=26.1 {
/*import com.mojang.blaze3d.pipeline.ColorTargetState;
*///?}
import com.mojang.blaze3d.pipeline.RenderPipeline;
import hack.echo.client.Echo;
import hack.echo.client.utils.Imports;
import lombok.Getter;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.PackType;
import net.minecraft.resources.Identifier;

@SuppressWarnings("deprecation")
public class Shaders implements SimpleSynchronousResourceReloadListener, Imports {
    public static RenderPipeline TRANSLUCENT_ENTITY_HIGHLIGHT_PIPELINE;
    public static RenderPipeline WIREFRAME_PIPELINE;




    @Getter
    private static boolean initialized = false;

    static {
        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new Shaders());
    }

    @Override
    public Identifier getFabricId() {
        return Identifier.fromNamespaceAndPath(Echo.MOD_ID, "reload_shaders");
    }

    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        load();
    }

    public static void load() {
        try {

            TRANSLUCENT_ENTITY_HIGHLIGHT_PIPELINE = RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
                    .withLocation("translucent_entity_highlight_pipeline")
                    //? if >=26.1 {
                    /*.withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                    *///?} else {
                    .withBlend(BlendFunction.TRANSLUCENT)
                    //?}
                    .withCull(true)
                    .build();

            WIREFRAME_PIPELINE = RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
                    //? if >=26.1 {
                    /*.withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                    *///?} else {
                    .withBlend(BlendFunction.TRANSLUCENT)
                    //?}
                    .withCull(false)
                    .withLocation("wireframe_outline")
                    .build();


            initialized = true;
        } catch (Exception e) {
            System.err.println("Failed to load shaders: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void init() {
        load();
    }

    public static void unload() {
        TRANSLUCENT_ENTITY_HIGHLIGHT_PIPELINE = null;
        WIREFRAME_PIPELINE = null;
        initialized = false;
    }
}
