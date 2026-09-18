package we.devs.opium.client.modules.combat;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import we.devs.opium.api.manager.module.Module;
import we.devs.opium.api.manager.module.RegisterModule;
import we.devs.opium.api.utilities.*;
import we.devs.opium.client.events.EventMotion;
import we.devs.opium.client.events.EventRender3D;
import we.devs.opium.client.values.impl.ValueBoolean;
import we.devs.opium.client.values.impl.ValueColor;
import we.devs.opium.client.values.impl.ValueEnum;
import we.devs.opium.client.values.impl.ValueNumber;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RegisterModule(name="AutoTrap", description="Automatically Traps a Player in Obsidian or Ender Chests.", category=Module.Category.COMBAT)
public class ModuleAutoTrap extends Module {
    ValueEnum itemSwitch = new ValueEnum("Item", "Item", "The item to place the blocks with.", InventoryUtils.ItemModes.Obsidian);
    ValueEnum autoSwitch = new ValueEnum("Switch", "Switch", "The mode for Switching.", InventoryUtils.SwitchModes.Silent);
    ValueNumber targetRange = new ValueNumber("TargetRange", "Target Range", "The max range to an enemy Player", 5.0f, 0.0f, 6.0f);
    ValueNumber placeRange = new ValueNumber("PlaceRange", "Place Range", "The range for placing.", 5.0, 0.0, 6.0);
    ValueNumber delay = new ValueNumber("Delay", "Delay", "max lvl delay", 50L, 0L, 1000L);
    ValueBoolean rotate = new ValueBoolean("Rotate", "Rotate", "Will rotate you to the pos", true);
    ValueBoolean rotateC = new ValueBoolean("Rotate Client Side", "Rotate Client Side", "Will move your camera to the pos", false);
    ValueBoolean render = new ValueBoolean("Render", "Render", "Render.", true);
    ValueColor color = new ValueColor("Color", "Color", "", new Color(255, 0, 213, 120));
    BlockPos[] OFFSETS = new BlockPos[] {
            new BlockPos(1, 0, 0),
            new BlockPos(0, 0, 1),
            new BlockPos(0, 1, 0)
    };
    List<BlockPos> renderPos = new ArrayList<>();
    private PlayerEntity target;

    @Override
    public void onMotion(EventMotion event) {
        if (mc.world == null || mc.player == null || mc.world.getPlayers() == null) {
            return;
        }

        int slot = InventoryUtils.getTargetSlot(itemSwitch.getValue().toString());
        int lastSlot = mc.player.getInventory().selectedSlot;
        if (slot == -1) {
            ChatUtils.sendMessage("Out of Blocks!");
            disable(false);
        }

        this.target = TargetUtils.getTarget(targetRange.getValue().floatValue());
        //Makes Sure that the Target Variable is not empty
        if (this.target == null) {
            return;
        }
        BlockPos pos = this.target.getBlockPos().up();

        InventoryUtils.switchSlot(slot, this.autoSwitch.getValue().equals(InventoryUtils.SwitchModes.Silent));
        for (BlockPos off : OFFSETS) {
            BlockPos newPos = pos.add(off);
            if (!mc.world.isAir(off)) {
                continue;
            }
            if (mc.player.getEyePos().distanceTo(newPos.toCenterPos()) > placeRange.getValue().doubleValue()) {
                continue;
            }
            if (rotate.getValue()) RotationsUtil.rotateToBlockPos(newPos, rotateC.getValue());
            BlockUtils.placeBlock(event, newPos, Hand.MAIN_HAND);
            renderPos.add(newPos);

            try {
                TimeUnit.MILLISECONDS.sleep(delay.getValue().longValue());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        InventoryUtils.switchSlot(lastSlot, this.autoSwitch.getValue().equals(InventoryUtils.SwitchModes.Silent));
    }

    @Override
    public void onRender3D(EventRender3D event) {
        if (render.getValue()) {
            for (BlockPos pos : renderPos) {
                Renderer3d.renderEdged(event.getMatrices(), color.getValue(), color.getValue(), Vec3d.of(pos), new Vec3d(1, 1, 1));
            }
        }
    }
}
