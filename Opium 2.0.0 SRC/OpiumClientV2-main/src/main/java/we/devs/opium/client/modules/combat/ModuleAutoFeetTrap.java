package we.devs.opium.client.modules.combat;

import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.util.math.Box;
import we.devs.opium.Opium;
import we.devs.opium.api.manager.module.Module;
import we.devs.opium.api.manager.module.RegisterModule;
import we.devs.opium.api.utilities.*;
import we.devs.opium.client.events.EventMotion;
import we.devs.opium.client.events.EventRender3D;
import we.devs.opium.client.values.impl.*;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


@RegisterModule(name="AutoFeetTrap", description="Places blocks around your feet to protect you from crystals.", category=Module.Category.COMBAT)
public class ModuleAutoFeetTrap extends Module {
    private final ValueEnum mode = new ValueEnum("Mode", "Mode", "The mode for the Surround.", Modes.Normal);
    private final ValueEnum autoSwitch = new ValueEnum("Switch", "Switch", "The mode for Switching.", InventoryUtils.SwitchModes.Normal);
    private final ValueEnum itemSwitch = new ValueEnum("Item", "Item", "The item to place the blocks with.", InventoryUtils.ItemModes.Obsidian);
    private final ValueNumber blocks = new ValueNumber("Blocks", "Blocks", "The amount of blocks that can be placed per tick.", 8, 1, 40);
    private final ValueEnum supports = new ValueEnum("Supports", "Supports", "The support blocks for the Surround.", Supports.Dynamic);
    private final ValueBoolean dynamic = new ValueBoolean("Dynamic", "Dynamic", "Makes the surround place dynamically.", true);
    private final ValueBoolean ignoreCrystals = new ValueBoolean("IgnoreCrystals", "Ignore Crystals", "Ignores crystals when checking if there are any entities in the block that needs to be placed.", false);
    private final ValueBoolean stepDisable = new ValueBoolean("StepDisable", "Step Disable", "Disable if step enabled.", true);
    private final ValueBoolean jumpDisable = new ValueBoolean("JumpDisable", "Jump Disable", "Disable if player jumps.", true);
    private final ValueBoolean rotate = new ValueBoolean("Rotate", "Packet Rotate", "Rotates to the block after placement.", true);
    private final ValueBoolean rotateC = new ValueBoolean("RotateC", "Rotate Client Side", "Rotates to the block after placement.", false);
    private final ValueNumber attackSpeed = new ValueNumber("AttackSpeed", "Attack Speed", "At What Speed To Attack The Crystals", 1, 1, 20);
    private final ValueEnum rotationE = new ValueEnum("Test", "Test", "Test", CxRotations.Rotate.Default);
    private int placements;
    private BlockPos startPosition;

    ValueCategory renderCategory = new ValueCategory("Render", "Render category.");
    ValueColor color = new ValueColor("Color", "Color", "", this.renderCategory, new Color(0, 170, 255, 120));
    ValueColor outline = new ValueColor("OutlineColor", "OutlineColor", "", this.renderCategory, new Color(0, 170, 255, 120).darker());
    ValueBoolean render = new ValueBoolean("Render", "Render", "Render the holes you are filling.", this.renderCategory, true);

    @Override
    public void onEnable() {
        super.onEnable();
        if (mc.player == null || mc.world == null) {
            this.disable(true);
            return;
        }
        this.startPosition = new BlockPos((int) Math.round(mc.player.getX()), (int) Math.round(mc.player.getY()), (int) Math.round(mc.player.getZ()));
    }

    @Override
    public void onMotion(EventMotion event) {
        super.onMotion(event);
        SmoothRotationUtil.updateRotation(rotateC.getValue());
        assert mc.player != null;
        if ((double)this.startPosition.getY() != MathUtils.roundToPlaces(mc.player.getY(), 0) && this.mode.getValue().equals(Modes.Normal)) {
            this.disable(true);
            return;
        }
        if (this.jumpDisable.getValue() && mc.options.jumpKey.isPressed() || this.stepDisable.getValue() && Opium.MODULE_MANAGER.isModuleEnabled("Step")) {
            this.disable(true);
            return;
        }
        int slot = InventoryUtils.getTargetSlot(this.itemSwitch.getValue().toString());
        int lastSlot = mc.player.getInventory().selectedSlot;
        if (slot == -1) {
            ChatUtils.sendMessage("No blocks could be found.", "Surround");
            this.disable(true);
            return;
        }
        if (!this.getUnsafeBlocks().isEmpty()) {
            InventoryUtils.switchSlot(slot, this.autoSwitch.getValue().equals(InventoryUtils.SwitchModes.Silent));
            for (BlockPos position : this.getUnsafeBlocks()) {
                if (!this.supports.getValue().equals(Supports.None) && (BlockUtils.getPlaceableSide(position) == null || this.supports.getValue().equals(Supports.Static))) {
                    this.placeBlock(event, position.down());
                }
                this.placeBlock(event, position);
            }
            if (!this.autoSwitch.getValue().equals(InventoryUtils.SwitchModes.Strict)) {
                InventoryUtils.switchSlot(lastSlot, this.autoSwitch.getValue().equals(InventoryUtils.SwitchModes.Silent));
            }
        }
        this.placements = 0;
        if (this.getUnsafeBlocks().isEmpty() && this.mode.getValue().equals(Modes.Toggle)) {
            this.disable(true);
        }
    }

