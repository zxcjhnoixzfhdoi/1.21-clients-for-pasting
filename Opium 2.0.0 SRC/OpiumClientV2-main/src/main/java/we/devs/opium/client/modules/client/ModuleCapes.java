package we.devs.opium.client.modules.client;

import we.devs.opium.api.manager.CapeManager;
import we.devs.opium.api.manager.module.Module;
import we.devs.opium.api.manager.module.RegisterModule;
import we.devs.opium.api.utilities.ChatUtils;
import we.devs.opium.client.values.impl.ValueEnum;
import we.devs.opium.client.values.impl.ValueNumber;

@RegisterModule(name="Capes", description="Rotations of the client", category=Module.Category.CLIENT)
public class ModuleCapes extends Module {
    ValueEnum cape = new ValueEnum("Cape", "Cape", "Nigga", Cape.cape);

    @Override
    public void onEnable() {
        super.onEnable();

        if (cape.getValue().equals(Cape.lightning) || cape.getValue().equals(Cape.cute_bear)) {
            CapeManager.setFullAnimatedCape(cape.getValue().name());
        } else {
            CapeManager.setCape(cape.getValue().name());
        }
        ChatUtils.sendMessage("Cape Set!" , "Capes");
        this.disable(false);
    }

    enum Cape {
        cape,
        crystal,
        cxiy,
        dreizz1,
        dreizz2,
        heedii,
        original,
        lightning,
        cute_bear,
        moppl
    }
}
