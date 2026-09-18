package wtf.opal.utility.misc;

import net.minecraft.client.MinecraftClient;
import sun.misc.Unsafe;
import wtf.opal.utility.misc.system.DialogUtility;
import wtf.opal.utility.security.SessionUtility;
import wtf.opal.utility.socket.client.MetadataUtility;

import javax.net.ssl.HttpsURLConnection;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;

// prot will replace calls to these methods with the method bodies
// all methods that call these methods should be natived
// do not use lambdas or create anonymous classes in these methods, they will break obfuscation
public final class Callables {

    private Callables() {
    }

    public static void segfault() {
        try {
            final Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);

            final Unsafe unsafe = (Unsafe) f.get(null);
            unsafe.putAddress(0, 0);
        } catch (Throwable ignored) {
        }
    }

    public static void sendIntegrityAlert(final int marker) {
        final String markerHex = String.format("%X", (marker + 88) ^ 0x5A8B3F9C).toUpperCase();

        // send alert to server
        try {
            // TODO: update domain
//            final HttpURLConnection conn = (HttpURLConnection) new URI("http://localhost:3000/api/c/0").toURL().openConnection();
            final HttpsURLConnection conn = (HttpsURLConnection) new URI("https://opalclient.com/api/c/0").toURL().openConnection();

            conn.setRequestProperty("Authorization", SessionUtility.getKeystoreToken());
            conn.setRequestProperty("User-Agent", MetadataUtility.getAgent());
            conn.setRequestProperty("Content-Type", "application/json");

            conn.setInstanceFollowRedirects(false);
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(10000);
            conn.setDoOutput(true);

            try (final OutputStream out = conn.getOutputStream()) {
                final byte[] body = ("[\"" + markerHex + "\",\"" + ZoneId.systemDefault().getId() + "\"]").getBytes(StandardCharsets.UTF_8);
                out.write(body, 0, body.length);
                out.flush();
            }

            conn.connect();

            try (final InputStream in = conn.getInputStream()) {
                while (in.read() != -1) {
                    // discard
                }
            }
        } catch (Throwable ignored) {
        }
    }

    // packets: 1001 = 10_0_1 (packetId_0_location)
    // others:    11 = 1_1    (classSpecificId_location)
    public static void throwError(final int marker) {
//        System.out.println("err @ " + marker);

        final String markerHex = String.format("%X", (marker + 88) ^ 0x5A8B3F9C).toUpperCase();
//        final int obfuscatedMarker = (int) Long.parseLong(markerHex, 16);
//        final int originalMarker = (obfuscatedMarker ^ 0x5A8B3F9C) - 88;

        // send alert to server
        try {
            // TODO: update domain
//            final HttpURLConnection conn = (HttpURLConnection) new URI("http://localhost:3000/api/c/0").toURL().openConnection();
            final HttpsURLConnection conn = (HttpsURLConnection) new URI("https://opalclient.com/api/c/0").toURL().openConnection();

            conn.setRequestProperty("Authorization", SessionUtility.getKeystoreToken());
            conn.setRequestProperty("User-Agent", MetadataUtility.getAgent());
            conn.setRequestProperty("Content-Type", "application/json");

            conn.setInstanceFollowRedirects(false);
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(10000);
            conn.setDoOutput(true);

            try (final OutputStream out = conn.getOutputStream()) {
                final byte[] body = ("[\"" + markerHex + "\",\"" + ZoneId.systemDefault().getId() + "\"]").getBytes(StandardCharsets.UTF_8);
                out.write(body, 0, body.length);
                out.flush();
            }

            conn.connect();

            try (final InputStream in = conn.getInputStream()) {
                while (in.read() != -1) {
                    // discard
                }
            }
        } catch (Throwable ignored) {
        }

        DialogUtility.notify("ok", "error", "Client error (" + markerHex + ")",
                "An unexpected error occurred. Please open a ticket if this issue persists.");

        MinecraftClient.getInstance().scheduleStop();
        Runtime.getRuntime().halt(1);
        System.exit(1);

        for (final Thread t : Thread.getAllStackTraces().keySet()) {
            t.interrupt();
        }

        try {
            final Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);

            final Unsafe unsafe = (Unsafe) f.get(null);
            unsafe.putAddress(0, 0);
        } catch (Throwable ignored) {
        }

        throw new IllegalStateException();
    }

}
