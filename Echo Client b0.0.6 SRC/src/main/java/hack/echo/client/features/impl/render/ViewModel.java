package hack.echo.client.features.impl.render;

import hack.echo.client.features.Feature;
import hack.echo.client.features.FeatureInfo;
import hack.echo.client.features.settings.impl.BoolSetting;
import hack.echo.client.features.settings.impl.FloatSetting;
import hack.echo.client.features.settings.impl.ModeSetting;
import hack.echo.client.utils.strings.Concat;
import hack.echo.client.features.Category;
import hack.echo.client.features.impl.render.swing.AbstractMode;
import hack.echo.client.features.impl.render.swing.DefaultMode;
import hack.echo.client.features.impl.render.swing.NoneMode;
import hack.echo.client.features.impl.render.swing.CustomMode;
import hack.echo.client.features.impl.render.swing.SmoothMode;
import hack.echo.client.features.impl.render.swing.BounceMode;
import hack.echo.client.features.impl.render.swing.SlashMode;
import hack.echo.client.features.impl.render.swing.PunchMode;
import hack.echo.client.features.impl.render.swing.SwipeMode;
import hack.echo.client.features.impl.render.swing.Spin360Mode;
import hack.echo.client.features.impl.render.swing.ScaleMode;
import hack.echo.client.features.impl.render.swing.SpinMode;
import hack.echo.client.features.impl.render.swing.KatanaMode;
import hack.echo.client.features.impl.render.swing.SigmaMode;
import hack.echo.client.features.impl.render.swing.PushMode;
import hack.echo.client.features.impl.render.swing.StabMode;
import hack.echo.client.features.impl.render.swing.SliceMode;
import hack.echo.client.features.impl.render.swing.ExhibitionMode;
import java.util.List;
import java.util.Arrays;

public class ViewModel extends Feature {

        public final List<AbstractMode> modes = Arrays.asList(
                new DefaultMode(),
                new NoneMode(),
                new CustomMode(),
                new SmoothMode(),
                new BounceMode(),
                new SlashMode(),
                new PunchMode(),
                new SwipeMode(),
                new Spin360Mode(),
                new ScaleMode(),
                new SpinMode(),
                new KatanaMode(),
                new SigmaMode(),
                new PushMode(),
                new StabMode(),
                new SliceMode(),
                new ExhibitionMode()
        );

        public ViewModel() {
                super(new FeatureInfo(
                        Concat.of("View Model"),
                        Concat.of("Modifies hand/item rendering"),
                        Category.RENDER,
                        false
                ));
        }

        public final BoolSetting eating = new BoolSetting(Concat.of("Eating Animation"), true);
        public final FloatSetting eatingBob = new FloatSetting(Concat.of("EatingBob"), 1.00f, 0.00f, 1.00f, 0.1f);
        public final ModeSetting swingMode = new ModeSetting(Concat.of("Styles"), Concat.of("Default"),
                Concat.of("Default"), Concat.of("None"), Concat.of("Custom"), Concat.of("Smooth"),
                Concat.of("Bounce"), Concat.of("Slash"), Concat.of("Punch"), Concat.of("Swipe"),
                Concat.of("Spin360"), Concat.of("Scale"), Concat.of("Spin"), Concat.of("Katana"),
                Concat.of("Sigma"), Concat.of("Push"), Concat.of("Stab"), Concat.of("Slice"),
                Concat.of("Exhibition"));
        public final FloatSetting strengthMain = new FloatSetting(Concat.of("Strength Main"), 1.0f, 0.0f, 2.0f, 0.1f);
        public final FloatSetting strengthOff = new FloatSetting(Concat.of("Strength Off"), 1.0f, 0.0f, 2.0f, 0.1f);
        public final FloatSetting scaleMain = new FloatSetting(Concat.of("Scale Main"), 1.0f, 0.1f, 2.0f, 0.2f);
        public final FloatSetting scaleOff = new FloatSetting(Concat.of("Scale Off"), 1.0f, 0.1f, 2.0f, 0.2f);
        public final FloatSetting posXMain = new FloatSetting(Concat.of("PosX Main"), 0.0f, -3.0f, 3.0f, 0.2f);
        public final FloatSetting posYMain = new FloatSetting(Concat.of("PosY Main"), 0.0f, -3.0f, 3.0f, 0.2f);
        public final FloatSetting posZMain = new FloatSetting(Concat.of("PosZ Main"), 0.0f, -3.0f, 3.0f, 0.2f);
        public final FloatSetting posXOff = new FloatSetting(Concat.of("PosX Off"), 0.0f, -3.0f, 3.0f, 0.2f);
        public final FloatSetting posYOff = new FloatSetting(Concat.of("PosY Off"), 0.0f, -3.0f, 3.0f, 0.2f);
        public final FloatSetting posZOff = new FloatSetting(Concat.of("PosZ Off"), 0.0f, -3.0f, 3.0f, 0.2f);
        public final FloatSetting rotXMain = new FloatSetting(Concat.of("RotX Main"), 0.0f, -180.0f, 180.0f, 1.00f);
        public final FloatSetting rotYMain = new FloatSetting(Concat.of("RotY Main"), 0.0f, -180.0f, 180.0f, 1.00f);
        public final FloatSetting rotZMain = new FloatSetting(Concat.of("RotZ Main"), 0.0f, -180.0f, 180.0f, 1.00f);
        public final FloatSetting rotXOff = new FloatSetting(Concat.of("RotX Off"), 0.0f, -180.0f, 180.0f, 1.00f);
        public final FloatSetting rotYOff = new FloatSetting(Concat.of("RotY Off"), 0.0f, -180.0f, 180.0f, 1.00f);
        public final FloatSetting rotZOff = new FloatSetting(Concat.of("RotZ Off"), 0.0f, -180.0f, 180.0f, 1.00f);

        public AbstractMode getSelectedMode() {
                String modeName = swingMode.getValueSequence().toString();
                for (AbstractMode mode : modes) {
                        if (mode.getName().equals(modeName)) {
                                return mode;
                        }
                }
                return modes.get(0);
        }
}
