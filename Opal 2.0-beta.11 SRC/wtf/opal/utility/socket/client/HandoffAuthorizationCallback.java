package wtf.opal.utility.socket.client;

import com.sun.net.httpserver.HttpServer;
import net.minecraft.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import wtf.opal.client.ReleaseInfo;
import wtf.opal.protection.annotation.NativeInclude;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;

@NativeInclude
public final class HandoffAuthorizationCallback implements Closeable {

    private HttpServer server;

    public @NotNull CompletableFuture<@Nullable String> start() {
        final CompletableFuture<String> future = new CompletableFuture<>();

        try {
            server = HttpServer.create(new InetSocketAddress(0), 0);
            server.createContext("/handoff/callback", exchange -> {
                if (!exchange.getRequestMethod().equals("GET")) {
                    return;
                }

                final String userAgent = exchange.getRequestHeaders().getFirst("user-agent");
                if (userAgent == null) {
                    return;
                }

                final byte[] responseBytes = "You may now close this window.".getBytes(StandardCharsets.UTF_8);

                exchange.sendResponseHeaders(200, responseBytes.length);
                try (final OutputStream outputStream = exchange.getResponseBody()) {
                    outputStream.write(responseBytes);
                }

                this.close();

                final String query = exchange.getRequestURI().getQuery();
                final String[] parts = query.split("token=");

                if (parts.length < 2) {
                    future.complete(null);
                    return;
                }

                future.complete(parts[1]);
            });

            server.start();

            final byte[] partA = {123, 34, 97, 34, 58, 34}; // {"a":"
            final byte[] partB = {34, 44, 34, 98, 34, 58}; // ","b":
            final byte[] partC = {125}; // }

            final byte[] channelBytes = ReleaseInfo.CHANNEL.toString().getBytes(StandardCharsets.UTF_8);
            final byte[] portBytes = String.valueOf(server.getAddress().getPort()).getBytes(StandardCharsets.UTF_8);

            final byte[] payload;
            try (final ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                outputStream.write(partA);
                outputStream.write(channelBytes);
                outputStream.write(partB);
                outputStream.write(portBytes);
                outputStream.write(partC);
                payload = outputStream.toByteArray();
            }

            String state = Base64.getUrlEncoder().encodeToString(payload);
            state = 'z' + state.substring(1, 5) + 'W' + state.substring(5);

            Util.getOperatingSystem().open("https://opalclient.com/handoff/authorize?client=wtf.opal.client&state=" + state);

//            Util.getOperatingSystem().open("https://dev.opalclient.com/handoff/authorize?client=wtf.opal.client&state=" + state);
//            System.out.println("https://dev.opalclient.com/handoff/authorize?client=wtf.opal.client&state=" + state);
        } catch (Exception e) {
            this.close();
            e.printStackTrace();
            future.completeExceptionally(e);
        }

        return future;
    }

    @Override
    public void close() {
        if (server != null) {
            server.stop(0);
        }
    }

}
