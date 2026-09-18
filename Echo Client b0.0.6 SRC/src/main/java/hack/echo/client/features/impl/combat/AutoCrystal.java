package hack.echo.client.features.impl.combat;

import hack.echo.client.event.EventSubscribe;
import hack.echo.client.event.impl.EventStartAttack;
import hack.echo.client.event.impl.EventStartUseItem;
import hack.echo.client.event.impl.EventTick;
import hack.echo.client.features.Category;
import hack.echo.client.features.Feature;
import hack.echo.client.features.FeatureInfo;
import hack.echo.client.features.settings.impl.BoolSetting;
import hack.echo.client.features.settings.impl.IntSetting;
import hack.echo.client.features.settings.impl.ItemPickerSetting;
import hack.echo.client.features.settings.impl.KeybindSetting;
import hack.echo.client.handlers.InputHandler;
import hack.echo.client.handlers.impl.SwapStateManager;
import hack.echo.client.utils.blocks.BlockUtils;
import hack.echo.client.utils.inventory.InventoryUtils;
import hack.echo.client.utils.math.TimerUtils;
import hack.echo.client.utils.strings.Concat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

public class AutoCrystal extends Feature {

	public AutoCrystal() {
		super(new FeatureInfo(
			Concat.of("Auto Crystal"),
			Concat.of("Automatically places and detonates crystals"),
			Category.COMBAT
		));
		itemWhitelist.setSelected(Items.END_CRYSTAL, true);
	}


    private final IntSetting placeDelay = new IntSetting(
        Concat.of("Place Delay"),
        1, 0, 20,
        Concat.of(" ticks")
    );
    private final IntSetting breakDelay = new IntSetting(
        Concat.of("Break Delay"),
        1, 0, 20,
        Concat.of(" ticks")
    );
    private final IntSetting failChance = new IntSetting(Concat.of("Fail Chance"), 0, 0, 100);
    private final BoolSetting inputSimulation = new BoolSetting(Concat.of("Input Simulation"),  false);
    private final BoolSetting stopOnKill = new BoolSetting(Concat.of("Stop on kill"), false);

    // Hit Crystal settings
    private final BoolSetting hitCrystal = new BoolSetting(Concat.of("Place Obsidian"), false);
    private final IntSetting switchTickDelay = new IntSetting(
        Concat.of("Switch Delay"),
        1, 0, 10,
        Concat.of(" ticks"),
        p -> hitCrystal.getValue()
    );
    private final IntSetting placeObsidianTickDelay = new IntSetting(
        Concat.of("Place Obsidian Delay"),
        1, 0, 10,
        Concat.of(" ticks"),
        p -> hitCrystal.getValue()
    );
    private final BoolSetting switchBack = new BoolSetting(Concat.of("Switch Back"), true, p -> hitCrystal.getValue());
    private final ItemPickerSetting itemWhitelist = new ItemPickerSetting(Concat.of("Item Whitelist"));
//    private final ModeSetting amountMode = new ModeSetting
//            (Concat.of("Amount"), Concat.of("Infinite"), p -> hitCrystal.getValue(), Concat.of("Single"), Concat.of("Double"), Concat.of("Triple"), Concat.of("Infinite"));
    private final KeybindSetting activateKey = new KeybindSetting(Concat.of("Activate Key"), -1);


    private final TimerUtils placeTimer = new TimerUtils();
    private final TimerUtils breakTimer = new TimerUtils();

    private enum HitCrystalStage {None, SwitchObsidian, PlaceObsidian, SwitchCrystal, Done}
    private HitCrystalStage currentStage = HitCrystalStage.None;
    private int switchCooldown, placeCooldown;
    private boolean placedObsidian, lastRmbPressed;
    private boolean simulateAttackNext = true;
    @Nullable
    private BlockPos expectedPlacementPos;

    @Override
    public void onEnable() {
        super.onEnable();
        placeTimer.reset();
        breakTimer.reset();
        resetHitCrystal();
    }

    @Override
    public void onDisable() {
        resetHitCrystal();
        super.onDisable();
    }

    private void resetHitCrystal() {
        currentStage = HitCrystalStage.None;
        switchCooldown = 0;
        placeCooldown = 0;
        SwapStateManager.cancel(this, false);
        placedObsidian = false;
        lastRmbPressed = false;
        simulateAttackNext = true;
        expectedPlacementPos = null;
    }

    @EventSubscribe
    private void onTick(EventTick event) {
        if(isNull()) return;
        if (hack.echo.client.api.MinecraftCompat.getScreen() != null) return;
        handleCrystalLogic();
    }

