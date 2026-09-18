package we.devs.opium.api.manager.module;

import com.mojang.blaze3d.systems.RenderSystem;
import we.devs.opium.Opium;
import we.devs.opium.api.manager.event.EventListener;
import we.devs.opium.api.utilities.IMinecraft;
import we.devs.opium.client.events.*;
import we.devs.opium.client.modules.client.*;
import we.devs.opium.client.modules.combat.*;
import we.devs.opium.client.modules.exploit.*;
import we.devs.opium.client.modules.miscellaneous.*;
import we.devs.opium.client.modules.movement.*;
import we.devs.opium.client.modules.player.*;
import we.devs.opium.client.modules.visuals.*;
import we.devs.opium.client.values.Value;
import org.lwjgl.opengl.GL11;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.stream.Collectors;

public class ModuleManager implements IMinecraft, EventListener {
    private final ArrayList<Module> modules;

    public ModuleManager() {
        Opium.EVENT_MANAGER.register(this);
        this.modules = new ArrayList<>();
        // write modules here

        //Client
        this.register(new ModuleColor());
        this.register(new ModuleCommands());
        this.register(new ModuleFont());
        this.register(new ModuleOutline());
        this.register(new ModuleGUI());
        this.register(new ModuleHUDEditor());
        this.register(new ModuleParticles());
        this.register(new ModuleRotations());
        this.register(new ModuleRPC());
        this.register(new ModuleAutoConfigSaving());
        this.register(new ModuleConfigEditor());
        this.register(new ModuleAutoTrap());
        this.register(new ModuleCapes());
        this.register(new ModuleLifeSatisfaction());

        //Combat
        this.register(new ModuleAutoArmor());
        this.register(new ModuleHoleFill());
        this.register(new ModuleOffhand());
        this.register(new ModulePopCounter());
        this.register(new ModuleAutoFeetTrap());
        this.register(new ModuleTriggerBot());
        this.register(new ModulePacketExp());
        this.register(new ModuleAntiCrawl());
        this.register(new ModuleAura());
        this.register(new ModuleCriticals());
        this.register(new ModuleAutoCrystal());

        //Exploit
        this.register(new ModuleHitboxDesync());

        //Miscellaneous
        this.register(new ModuleFakePlayer());
        this.register(new ModuleMiddleClick());
        this.register(new ModuleWelcomer());
        this.register(new ModuleFakeDuelMessage());

        //Movement
        this.register(new InstantSpeed());
        this.register(new ModuleReverseStep());
        this.register(new ModuleSpeed());
        this.register(new ModuleSprint());
        this.register(new ModuleVelocity());
        this.register(new ModuleStep());
        this.register(new ModuleNoSlow());

        //Player
        this.register(new ModuleElytraSwap());
        this.register(new ModuleFastPlace());
        this.register(new CxMine());
        this.register(new ModulePacketMine());

        //Visuals
        this.register(new ModuleCrosshair());
        this.register(ModuleCameraClip.INSTANCE);
        this.register(new ModulePopChams());
        this.register(new ModuleHoleESP());
        this.register(new ModuleNametags());
        this.register(new ModuleBurrowESP());
        this.register(new ModuleFullBright());
        this.register(new ModuleNoRender());
        this.register(new ModuleDeathEffects());
        this.register(new ModuleCustomFOV());

        this.modules.sort(Comparator.comparing(Module::getName));
    }

    public void register(Module module) {
        try {
            for (Field field : module.getClass().getDeclaredFields()) {
                if (!Value.class.isAssignableFrom(field.getType())) continue;
                if (!field.isAccessible()) {
                    field.setAccessible(true);
                }
                module.getValues().add((Value)field.get(module));
            }
            module.getValues().add(module.tag);
            module.getValues().add(module.chatNotify);
            module.getValues().add(module.drawn);
            module.getValues().add(module.bind);
            this.modules.add(module);
        }
        catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public ArrayList<Module> getModules() {
        return this.modules;
    }

    public ArrayList<Module> getModules(Module.Category category) {
        return (ArrayList<Module>) this.modules.stream().filter(mm -> mm.getCategory().equals(category)).collect(Collectors.toList());
    }

    public boolean isModuleEnabled(String name) {
        Module module = this.modules.stream().filter(mm -> mm.getName().equals(name)).findFirst().orElse(null);
        if (module != null) {
            return module.isToggled();
        }
        return false;
    }

    @Override
    public void onTick(EventTick event) {
        if (mc.player != null && mc.world != null) {
            this.modules.stream().filter(Module::isToggled).forEach(Module::onTick);
        }
    }

    @Override
    public void onMotion(EventMotion event) {
        if (mc.player != null && mc.world != null) {
            this.modules.stream().filter(Module::isToggled).forEach(Module::onUpdate);
        }
    }

    @Override
    public void onRender2D(EventRender2D event) {
        this.modules.stream().filter(Module::isToggled).forEach(m -> m.onRender2D(event));
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    @Override
    public void onRender3D(EventRender3D event) {
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        RenderSystem.defaultBlendFunc();
        RenderSystem.lineWidth(1.0f);
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        this.modules.stream().filter(Module::isToggled).forEach(mm -> mm.onRender3D(event));
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.enableBlend();
        RenderSystem.enableDepthTest();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    @Override
    public void onLogin(EventLogin event) {
        this.modules.stream().filter(Module::isToggled).forEach(Module::onLogin);
    }

    @Override
    public void onLogout(EventLogout event) {
        this.modules.stream().filter(Module::isToggled).forEach(Module::onLogout);
    }

    public Module getModule(String name) {
        return this.modules.stream().filter(module -> module.getName().equalsIgnoreCase(name)).findFirst().orElse(null);
    }
}
