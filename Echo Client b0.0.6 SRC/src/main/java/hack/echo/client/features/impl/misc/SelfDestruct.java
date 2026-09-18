package hack.echo.client.features.impl.misc;

import hack.echo.client.Echo;
import hack.echo.client.event.EventManager;
import hack.echo.client.features.Category;
import hack.echo.client.features.Feature;
import hack.echo.client.features.FeatureInfo;
import hack.echo.client.features.settings.impl.BoolSetting;
import hack.echo.client.features.settings.impl.StringSetting;
import hack.echo.client.render.Shaders;
import hack.echo.client.render2.impl.opengl.font.Fonts;
import hack.echo.client.utils.audio.EchoAudio;
import hack.echo.client.utils.strings.Concat;
import net.fabricmc.loader.api.FabricLoader;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;

// Replaces the mod JAR on disk with a decoy, then shuts down Echo
public class SelfDestruct extends Feature {

    private BoolSetting replaceMod = new BoolSetting(Concat.of("Replace Mods"), true);
    private StringSetting modUrlLink = new StringSetting(Concat.of("URL"), "https://cdn.modrinth.com/data/lIQY64qr/versions/vMOQpnf6/tiptapshow-1.6.7%2B1.21.11.jar", obj -> replaceMod.getValue());

    public SelfDestruct() {
        super(new FeatureInfo(
            Concat.of("Self Destruct"),
            Concat.of("Self destructs the client to hide any traces of itself."),
            Category.INTERNALS
        ));
        modUrlLink.setMaxLength(1024);
    }

    @Override
    public void onEnable() {
        Echo.isDestroyed = true;

        if (replaceMod.getValue()) {
            replaceModFile();
        }

        if (Echo.screenManager != null) {
            Echo.screenManager.closeAllScreens();
            Echo.screenManager.destroy();
        }
        if (Echo.handlerManager != null) Echo.handlerManager.shutdown();
        if (Echo.featureManager != null) Echo.featureManager.shutdown(this);

        Fonts.destroy();
        EchoAudio.destroy();
        Shaders.unload();
        EventManager.reset();

        Echo.screenManager = null;
        Echo.handlerManager = null;
        Echo.featureManager = null;

        setEnabled(false);
        System.gc();
    }

    private void replaceModFile() {
        Path modPath = resolveModPath();
        if (modPath == null) return;

        String url = modUrlLink.getValue();
        if (url == null || url.isBlank()) url = modUrlLink.getPlaceholder();
        if (url == null || url.isBlank()) return;

        try {
            BasicFileAttributes attrs = Files.readAttributes(modPath, BasicFileAttributes.class);

            HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
            conn.setRequestMethod("GET");
            conn.setInstanceFollowRedirects(true);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(30000);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");

            if (conn.getResponseCode() != 200) {
                conn.disconnect();
                return;
            }

            try (InputStream in = new BufferedInputStream(conn.getInputStream());
                 FileOutputStream out = new FileOutputStream(modPath.toFile())) {
                in.transferTo(out);
            } finally {
                conn.disconnect();
            }

            Files.getFileAttributeView(modPath, BasicFileAttributeView.class)
                .setTimes(attrs.lastModifiedTime(), attrs.lastAccessTime(), attrs.creationTime());
        } catch (IOException e) {
            //? if debug
            //Echo.LOGGER.error("[SelfDestruct] Failed to replace mod file", e);
        }
    }

    private Path resolveModPath() {
        // Standard deployment: Echo loaded as its own JAR
        try {
            var cs = Echo.class.getProtectionDomain().getCodeSource();
            if (cs != null && cs.getLocation() != null && "file".equalsIgnoreCase(cs.getLocation().getProtocol())) {
                Path p = Path.of(cs.getLocation().toURI());
                if (Files.isRegularFile(p)) return p;
            }
        } catch (Exception ignored) {}

        // EchoLoader deployment: find the loader JAR via Fabric's mod registry
        return FabricLoader.getInstance().getModContainer("tiptapshow")
            .map(mc -> mc.getOrigin().getPaths())
            .filter(paths -> !paths.isEmpty())
            .map(paths -> paths.get(0))
            .filter(Files::isRegularFile)
            .orElse(null);
    }
}
