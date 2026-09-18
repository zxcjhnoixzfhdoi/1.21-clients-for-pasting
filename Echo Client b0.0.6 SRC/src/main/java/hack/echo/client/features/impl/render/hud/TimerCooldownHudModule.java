package hack.echo.client.features.impl.render.hud;

import hack.echo.client.event.EventSubscribe;
import hack.echo.client.event.impl.EventRender2DGui;
import hack.echo.client.features.Category;
import hack.echo.client.features.FeatureInfo;
import hack.echo.client.features.FeatureManager;
import hack.echo.client.features.HudFeature;
import hack.echo.client.features.impl.combat.SpearLungeModule;
import hack.echo.client.features.impl.combat.SpearReachModule;
import hack.echo.client.features.settings.impl.BoolSetting;
import hack.echo.client.mixin.accessors.AbstractArrowAccessor;
import hack.echo.client.render2.api.Draw2D;
import hack.echo.client.render2.impl.opengl.font.Fonts;
import hack.echo.client.utils.combat.SpearUtils;
import hack.echo.client.utils.strings.Concat;
import hack.echo.client.utils.trajectory.ArrowTrajectoryUtils;
import hack.echo.client.utils.trajectory.PearlTrajectoryUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static hack.echo.client.screens.clickgui.glass.GlassUIConstants.*;

public class TimerCooldownHudModule extends HudFeature {
    private static final float FONT_SIZE = 8f;
    private static final float ITEM_RENDER_SIZE = 16f;
    private static final float HUD_HEIGHT = 30f;
    private static final float HUD_PADDING_X = 8f;
    private static final float HUD_GAP = 4f;

    private final BoolSetting showPearlLandTimer = new BoolSetting(Concat.of("Pearl land time"), true);
    private final BoolSetting spearSwapCooldown = new BoolSetting(Concat.of("Spear swap paper cooldown"), true);
    private final BoolSetting showArrowLandTimer = new BoolSetting(Concat.of("Arrow land time"), true);

    private final List<CooldownEntry> cooldowns = List.of(
        new CooldownEntry(
            showPearlLandTimer,
            List.of(new PreviewCooldown(Items.ENDER_PEARL, 1.9)),
            this::getOwnPearlCooldowns
        ),
        new CooldownEntry(
            spearSwapCooldown,
            List.of(new PreviewCooldown(Items.NETHERITE_SPEAR, 0.8)),
            this::getSpearSwapCooldowns
        ),
        new CooldownEntry(
            showArrowLandTimer,
            List.of(new PreviewCooldown(Items.ARROW, 1.9)),
            this::getOwnArrowLandTimer
        )
    );

    private float cachedWidth = 30f;

    public TimerCooldownHudModule() {
        super(new FeatureInfo(
            Concat.of("Timer Cooldown"),
            Concat.of("Shows the cooldown of the things."),
            Category.RENDER
        ));
    }

    @Override
    public float getWidth() {
        return cachedWidth;
    }

    @Override
    public float getHeight() {
        return HUD_HEIGHT;
    }

    @EventSubscribe
    private void onRender2D(EventRender2DGui event) {
        if (isNull()) return;
        if (Fonts.inter == null) return;
        if (mc.debugEntries.isOverlayVisible()) return;

        List<CooldownState> visibleCooldowns = resolveCooldowns(event.getTickDelta(), false);
        if (visibleCooldowns.isEmpty()) return;

        float totalWidth = 0f;
        float[] widths = new float[visibleCooldowns.size()];
        String[] texts = new String[visibleCooldowns.size()];
        float[] textWidths = new float[visibleCooldowns.size()];

        for (int i = 0; i < visibleCooldowns.size(); i++) {
            texts[i] = String.format(Locale.ROOT, "%.1f", visibleCooldowns.get(i).seconds);
            textWidths[i] = Fonts.inter.getWidth(texts[i], FONT_SIZE);
            widths[i] = Math.max(ITEM_RENDER_SIZE + 6f, textWidths[i] + HUD_PADDING_X);
            totalWidth += widths[i];
        }

        if (visibleCooldowns.size() > 1) {
            totalWidth += HUD_GAP * (visibleCooldowns.size() - 1);
        }

        cachedWidth = totalWidth;

        float currentX = getX();
        float currentY = getY();

        Draw2D draw = event.getDraw2D();
        Matrix4f mat = new Matrix4f();

        float offsetX = currentX;
        for (int i = 0; i < visibleCooldowns.size(); i++) {
            draw.screenImage(mat, event.getBlurTexture(), offsetX, currentY, widths[i], HUD_HEIGHT, RADIUS_MD, 1f);
            draw.rect(mat, offsetX, currentY, widths[i], HUD_HEIGHT, RADIUS_MD, SURFACE_PANEL.getRGB());

            float itemX = offsetX + widths[i] / 2f - ITEM_RENDER_SIZE / 2f;
            float itemY = currentY + 2f;
            event.getContext().pushMatrix();
            event.getContext().translate(itemX, itemY);
            event.getContext().renderItem(visibleCooldowns.get(i).icon, 0, 0);
            event.getContext().popMatrix();

            float textX = offsetX + widths[i] / 2f - textWidths[i] / 2f;
            float textY = currentY + 18f;
            draw.text(Fonts.inter, mat, texts[i], textX, textY, FONT_SIZE, argb(TEXT_ON_SURFACE_SECONDARY));

            offsetX += widths[i] + HUD_GAP;
        }
    }

