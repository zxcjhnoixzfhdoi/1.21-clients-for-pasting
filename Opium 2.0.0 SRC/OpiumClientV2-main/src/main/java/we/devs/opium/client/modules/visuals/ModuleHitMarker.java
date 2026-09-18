package we.devs.opium.client.modules.visuals;

import com.google.common.eventbus.Subscribe;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import we.devs.opium.api.events.PacketEvent2;
import we.devs.opium.api.manager.module.Module;
import we.devs.opium.api.manager.module.RegisterModule;
import we.devs.opium.api.utilities.Timer;
import we.devs.opium.client.values.impl.ValueNumber;

@RegisterModule(name = "HitMarker", description = "Shows a marker when hitting a player.", category = Module.Category.VISUALS)
public class ModuleHitMarker extends Module {

    public final ValueNumber time = new ValueNumber("Show Time", "Show Time", "Show Time", 3, 0, 60);
    //private final Identifier marker = new Identifier("opium", "hitmarker.png");

    public Timer timer=new Timer();
    public int ticks= 114514;


    public void onRender2D(DrawContext drawContext, float tickDelta) {
        if(timer.passedMs(1/20)) {
            timer.reset();
            //if (time.getValue()) {
                ++ticks;
                //drawContext.drawTexture(marker,mc.getWindow().getScaledWidth()/2-8,mc.getWindow().getScaledHeight()/2-8,0,0,0,16,16,16,16);
            //}
        }
    }

    @Subscribe
    public void onTick(PacketEvent2.Send event){
        if(event.getPacket() instanceof PlayerInteractEntityC2SPacket){
            ticks=0;

        }
        
    }
}
