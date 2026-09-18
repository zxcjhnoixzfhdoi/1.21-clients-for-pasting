package we.devs.opium.client.modules.visuals;

import we.devs.opium.api.manager.module.Module;
import we.devs.opium.api.manager.module.RegisterModule;
import we.devs.opium.asm.ducks.ISimpleOption;
import we.devs.opium.client.values.impl.ValueNumber;

import java.util.Objects;

@RegisterModule(name = "CustomFOV", description = "CustomFOV", tag = "CustomFOV", category = Module.Category.VISUALS)
public class ModuleCustomFOV extends Module {
    ValueNumber fov = new ValueNumber("FOV", "FOV", "FOV", 110, 1, 180);

    @Override
    public void onTick() {
        if (!Objects.equals(mc.options.getFov().getValue(), fov.getValue())) {
            ISimpleOption<Integer> fovOption = (ISimpleOption<Integer>) (Object) mc.options.getFov();
            fovOption.opium$setValue(fov.getValue().intValue());
        }
    }
}