    @EventSubscribe
    private void onDoAttack(EventStartAttack event) {
        if (inputSimulation.getValue()) return;

        if (isEnabled() && mc.player != null && mc.level != null && mc.hitResult instanceof BlockHitResult blockHit) {
            BlockPos pos = blockHit.getBlockPos();
            Block block = mc.level.getBlockState(pos).getBlock();
            
            if ((block == Blocks.OBSIDIAN || block == Blocks.BEDROCK) && 
                InputHandler.isBindDown(activateKey.getKey())) {
                event.cancel();
            }
        }
    }

    @EventSubscribe
    private void onItemUse(EventStartUseItem.Pre event) {
        if (isNull()) return;
        
        int key = activateKey.getKey();
        if (key == -1) return;
        
        boolean isRMB = (key & 0x80000000) != 0 && (key & 0xFF) == GLFW.GLFW_MOUSE_BUTTON_RIGHT;
        if (!isRMB) return;
        
        if (event.isTargetingBlock()) {
            BlockHitResult blockHit = event.getBlockHitResult();
            if (blockHit != null) {
                BlockPos pos = blockHit.getBlockPos();
                Block block = mc.level.getBlockState(pos).getBlock();
                Item heldItem = event.getStack().getItem();
                if ((block == Blocks.OBSIDIAN || block == Blocks.BEDROCK) &&
                    (heldItem == Items.OBSIDIAN || heldItem == Items.BEDROCK) &&
                    currentStage != HitCrystalStage.PlaceObsidian &&
                    !canPlaceHeldBlockAt(blockHit) &&
                    InputHandler.isBindDown(key)) {
                    if (!InventoryUtils.hasItemInHotbar(item -> item == Items.END_CRYSTAL)) return;
                    if (InventoryUtils.isFood(event.getStack())) return;
                    event.cancel();
                }
            }
        }
    }

    private void handleCrystalLogic() {
        if (isNull()) return;
        if (mc.player.isUsingItem()) return;
        if (isDeadBodyNearby() && stopOnKill.getValue()) return;

        boolean rmbPressed = InputHandler.isBindDown(activateKey.getKey());

        Item heldItem = mc.player.getMainHandItem().getItem();
        boolean validActivationItem = isValidActivationItem(heldItem);

        if (hitCrystal.getValue() && rmbPressed && validActivationItem) {
            if (currentStage == HitCrystalStage.None && canActivateHitCrystal()) {
                activateHitCrystal();
            }
        }
        lastRmbPressed = rmbPressed;

        if (!rmbPressed && currentStage != HitCrystalStage.None) {
            SwapStateManager.cancel(this, switchBack.getValue());
            currentStage = HitCrystalStage.None;
        }


        if (currentStage != HitCrystalStage.None) {
            handleHitCrystalLogic();
            return;
        }

        if (!rmbPressed || !validActivationItem) return;
        handleNormalCrystalLogic();
    }

    private boolean isValidActivationItem(Item item) {
        return itemWhitelist.getSelectedCount() > 0 && itemWhitelist.isSelected(item);
    }

    private boolean canActivateHitCrystal() {
        if (mc.hitResult instanceof BlockHitResult blockHit) {
            BlockPos pos = blockHit.getBlockPos();
            BlockState state = mc.level.getBlockState(pos);
            Block block = state.getBlock();
            // If we're looking at obsidian/bedrock already, only need crystals
            if (block == Blocks.OBSIDIAN || block == Blocks.BEDROCK) {
                return InventoryUtils.hasItemInHotbar(item -> item == Items.END_CRYSTAL);
            }
            // Otherwise we need obsidian (or bedrock) to place and crystals
            return InventoryUtils.hasItemInHotbar(item -> item == Items.OBSIDIAN || item == Items.BEDROCK) && InventoryUtils.hasItemInHotbar(item -> item == Items.END_CRYSTAL);
        }
        return false;
    }

    private void activateHitCrystal() {
        // If we're already looking at obsidian/bedrock, go straight to switching to crystal
        if (mc.hitResult instanceof BlockHitResult blockHit) {
            BlockPos pos = blockHit.getBlockPos();
            BlockState hitState = mc.level.getBlockState(pos);
            Block block = hitState.getBlock();
            if (block == Blocks.OBSIDIAN || block == Blocks.BEDROCK) {
                currentStage = HitCrystalStage.SwitchCrystal;
                switchCooldown = switchTickDelay.getValue();
                placedObsidian = false;
                return;
            }
            
            if (hitState.isAir()) {
                return;
            }
            
            Direction hitFace = blockHit.getDirection();
            BlockPos placementPos = hitState.canBeReplaced() ? pos : pos.relative(hitFace);
            BlockState placementState = mc.level.getBlockState(placementPos);
            
            if (!placementState.isAir() && !placementState.canBeReplaced()) {
                return;
            }
        } else {
            return;
        }
        
        currentStage = HitCrystalStage.SwitchObsidian;
        switchCooldown = switchTickDelay.getValue();
        placeCooldown = placeObsidianTickDelay.getValue();
        placedObsidian = false;
    }