    public void placeBlock(EventMotion event, BlockPos position) {
        if (BlockUtils.surroundPlaceableCheck(position, true, true) && this.placements < this.blocks.getValue().intValue()) {
            assert mc.player != null;

            // Attack blocking crystals before placing the block
            attackBlockingCrystals(position);

            boolean found = false;
            for (Block fade : fades) {
                if (fade.pos.equals(position)) {
                    found = true;
                    break;
                }
            }
            if (!found) fades.add(new Block(position));

            if (rotate.getValue()) {
                CxRotations.rotateToBlock(position, CxRotations.Rotate.Grim, false);
            }
            //BlockUtils.placeBlock(event, position, Hand.MAIN_HAND);
            CxBlockUtil.placeBlock(position, CxRotations.Rotate.Grim, false);

            // imma kms :sob:
            ++this.placements;
        }
    }


    public List<BlockPos> getUnsafeBlocks() {
        ArrayList<BlockPos> positions = new ArrayList<>();
        for (BlockPos position : this.getOffsets()) {
            assert mc.world != null;
            assert mc.player != null;
            if (!mc.world.getBlockState(position).canReplace(new ItemPlacementContext(mc.player, Hand.MAIN_HAND, mc.player.getStackInHand(Hand.MAIN_HAND), new BlockHitResult(Vec3d.of(position), Direction.UP, position, false)))) continue;
            positions.add(position);
        }
        return positions;
    }

    private List<BlockPos> getOffsets() {
        ArrayList<BlockPos> offsets = new ArrayList<>();
        if (this.dynamic.getValue()) {
            int z;
            int x;
            assert mc.player != null;
            double decimalX = Math.abs(mc.player.getX()) - Math.floor(Math.abs(mc.player.getX()));
            double decimalZ = Math.abs(mc.player.getZ()) - Math.floor(Math.abs(mc.player.getZ()));
            int lengthX = this.calculateLength(decimalX, false);
            int negativeLengthX = this.calculateLength(decimalX, true);
            int lengthZ = this.calculateLength(decimalZ, false);
            int negativeLengthZ = this.calculateLength(decimalZ, true);
            ArrayList<BlockPos> tempOffsets = new ArrayList<>();
            offsets.addAll(this.getOverlapPositions());
            for (x = 1; x < lengthX + 1; ++x) {
                tempOffsets.add(this.addToPosition(this.getPlayerPosition(), x, 1 + lengthZ));
                tempOffsets.add(this.addToPosition(this.getPlayerPosition(), x, -(1 + negativeLengthZ)));
            }
            for (x = 0; x <= negativeLengthX; ++x) {
                tempOffsets.add(this.addToPosition(this.getPlayerPosition(), -x, 1 + lengthZ));
                tempOffsets.add(this.addToPosition(this.getPlayerPosition(), -x, -(1 + negativeLengthZ)));
            }
            for (z = 1; z < lengthZ + 1; ++z) {
                tempOffsets.add(this.addToPosition(this.getPlayerPosition(), 1 + lengthX, z));
                tempOffsets.add(this.addToPosition(this.getPlayerPosition(), -(1 + negativeLengthX), z));
            }
            for (z = 0; z <= negativeLengthZ; ++z) {
                tempOffsets.add(this.addToPosition(this.getPlayerPosition(), 1 + lengthX, -z));
                tempOffsets.add(this.addToPosition(this.getPlayerPosition(), -(1 + negativeLengthX), -z));
            }
            offsets.addAll(tempOffsets);
        } else {
            for (Direction side : Direction.Type.HORIZONTAL) {
                offsets.add(this.getPlayerPosition().add(side.getOffsetX(), 0, side.getOffsetZ()));
            }
        }
        return offsets;
    }

