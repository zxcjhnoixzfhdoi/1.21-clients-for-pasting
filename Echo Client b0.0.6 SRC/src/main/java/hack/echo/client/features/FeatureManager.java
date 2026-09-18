package hack.echo.client.features;


import hack.echo.client.Echo;
import hack.echo.client.event.EventManager;
import hack.echo.client.features.impl.combat.*;
import hack.echo.client.features.impl.macros.*;
import hack.echo.client.features.impl.macros.donut.InstaAuctionSell;
import hack.echo.client.features.impl.macros.donut.InstaCrate;
import hack.echo.client.features.impl.macros.donut.InstaSell;
import hack.echo.client.features.impl.misc.*;
import hack.echo.client.features.impl.movement.*;
import hack.echo.client.features.impl.player.*;
import hack.echo.client.features.impl.render.*;
import hack.echo.client.features.impl.render.hud.*;
import hack.echo.client.features.impl.player.AutoLava;
import hack.echo.client.features.impl.player.BlockBruteforcer;
import hack.echo.client.features.impl.player.FastPlace;
import lombok.Getter;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Getter
public class FeatureManager {
    private final CopyOnWriteArrayList<Feature> features = new CopyOnWriteArrayList<>();
    private final Map<Class<? extends Feature>, Feature> featureCache = new ConcurrentHashMap<>();
    private final Map<Category, List<Feature>> categoryCache = new ConcurrentHashMap<>();
    private volatile boolean destroyed = false;

    public void initialize() {
        //? if !auth {
        /*features.add(new TimerCooldownHudModule());
        features.add(new WatermarkHudModule());
        features.add(new ArrayListModule());
        features.add(new NotificationsHudModule());

        //Movement
        features.add(new Sprint());
        features.add(new NoJumpDelay());
        features.add(new JumpReset());
        features.add(new MoveFix());
        features.add(new ComboHelper());
        features.add(new GuiWalk());
        features.add(new SnapTap());
        features.add(new Speed());
        features.add(new TimerExploit());
        features.add(new AutoWalkModule());
        features.add(new KeepSprintModule());
        features.add(new NoSlowModule());
        features.add(new FastStairsModule());
        features.add(new VelocityModule());
        features.add(new StepModule());
        features.add(new NoWebModule());


        //Render
        features.add(new KeybindHudModule());
        features.add(new ClickGUI());
        features.add(new Nametags());
        features.add(new AmbienceMod());
        features.add(new PlayerESP());
        features.add(new TargetESP());
        features.add(new ViewModel());
        features.add(new NoRender());
        features.add(new XrayModule());
        features.add(new AimVisualizerModule());
        features.add(new FullbrightModule());
        features.add(new StorageESPModule());
        features.add(new SwingSpeedModule());
        features.add(new LightESPModule());
        features.add(new Freecam());
        features.add(new TrajectoryRenderModule());
//        features.add(new Test());
        features.add(new Search());

        //Combat
        features.add(new AimAssist());
        features.add(new ShieldBreaker());
        features.add(new TriggerBot());
        features.add(new Piercing());
        features.add(new AutoCrystal());
        features.add(new AutoAnchor());
        features.add(new SpearLungeModule());
        features.add(new SpearReachModule());
        features.add(new SpearAssistModule());
        features.add(new MaceSwap());
        features.add(new Prevent());
        features.add(new Criticals());
        features.add(new CrystalOptimizer());
        features.add(new TotemHitModule());
        features.add(new AutoCart());
        features.add(new Backtrack());
        features.add(new KnockbackDelayModule());
        features.add(new AutoMace());
        //? if debug
        //features.add(new KnockbackDisplaceModule());
        
        //Client
        features.add(new Transactions());
        features.add(new Teams());
        features.add(new CapeModule());
        features.add(new SelfDestruct());
        features.add(new TargetControlModule());
        features.add(new CommandLogger());
        features.add(new VoxelEspConfig());
        //? if debug
        //features.add(new Export());
        
        // Macros
        features.add(new InstaCrate());
        features.add(new KeyCharge());
        features.add(new InstaAuctionSell());
        features.add(new InstaSell());
        features.add(new Tunneler());
        features.add(new KeyPearlMacro());
        features.add(new ChestStealer());
        features.add(new PearlCatch());
        features.add(new DiveBomb());
        
        // Player
        features.add(new SmartPot());
        features.add(new PotRefill());
        features.add(new FastEXP());
        features.add(new ElytraSwap());
        features.add(new TotemRefill());
        features.add(new AutoTool());
        features.add(new AutoTotem());
        features.add(new AntiTrap());
        //? if debug
        //features.add(new Replenish());
        features.add(new TotemOffhandModule());
        features.add(new TotemDoubleHandModule());
        features.add(new NoBreakDelay());
        features.add(new AttributeMineModule());
        features.add(new FastPlace());

        // World
//        module unstable
        //? if debug
        //features.add(new AutoLava());
        features.add(new BlockBruteforcer());

        //Exploit
        features.add(new HitboxMod());
        features.add(new NoLungeCooldown());
        features.forEach(Feature::initSettings);
        features.sort((a, b) -> compareCharSequenceIgnoreCase(a.getName(), b.getName()));

        // internals
        //? if debug
        //features.add(new SlotInfoModule());

        // Build cache for O(1) lookups
        // If we don't do this FPS will drop by 50%+ when native'd
        for (Feature feature : features) {
            featureCache.put(feature.getClass(), feature);
        }
        
        for (Category category : Category.values()) {
            categoryCache.put(category, new ArrayList<>());
        }
        for (Feature feature : features) {
            categoryCache.get(feature.getCategory()).add(feature);
        }
        for (Category category : Category.values()) {
            categoryCache.put(category, Collections.unmodifiableList(categoryCache.get(category)));
        }
        *///?}
    }

