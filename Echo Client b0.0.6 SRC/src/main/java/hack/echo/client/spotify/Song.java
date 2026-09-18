package hack.echo.client.spotify;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import hack.echo.client.render2.api.CrossTexture;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class Song {

    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    Lyric[] lyrics = new Lyric[0];
    public CrossTexture cover;
    final String id;
    public final String name;
    public final String author;
    final String album;
    public final long duration;
    public boolean isPlaying;
    public long progressMs;
    int lyricIndex = 0;
    public long initTimeMs;

    Song(String id, String name, String author, String album, long duration, String coverUrl) {
        this.id = id;
        this.name = name;
        this.author = author;
        this.album = album;
        this.duration = duration;
        this.initTimeMs = System.currentTimeMillis();
        CompletableFuture.runAsync(this::fetchLyrics);
        CompletableFuture.runAsync(() -> fetchCover(coverUrl));
    }

    private void fetchCover(String coverUrl) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(coverUrl))
                    .GET()
                    .build();
            byte[] bytes = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofByteArray()).body();
            this.cover = CrossTexture.from(bytes);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void fetchLyrics() {
        try {
            String url = "https://lrclib.net/api/get?" +
                    "track_name=" + URLEncoder.encode(name, StandardCharsets.UTF_8) +
                    "&artist_name=" + URLEncoder.encode(author, StandardCharsets.UTF_8) +
                    "&album_name=" + URLEncoder.encode(album, StandardCharsets.UTF_8) +
                    "&duration=" + (duration / 1000);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "Echo/1.0")
                    .GET()
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) return;

            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
            JsonElement syncedEl = json.get("syncedLyrics");
            if (syncedEl == null || syncedEl.isJsonNull()) return;

            String syncedLyrics = syncedEl.getAsString();
            if (syncedLyrics.isBlank()) return;

            List<Lyric> list = getLyrics(syncedLyrics);

            this.lyrics = list.toArray(new Lyric[0]);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static @NotNull List<Lyric> getLyrics(String syncedLyrics) {
        List<Lyric> list = new ArrayList<>();
        for (String line : syncedLyrics.split("\n")) {
            if (!line.startsWith("[")) continue;
            int close = line.indexOf(']');
            if (close < 0) continue;
            String[] colonParts = line.substring(1, close).split(":");
            String[] secParts = colonParts[1].split("\\.");
            long ms = Long.parseLong(colonParts[0]) * 60_000
                    + Long.parseLong(secParts[0]) * 1000
                    + Long.parseLong(secParts[1]) * 10;
            int textStart = close + 1;
            if (textStart < line.length() && line.charAt(textStart) == ' ') textStart++;
            String text = textStart < line.length() ? line.substring(textStart) : "";
            list.add(new Lyric(text, ms));
        }
        return list;
    }

    public String getCurrentLyric(long offset) {
        if (lyrics.length == 0) return "";
        if (lyricIndex + 1 < lyrics.length && offset >= lyrics[lyricIndex + 1].atMs()) {
            lyricIndex++;
        } else if (lyricIndex > 0 && offset < lyrics[lyricIndex].atMs()) {
            lyricIndex--;
        }
        return lyrics[lyricIndex].lyric();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (!(obj instanceof Song song)) return false;
        return this.id.equals(song.id);
    }
}
