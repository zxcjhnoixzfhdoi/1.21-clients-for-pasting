package wtf.opal.client.socket.data;

import wtf.opal.utility.socket.user.ResolvedUser;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class UserCache {

    private final Map<UUID, ResolvedUser> resolvedUsers = new ConcurrentHashMap<>();
    private final Set<UUID> checkedUUIDs = new HashSet<>();

    public Map<UUID, ResolvedUser> getResolvedUsers() {
        return resolvedUsers;
    }

    public Set<UUID> getCheckedUUIDs() {
        return checkedUUIDs;
    }

    public void clear() {
        resolvedUsers.clear();
        checkedUUIDs.clear();
    }

}
