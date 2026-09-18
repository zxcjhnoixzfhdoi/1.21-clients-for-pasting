package hack.echo.client.features.impl.combat;

import hack.echo.client.features.Category;
import hack.echo.client.features.Feature;
import hack.echo.client.features.FeatureInfo;
import hack.echo.client.features.settings.impl.BoolSetting;
import hack.echo.client.features.settings.impl.FloatSetting;
import hack.echo.client.utils.strings.Concat;

public class HitboxMod extends Feature {

	public HitboxMod() {
		super(new FeatureInfo(
			Concat.of("Hitboxes"),
			Concat.of("Modifies hitbox sizes"),
			Category.COMBAT,
			false
		));
	}
    public BoolSetting uniformExpand = new BoolSetting(Concat.of("Uniform"), true);

    public final FloatSetting expandX = new FloatSetting(Concat.of("Expand X"), 0.0f, 0.0f, 2.0f, 0.1f, p -> !uniformExpand.getValue());
    public final FloatSetting expandY = new FloatSetting(Concat.of("Expand Y"), 0.0f, 0.0f, 2.0f, 0.1f, p -> !uniformExpand.getValue());
    public final FloatSetting ExpandZ = new FloatSetting(Concat.of("Expand Z"), 0.0f, 0.0f, 2.0f,0.1f, p -> !uniformExpand.getValue());

    public final FloatSetting uniformExpandSize = new FloatSetting(Concat.of("Expand Size"), 0.0f, 0.0f, 2.0f, 0.1f, p -> uniformExpand.getValue());
    public BoolSetting staticHitbox = new BoolSetting(Concat.of("Static Hitbox"), false);
    public BoolSetting playersOnly = new BoolSetting(Concat.of("Players Only"), true);

    public final FloatSetting distanceSetting = new FloatSetting(Concat.of("Range"), 6.0f, 1, 20, 0.2f);

}
