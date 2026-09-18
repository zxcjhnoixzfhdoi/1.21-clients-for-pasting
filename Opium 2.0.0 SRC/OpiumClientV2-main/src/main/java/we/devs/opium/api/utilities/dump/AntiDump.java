package we.devs.opium.api.utilities.dump;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.*;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.reflect.Method;
import java.util.*;
import static org.objectweb.asm.Opcodes.*;

public class AntiDump {
    private static final String[] FLAGS = {
            "-XBootclasspath", "-javaagent", "-Xdebug", "-agentlib", "-Xrunjdwp", "-Xnoagent",
            "-verbose", "-DproxySet", "-DproxyHost", "-DproxyPort",
            "-Djavax.net.ssl.trustStore", "-Djavax.net.ssl.trustStorePassword"
    };

    static {
        try {
            byte[] bytes = createDummyClass("dummy/class/path/MaliciousClassFilter");
            DynamicClassLoader.INSTANCE.defineClass("dummy.class.path.MaliciousClassFilter", bytes, 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void check() {
        try {
            RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();
            List<String> arguments = runtimeMxBean.getInputArguments();
            for (String flag : FLAGS) {
                for (String inputArgument : arguments) {
                    if (inputArgument.contains(flag)) {
                        System.out.println("Detected illegal program arguments!");
                        dumpDetected();
                    }
                }
            }

            if (isClassLoaded("sun.instrument.InstrumentationImpl")) {
                System.out.println("Detected sun.instrument.InstrumentationImpl!");
                dumpDetected();
            }

            System.setProperty("sun.jvm.hotspot.tools.jcore.filter", "dummy.class.path.MaliciousClassFilter");
        } catch (Throwable e) {
            e.printStackTrace();
            dumpDetected();
        }
    }

    private static boolean isClassLoaded(String className) {
        try {
            Method m = ClassLoader.class.getDeclaredMethod("findLoadedClass", String.class);
            m.setAccessible(true);
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            return m.invoke(cl, className) != null;
        } catch (Exception e) {
            return false;
        }
    }

    private static byte[] createDummyClass(String name) {
        ClassNode classNode = new ClassNode();
        classNode.name = name.replace('.', '/');
        classNode.access = ACC_PUBLIC;
        classNode.version = V1_8;
        classNode.superName = "java/lang/Object";

        MethodNode methodNode = new MethodNode(ACC_PUBLIC + ACC_STATIC, "<clinit>", "()V", null, null);
        InsnList insn = new InsnList();
        insn.add(new FieldInsnNode(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;"));
        insn.add(new LdcInsnNode("Nice try"));
        insn.add(new MethodInsnNode(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false));
        insn.add(new TypeInsnNode(NEW, "java/lang/Throwable"));
        insn.add(new InsnNode(DUP));
        insn.add(new LdcInsnNode("owned"));
        insn.add(new MethodInsnNode(INVOKESPECIAL, "java/lang/Throwable", "<init>", "(Ljava/lang/String;)V", false));
        insn.add(new InsnNode(ATHROW));
        methodNode.instructions = insn;

        classNode.methods.add(methodNode);
        ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        classNode.accept(classWriter);
        return classWriter.toByteArray();
    }

    private static void dumpDetected() {
        System.err.println("Unauthorized activity detected. Terminating process.");
        System.exit(0);
    }

    public static TimerTask get() {
        return new TimerTask() {
            @Override
            public void run() {
                AntiDump.check();
            }
        };
    }
}
