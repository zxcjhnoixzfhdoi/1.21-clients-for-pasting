package we.devs.opium.client.modules.combat;

import net.minecraft.entity.Entity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import we.devs.opium.Opium;
import we.devs.opium.api.manager.module.Module;
import we.devs.opium.api.manager.module.RegisterModule;
import we.devs.opium.api.utilities.*;
import we.devs.opium.client.events.EventMotion;
import we.devs.opium.client.events.EventPacketReceive;
import we.devs.opium.client.events.EventRender3D;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import we.devs.opium.client.modules.client.ModuleCommands;
import we.devs.opium.client.values.impl.*;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@RegisterModule(name="HoleFill", tag="Hole Fill", description="Fills holes so enemies cant go in.", category=Module.Category.COMBAT)
public class ModuleHoleFill extends Module {
    ValueEnum mode = new ValueEnum("Mode", "Mode", "Mode for the holefill.", Modes.Smart);
    ValueEnum autoSwitch = new ValueEnum("Switch", "Switch", "The mode for Switching.", InventoryUtils.SwitchModes.Normal);
    ValueEnum itemSwitch = new ValueEnum("Item", "Item", "The item to place the blocks with.", InventoryUtils.ItemModes.Obsidian);
    ValueNumber range = new ValueNumber("Range", "Range", "Range to place blocks.", Float.valueOf(8.0f), Float.valueOf(3.0f), Float.valueOf(8.0f));
    ValueNumber targetRange = new ValueNumber("TargetRange", "TargetRange", "The range for the target.", Float.valueOf(6.0f), Float.valueOf(0.0f), Float.valueOf(10.0f));
    ValueBoolean noTargetDisable = new ValueBoolean("NoTargetDisable", "No Target Check", "Disable the module if there is no target.", false);
    ValueBoolean noHolesDisable = new ValueBoolean("NoHolesDisable", "No Holes Disable", "Disable the module if there are no more holes to fill.", false);
    ValueBoolean nearIgnore = new ValueBoolean("NearIgnore", "Near Ignore", "Ignores the holes that are near you.", false);
    ValueNumber nearRange = new ValueNumber("NearIgnoreRange", "Near Range", "Range to ignore the holes near you.", Float.valueOf(3.0f), Float.valueOf(1.0f), Float.valueOf(5.0f));
    ValueBoolean onlyIfSafe = new ValueBoolean("OnlyIfSafe", "Only If Safe", "Only holefills when you are safe.", false);
    ValueBoolean doubles = new ValueBoolean("DoubleHoles", "Double Holes", "Also fills double holes.", true);
    ValueBoolean rotate = new ValueBoolean("Rotate", "Rotate", "Will rotate you to the pos", true);
    ValueBoolean rotateC = new ValueBoolean("Rotate Client Side", "Rotate Client Side", "Will move your camera to the pos", false);
    ValueNumber delay = new ValueNumber("Delay", "Delay", "Delay to fill holes.", 100, 0, 200);
    ValueCategory renderCategory = new ValueCategory("Render", "Render category.");
    ValueBoolean render = new ValueBoolean("FillRender", "Render", "Render the holes you are filling.", this.renderCategory, true);
    ValueColor color = new ValueColor("Color", "Color", "", this.renderCategory, new Color(0, 170, 255, 120));
    private List<BlockPos> placeableHoles = Collections.synchronizedList(new ArrayList<>());
    private PlayerEntity target = null;
    private TimerUtils cooldown = new TimerUtils();
    private BlockPos currentHole = null;

    @Override
    public void onMotion(EventMotion event) {
        if (this.nullCheck()) {
            return;
        }
        this.placeableHoles = this.getHoles();
        this.target = TargetUtils.getTarget(this.targetRange.getValue().floatValue());
        int slot = InventoryUtils.getTargetSlot(this.itemSwitch.getValue().toString());
        int lastSlot = mc.player.getInventory().selectedSlot;
        if (this.placeableHoles.isEmpty()) {
            this.currentHole = null;
        }
        if (slot == -1) {
            ChatUtils.sendMessage("No blocks could be found.", "HoleFill");
            this.disable(true);
            return;
        }
        if (this.noTargetDisable.getValue() && this.target == null) {
            this.disable(true);
            return;
        }
        if (this.mode.getValue().equals(Modes.Smart) && this.target == null || this.onlyIfSafe.getValue() && !HoleUtils.isInHole(ModuleHoleFill.mc.player)) {
            return;
        }
        if (!this.placeableHoles.isEmpty()) {
            for (BlockPos pos : this.placeableHoles.stream().sorted(Comparator.comparing(p -> mc.player.squaredDistanceTo(Vec3d.of(p)))).toList()) {
                if (!this.cooldown.hasTimeElapsed(this.delay.getValue().intValue())) continue;
                this.currentHole = pos;
                InventoryUtils.switchSlot(slot, this.autoSwitch.getValue().equals(InventoryUtils.SwitchModes.Silent));
                if (rotate.getValue()) RotationsUtil.rotateToBlockPos(pos, rotateC.getValue());
                BlockUtils.placeBlock(event, pos, Hand.MAIN_HAND);
                if (!this.autoSwitch.getValue().equals(InventoryUtils.SwitchModes.Strict)) {
                    InventoryUtils.switchSlot(lastSlot, this.autoSwitch.getValue().equals(InventoryUtils.SwitchModes.Silent));
                }
                this.cooldown.reset();
            }
        }
        if (this.noHolesDisable.getValue() && this.placeableHoles.isEmpty()) {
            this.disable(true);
        }
    }

    @Override
    public void onRender3D(EventRender3D event) {
        super.onRender3D(event);
        if (this.nullCheck()) {
            return;
        }
        if (this.currentHole != null && this.render.getValue()) {
            Renderer3d.renderEdged(event.getMatrices(), color.getValue(), color.getValue().darker(), Vec3d.of(this.currentHole), new Vec3d(1, 1, 1));
        }
    }

    public List<BlockPos> getHoles() {
        List<BlockPos> holes = Collections.synchronizedList(new ArrayList<>());
        for (BlockPos position : PlayerUtils.getSphere(this.range.getValue().floatValue(), true, false)) {
            if (HoleUtils.isHole(position)) {
                if (this.nearIgnore.getValue() && mc.player.squaredDistanceTo(Vec3d.of(position)) <= (double)MathUtils.square(this.nearRange.getValue().floatValue()) || !BlockUtils.isPositionPlaceable(position, true, true, false)) continue;
                holes.add(position);
                continue;
            }
            if (!HoleUtils.isDoubleHole(position) || !this.doubles.getValue() || ModuleHoleFill.mc.world.getBlockState(position).getBlock() != Blocks.AIR || ModuleHoleFill.mc.world.getBlockState(position.up()).getBlock() != Blocks.AIR || ModuleHoleFill.mc.world.getBlockState(position.up().up()).getBlock() != Blocks.AIR || this.nearIgnore.getValue() && ModuleHoleFill.mc.player.squaredDistanceTo(Vec3d.of(position)) <= (double)MathUtils.square(this.nearRange.getValue().floatValue()) || !BlockUtils.isPositionPlaceable(position, true, true, false)) continue;
            holes.add(position);
        }
        return holes;
    }

    public enum Modes {
        Normal,
        Smart
    }
}