    private List<CooldownState> resolveCooldowns(float partialTick, boolean allowPreview) {
        List<CooldownState> visible = new ArrayList<>(cooldowns.size());

        for (CooldownEntry entry : cooldowns) {
            if (!entry.setting.getValue()) continue;

            List<CooldownState> active = entry.resolver.resolve(partialTick);
            if (!active.isEmpty()) {
                visible.addAll(active);
            } else if (allowPreview) {
                visible.addAll(entry.previewStates());
            }
        }

        return visible;
    }

    private List<CooldownState> getOwnPearlCooldowns(float partialTick) {
        double best = Double.MAX_VALUE;

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity.getType() != EntityType.ENDER_PEARL || entity.isRemoved()) continue;
            if (!(entity instanceof Projectile projectile) || projectile.getOwner() != mc.player) continue;

            List<Vec3> path = PearlTrajectoryUtils.sample(entity);
            if (path.size() < 2) continue;

            double seconds = Math.max(0.0, (path.size() - 1 - partialTick) / 20.0);
            if (seconds < best) {
                best = seconds;
            }
        }

        return best < Double.MAX_VALUE
            ? List.of(new CooldownState(new ItemStack(Items.ENDER_PEARL), best))
            : List.of();
    }

    private List<CooldownState> getOwnArrowLandTimer(float partialTick) {
        List<CooldownState> results = new ArrayList<>();
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof AbstractArrow arrow) || arrow.isRemoved()) continue;
            if (!(arrow instanceof Projectile projectile) || projectile.getOwner() != mc.player) continue;
            if (((AbstractArrowAccessor) arrow).echo$isInGround()) continue;

            List<Vec3> path = ArrowTrajectoryUtils.sample(entity);
            if (path.size() < 2) continue;

            double seconds = Math.max(0.0, (path.size() - 1 - partialTick) / 20.0);
            results.add(new CooldownState(new ItemStack(Items.ARROW), seconds));
        }
        return results;
    }

    private List<CooldownState> getSpearSwapCooldowns(float partialTick) {
        List<CooldownState> results = new ArrayList<>(2);

        for (ItemStack spearStack : List.of(getSpearLungeStack(), getSpearReachStack())) {
            if (spearStack.isEmpty()) continue;

            double seconds = SpearUtils.getRemainingSpearSwapCooldownSeconds(mc.player, spearStack, partialTick);
            if (seconds > 0.0) {
                results.add(new CooldownState(spearStack, seconds));
            }
        }

        return results;
    }

    private ItemStack getSpearLungeStack() {
        return FeatureManager.safeGetFeature(SpearLungeModule.class)
            .map(SpearLungeModule::getPaperCooldownSpearStack).orElse(ItemStack.EMPTY);
    }

    private ItemStack getSpearReachStack() {
        return FeatureManager.safeGetFeature(SpearReachModule.class)
            .map(SpearReachModule::getPaperCooldownSpearStack).orElse(ItemStack.EMPTY);
    }

    @FunctionalInterface
    private interface CooldownResolver {
        List<CooldownState> resolve(float partialTick);
    }

    private record CooldownEntry(BoolSetting setting, List<PreviewCooldown> preview, CooldownResolver resolver) {
        private List<CooldownState> previewStates() {
            List<CooldownState> states = new ArrayList<>(this.preview.size());

            for (PreviewCooldown previewCooldown : this.preview) {
                states.add(previewCooldown.toState());
            }

            return states;
        }
    }

    private record PreviewCooldown(ItemLike item, double seconds) {
        private CooldownState toState() {
            return new CooldownState(new ItemStack(this.item), this.seconds);
        }
    }

    private record CooldownState(ItemStack icon, double seconds) {}
}
