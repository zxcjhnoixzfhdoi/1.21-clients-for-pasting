package wtf.opal.client.socket.data;

import wtf.opal.protection.annotation.NativeInclude;
import wtf.opal.utility.data.Config;

import java.util.Collections;
import java.util.List;

@NativeInclude
public final class ConfigCache {

    private List<Config> configs = Collections.emptyList();
    private boolean requestPacketSent;

    public List<Config> getConfigs() {
        return configs;
    }

    public void setConfigs(final List<Config> configs) {
        this.configs = configs;
    }

    public boolean isRequestPacketSent() {
        return requestPacketSent;
    }

    public void setRequestPacketSent(final boolean requestPacketSent) {
        this.requestPacketSent = requestPacketSent;
    }

}
