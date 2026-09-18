package we.devs.opium.client.modules.player;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import we.devs.opium.Opium;
import we.devs.opium.api.manager.module.Module;
import we.devs.opium.api.manager.module.RegisterModule;
import we.devs.opium.api.utilities.*;
import we.devs.opium.client.events.DamageBlockEvent;
import we.devs.opium.client.events.EventRender3D;
import we.devs.opium.client.values.impl.ValueBoolean;
import we.devs.opium.client.values.impl.ValueCategory;
import we.devs.opium.client.values.impl.ValueColor;
import we.devs.opium.client.values.impl.ValueNumber;

import java.awt.*;

@RegisterModule(name = "CxMine", tag = "CxMine", description = "Cev any block", category = Module.Category.PLAYER)
public class CxMine extends Module {

    private final ValueBoolean pauseOnGap = new ValueBoolean("PauseOnGap", "Pause on Gap", "Pauses while using an item", true);
    private final ValueBoolean silent = new ValueBoolean("SilentSwitch", "Silent Switch", "Silent Switch", true);
    private final ValueBoolean reset = new ValueBoolean("ResetPosAfterMine", "Reset After Mine", "Resets after it mines", true);
    private final ValueBoolean placeInside = new ValueBoolean("PlaceInside", "Place Inside", "Places crystal inside the block after it's broken", true);
    private final ValueNumber cryRange = new ValueNumber("Range", "Crystal Range", "Range in which crystals explode", 5, 1, 20);
    ValueBoolean rotate = new ValueBoolean("Rotate", "Rotate", "Will rotate you to the pos", true);
    ValueBoolean rotateC = new ValueBoolean("Rotate Client Side", "Rotate Client Side", "Will move your camera to the pos", false);
    ValueCategory renderCategory = new ValueCategory("Render", "Render category.");
    ValueBoolean render = new ValueBoolean("FillRender", "Render", "Render the holes you are filling.", this.renderCategory, true);
    ValueColor color = new ValueColor("Color", "Color", "", this.renderCategory, new Color(66, 42, 110, 120));

    private BlockPos targetPos = null;
    private final BlockPos.Mutable mutableBlockPos = new BlockPos.Mutable(0, -1, 0);
    private Direction direction;
    private BlockPos blockPosBelow;

    @Override
    public void onEnable() {
        super.onEnable();
        if (mc.player == null || mc.world == null) {
            this.disable(false);
            mutableBlockPos.set(0, -128, 0);
        }
    }

    @Override
    public void onDamageBlock(DamageBlockEvent event) {
        super.onDamageBlock(event);
        if (mc.player == null || mc.world == null) {
            this.disable(false);
            return;
        }

        targetPos = event.getPos();
        BlockState blockState = mc.world.getBlockState(targetPos);

        if (blockState.getBlock().equals(Blocks.OBSIDIAN)) {
            direction = event.getDirection();
            mutableBlockPos.set(targetPos);
        }
    }

    @Override
    public void onTick() {
        assert mc.interactionManager != null;
        if (mc.player == null || mc.world == null) {
            this.disable(false);
            return;
        }

        super.onTick();

        if (mutableBlockPos.getX() == 0 && mutableBlockPos.getY() == -128 && mutableBlockPos.getZ() == 0) return;

        if (!mc.interactionManager.getCurrentGameMode().equals(GameMode.SURVIVAL)) {
            mutableBlockPos.set(0, -128, 0);
            return;
        }

        executeCxMine();
    }

    private void executeCxMine() {
        assert mc.player != null;
        assert mc.world != null;
        BlockState blockState = mc.world.getBlockState(mutableBlockPos);

        if (pauseOnGap.getValue() && mc.player.isUsingItem()) return;

        int crystalSlot = getCrystalSlot();
        if (crystalSlot == -1) {
            ChatUtils.sendMessage("Out of Crystals!", "CxMine");
            disable(true);
            return;
        }

        int lastSlot = mc.player.getInventory().selectedSlot;
        if (blockState.getBlock().equals(Blocks.OBSIDIAN)) {
            placeCrystal(crystalSlot, mutableBlockPos, lastSlot);
        }

        blockPosBelow = mutableBlockPos.down(1);
        triggerCrystalExplosion(blockState, crystalSlot, lastSlot);
    }

    private int getCrystalSlot() {
        if (InventoryUtils.testInOffHand(Items.END_CRYSTAL)) {
            return 40;
        } else {
            return InventoryUtils.findItem(Items.END_CRYSTAL, 0, 9);
        }
    }

    private void triggerCrystalExplosion(BlockState blockState, int crystalSlot, int lastSlot) {
        assert mc.world != null;
        assert mc.player != null;
        assert mc.interactionManager != null;
        if (blockState.isAir()) {
            for (Entity entity : mc.world.getEntities()) {
                if (entity instanceof EndCrystalEntity crystal) {
                    if (crystal.getBlockPos().equals(mutableBlockPos.up(1)) &&
                            crystal.getPos().isInRange(mc.player.getPos(), cryRange.getValue().doubleValue())) {
                        if (rotate.getValue()) RotationsUtil.rotateToBlockPos(crystal.getBlockPos(), rotateC.getValue());
                        mc.interactionManager.attackEntity(mc.player, crystal);
                        if (reset.getValue()) {
                            mutableBlockPos.set(0, -128, 0);
                        }
                    }
                }
            }

            if (placeInside.getValue()) {
                placeCrystal(crystalSlot, blockPosBelow, lastSlot);
            }
        }
    }

    private void placeCrystal(int slot, BlockPos pos, int lastSlot) {
        if (slot == 40) {
            if (rotate.getValue()) RotationsUtil.rotateToBlockPos(pos, rotateC.getValue());
            assert mc.interactionManager != null;
            mc.interactionManager.interactBlock(mc.player, Hand.OFF_HAND, new BlockHitResult(
                    new Vec3d(pos.getX() + 0.1, pos.getY() + 0.1, pos.getZ() + 0.1), direction, pos, true));
        } else {
            InventoryUtils.switchSlot(slot, silent.getValue());
            if (rotate.getValue()) RotationsUtil.rotateToBlockPos(pos, rotateC.getValue());
            assert mc.interactionManager != null;
            mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(
                    new Vec3d(pos.getX() + 0.1, pos.getY() + 0.1, pos.getZ() + 0.1), direction, pos, true));
            InventoryUtils.switchSlot(lastSlot, silent.getValue());
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        mutableBlockPos.set(0, -128, 0);
        targetPos = null;
    }

    @Override
    public void onRender3D(EventRender3D event) {
        if (render.getValue()) {
            Renderer3d.renderEdged(event.getMatrices(), color.getValue(), color.getValue(), Vec3d.of(mutableBlockPos), new Vec3d(1, 1, 1));
        }
    }

    // Listen for the player disconnect event and disable the module when the player leaves the world.
    static {
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            if (Opium.MODULE_MANAGER.isModuleEnabled("CxMine")) {
                Opium.MODULE_MANAGER.getModule("CxMine").disable(false);
            }
        });
    }
}
