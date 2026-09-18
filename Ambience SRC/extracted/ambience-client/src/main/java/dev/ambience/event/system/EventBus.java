package dev.ambience.event.system;
import x.Event;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;
import java.lang.reflect.Method;
public class EventBus {
    private final Map<Class<?>, CopyOnWriteArrayList<Listener>> listeners = new ConcurrentHashMap<>();
    public void register(Object var1) { register(var1, var1.getClass()); }
    public void unregister(Object var1) {
        for (CopyOnWriteArrayList<Listener> var3 : this.listeners.values())
            var3.removeIf(var1x -> var1x.host().equals(var1));
    }
    public boolean post(Event var1) {
        List<Listener> var2 = this.listeners.get(var1.getClass());
        if (var2 == null) return false;
        for (Listener var4 : var2) {
            if (var1.isCancelled()) return true;
            var4.invoke(var1);
        }
        return var1.isCancelled();
    }
    private void register(Object var1, Class<?> var2) {
        for (Method var6 : var2.getDeclaredMethods()) {
            Subscribe var7 = var6.getAnnotation(Subscribe.class);
            if (var7 != null) {
                Class<?>[] var8 = var6.getParameterTypes();
                if (var8.length == 1 && Event.class.isAssignableFrom(var8[0])) {
                    this.listeners.computeIfAbsent(var8[0], k -> new CopyOnWriteArrayList<>())
                        .add(Listener.of(var1, var7.priority(), var6));
                }
            }
        }
        Class<?> parent = var2.getSuperclass();
        if (parent != null && parent != Object.class) register(var1, parent);
    }
}
