package wtf.opal.utility.security;

import net.minecraft.util.Util;
import wtf.opal.protection.annotation.NativeInclude;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

@NativeInclude
public final class HWIDUtility {

    private HWIDUtility() {
    }

//    public static void main(String[] args) {
//        System.out.println(getHardwareId());
//    }

    public static String getHardwareId() {
        try {
            final Class<?> systemClass = Class.forName("java.lang.System");
            final Method getEnvVariableMethod = systemClass.getDeclaredMethod("getenv", String.class);
            final Method getPropertyMethod = systemClass.getDeclaredMethod("getProperty", String.class);

            final Class<?> runtimeClass = Class.forName("java.lang.Runtime");
            final Method getRuntimeMethod = runtimeClass.getDeclaredMethod("getRuntime");
            final Method availableProcessorsMethod = runtimeClass.getDeclaredMethod("availableProcessors");

            final StringBuilder builder = new StringBuilder("$"
                    + getPropertyMethod.invoke(null, "os.name")
                    + getPropertyMethod.invoke(null, "os.arch")
                    + getEnvVariableMethod.invoke(null, "PROCESSOR_IDENTIFIER")
                    + getEnvVariableMethod.invoke(null, "PROCESSOR_IDENTIFIER")
                    + getEnvVariableMethod.invoke(null, "PROCESSOR_ARCHITECTURE")
                    + getEnvVariableMethod.invoke(null, "PROCESSOR_ARCHITEW6432")
                    + getEnvVariableMethod.invoke(null, "NUMBER_OF_PROCESSORS")
                    + availableProcessorsMethod.invoke(getRuntimeMethod.invoke(null)));

            {
                final String[] command = switch (Util.getOperatingSystem()) {
                    case WINDOWS -> new String[]{
                            "powershell", "-c", "Get-CimInstance Win32_DiskDrive | Where-Object MediaType -eq 'Fixed hard disk media' | Select-Object MediaType,Model,SerialNumber"
                    };
                    case OSX -> new String[]{
                            "bash", "-c", "ioreg -d2 -c IOPlatformExpertDevice | awk -F\\\" '/IOPlatformUUID/{print $(NF-1)}'"
                    };
                    case LINUX -> new String[]{
                            "lsblk", "-e7", "-d", "-o", "size,model,vendor,serial"
                    };
                    default -> throw new RuntimeException("Unsupported operating system");
                };

                final ProcessBuilder pb = new ProcessBuilder(command);
                final Process process = pb.start();

                try (final BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        builder.append(line);
                    }
                }
            }

            final MessageDigest sha512 = MessageDigest.getInstance("SHA-512");
            final byte[] hashBytes = sha512.digest(builder.toString().getBytes(StandardCharsets.UTF_8));
            final byte[] encodedBytes = Base64.getEncoder().encode(hashBytes);

            return new String(encodedBytes, StandardCharsets.UTF_8);
        } catch (Exception ignored) {
            throw new RuntimeException("Unsupported operating system");
        }
    }

}
