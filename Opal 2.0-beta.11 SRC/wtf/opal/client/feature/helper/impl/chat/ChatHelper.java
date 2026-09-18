package wtf.opal.client.feature.helper.impl.chat;

import net.minecraft.util.Formatting;
import wtf.opal.utility.misc.chat.ChatUtility;
import wtf.opal.utility.socket.ChatChannel;

public final class ChatHelper {

    private ChatChannel channel = ChatChannel.ALL;
    private String whisperUsername;

    private ChatHelper() {
    }

    public ChatChannel getChannel() {
        return this.channel;
    }

    public String getWhisperUsername() {
        return this.whisperUsername;
    }

    public void setChannel(final ChatChannel channel) {
        this.channel = this.channel == channel ? ChatChannel.ALL : channel;
        ChatUtility.success("You are now in the " + Formatting.GOLD + this.channel.name() + Formatting.GRAY + " channel.");
    }

    public void setWhisperUsername(final String whisperUsername) {
        this.whisperUsername = whisperUsername;
    }

    private static ChatHelper instance;

    public static ChatHelper getInstance() {
        return instance;
    }

    public static void setInstance() {
        instance = new ChatHelper();
    }

}
