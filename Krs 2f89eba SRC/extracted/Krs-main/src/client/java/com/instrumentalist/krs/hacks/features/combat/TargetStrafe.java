package com.instrumentalist.krs.hacks.features.combat;



import com.instrumentalist.krs.events.features.*;
import com.instrumentalist.krs.hacks.Module;
import com.instrumentalist.krs.hacks.ModuleCategory;
import com.instrumentalist.krs.hacks.ModuleManager;
import com.instrumentalist.krs.hacks.features.movement.fly.FlyModule;
import com.instrumentalist.krs.hacks.features.movement.speed.SpeedModule;
import com.instrumentalist.krs.utils.move.MovementUtil;
import com.instrumentalist.krs.utils.value.BooleanValue;
import com.instrumentalist.krs.utils.value.FloatValue;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;

public class TargetStrafe extends Module {

    public TargetStrafe() {
        super("Target Strafe", ModuleCategory.Combat, GLFW.GLFW_KEY_UNKNOWN, false, true);
    }

    @Setting
    public static FloatValue distance = new FloatValue(
            "Distance",
            1f,
            0f,
            8f
    );

    @Setting
    private static final BooleanValue jumpOnly = new BooleanValue("Jump Only", true);

    public static int direction = 1;

    public static boolean targetStrafeHook() {
        return ModuleManager.getModuleState(TargetStrafe.class) && shouldDoStrafe();
    }

    private static boolean shouldDoStrafe() {
        if (mc.player == null || !ModuleManager.getModuleState(KillAura.class) || KillAura.closestEntity == null) {
            direction = 1;
            return false;
        }

        return (ModuleManager.getModuleState(FlyModule.class) || ModuleManager.getModuleState(SpeedModule.class)) && (!jumpOnly.get() || InputConstants.isKeyDown(mc.getWindow(), InputConstants.getKey(mc.options.keyJump.saveString()).getValue())) && !InputConstants.isKeyDown(mc.getWindow(), InputConstants.getKey(mc.options.keyLeft.saveString()).getValue()) && !InputConstants.isKeyDown(mc.getWindow(), InputConstants.getKey(mc.options.keyRight.saveString()).getValue()) && !InputConstants.isKeyDown(mc.getWindow(), InputConstants.getKey(mc.options.keyDown.saveString()).getValue());
    }

    @Override
    public String description() {
        return "Use by pressing only the forward key";
    }

    @Override
    public void onDisable() {
        direction = 1;
    }

    @Override
    public void onEnable() {
    }

    @Override
    public void onWorld(WorldEvent event) {
        direction = 1;
    }

    @Override
    public void onBlockEdge(BlockEdgeEvent event) {
        if (!shouldDoStrafe()) return;

        if (!MovementUtil.isBlockBelow() && !ModuleManager.getModuleState(FlyModule.class))
            event.cancel();
    }

    @Override
    public void onMotion(MotionEvent event) {
        if (!shouldDoStrafe()) return;

        if (mc.player.horizontalCollision || !MovementUtil.isBlockBelow() && !ModuleManager.getModuleState(FlyModule.class))
            direction = -direction;
    }
}