    private void handleHitCrystalLogic() {
        HitCrystalStage prevStage = null;
        while (currentStage != HitCrystalStage.None && currentStage != prevStage) {
            prevStage = currentStage;

            if (currentStage == HitCrystalStage.PlaceObsidian) {
                if (placeCooldown > 0) {
                    placeCooldown--;
                    return;
                }
            } else if (currentStage == HitCrystalStage.SwitchObsidian || currentStage == HitCrystalStage.SwitchCrystal) {
                if (switchCooldown > 0) {
                    switchCooldown--;
                    return;
                }
            }

            switch (currentStage) {
                case SwitchObsidian -> {
                    int obsidianSlot = InventoryUtils.findItemWithPredicateInHotbar(itemStack -> itemStack.getItem() == Items.OBSIDIAN);
                    if (obsidianSlot == -1) obsidianSlot = InventoryUtils.findItemWithPredicateInHotbar(itemStack -> itemStack.getItem() == Items.BEDROCK);
                    if (obsidianSlot != -1) {
                        if (!SwapStateManager.swapToIfNeeded(this, obsidianSlot, inputSimulation.getValue(), -1, false)) {
                            currentStage = HitCrystalStage.None;
                            break;
                        }
                        currentStage = HitCrystalStage.PlaceObsidian;
                    } else {
                        currentStage = HitCrystalStage.None;
                    }
                }
                case PlaceObsidian -> {
                    // If we previously attempted to place an obsidian, check if it actually placed
                    if (placedObsidian && expectedPlacementPos != null) {
                        BlockState placedState = mc.level.getBlockState(expectedPlacementPos);
                        Block placedBlock = placedState.getBlock();
                        if (placedBlock == Blocks.OBSIDIAN || placedBlock == Blocks.BEDROCK) {
                            currentStage = HitCrystalStage.SwitchCrystal;
                            switchCooldown = switchTickDelay.getValue();
                            placedObsidian = false;
                            expectedPlacementPos = null;
                            continue;
                        }
                        // still not placed; wait until cooldown expiry then try again
                        placeCooldown = placeObsidianTickDelay.getValue();
                        return;
                    }

                    // Try to place obsidian
                    if (mc.hitResult instanceof BlockHitResult blockHit) {
                        BlockPos placementPos = resolvePlacementPos(blockHit);
                        BlockState placementState = mc.level.getBlockState(placementPos);
                        Block placementBlock = placementState.getBlock();

                        // Only place if the target position doesn't already have obsidian/bedrock
                        if (placementBlock != Blocks.OBSIDIAN && placementBlock != Blocks.BEDROCK) {
                            InputHandler.simulateClick(mc.options.keyUse, inputSimulation.getValue());
                            placedObsidian = true;
                            expectedPlacementPos = placementPos;
                        } else {
                            // Already has obsidian/bedrock, switch to crystal
                            currentStage = HitCrystalStage.SwitchCrystal;
                            switchCooldown = switchTickDelay.getValue();
                            placedObsidian = false;
                            expectedPlacementPos = null;
                        }
                    }
                }
                case SwitchCrystal -> {
                    int crystalSlot = InventoryUtils.findItemWithPredicateInHotbar(itemStack -> itemStack.getItem() == Items.END_CRYSTAL);
                    if (crystalSlot != -1) {
                        if (!SwapStateManager.swapToIfNeeded(this, crystalSlot, inputSimulation.getValue(), -1, false)) {
                            currentStage = HitCrystalStage.None;
                            break;
                        }
                        currentStage = HitCrystalStage.Done;
                        placeTimer.reset();
                    } else {
                        currentStage = HitCrystalStage.None;
                    }
                }
                case Done -> {
                    handleNormalCrystalLogic();
                }
                case None -> {}
            }
        }
    }

    private BlockPos resolvePlacementPos(BlockHitResult blockHit) {
        BlockPlaceContext placeContext = new BlockPlaceContext(
            mc.player,
            InteractionHand.MAIN_HAND,
            mc.player.getMainHandItem(),
            blockHit
        );
        return placeContext.getClickedPos();
    }

    private boolean canPlaceHeldBlockAt(BlockHitResult blockHit) {
        if (mc.player == null || mc.level == null) return false;

        BlockPos placementPos = resolvePlacementPos(blockHit);
        BlockState placementState = mc.level.getBlockState(placementPos);
        return placementState.isAir() || placementState.canBeReplaced();
    }

