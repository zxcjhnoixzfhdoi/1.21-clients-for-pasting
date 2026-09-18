package we.devs.opium.api.manager.event;

import we.devs.opium.client.events.*;

public interface EventListener {
    // write listeners here
    default void onChatSend(EventChatSend event) {
    }

    default void onClient(EventClient event) {
    }

    default void onKey(EventKey event) {
    }

    default void onLogin(EventLogin event) {
    }

    default void onLogout(EventLogout event) {
    }

    default void onMotion(EventMotion event) {
    }

    default void onPacketSend(EventPacketSend event) {
    }

    default void onPacketReceive(EventPacketReceive event) {
    }

    default void onPush(EventPush event) {
    }

    default void onRender2D(EventRender2D event) {
    }

    default void onRender3D(EventRender3D event) {
    }

    default void onTick(EventTick event) {
    }

    default void onDeath(DeathEvent event) {
    }

    default void onDamageBlock(DamageBlockEvent event) {
    }

    default void onPlayerMove(PlayerMoveEvent event) {
    }
}
