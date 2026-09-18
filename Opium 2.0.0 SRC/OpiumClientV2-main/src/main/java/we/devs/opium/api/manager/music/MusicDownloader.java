package we.devs.opium.api.manager.music;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class MusicDownloader {

    private static final String GITHUB_BASE_URL = "https://raw.githubusercontent.com/VoidMatterRules/OpiumClientV2HWID/main/Music/";
    private static final String MUSIC_FOLDER = "opium/mod_music"; // Folder to store music files locally

    public static void downloadMusicFiles() {
        String[] musicFiles = {
                "destroylonely_intheair.ogg",
                "destroylonely_nostylist.ogg",
                "destroylonely_vvsvalentine.ogg",
                "homixidegang_guitars.ogg",
                "homixidegang_rckstarbitch.ogg",
                "kencarson_boss.ogg",
                "kencarson_freeyoungthug.ogg",
                "kencarson_hardcore.ogg",
                "kencarson_mdma.ogg",
                "kencarson_moneyandsex.ogg",
                "kencarson_overseas.ogg",
                "kencarson_ss.ogg",
                "kencarson_swagoverload.ogg",
                "playboicarti_24songs.ogg",
                "playboicarti_eviljordan.ogg",
                "playboicarti_lean4real.ogg",
                "playboicarti_longtime.ogg",
                "playboicarti_onthattime.ogg",
                "playboicarti_vampanthem.ogg",
                "linggangguliguliguli.ogg",
                "moskau.ogg"
        };

        // Create the music folder if it doesn't exist
        File folder = new File(MUSIC_FOLDER);
        if (!folder.exists()) {
            folder.mkdirs();
        }

        for (String fileName : musicFiles) {
            Path filePath = Paths.get(MUSIC_FOLDER, fileName);
            if (Files.exists(filePath)) {
                System.out.println("File already exists: " + fileName);
                continue; // Skip if the file already exists
            }

            try {
                URL url = new URL(GITHUB_BASE_URL + fileName);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    try (InputStream inputStream = connection.getInputStream();
                         FileOutputStream outputStream = new FileOutputStream(filePath.toFile())) {
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = inputStream.read(buffer)) != -1) {
                            outputStream.write(buffer, 0, bytesRead);
                        }
                    }
                    System.out.println("Downloaded: " + fileName);
                } else {
                    System.err.println("Failed to download: " + fileName + " (HTTP " + connection.getResponseCode() + ")");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}