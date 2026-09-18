package hack.echo.client.render;

import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.resources.Identifier;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import hack.echo.client.Echo;
import hack.echo.client.mixin.accessors.RenderTypeAccessor;

// What would be Layers.java in pre-25w43a versions
public class RenderSetups {
    public static RenderType invokeOf(String name, RenderSetup setup) {
        return RenderTypeAccessor.invokeCreate(name, setup);
    }

    static RenderSetup TRANSLUCENT_ENTITY_HIGHLIGHT_SETUP = RenderSetup.builder(Shaders.TRANSLUCENT_ENTITY_HIGHLIGHT_PIPELINE)
            .sortOnUpload()
            .bufferSize(256)
            .createRenderSetup();

    public static final RenderType TRANSLUCENT_ENTITY_HIGHLIGHT =
            invokeOf("translucent_entity_highlight", TRANSLUCENT_ENTITY_HIGHLIGHT_SETUP);


    // caching is possibly useless as line width is a per-vertex attribute
    // not a layer level property
    private static final Map<Double, RenderType> WIREFRAME_CACHE = new ConcurrentHashMap<>();

    public static RenderType getWireFrame(double lineWidth) {
        return WIREFRAME_CACHE.computeIfAbsent(lineWidth, lw -> {
            RenderSetup setup = RenderSetup.builder(Shaders.WIREFRAME_PIPELINE)
                    .sortOnUpload()
                    .bufferSize(256)
                    .setOutline(RenderSetup.OutlineProperty.IS_OUTLINE) // Still no clue what this does
                    .createRenderSetup();
            return invokeOf("wireframe_outline_" + ((int)(lw * 1000)), setup);
        });
    }

//   This is an alternative implementation that allows for different line widths without caching
//
//   public static RenderLayer getWireFrame(double lineWidth) {
//       return invokeOf(
//               "wireframe_outline",
//               RenderSetup.builder(Shaders.WIREFRAME_PIPELINE)
//                       .expectedBufferSize(256)
//                       .translucent()
//                       .outlineMode(RenderSetup.OutlineMode.IS_OUTLINE)
//                       .build()
//
//               // I deadass turned everything on and off in this and nothing changed :sob:
//       );
//   }




}