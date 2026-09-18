package hack.echo.client.spotify;

public class ApiKey {
    String key;
    long expiresAt;

    public ApiKey(String key, long duration) {
        this.expiresAt = System.currentTimeMillis() + duration;
        this.key = key;
    }
}