package wtf.opal.utility.security;

import com.github.javakeyring.Keyring;
import net.minecraft.client.MinecraftClient;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.util.tinyfd.TinyFileDialogs;
import wtf.opal.client.ReleaseInfo;
import wtf.opal.protection.annotation.NativeInclude;
import wtf.opal.utility.misc.Callables;
import wtf.opal.utility.socket.client.HandoffAuthorizationCallback;

import java.util.concurrent.CompletableFuture;

@NativeInclude
public final class SessionUtility {

    private SessionUtility() {
    }

    @Nullable
    public static String getKeystoreToken() {
        try (final Keyring keyring = Keyring.create()) {
            return keyring.getPassword("wtf.opal.client.token", ReleaseInfo.CHANNEL.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private static CompletableFuture<Boolean> requestHandoffAuthorization() {
        final String channel = ReleaseInfo.CHANNEL.toString();
        final String service = "wtf.opal.client.token";

        try (final Keyring keyring = Keyring.create()) {
            keyring.deletePassword(service, channel);
        } catch (Exception ignored) {
        }

        final CompletableFuture<Boolean> future = new CompletableFuture<>();

        final boolean approved = TinyFileDialogs.tinyfd_messageBox(
                "Authentication required", "Please click OK to log in to your Opal account using your default browser.",
                "okcancel", "info", true);
        if (!approved) {
            future.complete(false);
            return future;
        }

        try (final HandoffAuthorizationCallback callback = new HandoffAuthorizationCallback()) {
            final CompletableFuture<String> callbackFuture = callback.start();

            final String token = callbackFuture.get();
            if (token == null) {
                future.complete(false);
                return future;
            }

            try (final Keyring keyring = Keyring.create()) {
                keyring.setPassword(service, channel, token);
                future.complete(true);
            } catch (Exception e) {
                future.complete(false);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Callables.throwError(4_1);
        }

        return future;
    }

    /**
     * @return true if authorization was successful
     */
    public static boolean requestHandoffAuthorizationOrStop() {
        final CompletableFuture<Boolean> future = requestHandoffAuthorization();

        boolean success;
        try {
            success = future.get();
        } catch (Exception ignored) {
            MinecraftClient.getInstance().stop();
            return false;
        }

        if (!success) {
            MinecraftClient.getInstance().stop();
            return false;
        }

        return true;
    }

}
