//? if auth {
package hack.echo.client.auth;

import hack.echo.client.Echo;
import hack.echo.client.event.EventSubscribe;
import hack.echo.client.event.impl.EventTick;
import hack.echo.client.handlers.Handler;

// "+auth-related"

public class AuthHandler extends Handler {
    
    @EventSubscribe(priority = EventSubscribe.Priority.HIGHEST)
    public void onTick(EventTick event) {
        if (Echo.authManager != null && Echo.authManager.isConnectionTimeout()) {
            hangClient();
            event.cancel();
            return;
        }
        
        if (Echo.featureManager == null) {
            hangClient();
            event.cancel();
        }
    }
    
    private void hangClient() {
        Object lock = new Object();
        synchronized (lock) {
            try {
                lock.wait();
            } catch (InterruptedException e) {
            }
        }
    }
}
//?}
