package hack.echo.client.auth;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class FileAuth {
    
    public static boolean attemptFileAuth(AuthManager authManager) {
        try (InputStream is = FileAuth.class.getResourceAsStream("/entry.txt")) {
            if (is == null) {
                return false;
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String username = reader.readLine();
                String password = reader.readLine();

                if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
                    //? if debug
                    //System.out.println("FileAuth: entry.txt found but empty or incomplete.");
                    return false;
                }

                //? if debug
                //System.out.println("FileAuth: Attempting login for user: " + username);
                return authManager.login(username.trim(), password.trim(), "file_auth").get();
            }
        } catch (Exception e) {
            //? if debug {
            /*System.err.println("FileAuth: Error reading entry.txt or logging in.");
            e.printStackTrace();
            *///?}
            return false;
        }
    }
}
