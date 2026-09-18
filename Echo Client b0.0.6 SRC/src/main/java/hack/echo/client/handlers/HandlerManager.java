package hack.echo.client.handlers;

//? if auth {
import hack.echo.client.Echo;
import hack.echo.client.auth.AuthHandler;
import hack.echo.client.auth.AuthManager;
//?}
import hack.echo.client.event.EventManager;
import hack.echo.client.handlers.impl.*;

import java.util.ArrayList;

public class HandlerManager {
    private final ArrayList<Handler> handlers = new ArrayList<>();
    
    // "+auth-related"
    public void initialize() {
        //? if auth {
        if (!validateSession()) {
            return;
        }
        
        handlers.add(new AuthHandler());
        //?}
        
        handlers.add(new KeyHandler());
        handlers.add(new HurtTickHandler());
        handlers.add(new SprintController());
        handlers.add(new SwapStateManager());
        handlers.add(new CommandHandler());
        handlers.add(new BroadcastHandler());

        for (Handler handler : handlers) {
            EventManager.register(handler);
        }
    }

    public void shutdown() {
        for (Handler handler : handlers) {
            EventManager.unregister(handler);
        }
        SwapStateManager.clear();
        handlers.clear();
    }
    
    //? if auth {
    private boolean validateSession() {
        if (Echo.authManager == null) return false;
        return Echo.authManager.validateSession()
            && Echo.authManager.validateWebSocket()
            && authFingerprintMatches(Echo.authManager);
    }
    //?}

    //? if auth {
    private boolean authFingerprintMatches(AuthManager manager) {
        int delta = manager.getAuthFingerprint() ^ manager.getAuthFingerprintTarget();
        delta |= delta >> 4;
        delta |= delta >> 2;
        delta |= delta >> 1;
        return (delta & 0x01) == 0;
    }
    //?}
}
