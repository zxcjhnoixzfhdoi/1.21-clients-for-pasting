package we.devs.opium.client.elements;

import net.minecraft.client.util.math.MatrixStack;
import we.devs.opium.Opium;
import we.devs.opium.api.manager.element.Element;
import we.devs.opium.api.manager.element.RegisterElement;
import we.devs.opium.api.utilities.RenderUtils;
import we.devs.opium.client.events.EventRender2D;
import we.devs.opium.client.modules.client.ModuleColor;
import we.devs.opium.client.modules.client.ModuleHUDEditor;
import we.devs.opium.client.values.impl.ValueString;
import net.minecraft.entity.player.PlayerEntity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.stream.Collectors;

// Register this element with a name and description
@RegisterElement(name = "Friends", description = "Gives you a list of friends in your chunk distance.")
public class ElementFriends extends Element {
    // Configuration value for naming the group of friends
    private final ValueString name;

    // Constructor to initialize the name value
    public ElementFriends() {
        this.name = new ValueString("Name", "Name", "The name for the group of friends.", "Opium Enjoyer");
    }

    // Called to render the element on the HUD
    @Override
    public void onRender2D(EventRender2D event) {
        super.onRender2D(event);
        if(RenderUtils.getFontRenderer() == null) return;
        // Filter players within render distance and check if they are friends
        assert mc.world != null;
        ArrayList<PlayerEntity> friends = mc.world.getPlayers().stream()
                .filter(p -> Opium.FRIEND_MANAGER.isFriend(p.getName().getString())) // Check if the player is a friend
                .sorted(Comparator.comparing(player -> player.getName().getString())) // Sort alphabetically by name
                .collect(Collectors.toCollection(ArrayList::new)); // Collect the results into a list

        // Adjust the frame's width and height based on the list of friends
        this.frame.setWidth(
                friends.isEmpty()
                        ? mc.textRenderer.getWidth(this.name.getValue()) // Default width for empty list
                        : mc.textRenderer.getWidth(friends.getFirst().getName()) // Width based on the longest name
        );
        this.frame.setHeight(
                mc.textRenderer.fontHeight +
                        (friends.isEmpty()
                                ? 0.0f // No extra height if the list is empty
                                : (1.0f + (mc.textRenderer.fontHeight + 1.0f) * (friends.size() + 1)) // Adjust height based on the number of friends
                        )
        );

        // Render the group name at the frame's position
        RenderUtils.drawString(
                new MatrixStack(),
                this.name.getValue(),
                (int) this.frame.getX(),
                (int) this.frame.getY(),
                ModuleColor.getColor().getRGB() // Primary color for text
        );

        // Render each friend's name below the group name
        int index = 10; // Vertical spacing between names
        for (PlayerEntity player : friends) {
            RenderUtils.drawString(
                    new MatrixStack(),
                    player.getName().getString(),
                    (int) this.frame.getX(),
                    (int) (this.frame.getY() + index),
                    ModuleHUDEditor.INSTANCE.getSecondColor().getColorIndex() // Secondary color for text
            );
            index += 10; // Move to the next line
        }
    }

    // Enum to define potential color options for this element
    public enum Colors {
        Normal,
        White,
        Gray
    }
}
