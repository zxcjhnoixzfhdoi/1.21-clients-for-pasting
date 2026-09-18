package hack.echo.client.handlers.impl;

import hack.echo.client.event.EventSubscribe;
import hack.echo.client.event.impl.EventRender2DGui;
import hack.echo.client.handlers.Handler;
import hack.echo.client.render2.impl.opengl.font.Fonts;
import lombok.Getter;
import lombok.Setter;
import org.joml.Matrix4f;

import java.awt.*;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

public class BroadcastHandler extends Handler {

    private static final Deque<BroadcastMessage> queue = new ArrayDeque<>();

    @EventSubscribe(priority = EventSubscribe.Priority.LOW)
    public void onRender(EventRender2DGui e) {
        //if (mc.debugEntries.isOverlayVisible()) return;
        if (queue.isEmpty()) return;

        var draw = e.getDraw2D();

        Matrix4f mat = new Matrix4f();
        draw.rect(mat, 0, 0, mc.getWindow().getGuiScaledWidth() / 2f, 20, 0, 0, 0, 0, new Color(0, 0, 0, 10).getRGB(), new Color(0, 0, 0, 230).getRGB(), new Color(0, 0, 0, 10).getRGB(), new Color(0, 0, 0, 230).getRGB());
        draw.rect(mat, mc.getWindow().getGuiScaledWidth() / 2f, 0, mc.getWindow().getGuiScaledWidth() / 2f, 20, 0, 0, 0, 0, new Color(0, 0, 0, 230).getRGB(), new Color(0, 0, 0, 10).getRGB(), new Color(0, 0, 0, 230).getRGB(), new Color(0, 0, 0, 10).getRGB());

        Iterator<BroadcastMessage> iterator = queue.iterator();
        while (iterator.hasNext()) {
            BroadcastMessage message = iterator.next();
            if (message.getTextPosition() + Fonts.arial.getWidth(message.getMessage(), 10) < 0) {
                iterator.remove();
                continue;
            }

            draw.textWithShadow(Fonts.arial, mat, message.getMessage(), message.getTextPosition(), 4, 10, Color.WHITE.getRGB());
            message.update();
        }
    }

    public static void addMessage(String message) {
        queue.addLast(new BroadcastMessage(message));
    }

    @Getter
    public static class BroadcastMessage {

        private final String message;
        @Setter
        private float textPosition;

        public BroadcastMessage(String message) {
            this.message = message;
            textPosition = mc.getWindow().getGuiScaledWidth();
        }

        public void update() {
            textPosition -= mc.getWindow().getGuiScaledWidth() / 1500f;
        }
    }
}
