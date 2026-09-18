package onl.luka.grizzly.mixin.client;

import com.mojang.brigadier.suggestion.Suggestions;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Mixin(CommandSuggestions.class)
public interface CommandSuggestionsAccessor {

    @Accessor("input")
    EditBox medved$getInput();

    @Accessor("suggestions")
    void medved$setSuggestions(CommandSuggestions.SuggestionsList suggestions);

    @Accessor("pendingSuggestions")
    void medved$setPendingSuggestions(CompletableFuture<Suggestions> pendingSuggestions);

    @Accessor("commandUsage")
    List<FormattedCharSequence> medved$getCommandUsage();

    @Accessor("commandUsagePosition")
    void medved$setCommandUsagePosition(int commandUsagePosition);

    @Accessor("commandsOnly")
    boolean medved$isCommandsOnly();

    @Accessor("allowSuggestions")
    boolean medved$getAllowSuggestions();
}
