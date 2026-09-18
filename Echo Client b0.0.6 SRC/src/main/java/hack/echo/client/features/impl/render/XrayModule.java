package hack.echo.client.features.impl.render;

import hack.echo.client.Echo;
import hack.echo.client.event.EventSubscribe;
import hack.echo.client.event.impl.EventChunkOcclusion;
import hack.echo.client.features.Category;
import hack.echo.client.features.Feature;
import hack.echo.client.features.FeatureInfo;
import hack.echo.client.features.settings.impl.BoolSetting;
import hack.echo.client.features.settings.impl.MultiModeSetting;
import hack.echo.client.mixin.render.blockrendererdispatcher.MixinBlockRenderDispatcher_1_21_11;
import hack.echo.client.mixin.render.modelblockrenderer.MixinModelBlockRenderer_1_21_11;
import hack.echo.client.utils.strings.Concat;
import hack.echo.client.api.BlocksCompat;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Xray feature entrypoint used by renderer-specific mixin hooks.
 *
 * @see MixinBlockRenderDispatcher_1_21_11
 * @see MixinModelBlockRenderer_1_21_11
 * @see hack.echo.client.mixin.render.MixinSectionCompiler
 * @see hack.echo.client.mixin.render.indigo.versioned.MixinAbstractTerrainRenderContextLight_1_21_11
 * @see hack.echo.client.mixin.render.indigo.versioned.MixinTerrainRenderContext_1_21_11
 * @see hack.echo.client.mixin.render.indigo.versioned.MixinBlockRenderInfo_1_21_11
 * @see hack.echo.client.mixin.render.sodium.versioned.MixinBlockOcclusionCache_1_21_11
 * @see hack.echo.client.mixin.render.sodium.versioned.MixinBlockRenderer_1_21_11
 * @see hack.echo.client.mixin.render.sodium.versioned.MixinBlockRenderer_26_1
 * @see hack.echo.client.mixin.render.sodium.MixinBlockRendererLight
 * @see hack.echo.client.mixin.render.sodium.MixinChunkBuilderMeshingTask
 * @see hack.echo.client.mixin.render.sodium.MixinFluidRenderer
 * @see hack.echo.client.mixin.render.vulkan.versioned.MixinAbstractBlockRenderContextLight_1_21_11
 * @see hack.echo.client.mixin.render.vulkan.MixinAbstractBlockRenderContextOcclusion
 * @see hack.echo.client.mixin.render.vulkan.MixinBlockRenderer
 * @see hack.echo.client.mixin.render.vulkan.MixinBuildTask
 * @see hack.echo.client.mixin.render.vulkan.versioned.MixinFluidRenderer_1_21_11
 */
public class XrayModule extends Feature {
    private static volatile XrayModule activeInstance;
    private volatile Set<Block> selectedBlocksCache = Collections.emptySet();

    public XrayModule() {
        super(new FeatureInfo(
                Concat.of("Xray"),
                Concat.of("Renders blocks through walls using a shader."),
                Category.RENDER
        ));
        blocks.onChanged(() -> {
            rebuildSelectedBlocks();
            if (isEnabled() && mc.levelRenderer != null) {
                mc.levelRenderer.allChanged();
            }
        });
        seeThroughLiquids.onChanged(() -> {
            if (isEnabled() && mc.levelRenderer != null) {
                mc.levelRenderer.allChanged();
            }
        });
    }

    @Override
    public void onEnable() {
        rebuildSelectedBlocks();
        activeInstance = this;
        if (mc.levelRenderer != null) {
            mc.levelRenderer.allChanged();
        }
    }

    @Override
    public void onDisable() {
        if (activeInstance == this) {
            activeInstance = null;
        }
        if (mc.levelRenderer != null) {
            mc.levelRenderer.allChanged();
        }
    }

