package we.devs.opium.api.utilities.dump;

import we.devs.opium.Opium;

public class DynamicClassLoader extends ClassLoader {

    public static DynamicClassLoader INSTANCE = new DynamicClassLoader();
    private DynamicClassLoader() {}

    public Class<?> defineClass(String name, byte[] b, int offset) {
        return defineClass(name, b, offset, b.length);
    }


}