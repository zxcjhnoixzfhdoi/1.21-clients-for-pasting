package hack.echo.client.friends;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class FriendManager {

    private final Map<String, String> friendsByKey = new LinkedHashMap<>();

    public synchronized boolean addFriend(String name) {
        String cleaned = clean(name);
        if (cleaned == null) {
            return false;
        }

        String key = normalize(cleaned);
        String previous = friendsByKey.put(key, cleaned);
        return previous == null;
    }

    public synchronized boolean removeFriend(String name) {
        String key = normalize(name);
        if (key == null) {
            return false;
        }

        return friendsByKey.remove(key) != null;
    }

    public synchronized boolean isFriend(String name) {
        String key = normalize(name);
        if (key == null) {
            return false;
        }

        return friendsByKey.containsKey(key);
    }

    public synchronized List<String> getFriends() {
        return new ArrayList<>(friendsByKey.values());
    }

    public synchronized void setFriends(Collection<String> names) {
        friendsByKey.clear();
        if (names == null) {
            return;
        }

        for (String name : names) {
            addFriend(name);
        }
    }

    public synchronized void clearFriends() {
        friendsByKey.clear();
    }

    public synchronized int size() {
        return friendsByKey.size();
    }

    private String clean(String name) {
        if (name == null) {
            return null;
        }

        String cleaned = name.trim();
        if (cleaned.isEmpty()) {
            return null;
        }

        return cleaned;
    }

    private String normalize(String name) {
        String cleaned = clean(name);
        if (cleaned == null) {
            return null;
        }

        return cleaned.toLowerCase(java.util.Locale.ROOT);
    }
}