    private void handleClickSimulation(boolean rmbPressed) {
        if (!rmbPressed) return;
        if (!isLookingAtValidTarget()) return;

        boolean breakReady = breakTimer.hasReachedTicks(breakDelay.getValue());
        boolean placeReady = placeTimer.hasReachedTicks(placeDelay.getValue());
        if (!breakReady && !placeReady) return;

        if (simulateAttackNext) {
            if (breakReady) {
                if (Math.random() * 100 >= failChance.getValue()) {
                    InputHandler.simulateClick(mc.options.keyAttack, inputSimulation.getValue());
                }
                breakTimer.reset();
                simulateAttackNext = false;
                return;
            }

            if (placeReady) {
                if (Math.random() * 100 >= failChance.getValue()) {
                    InputHandler.simulateClick(mc.options.keyUse, inputSimulation.getValue());
                }
                placeTimer.reset();
                simulateAttackNext = true;
            }
            return;
        }

        if (placeReady) {
            if (Math.random() * 100 >= failChance.getValue()) {
                InputHandler.simulateClick(mc.options.keyUse, inputSimulation.getValue());
            }
            placeTimer.reset();
            simulateAttackNext = true;
            return;
        }

        if (breakReady) {
            if (Math.random() * 100 >= failChance.getValue()) {
                InputHandler.simulateClick(mc.options.keyAttack, inputSimulation.getValue());
            }
            breakTimer.reset();
            simulateAttackNext = false;
        }
    }

    private boolean isLookingAtValidTarget() {
        if (mc.hitResult instanceof EntityHitResult entityHit) {
            if (entityHit.getEntity() instanceof EndCrystal crystal) {
                return !crystal.isRemoved() && crystal.isAlive();
            }
            return false;
        }
        if (mc.hitResult instanceof BlockHitResult blockHit) {
            BlockState state = mc.level.getBlockState(blockHit.getBlockPos());
            return state.is(Blocks.OBSIDIAN) || state.is(Blocks.BEDROCK);
        }
        return false;
    }

    private void handleNormalCrystalLogic() {
        if (inputSimulation.getValue()) {
            boolean hasCrystalInHand = mc.player.getMainHandItem().is(Items.END_CRYSTAL);
            boolean hasCrystalInHotbar = InventoryUtils.hasItemInHotbar(item -> item == Items.END_CRYSTAL);
            boolean hasCrystalInOffhand = mc.player.getOffhandItem().is(Items.END_CRYSTAL);
            if (!hasCrystalInHand && !hasCrystalInHotbar && !hasCrystalInOffhand) return;

            if (!hasCrystalInHand && hasCrystalInHotbar) {
                int crystalSlot = InventoryUtils.findItemWithPredicateInHotbar(itemStack -> itemStack.getItem() == Items.END_CRYSTAL);
                InventoryUtils.setInvSlot(crystalSlot, true);
                return;
            }

            handleClickSimulation(true);
            return;
        }
        handlePlaceCrystal();
        handleBreakCrystal();
    }

    private void handlePlaceCrystal() {
        if (!(mc.hitResult instanceof BlockHitResult blockHit)) return;
        
        BlockPos pos = blockHit.getBlockPos();
        BlockState state = mc.level.getBlockState(pos);
        boolean validBlock = state.is(Blocks.OBSIDIAN) || state.is(Blocks.BEDROCK);
        
        if (!validBlock) return;
        if (BlockUtils.hasCrystalOnBlock(pos)) return;
        if (mc.player.getMainHandItem().getItem() != Items.END_CRYSTAL) {
            int crystalSlot = InventoryUtils.findItemWithPredicateInHotbar(itemStack -> itemStack.getItem() == Items.END_CRYSTAL);
            if (crystalSlot != -1) {
                InventoryUtils.setInvSlot(crystalSlot, inputSimulation.getValue());
                return; 
            }
            return;
        }

        if (placeTimer.hasReachedTicks(placeDelay.getValue())) {
            if (Math.random() * 100 < failChance.getValue()) return;
            InputHandler.simulateClick(mc.options.keyUse, inputSimulation.getValue());
            placeTimer.reset();
        }
    }

    private void handleBreakCrystal() {
        if (!(mc.hitResult instanceof EntityHitResult entityHit)) return;
        if (!(entityHit.getEntity() instanceof EndCrystal crystal)) return;
        
        if (crystal.isRemoved() || !crystal.isAlive()) return;

        if (breakTimer.hasReachedTicks(breakDelay.getValue())) {
            if (Math.random() * 100 < failChance.getValue()) return;
            InputHandler.simulateClick(mc.options.keyAttack, inputSimulation.getValue());
            breakTimer.reset();
        }
    }

    private boolean isDeadBodyNearby() {
        for (var player : mc.level.players()) {
            if (mc.player == player) continue;

            double distanceSquared = mc.player.distanceToSqr(player);
            if (distanceSquared < 36 && player.isDeadOrDying()) {
                return true;
            }
        }
        return false;
    }

    public int getActivateKey() {
        return activateKey.getKey();
    }

    public boolean isHitCrystalEnabled() {
        return hitCrystal.getValue();
    }
}
