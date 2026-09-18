package we.devs.opium.client.modules.visuals;

import we.devs.opium.api.manager.module.Module;
import we.devs.opium.api.manager.module.RegisterModule;
import we.devs.opium.asm.ducks.ISimpleOption;

@RegisterModule(name="FullBright", description="more mcswag.", category=Module.Category.VISUALS)
public class ModuleFullBright extends Module {
    double oldGamma;

    @Override
    public void onEnable() {
        oldGamma = mc.options.getGamma().getValue();
        ISimpleOption<Double> gammaOption = (ISimpleOption<Double>) (Object) mc.options.getGamma();
        gammaOption.opium$setValue(1000.0);
    }

    @Override
    public void onDisable() {
        ISimpleOption<Double> gammaOption = (ISimpleOption<Double>) (Object) mc.options.getGamma();
        gammaOption.opium$setValue(oldGamma);
    }
}
