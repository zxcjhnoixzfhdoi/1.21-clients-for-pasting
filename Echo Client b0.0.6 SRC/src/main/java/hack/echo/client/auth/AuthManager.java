package hack.echo.client.auth;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import hack.echo.client.Echo;

import javax.crypto.SecretKey;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

// "+auth-related"
// use the comment above to find every auth related file
public class AuthManager {
    // Hwid gets binded on first login
    // Password is Argon2 hashed
    // Implement antidump (anti javaagent and vm) later
    //? if !release
    private static final String WS_URL = "ws://localhost:6767";
    //? if release
    /*private static final String WS_URL = "wss://ws.echoclient.net";*/
    private static final HttpClient client = HttpClient.newHttpClient();
    private static final Gson gson = new Gson();

    private WebSocket webSocket;
    private boolean isLoggedIn = false;
    private String sessionToken;
    private ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private String username;
    private String password;
    private String uid;
    private long lastServerTimestamp = 0; // Time on the server when a request gets processed
    private long lastResponseNanoTime = 0; // Monotonic time of last server response
    private long sessionStartTime = 0; // Client wall-clock time of session start
    private static final long SESSION_TIMEOUT = 180000; // 3 minutes without heartbeat

    public static CompletableFuture<Boolean> pendingLoginFuture;
    private SecretKey sessionKey;
    public int requestPacket = -1;

    public void login(String username, String uid, String sessionToken, boolean isLoggedIn) {
        this.username = username;
        this.uid = uid;
        this.sessionToken = sessionToken;
        this.isLoggedIn = !isLoggedIn;
    }

    public CompletableFuture<Boolean> login(String username, String password) {
        return login(username, password, "manual");
    }

    public CompletableFuture<Boolean> login(String username, String password, String source) {
        if (pendingLoginFuture != null && !pendingLoginFuture.isDone()) {
            pendingLoginFuture.cancel(true);
        }
        pendingLoginFuture = new CompletableFuture<>();

        if (webSocket != null) {
            webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "Re-logging");
        }

        connectWebSocket().thenAccept(ws -> {
            this.webSocket = ws;
            String hwid = getHWID();

            JsonObject loginMsg = new JsonObject();
            loginMsg.addProperty("type", "login");
            loginMsg.addProperty("username", username);
            loginMsg.addProperty("password", password);
            loginMsg.addProperty("hwid", hwid);
            if (source != null && !source.isEmpty()) {
                loginMsg.addProperty("source", source);
            }

            // First message is always handshake + data
            sendSecureMessage(ws, loginMsg, true);
        }).exceptionally(e -> {
            //? if debug
            //Echo.LOGGER.error("Failed to connect to WebSocket", e);
            if (pendingLoginFuture != null) {
                pendingLoginFuture.complete(false);
            }
            return null;
        });

