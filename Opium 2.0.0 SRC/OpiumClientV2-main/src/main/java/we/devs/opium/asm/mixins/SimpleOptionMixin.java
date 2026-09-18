package we.devs.opium.asm.mixins;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.SimpleOption;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import we.devs.opium.asm.ducks.ISimpleOption;

import java.util.function.Consumer;

@Mixin(SimpleOption.class)
public class SimpleOptionMixin<T> implements ISimpleOption<T> {
    @Shadow
    T value;

    @Shadow
    @Final
    private Consumer<T> changeCallback;

    @Override
    public void opium$setValue(T newValue) {
        if (!MinecraftClient.getInstance().isRunning()) {
            value = newValue;
            return;
        }

        if (!value.equals(newValue)) {
            value = newValue;
            changeCallback.accept(value);
        }
    }
}
