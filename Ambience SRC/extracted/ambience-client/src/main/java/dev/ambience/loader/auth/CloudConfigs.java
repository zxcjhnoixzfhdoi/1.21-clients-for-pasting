package dev.ambience.loader.auth;
import java.util.Collections;
import java.util.List;
public class CloudConfigs {
    public static Object download(String id) { return null; }
    public static Object upload(String name, byte[] data) { return null; }
    public static List<Entry> listMine() { return Collections.emptyList(); }
    public static List<Entry> listPublic() { return Collections.emptyList(); }
    public static void rename(String id, String name) {}
    public static void setVisibility(String id, boolean pub) {}
    public static void delete(String id) {}
    public static class Entry {
        public String id;
        public String name;
        public boolean pub;
        public String owner;
        public Entry() {}
    }
}
