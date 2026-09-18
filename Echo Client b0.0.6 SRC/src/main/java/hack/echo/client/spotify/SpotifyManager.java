package hack.echo.client.spotify;

import lombok.Getter;

import java.util.StringJoiner;
import java.util.concurrent.atomic.AtomicBoolean;

public class SpotifyManager {

    private String refreshToken;
    private ApiKey accessToken;
    @Getter
    Song currentSong;

    private final SpotifyStuff auth = new SpotifyStuff();
    private final AtomicBoolean sentSongRequest = new AtomicBoolean(false);

    private void ensureKey() {
        if (refreshToken == null) {
            auth.getRefreshToken().thenAccept(token -> {
                if (token == null) return;
                this.refreshToken = token;
                auth.getAccessToken().thenAccept(key -> {
                    if (key != null) this.accessToken = key;
                });
            });
            return;
        }

        if (accessToken == null || accessToken.expiresAt <= System.currentTimeMillis()) {
            auth.getAccessToken().thenAccept(key -> {
                if (key != null) this.accessToken = key;
            });
        }
    }

    public void updateCurrentSong() {
        ensureKey();
        if (accessToken == null) return;

        SpotifyStuff.withFlags(sentSongRequest, null, () -> {
            var json = auth.get("https://api.spotify.com/v1/me/player/currently-playing", accessToken.key).join();
            if (json == null) return null;

            var item = json.getAsJsonObject("item");
            if (item == null) return null;

            String id = item.get("id").getAsString();
            long progressMs = json.get("progress_ms").getAsLong();
            boolean isPlaying = json.get("is_playing").getAsBoolean();

            if (currentSong == null || !currentSong.id.equals(id)) {
                var artistsArray = item.getAsJsonArray("artists");
                var artists = new StringJoiner(", ");
                for (var artist : artistsArray) {
                    artists.add(artist.getAsJsonObject().get("name").getAsString());
                }
                var images = item.getAsJsonObject("album").getAsJsonArray("images");
                String coverUrl = images.get(0).getAsJsonObject().get("url").getAsString();
                currentSong = new Song(
                        id,
                        item.get("name").getAsString(),
                        artists.toString(),
                        item.getAsJsonObject("album").get("name").getAsString(),
                        item.get("duration_ms").getAsLong(),
                        coverUrl
                );
            }

            currentSong.progressMs = progressMs;
            currentSong.isPlaying = isPlaying;
            currentSong.initTimeMs = System.currentTimeMillis();
            return null;
        });
    }
}