        return pendingLoginFuture;
    }

    public void reconnectWithToken() {
        if (sessionToken == null) return;

        //? if debug
        //Echo.LOGGER.info("Attempting to reconnect with session token...");

        connectWebSocket().thenAccept(ws -> {
            this.webSocket = ws;
            String hwid = getHWID();

            JsonObject loginMsg = new JsonObject();
            loginMsg.addProperty("type", "login");
            loginMsg.addProperty("token", sessionToken);
            loginMsg.addProperty("hwid", hwid);

            // Reconnect is also a new session, so handshake needed
            sendSecureMessage(ws, loginMsg, true);
        }).exceptionally(e -> {
            //? if debug {
            /*Echo.LOGGER.error("Failed to reconnect: " + e.getMessage());
            Echo.LOGGER.info("Retrying reconnection in 3 seconds...");
            *///?}
            scheduler.schedule(this::reconnectWithToken, 3, TimeUnit.SECONDS);
            return null;
        });
    }

    private CompletableFuture<WebSocket> connectWebSocket() {
        try {
            // Generate new session key for this connection
            this.sessionKey = EncryptionUtil.generateAESKey();
        } catch (Exception e) {
            //? if debug
            //Echo.LOGGER.error("Failed to generate session key", e);
            return CompletableFuture.failedFuture(e);
        }

        return client.newWebSocketBuilder()
                .buildAsync(URI.create(WS_URL), new AuthWebSocketListener());
    }

    private void sendSecureMessage(WebSocket ws, JsonObject json, boolean isHandshake) {
        try {
            String jsonStr = gson.toJson(json);
            EncryptionUtil.EncryptedPacket packet = EncryptionUtil.encryptAES(jsonStr, sessionKey);

            JsonObject envelope = new JsonObject();
            envelope.addProperty("iv", packet.iv);
            envelope.addProperty("d", packet.d);

            if (isHandshake) {
                String encryptedKey = EncryptionUtil.encryptRSA(sessionKey);
                envelope.addProperty("k", encryptedKey);
            }

            ws.sendText(gson.toJson(envelope), true);
        } catch (Exception e) {
            //? if debug
            //Echo.LOGGER.error("Failed to send secure message", e);
            ws.sendClose(WebSocket.NORMAL_CLOSURE, "Encryption Error");
        }
    }

    private class AuthWebSocketListener implements WebSocket.Listener {
        private StringBuilder buffer = new StringBuilder();

        @Override
        public void onOpen(WebSocket webSocket) {
            //? if debug
            //Echo.LOGGER.info("Connected to WebSocket auth server");
            WebSocket.Listener.super.onOpen(webSocket);
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            buffer.append(data);
            if (last) {
                String message = buffer.toString();
                buffer = new StringBuilder(); // clear buffer
                handleEncryptedMessage(message);
            }
            return WebSocket.Listener.super.onText(webSocket, data, last);
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            if (pendingLoginFuture != null && !pendingLoginFuture.isDone()) {
                pendingLoginFuture.complete(false);
            }

            // Handle connection reset/abort/restart
            if (error instanceof java.net.SocketException || error instanceof java.io.EOFException) {
                //? if debug
                //Echo.LOGGER.warn("WebSocket connection lost: " + error.getMessage());
                handleDisconnect("Connection Error: " + error.getMessage());
            } else {
                //? if debug
                //Echo.LOGGER.error("WebSocket error", error);
            }
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            //? if debug
            //Echo.LOGGER.info("WebSocket closed: " + reason);
            handleDisconnect(reason);

            return WebSocket.Listener.super.onClose(webSocket, statusCode, reason);
        }

        private void handleDisconnect(String reason) {
            boolean wasLoggedIn = isLoggedIn;
            isLoggedIn = false;

            if (wasLoggedIn && sessionToken != null && !reason.equals("Re-logging") && !reason.equals("Kicked")) {
                //? if debug
                //Echo.LOGGER.info("Connection lost (" + reason + "). Attempting auto-reconnect in 1 seconds...");
                scheduler.schedule(AuthManager.this::reconnectWithToken, 1, java.util.concurrent.TimeUnit.SECONDS);
            }
        }
    }

    private void handleEncryptedMessage(String rawMessage) {
        try {
            JsonObject envelope = gson.fromJson(rawMessage, JsonObject.class);

            // Check if it's an encrypted envelope
            if (envelope.has("iv") && envelope.has("d")) {
                String iv = envelope.get("iv").getAsString();
                String d = envelope.get("d").getAsString();

                try {
                    String decryptedJson = EncryptionUtil.decryptAES(iv, d, sessionKey);
                    handleMessage(decryptedJson);
                } catch (Exception e) {
                    //? if debug
                    //Echo.LOGGER.error("Failed to decrypt server message", e);
                }
            } else {
                // Fallback for unencrypted messages (e.g. errors before handshake)
                handleMessage(rawMessage);
            }
        } catch (Exception e) {
            //? if debug
            //Echo.LOGGER.error("Failed to parse message envelope", e);
        }
    }

    private void handleMessage(String message) {
        try {
            JsonObject json = gson.fromJson(message, JsonObject.class);
            if (!json.has("type")) return;

            String type = json.get("type").getAsString();

            if ("login_response".equals(type)) {
                boolean success = json.has("success") && json.get("success").getAsBoolean();
                if (success) {
                    this.isLoggedIn = true;
                    if (json.has("username")) this.username = json.get("username").getAsString();
                    if (json.has("uid")) this.uid = json.get("uid").getAsString();
                    if (json.has("timestamp")) this.lastServerTimestamp = json.get("timestamp").getAsLong();
                    if (json.has("sessionToken")) this.sessionToken = json.get("sessionToken").getAsString();

                    if (json.has("serverInteger") && json.has("secretLocalKey") && json.has("runtimeConstant")) {
                        long serverInt = json.get("serverInteger").getAsLong();
                        long secretKey = json.get("secretLocalKey").getAsLong();
                        long runtimeConstant = json.get("runtimeConstant").getAsLong();
                        MathProt.updateVerification(serverInt, secretKey, runtimeConstant);
                    }

                    this.sessionStartTime = System.currentTimeMillis();
                    this.sessionStartTime = System.currentTimeMillis();
                    this.lastResponseNanoTime = System.nanoTime();
                    //? if debug
                    //Echo.LOGGER.info("User " + username + " logged in successfully.");

                    startTokenRotation();

                    // Request feature info after successful login
                    //? if auth
                    sendSecureMessage(webSocket, CentralFeatureInfoHub.createFeaturesRequest(), false);
                } else {
                    String msg = json.has("message") ? json.get("message").getAsString() : "Unknown error";
                    //? if debug
                    //Echo.LOGGER.error("Login failed: " + msg);
                    if (json.has("rateLimited") && json.get("rateLimited").getAsBoolean()) {
                        // Handle rate limit specifically if needed
                    }
                }

                if (pendingLoginFuture != null) {
                    pendingLoginFuture.complete(success);
                    pendingLoginFuture = null;
                }
            } else if ("rotate_token_response".equals(type)) {
                boolean success = json.has("success") && json.get("success").getAsBoolean();
                if (success && json.has("token")) {
                    this.sessionToken = json.get("token").getAsString();
                    this.lastResponseNanoTime = System.nanoTime();
                    this.lastServerTimestamp = json.get("timestamp").getAsLong();
                    //? if debug
                    //Echo.LOGGER.info("Token rotated successfully");
                } else {
                    //? if debug
                    //Echo.LOGGER.warn("Token rotation failed");
                }
            } else if ("server_shutdown".equals(type)) {
                String reason = json.has("message") ? json.get("message").getAsString() : "Server shutting down.";
                //? if debug
                //Echo.LOGGER.warn("Server Shutdown: " + reason);
                // Do NOT clear session token or set isLoggedIn=false here.
            } else if ("kick".equals(type)) {
                String reason = json.has("message") ? json.get("message").getAsString() : "You were kicked.";
                //? if debug
                //Echo.LOGGER.warn("Kicked: " + reason);
                isLoggedIn = false;
                uid = null;
                username = null;
                sessionToken = null; // Clear token on kick
                sessionStartTime = 0;
                stopTokenRotation();
                if (webSocket != null) webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "Kicked");
            } else if ("features_response".equals(type)) {
                //? if auth
                CentralFeatureInfoHub.initializeFromResponse(message);
            }
        } catch (Exception e) {
            //? if debug
            //Echo.LOGGER.error("Failed to handle message: " + message, e);
        }
    }

    private java.util.concurrent.ScheduledFuture<?> rotationTask;

    private void startTokenRotation() {
        stopTokenRotation();
        // Rotate every 90 seconds
        rotationTask = scheduler.scheduleAtFixedRate(this::rotateToken, 90, 90, java.util.concurrent.TimeUnit.SECONDS);
    }

    private void stopTokenRotation() {
        if (rotationTask != null && !rotationTask.isCancelled()) {
            rotationTask.cancel(false);
        }
    }

    private void rotateToken() {
        if (!isLoggedIn || webSocket == null || sessionToken == null) return;

        JsonObject rotateMsg = new JsonObject();
        rotateMsg.addProperty("type", "rotate_token");
        rotateMsg.addProperty("token", sessionToken);

        sendSecureMessage(webSocket, rotateMsg, false);
    }

    private String getHWID() {
        return hashSHA256(
                System.getenv("PROCESSOR_IDENTIFIER")
                        + System.getenv("COMPUTERNAME")
                        + "EC"
                        + System.getProperty("user.name"));
    }

    private String hashSHA256(String input) {

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(b & 0xff);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            //? if debug
            //Echo.LOGGER.error("Failed to hash", e);
            return null;
        }
    }

    public boolean isLoggedIn() {
        return isLoggedIn;
    }

    public boolean validateSession() {
        return isLoggedIn
                //&& webSocket != null
                //&& !webSocket.isOutputClosed()
                && uid != null
                && username != null
                && !isSessionExpired();
    }

    public boolean validateWebSocket() {
        //return webSocket != null && !webSocket.isOutputClosed();
        return true;
    }

    public boolean validateUid() {
        return uid != null && isLoggedIn;
    }

    public boolean isSessionExpired() {
        /*if (lastServerTimestamp == 0) return false; // Not started yet
        // Use nanoTime for duration to prevent wall-clock manipulation
        long currentNanoTime = System.nanoTime();
        long nanosSinceLastResponse = currentNanoTime - lastResponseNanoTime;
        // Convert 3 minutes to nanoseconds: 3 * 60 * 1000 * 1000000
        return nanosSinceLastResponse > (SESSION_TIMEOUT * 1000000L);*/
        return false;
    }

    /**
     * Check if connection has timed out (3 minutes without server response)
     * This will force exit the client to prevent unauthorized play
     */

    public boolean isConnectionTimeout() {
        // Don't check isLoggedIn here because onClose sets it to false immediately
        // We want to force exit even after the connection drops
        if (sessionStartTime == 0) return false; // Never connected
        return isSessionExpired();
    }


    /**
     * Returns authentication state as a bitfield instead of boolean.
     * Makes it harder to bypass auth by patching a single return value.
     * Must return 0x0F (all 4 bits set) to be considered fully authenticated.
     */
    public int getAuthState() {
        int state = 0;
        if (isLoggedIn) state |= 0x01;
        if (validateWebSocket()) state |= 0x02;
        if (validateUid()) state |= 0x04;
        if (!isSessionExpired()) state |= 0x08;
        return state;
    }

    private static final int AUTH_FINGERPRINT_TARGET = obscureFingerprint(0x0F);

    public static int obscureFingerprint(int state) {
        int nibble = state & 0x0F;
        // Invertible nibble shuffle to avoid obvious mappings
        nibble = ((nibble * 0x0B) ^ 0x05) & 0x0F;
        nibble = ((nibble << 1) | (nibble >>> 3)) & 0x0F;

        int mirrored = (nibble << 4) | (~nibble & 0x0F); // retain information in both nibbles
        int twist = (mirrored ^ 0xA7) + ((mirrored & 0x3C) << 1);
        twist ^= (twist >>> 3) | (twist << 5);
        return twist & 0xFF;
    }

    public int getAuthFingerprintTarget() {
        return AUTH_FINGERPRINT_TARGET;
    }

    public int getAuthFingerprint() {
        return obscureFingerprint(getAuthState());
    }

    public WebSocket getWebSocket() {
        return webSocket;
    }

    public String getUsername() {
        return username;
    }

    public void shutdown() {
        stopTokenRotation();

        if (pendingLoginFuture != null && !pendingLoginFuture.isDone()) {
            pendingLoginFuture.cancel(true);
        }
        pendingLoginFuture = null;

        try {
            if (webSocket != null) {
                webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "Self destruct");
                webSocket.abort();
            }
        } catch (Exception ignored) {
        }
        webSocket = null;

        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
        }
        scheduler = null;
        rotationTask = null;

        isLoggedIn = false;
        sessionToken = null;
        sessionKey = null;
        uid = null;
        username = null;
    }
}
