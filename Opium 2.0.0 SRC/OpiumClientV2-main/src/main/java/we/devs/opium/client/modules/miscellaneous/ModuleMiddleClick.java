package we.devs.opium.client.modules.miscellaneous;

import net.minecraft.block.Blocks;
import net.minecraft.util.hit.BlockHitResult;
import we.devs.opium.Opium;
import we.devs.opium.api.manager.module.Module;
import we.devs.opium.api.manager.module.RegisterModule;
import we.devs.opium.api.utilities.ChatUtils;
import we.devs.opium.api.utilities.InventoryUtils;
import we.devs.opium.client.modules.client.ModuleCommands;
import we.devs.opium.client.values.impl.ValueBoolean;
import we.devs.opium.client.values.impl.ValueEnum;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import we.devs.opium.client.values.impl.ValueNumber;

@RegisterModule(name="MiddleClick", tag="Middle Click", description="Add actions to middle click.", category=Module.Category.MISCELLANEOUS)
public class ModuleMiddleClick extends Module {
    public static ModuleMiddleClick INSTANCE;
    ValueEnum<GroundMode> groundMode = new ValueEnum<>("GroundMode", "GroundMode", "", GroundMode.XP);
    ValueEnum<AirMode> airMode = new ValueEnum<>("AirMode", "AirMode", "when interacting with air", AirMode.Pearl);
    ValueBoolean doElytra = new ValueBoolean("ElytraFirework", "ElytraFirework", "use a firework when flying with an elytra",true);
    ValueNumber actionDelay = new ValueNumber("ActionDelay", "ActionDelay", "Delay in ticks for fireworks / adding friends", 10, 0, 20);
    int oldSlot = -1;
    private int delay = 0;
    public boolean throwingXP = false;

    public ModuleMiddleClick() {
        INSTANCE = this;
    }
    @Override
    public void onEnable() {
        throwingXP = false;
        oldSlot = -1;
    }

    @Override
    public void onUpdate() {
        if(nullCheck()) return;
        assert mc.player != null;
        this.oldSlot = mc.player.getInventory().selectedSlot;
        int pearlSlot = InventoryUtils.findItem(Items.ENDER_PEARL, 0, 9);
        int expSlot = InventoryUtils.findItem(Items.EXPERIENCE_BOTTLE, 0, 9);
        int fireworkSlot = InventoryUtils.findItem(Items.FIREWORK_ROCKET, 0, 9);
        HitResult hit = mc.crosshairTarget;
        ++this.delay;
        throwingXP = false; // reset every tick

        if (mc.mouse.wasMiddleButtonClicked()) {
            if(mc.player.isFallFlying() && doElytra.getValue() && delay > actionDelay.getValue().intValue()) {
                if (fireworkSlot != -1) {
                    interact(fireworkSlot);
                    delay = 0;
                }
            } else if(hit == null || (hit instanceof BlockHitResult bhr && mc.world.getBlockState(bhr.getBlockPos()).isOf(Blocks.AIR))) {
                switch (this.airMode.getValue()) {
                    case Pearl -> {
                        if(pearlSlot != -1 && delay > actionDelay.getValue().intValue()) interact(pearlSlot);
                    }
                    case XP -> {
                        if (expSlot != -1) {
                            interact(expSlot);
                            this.throwingXP = true;
                        }
                    }
                }
            } else {
                switch (this.groundMode.getValue()) {
                    case XP -> {
                        if (expSlot != -1) {
                            interact(expSlot);
                            this.throwingXP = true;
                        }
                    }
                    case Pearl -> {
                        if(pearlSlot != -1) interact(pearlSlot);
                    }
                    case Friend -> {
                        if(!(hit instanceof EntityHitResult ehr)) return;
                        Entity entity = ehr.getEntity();
                        if (entity instanceof PlayerEntity pl && !mc.player.isDead()) {
                            String name = pl.getGameProfile().getName();
                            if (mc.currentScreen == null && !Opium.FRIEND_MANAGER.isFriend(name) && this.delay > actionDelay.getValue().intValue()) {
                                Opium.FRIEND_MANAGER.addFriend(name);
                                ChatUtils.sendMessage(Formatting.GREEN + "Added " + ModuleCommands.getSecondColor() + name + ModuleCommands.getFirstColor() + " as friend");
                                this.delay = 0;
                            } else if(delay > actionDelay.getValue().intValue()){
                                Opium.FRIEND_MANAGER.removeFriend(name);
                                ChatUtils.sendMessage(Formatting.RED + "Removed " + ModuleCommands.getSecondColor() + name + ModuleCommands.getFirstColor() + " as friend");
                                this.delay = 0;
                            }
                        }
                    }
                }
            }
        }
    }

    void interact(int slot) {
        assert mc.player != null;
        mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(slot));
        mc.player.networkHandler.sendPacket(new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, mc.player.getInventory().main.get(slot).getCount(), mc.player.getYaw(), mc.player.getPitch()));
        mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(this.oldSlot));
    }

    public enum GroundMode {
        Friend,
        XP,
        Pearl
    }

    public enum AirMode {
        XP,
        Pearl
    }
}
