package wtf.opal.utility.data;

import wtf.opal.client.binding.IBindable;
import wtf.opal.client.socket.ClientSocket;
import wtf.opal.client.socket.packet.impl.c2s.config.C2SConfigLoadPacket;

import java.util.Date;

public final class Config implements IBindable {

    private final String name;

    private String description;
    private boolean pinned;
    private Date updatedAt;

    public Config(final String name) {
        this.name = name;
    }

    public Config(final String name, final String description, final boolean pinned, final Date updatedAt) {
        this.name = name;
        this.description = description;
        this.pinned = pinned;
        this.updatedAt = updatedAt;
    }

    @Override
    public void onBindingInteraction() {
        ClientSocket.getInstance().sendPacket(new C2SConfigLoadPacket(name));
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public boolean isPinned() {
        return pinned;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }
}