    private BlockPos getPlayerPosition() {
        assert mc.player != null;
        return new BlockPos(mc.player.getBlockX(), (int) (mc.player.getBlockY() - Math.floor(mc.player.getY()) > 0.8 ? Math.floor(mc.player.getY()) + 1.0 : Math.floor(mc.player.getY())), mc.player.getBlockZ());
    }

    private List<BlockPos> getOverlapPositions() {
        ArrayList<BlockPos> positions = new ArrayList<>();
        assert mc.player != null;
        int offsetX = this.calculateOffset(mc.player.getX() - Math.floor(mc.player.getX()));
        int offsetZ = this.calculateOffset(mc.player.getZ() - Math.floor(mc.player.getZ()));
        positions.add(this.getPlayerPosition());
        for (int x = 0; x <= Math.abs(offsetX); ++x) {
            for (int z = 0; z <= Math.abs(offsetZ); ++z) {
                int properX = x * offsetX;
                int properZ = z * offsetZ;
                positions.add(this.getPlayerPosition().add(properX, -1, properZ));
            }
        }
        return positions;
    }

    private BlockPos addToPosition(BlockPos position, double x, double z) {
        if (position.getX() < 0) {
            x = -x;
        }
        if (position.getZ() < 0) {
            z = -z;
        }
        return position.add((int) x, 0, (int) z);
    }

    private int calculateOffset(double dec) {
        return dec >= 0.7 ? 1 : (dec <= 0.3 ? -1 : 0);
    }

    private int calculateLength(double decimal, boolean negative) {
        if (negative) {
            return decimal <= 0.3 ? 1 : 0;
        }
        return decimal >= 0.7 ? 1 : 0;
    }

    private void attackBlockingCrystals(BlockPos position) {
        assert mc.world != null;
        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof EndCrystalEntity) {
                // Check if the crystal is within range of the block position
                assert mc.player != null;
                if (isBlocking(entity, position)) {
                    attackCrystal(entity);
                    return; // Attack only one crystal at a time (you can modify this to attack all nearby crystals)
                }
            }
        }
    }

    private boolean isBlocking(Entity crystal, BlockPos pos) {
        // Check if the crystal is blocking the position
        // This is a simple check: you can improve it based on your needs (e.g., checking bounding box, distance, etc.)
        return crystal.getBoundingBox().intersects(new Box(pos));
    }

    private long lastAttack = 0;

    private void attackCrystal(Entity crystal) {
        try {
            if (System.currentTimeMillis() - lastAttack >= 1000 / attackSpeed.getValue().floatValue()) {
                assert mc.player != null;
                sendPacket(PlayerInteractEntityC2SPacket.attack(crystal, mc.player.isSneaking()));
                if (rotate.getValue()) RotationsUtil.rotateToBlockPos(crystal.getBlockPos(), rotateC.getValue());
                lastAttack = System.currentTimeMillis();
            }
        } catch (Exception e) {
            Opium.LOGGER.error("[Surround] Error while attacking crystal", e);
        }
    }

    private void sendPacket(PlayerInteractEntityC2SPacket packet) {
        ClientPlayNetworkHandler networkHandler = mc.getNetworkHandler();
        if (networkHandler != null) {
            networkHandler.sendPacket(packet);
        }
    }


    ArrayList<Block> fades = new ArrayList<>();
    @Override
    public void onRender3D(EventRender3D event) {
        Iterator<Block> i = fades.iterator();
        while (i.hasNext()) {
            Block next = i.next();
            if(next.shouldRemove()) i.remove();
            else next.render(event.getMatrices());
        }
    }

    class Block {
        private final BlockPos pos;
        private final FadeUtils fade = new FadeUtils(450);

        public Block(BlockPos pos) {
            this.pos = pos;
        }

        public void render(MatrixStack matrices) {
            if(!render.getValue()) return;
            Color init = color.getValue();
            Color initO = outline.getValue();
            Renderer3d.renderEdged(matrices, new Color(init.getRed(), init.getGreen(), init.getBlue(), (int) (150 * (1 - fade.ease(FadeUtils.Ease.Fast)))), new Color(initO.getRed(), initO.getGreen(), initO.getBlue(), (int) (150 * (1 - fade.ease(FadeUtils.Ease.Fast)))), Vec3d.of(pos), new Vec3d(1, 1, 1));
        }

        public boolean shouldRemove() {
            return fade.isEnd();
        }
    }

    public enum Supports {
        None,
        Dynamic,
        Static
    }

    public enum Modes {
        Normal,
        Persistent,
        Toggle,
        Shift
    }
}
