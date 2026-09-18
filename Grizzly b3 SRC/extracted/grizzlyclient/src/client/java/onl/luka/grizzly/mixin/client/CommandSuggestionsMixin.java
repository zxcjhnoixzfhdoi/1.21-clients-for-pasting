package onl.luka.grizzly.mixin.client;

import com.mojang.brigadier.suggestion.Suggestions;
import onl.luka.grizzly.command.CommandEngine;
import net.minecraft.client.gui.components.CommandSuggestions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.CompletableFuture;

@Mixin(CommandSuggestions.class)
public class CommandSuggestionsMixin {

    @Inject(method = "updateCommandInfo", at = @At("HEAD"), cancellable = true)
    private void medved$customSuggestions(CallbackInfo ci) {
        CommandSuggestionsAccessor accessor = (CommandSuggestionsAccessor) this;
        if (accessor.medved$isCommandsOnly()) return;
        if (!accessor.medved$getAllowSuggestions()) return;

        String text = accessor.medved$getInput().getValue();
        if (text.isEmpty()) return;
        char prefix = text.charAt(0);
        if (!CommandEngine.INSTANCE.isCustomPrefix(prefix)) return;

        accessor.medved$getInput().setSuggestion(null);
        accessor.medved$setSuggestions(null);
        accessor.medved$getCommandUsage().clear();
        accessor.medved$setCommandUsagePosition(0);

        Suggestions suggestions = CommandEngine.INSTANCE.suggestions(text, accessor.medved$getInput().getCursorPosition());
        if (suggestions == null || suggestions.isEmpty()) {
            accessor.medved$setPendingSuggestions(null);
        } else {
            accessor.medved$setPendingSuggestions(CompletableFuture.completedFuture(suggestions));
            ((CommandSuggestions) (Object) this).showSuggestions(true);
        }
        ci.cancel();
    }
}
