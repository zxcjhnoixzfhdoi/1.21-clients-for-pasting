package hack.echo.client.mixin.accessors;

import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Minecraft.class)
public interface MinecraftAccessor {
    @Accessor
    MouseHandler getMouseHandler();

    @Accessor
    KeyboardHandler getKeyboardHandler();

    @Invoker
    void invokeStartUseItem();

    @Invoker
    boolean invokeStartAttack();

    @Accessor(value = "frames")
    int getFrames();

    @Accessor(value= "rightClickDelay")
    void setRightClickDelay(int var1);
}
