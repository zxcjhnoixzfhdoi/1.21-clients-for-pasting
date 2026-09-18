package hack.echo.client.features.impl.misc;

import hack.echo.client.features.Category;
import hack.echo.client.features.Feature;
import hack.echo.client.features.FeatureInfo;
import hack.echo.client.utils.strings.Concat;
import hack.echo.client.features.settings.impl.BoolSetting;
import hack.echo.client.features.settings.impl.ModeSetting;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class CapeModule extends Feature {

    private static final Concat ERIS = Concat.of("Eris");
    private static final Concat VANILLA = Concat.of("Vanilla");
    private static final Concat ONE_MILLION_CUSTOMER = Concat.of("1 Million Customer");
    private static final Concat FIFTEEN_ANNIVERSARY = Concat.of("15 Anniversary");
    private static final Concat BIRTHDAY = Concat.of("Birthday");
    private static final Concat CHERRY_BLOSSOM = Concat.of("Cherry Blossom");
    private static final Concat COBALT = Concat.of("Cobalt");
    private static final Concat DEBUG = Concat.of("Debug");
    private static final Concat FOLLOWER = Concat.of("Follower");
    private static final Concat MCC_15_YEAR = Concat.of("MCC 15 Year");
    private static final Concat MC_EXPERIENCE = Concat.of("MC Experience");
    private static final Concat MIGRATOR = Concat.of("Migrator");
    private static final Concat MINECON_2011 = Concat.of("Minecon 2011");
    private static final Concat MINECON_2012 = Concat.of("Minecon 2012");
    private static final Concat MINECON_2013 = Concat.of("Minecon 2013");
    private static final Concat MINECON_2015 = Concat.of("Minecon 2015");
    private static final Concat MINECON_2016 = Concat.of("Minecon 2016");
    private static final Concat MOJANG = Concat.of("Mojang");
    private static final Concat MOJANG_CLASSIC = Concat.of("Mojang Classic");
    private static final Concat MOJANG_OFFICE = Concat.of("Mojang Office");
    private static final Concat MOJANG_STUDIOS = Concat.of("Mojang Studios");
    private static final Concat MOJIRA_MOD = Concat.of("Mojira Mod");
    private static final Concat PRISMARINE = Concat.of("Prismarine");
    private static final Concat PURPLE_HEART = Concat.of("Purple Heart");
    private static final Concat REALM_MAKER = Concat.of("Realm Maker");
    private static final Concat SCROLLS = Concat.of("Scrolls");
    private static final Concat SNOWMAN = Concat.of("Snowman");
    private static final Concat SPADE = Concat.of("Spade");
    private static final Concat TEST = Concat.of("Test");
    private static final Concat TRANSLATOR = Concat.of("Translator");
    private static final Concat VALENTINES = Concat.of("Valentines");

    private static final List<Concat> VANILLA_MODES = List.of(
        ONE_MILLION_CUSTOMER, FIFTEEN_ANNIVERSARY, BIRTHDAY, CHERRY_BLOSSOM,
        COBALT, DEBUG, FOLLOWER, MCC_15_YEAR, MC_EXPERIENCE, MIGRATOR,
        MINECON_2011, MINECON_2012, MINECON_2013, MINECON_2015, MINECON_2016,
        MOJANG, MOJANG_CLASSIC, MOJANG_OFFICE, MOJANG_STUDIOS, MOJIRA_MOD,
        PRISMARINE, PURPLE_HEART, REALM_MAKER, SCROLLS, SNOWMAN, SPADE,
        TEST, TRANSLATOR, VALENTINES, VANILLA
    );

    private final BoolSetting vanillaCapes = new BoolSetting(Concat.of("Vanilla Capes"), false);

    private final ModeSetting capes = new ModeSetting(
        Concat.of("Capes"),
        ERIS,
        new ArrayList<>(List.of(ERIS))
    );

    public CapeModule() {
        super(new FeatureInfo(
            Concat.of("Capes"),
            Concat.of("Custom cape settings"),
            Category.INTERNALS
        ));

        vanillaCapes.onChanged(() -> {
            List<CharSequence> modes = new ArrayList<>();
            modes.add(ERIS);
            if (vanillaCapes.getValue()) {
                modes.addAll(VANILLA_MODES);
            } else if (!capes.is(ERIS)) {
                capes.setValue(ERIS);
            }
            capes.setModes(modes);
        });
    }

    private final Map<Concat, String> capePaths = Map.ofEntries(
        Map.entry(ERIS, "eris"),
        Map.entry(ONE_MILLION_CUSTOMER, "1millioncustomer"),
        Map.entry(FIFTEEN_ANNIVERSARY, "15anniversary"),
        Map.entry(BIRTHDAY, "birthday"),
        Map.entry(CHERRY_BLOSSOM, "cherryblossom"),
        Map.entry(COBALT, "cobalt"),
        Map.entry(DEBUG, "debug"),
        Map.entry(FOLLOWER, "follower"),
        Map.entry(MCC_15_YEAR, "mcc15year"),
        Map.entry(MC_EXPERIENCE, "mcexperience"),
        Map.entry(MIGRATOR, "migrator"),
        Map.entry(MINECON_2011, "minecon2011"),
        Map.entry(MINECON_2012, "minecon2012"),
        Map.entry(MINECON_2013, "minecon2013"),
        Map.entry(MINECON_2015, "minecon2015"),
        Map.entry(MINECON_2016, "minecon2016"),
        Map.entry(MOJANG, "mojang"),
        Map.entry(MOJANG_CLASSIC, "mojangclassic"),
        Map.entry(MOJANG_OFFICE, "mojangoffice"),
        Map.entry(MOJANG_STUDIOS, "mojangstudios"),
        Map.entry(MOJIRA_MOD, "mojiramod"),
        Map.entry(PRISMARINE, "prismarine"),
        Map.entry(PURPLE_HEART, "purpleheart"),
        Map.entry(REALM_MAKER, "realmmaker"),
        Map.entry(SCROLLS, "scrolls"),
        Map.entry(SNOWMAN, "snowman"),
        Map.entry(SPADE, "spade"),
        Map.entry(TEST, "test"),
        Map.entry(TRANSLATOR, "translator"),
        Map.entry(VALENTINES, "valentines"),
        Map.entry(VANILLA, "vanilla")
    );

    public String getCapePath() {
        Concat value = (Concat) capes.getValueSequence();
        for (Map.Entry<Concat, String> entry : capePaths.entrySet()) {
            if (value.fragmentedEquals(entry.getKey())) {
                return entry.getValue();
            }
        }
        return "eris";
    }

}
