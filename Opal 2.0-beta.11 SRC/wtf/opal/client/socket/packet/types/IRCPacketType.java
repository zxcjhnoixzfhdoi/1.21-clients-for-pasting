package wtf.opal.client.socket.packet.types;

public final class IRCPacketType {

    public static final int BROADCAST = 0;
    public static final int WHISPER_RECEIVED = 1;
    public static final int WHISPER_SENT = 2;
    public static final int LIST_ONLINE = 3;

    private IRCPacketType() {
    }

}
