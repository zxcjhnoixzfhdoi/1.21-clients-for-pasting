package hack.echo.client.mixin.accessors;

import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.client.KeyboardHandler;

@Mixin(KeyboardHandler.class)
public interface KeyboardHandlerAccessor {
    @Invoker("keyPress")
    void invokeKeyPress(long window, int action, KeyEvent keyEvent);

    @Invoker("charTyped")
    void invokeCharTyped(long window, CharacterEvent characterEvent);
}