    public BoolSetting seeThroughLiquids = new BoolSetting(Concat.of("Through Liquids"), true);
    public MultiModeSetting blocks = new MultiModeSetting(Concat.of("Blocks"),
            Concat.of("Diamond"),
            Concat.of("Netherite"),
            Concat.of("Gold"),
            Concat.of("Iron"),
            Concat.of("Emerald"),
            Concat.of("Redstone"),
            Concat.of("Lapiz"),
            Concat.of("Coal"),
            Concat.of("Copper"),
            Concat.of("Quartz")
    );

    public static XrayModule getActive() {
        if (Echo.isDestroyed) {
            return null;
        }

        XrayModule xray = activeInstance;
        return xray != null && xray.isEnabled() ? xray : null;
    }
    
    public Set<Block> getSelectedBlocks() {
        return selectedBlocksCache;
    }

    private void rebuildSelectedBlocks() {
        Set<Block> selectedBlocks = new HashSet<>();

        if (blocks.isEnabled(Concat.of("Diamond"))) {
            selectedBlocks.add(Blocks.DIAMOND_ORE);
            selectedBlocks.add(Blocks.DEEPSLATE_DIAMOND_ORE);
            selectedBlocks.add(Blocks.DIAMOND_BLOCK);
        }
        if (blocks.isEnabled(Concat.of("Netherite"))) {
            selectedBlocks.add(Blocks.ANCIENT_DEBRIS);
            selectedBlocks.add(Blocks.NETHERITE_BLOCK);
        }
        if (blocks.isEnabled(Concat.of("Gold"))) {
            selectedBlocks.add(Blocks.GOLD_ORE);
            selectedBlocks.add(Blocks.DEEPSLATE_GOLD_ORE);
            selectedBlocks.add(Blocks.NETHER_GOLD_ORE);
            selectedBlocks.add(Blocks.GOLD_BLOCK);
        }
        if (blocks.isEnabled(Concat.of("Iron"))) {
            selectedBlocks.add(Blocks.IRON_ORE);
            selectedBlocks.add(Blocks.DEEPSLATE_IRON_ORE);
            selectedBlocks.add(Blocks.IRON_BLOCK);
        }
        if (blocks.isEnabled(Concat.of("Emerald"))) {
            selectedBlocks.add(Blocks.EMERALD_ORE);
            selectedBlocks.add(Blocks.DEEPSLATE_EMERALD_ORE);
            selectedBlocks.add(Blocks.EMERALD_BLOCK);
        }
        if (blocks.isEnabled(Concat.of("Redstone"))) {
            selectedBlocks.add(Blocks.REDSTONE_ORE);
            selectedBlocks.add(Blocks.DEEPSLATE_REDSTONE_ORE);
            selectedBlocks.add(Blocks.REDSTONE_BLOCK);
        }
        if (blocks.isEnabled(Concat.of("Lapiz"))) {
            selectedBlocks.add(Blocks.LAPIS_ORE);
            selectedBlocks.add(Blocks.DEEPSLATE_LAPIS_ORE);
            selectedBlocks.add(Blocks.LAPIS_BLOCK);
        }
        if (blocks.isEnabled(Concat.of("Coal"))) {
            selectedBlocks.add(Blocks.COAL_ORE);
            selectedBlocks.add(Blocks.DEEPSLATE_COAL_ORE);
            selectedBlocks.add(Blocks.COAL_BLOCK);
        }
        if (blocks.isEnabled(Concat.of("Copper"))) {
            selectedBlocks.add(Blocks.COPPER_ORE);
            selectedBlocks.add(Blocks.DEEPSLATE_COPPER_ORE);
            selectedBlocks.add(BlocksCompat.getCopperBlock());
        }
        if (blocks.isEnabled(Concat.of("Quartz"))) {
            selectedBlocks.add(Blocks.NETHER_QUARTZ_ORE);
            selectedBlocks.add(Blocks.QUARTZ_BLOCK);
        }

        selectedBlocksCache = Collections.unmodifiableSet(selectedBlocks);
    }


    public boolean shouldRenderBlock(Block block) {
        return selectedBlocksCache.contains(block);
    }
}
