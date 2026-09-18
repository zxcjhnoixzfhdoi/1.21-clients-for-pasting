package we.devs.opium.client.elements;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Formatting;
import we.devs.opium.Opium;
import we.devs.opium.api.manager.element.Element;
import we.devs.opium.api.manager.element.RegisterElement;
import we.devs.opium.api.manager.miscellaneous.UUIDManager;
import we.devs.opium.api.utilities.RenderUtils;
import we.devs.opium.client.events.EventRender2D;
import we.devs.opium.client.modules.client.ModuleColor;
import we.devs.opium.client.values.impl.ValueBoolean;
import we.devs.opium.client.values.impl.ValueNumber;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.stream.Collectors;

@RegisterElement(name="PlayerList", tag="Player List", description="Shows the players that are near")
public class ElementPlayerList extends Element {
    ValueNumber maxPlayers = new ValueNumber("MaxPlayers", "Max Players", "Max players that can be shown in the player list.", 8, 3, 20);
    ValueBoolean opiumCheck = new ValueBoolean("Show Opium", "Show Opium", "Shows Opium members that are added in the UUID Manager", true);

    @Override
    public void onRender2D(EventRender2D event) {
        if(RenderUtils.getFontRenderer() == null) return;
        super.onRender2D(event);
        if (this.nullCheck()) {
            return;
        }

        ArrayList<PlayerEntity> players = new ArrayList<>();
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player || !player.isAlive() || mc.player.getHealth() <= 0.0f) continue;
            players.add(player);
        }

        int i = 0;
        for (PlayerEntity player : players.stream()
                .sorted(Comparator.comparing(p -> mc.player.distanceTo(p)))
                .toList()) {
            if (i + 1 > this.maxPlayers.getValue().intValue()) continue;
            if (opiumCheck.getValue() && UUIDManager.isAdded(player.getUuid())) {
                RenderUtils.drawString(
                        new MatrixStack(),
                        Formatting.WHITE.toString() + "[Opium] " + Formatting.RESET.toString() + this.getHealthColor(player).toString() +
                                (int) (player.getHealth() + player.getAbsorptionAmount()) + " " +
                                (Opium.FRIEND_MANAGER.isFriend(player.getName().getString()) ? Formatting.AQUA.toString() : Formatting.RESET.toString()) +
                                player.getName().getString() + " " +
                                this.getDistanceColor(player).toString() +
                                (int) mc.player.distanceTo(player),
                        (int) this.frame.getX(),
                        (int) (this.frame.getY() + (float) (10 * i)),
                        ModuleColor.getColor().getRGB()
                );
            } else {
                RenderUtils.drawString(
                        new MatrixStack(),
                        this.getHealthColor(player).toString() +
                                (int) (player.getHealth() + player.getAbsorptionAmount()) + " " +
                                (Opium.FRIEND_MANAGER.isFriend(player.getName().getString()) ? Formatting.AQUA.toString() : Formatting.RESET.toString()) +
                                player.getName().getString() + " " +
                                this.getDistanceColor(player).toString() +
                                (int) mc.player.distanceTo(player),
                        (int) this.frame.getX(),
                        (int) (this.frame.getY() + (float) (10 * i)),
                        ModuleColor.getColor().getRGB()
                );
            }

            ++i;
        }

        float longestName = 0.0f;
        for (PlayerEntity entityPlayer : players) {
            StringBuilder stringBuilder = new StringBuilder()
                    .append(this.getHealthColor(entityPlayer).toString())
                    .append((int) (entityPlayer.getHealth() + entityPlayer.getAbsorptionAmount()))
                    .append(" ");
            Formatting chatFormatting = Opium.FRIEND_MANAGER.isFriend(entityPlayer.getName().getString()) ? Formatting.AQUA : Formatting.RESET;
            String text = stringBuilder
                    .append(chatFormatting.toString())
                    .append(entityPlayer.getName().getString())
                    .append(" ")
                    .append(this.getDistanceColor(entityPlayer).toString())
                    .append((int) mc.player.distanceTo(entityPlayer))
                    .toString();
            if (mc.textRenderer.getWidth(text) > longestName) {
                longestName = mc.textRenderer.getWidth(text);
            }
        }
        this.frame.setWidth(longestName);
        this.frame.setHeight(10 * i);
    }

    public Formatting getHealthColor(LivingEntity player) {
        float totalHealth = player.getHealth() + player.getAbsorptionAmount();
        if (totalHealth <= 5.0f) {
            return Formatting.RED;
        } else if (totalHealth > 5.0f && totalHealth < 15.0f) {
            return Formatting.YELLOW;
        } else if (totalHealth >= 15.0f) {
            return Formatting.GREEN;
        } else {
            return Formatting.WHITE;
        }
    }

    public Formatting getDistanceColor(LivingEntity player) {
        float distance = mc.player.distanceTo(player);
        if (distance < 20.0f) {
            return Formatting.RED;
        } else if (distance >= 20.0f && distance < 50.0f) {
            return Formatting.YELLOW;
        } else if (distance >= 50.0f) {
            return Formatting.GREEN;
        } else {
            return Formatting.WHITE;
        }
    }
}