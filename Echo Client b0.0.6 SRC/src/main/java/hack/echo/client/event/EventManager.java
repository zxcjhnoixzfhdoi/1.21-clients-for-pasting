package hack.echo.client.event;

import hack.echo.client.Echo;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
// import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

public final class EventManager {

    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    private static final Map<Class<?>, SortedMap<EventSubscribe.Priority, ListenerData>> listeners = new ConcurrentHashMap<>();
    private static final Map<Object, Boolean> subscribers = new ConcurrentHashMap<>();

    public static void register(Object handler) {
        if (Echo.isDestroyed) return;
        if (handler == null || subscribers.containsKey(handler)) return;
        subscribers.put(handler, true);

        final Class<?> clazz = handler.getClass();
        for (Method method : clazz.getDeclaredMethods()) {
            final Class<?>[] parameterTypes = method.getParameterTypes();
            if (parameterTypes.length != 1 || !Event.class.isAssignableFrom(parameterTypes[0]) || !method.isAnnotationPresent(EventSubscribe.class)) {
                continue;
            }
            final EventSubscribe.Priority priority = method.getAnnotation(EventSubscribe.class).priority();
            final Map<EventSubscribe.Priority, ListenerData> listeners = EventManager.listeners.computeIfAbsent(
                    parameterTypes[0], key -> new TreeMap<>()
            );
            final ListenerData data = listeners.computeIfAbsent(priority, key -> new ListenerData());
            data.put(handler, method);
        }
    }

    public static void unregister(Object handler) {
        if (!subscribers.containsKey(handler)) return;
        subscribers.remove(handler);
        for (Map<EventSubscribe.Priority, ListenerData> datas : listeners.values()) {
            for (ListenerData data : datas.values()) {
                data.remove(handler);
            }
        }
    }

    public static boolean post(Event event) {
        if (Echo.isDestroyed) return false;
        
        final Map<EventSubscribe.Priority, ListenerData> datas = listeners.get(event.getClass());
        if (datas == null) return false;

        for (ListenerData data : datas.values()) {
            data.post(event);
        }

        return event.cancelled;
    }

    public static void reset() {
        listeners.clear();
        subscribers.clear();
    }

    private static final class ListenerData {

        private final Map<Object, MethodHandle[]> handlers = new ConcurrentHashMap<>();

        public synchronized void put(Object handler, Method method) {
            final MethodHandle[] handles;

            if (handlers.containsKey(handler)) {
                handles = new MethodHandle[handlers.get(handler).length + 1];
                System.arraycopy(handlers.get(handler), 0, handles, 0, handles.length - 1);
            } else {
                handles = new MethodHandle[1];
            }

            try {
                method.setAccessible(true);
                handles[handles.length - 1] = LOOKUP.unreflect(method);
            } catch (IllegalAccessException error) {
                error.printStackTrace();
                return;
            }

            handlers.put(handler, handles);
        }

        public synchronized void remove(Object handler) {
            handlers.remove(handler);
        }

        public synchronized void post(Event event) {
            for (Map.Entry<Object, MethodHandle[]> entry : handlers.entrySet()) {
                final Object object = entry.getKey();
                for (MethodHandle handle : entry.getValue()) {
                    try {
                        handle.invoke(object, event);
                    } catch (Throwable error) {
                        error.printStackTrace();
                    }
                }
            }
        }
    }
}