    private static int compareCharSequenceIgnoreCase(CharSequence a, CharSequence b) {
        int len1 = a.length();
        int len2 = b.length();
        int lim = Math.min(len1, len2);
        for (int i = 0; i < lim; i++) {
            char c1 = Character.toLowerCase(a.charAt(i));
            char c2 = Character.toLowerCase(b.charAt(i));
            if (c1 != c2) {
                return c1 - c2;
            }
        }
        return len1 - len2;
    }

    @SuppressWarnings("unchecked")
    public <T extends Feature> T getFeatureByClass(Class<T> featureClass) {
        if (destroyed) return null;
        Feature feature = featureCache.get(featureClass);
        return feature != null ? (T) feature : null;
    }

    public List<Feature> getFeaturesByCategory(Category category) {
        if (destroyed) return Collections.emptyList();
        return categoryCache.getOrDefault(category, Collections.emptyList());
    }

    public static List<Feature> safeFeatures() {
        FeatureManager fm = Echo.featureManager;
        if (fm == null || fm.destroyed) return Collections.emptyList();
        return Collections.unmodifiableList(fm.features);
    }

    public static <T extends Feature> Optional<T> safeGetFeature(Class<T> featureClass) {
        FeatureManager fm = Echo.featureManager;
        if (fm == null || fm.destroyed) return Optional.empty();
        return Optional.ofNullable(fm.getFeatureByClass(featureClass));
    }

    public void shutdown(Feature skipFeature) {
        destroyed = true;
        for (Feature feature : features) {
            if (feature == null || feature == skipFeature) continue;
            try {
                feature.setEnabled(false);
            } catch (Exception ignored) {
            }
            EventManager.unregister(feature);
            feature.settings.clear();
        }
        features.clear();
        featureCache.clear();
        categoryCache.clear();
    }

    //? if auth {
    public void addFeatures(CopyOnWriteArrayList<Feature> features) {
        this.features.addAll(features);
        for (Feature feature : features) {
            featureCache.put(feature.getClass(), feature);
        }
    }

    public CopyOnWriteArrayList<Feature> sort(CopyOnWriteArrayList<Feature> features) {
        features.sort((a, b) -> compareCharSequenceIgnoreCase(a.getName(), b.getName()));
        return features;
    }

    public void addCategoryCache(Map<Category, List<Feature>> categoryCache) {
        this.categoryCache.putAll(categoryCache);
    }
    //?}
}
