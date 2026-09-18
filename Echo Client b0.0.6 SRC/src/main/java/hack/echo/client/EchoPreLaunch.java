package hack.echo.client;

//? if auth {
import hack.echo.client.auth.AuthManager;
import hack.echo.client.auth.FileAuth;
import hack.echo.client.ui.LoginFrame;
//?}
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//? if auth {
import javax.swing.*;
import java.util.concurrent.CompletableFuture;
//?}

// Error seems to happen when pasting
public class EchoPreLaunch implements PreLaunchEntrypoint {

    private static final Logger LOGGER = LoggerFactory.getLogger("echo-prelaunch");

    @Override
    public void onPreLaunch() {
        //? if auth {
        try {
            if (Echo.authManager == null) {
                Echo.authManager = new AuthManager();
            }

            /*final AuthManager auth = Echo.authManager;

            //? if debug
            LOGGER.info("PreLaunch: Starting auth flow...");


            boolean fileAuthSuccessful = false;
            try {
                fileAuthSuccessful = FileAuth.attemptFileAuth(auth);
            } catch (Exception e) {
                //? if debug
                LOGGER.warn("PreLaunch: FileAuth attempt threw an exception", e);
            }

            if (fileAuthSuccessful) {
                //? if debug
                LOGGER.info("PreLaunch: FileAuth successful, continuing.");
                return;
            }

            //? if debug
            LOGGER.info("PreLaunch: FileAuth failed or not present. Prompting user for credentials.");

            final LoginFrame[] loginFrameRef = new LoginFrame[1];
            SwingUtilities.invokeAndWait(() -> loginFrameRef[0] = new LoginFrame(auth));

            final LoginFrame loginFrame = loginFrameRef[0];
            if (loginFrame != null) {
                SwingUtilities.invokeLater(() -> loginFrame.setVisible(true));
                final CompletableFuture<Boolean> future = loginFrame.waitForLogin();

                boolean success;
                try {
                    success = future.get();
                } catch (Exception e) {
                    //? if debug
                    LOGGER.warn("PreLaunch: Exception while waiting for login", e);
                    success = false;
                }

                if (success) {
                    //? if debug
                    LOGGER.info("PreLaunch: Login succeeded from UI");
                } else {
                    //? if debug
                    LOGGER.info("PreLaunch: Login failed or cancelled by user");
                    System.exit(0);
                }
            }*/
        } catch (Throwable t) {
            //? if debug
            //LOGGER.error("PreLaunch: Unexpected error while attempting login", t); 
        }
        //?}
    }
}
