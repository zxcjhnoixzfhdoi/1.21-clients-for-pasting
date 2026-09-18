package we.devs.opium.client.modules.visuals;

import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import me.x150.renderer.render.Renderer2d;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4d;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;
import we.devs.opium.Opium;
import we.devs.opium.api.manager.module.Module;
import we.devs.opium.api.manager.module.RegisterModule;
import we.devs.opium.api.utilities.RenderUtils;
import we.devs.opium.client.events.EventRender2D;
import we.devs.opium.client.modules.combat.ModulePopCounter;
import we.devs.opium.client.values.impl.ValueBoolean;
import we.devs.opium.client.values.impl.ValueColor;
import we.devs.opium.client.values.impl.ValueEnum;
import we.devs.opium.client.values.impl.ValueNumber;

import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static me.x150.renderer.util.RendererUtils.*;
import static we.devs.opium.client.modules.visuals.ModuleNametags.Armor.Durability;
import static we.devs.opium.client.modules.visuals.ModuleNametags.Armor.OnlyArmor;

@RegisterModule(name = "Nametags", description = "Nametags", tag = "Nametags", category = Module.Category.VISUALS)
public class ModuleNametags extends Module {
    public static ModuleNametags INSTANCE;
    private final ValueBoolean self = new ValueBoolean("Self","Self","self asf", false);
    private final ValueNumber scale = new ValueNumber("Scale","Scale","Scale", 0.68f, 0.1f, 2f);
    private final ValueNumber minScale = new ValueNumber("MinScale","MinScale","MinScale", 1f, 0.1f, 1f);
    private final ValueNumber scaled = new ValueNumber("Scaled","Scaled","Scaled", 1, 0, 2);
    private final ValueNumber offset = new ValueNumber("Offset","Offset","Offset", 0.315f, 0.001f, 1f);
    private final ValueNumber height = new ValueNumber("Height","Height","Height", 0, -3, 3);
    private final ValueBoolean god = new ValueBoolean("God","God","God", true);
    private final ValueBoolean gamemode = new ValueBoolean("Gamemode","Gamemode","Gamemode", false);
    private final ValueBoolean ping = new ValueBoolean("Ping","Ping","Ping", false);
    private final ValueBoolean health = new ValueBoolean("Health","Health","Health", true);
    private final ValueBoolean distance = new ValueBoolean("Distance","Distance","Distance", true);
    private final ValueBoolean pops = new ValueBoolean("TotemPops","TotemPops","TotemPops", true);
    private final ValueBoolean enchants = new ValueBoolean("Enchants","Enchants","Enchants", true);
    private final ValueColor friendColor = new ValueColor("FriendColor","FriendColor","FriendColor", new Color(0xFF1DFF1D, true));
    private final ValueColor color = new ValueColor("Color","Color","Color", new Color(0xFFFFFFFF, true));

//    public final ValueEnum font = new ValueEnum("FontMode","FontMode","FontMode", Font.Fast);
    private final ValueNumber armorHeight = new ValueNumber("ArmorHeight","ArmorHeight","ArmorHeight", 0.3f, -10, 10f);
    private final ValueNumber armorScale = new ValueNumber("ArmorScale","ArmorScale","ArmorScale", 0.9f, 0.1f, 2f);
    private final ValueEnum armorMode = new ValueEnum("ArmorMode","ArmorMode","ArmorMode", Armor.Full);

    public ModuleNametags() {
        INSTANCE = this;
    }

    @Override
    public void onRender2D(EventRender2D context) {
        float tickDelta = mc.getRenderTickCounter().getTickDelta(true);
        for (PlayerEntity ent : mc.world.getPlayers()) {
            if (ent == mc.player && (mc.options.getPerspective().isFirstPerson() || !self.getValue())) continue;
            double x = ent.prevX + (ent.getX() - ent.prevX) * mc.getRenderTickCounter().getTickDelta(true);
            double y = ent.prevY + (ent.getY() - ent.prevY) * mc.getRenderTickCounter().getTickDelta(true);
            double z = ent.prevZ + (ent.getZ() - ent.prevZ) * mc.getRenderTickCounter().getTickDelta(true);
            Vec3d vector = new Vec3d(x, y + height.getValue().doubleValue() + ent.getBoundingBox().getLengthY() + 0.3, z);
            Vec3d preVec = vector;
            vector = worldSpaceToScreenSpace(new Vec3d(vector.x, vector.y, vector.z));
            if (vector.z > 0 && vector.z < 1) {
                Vector4d position = new Vector4d(vector.x, vector.y, vector.z, 0);
                position.x = Math.min(vector.x, position.x);
                position.y = Math.min(vector.y, position.y);
                position.z = Math.max(vector.x, position.z);

                String final_string = "";
                if (god.getValue() && ent.hasStatusEffect(StatusEffects.SLOWNESS)) {
                    final_string += "§4GOD ";
                }
                if (ping.getValue()) {
                    final_string += getEntityPing(ent) + "ms ";
                }
                if (gamemode.getValue()) {
                    final_string += translateGamemode(getEntityGamemode(ent)) + " ";
                }
                final_string += Formatting.RESET + ent.getName().getString();
                if (health.getValue()) {
                    final_string += " " + getHealthColor(ent) + round2(ent.getAbsorptionAmount() + ent.getHealth());
                }
                if (distance.getValue()) {
                    final_string += " " + Formatting.RESET + String.format("%.1f", mc.player.distanceTo(ent)) + "m";
                }
                if (pops.getValue() && ModulePopCounter.popCount.get(ent.getName().getString()) != null && ModulePopCounter.popCount.get(ent.getName().getString()) != 0) {
                    final_string += " §bPop" + " " + Formatting.LIGHT_PURPLE + ModulePopCounter.popCount.get(ent.getName().getString());
                }

                double posX = position.x;
                double posY = position.y;
                double endPosX = position.z;

                float diff = (float) (endPosX - posX) / 2;
                float textWidth;

//                if (font.getValue() == Font.Fancy) {
//                    textWidth = (RenderUtils.getFontRenderer().getStringWidth(final_string) * 1);
//                } else {
                    textWidth = mc.textRenderer.getWidth(final_string);
//                }

                float tagX = (float) ((posX + diff - textWidth / 2) * 1);

                ArrayList<ItemStack> stacks = new ArrayList<>();

                stacks.add(ent.getMainHandStack());
                stacks.add(ent.getInventory().armor.get(3));
                stacks.add(ent.getInventory().armor.get(2));
                stacks.add(ent.getInventory().armor.get(1));
                stacks.add(ent.getInventory().armor.get(0));
                stacks.add(ent.getOffHandStack());

                context.getContext().getMatrices().push();
                context.getContext().getMatrices().translate(tagX - 2 + (textWidth + 4) / 2f, (float) (posY - 13f) + 6.5f, 0);
                float size = (float) Math.max(1 - MathHelper.sqrt((float) mc.cameraEntity.squaredDistanceTo(preVec)) * 0.01 * scaled.getValue().doubleValue(), 0);
                context.getContext().getMatrices().scale(Math.max(scale.getValue().floatValue() * size, minScale.getValue().floatValue()), Math.max(scale.getValue().floatValue() * size, minScale.getValue().floatValue()), 1f);
                context.getContext().getMatrices().translate(0, offset.getValue().floatValue() * MathHelper.sqrt((float) mc.player.getEyePos().squaredDistanceTo(preVec)), 0);
                context.getContext().getMatrices().translate(-(tagX - 2 + (textWidth + 4) / 2f), -(float) ((posY - 13f) + 6.5f), 0);

                float item_offset = 0;
                if (armorMode.getValue() != Armor.None) {
                    int count = 0;
                    for (ItemStack armorComponent : stacks) {
                        count++;
                        if (!armorComponent.isEmpty()) {
                            context.getContext().getMatrices().push();
                            context.getContext().getMatrices().translate(tagX - 2 + (textWidth + 4) / 2f, (float) (posY - 13f) + 6.5f, 0);
                            context.getContext().getMatrices().scale(armorScale.getValue().floatValue(), armorScale.getValue().floatValue(), 1f);
                            context.getContext().getMatrices().translate(-(tagX - 2 + (textWidth + 4) / 2f), -(float) ((posY - 13f) + 6.5f), 0);
                            context.getContext().getMatrices().translate(posX - 52.5 + item_offset, (float) (posY - 29f) + armorHeight.getValue().floatValue(), 0);
                            float durability = armorComponent.getMaxDamage() - armorComponent.getDamage();
                            int percent = (int) ((durability / (float) armorComponent.getMaxDamage()) * 100F);
                            Color color;
                            if (percent <= 33) {
                                color = Color.RED;
                            } else if (percent <= 66) {
                                color = Color.ORANGE;
                            } else {
                                color = Color.GREEN;
                            }
                            switch (armorMode.getValue()) {
                                case OnlyArmor -> {
                                    if (count > 1 && count < 6) {
                                        DiffuseLighting.disableGuiDepthLighting();
                                        context.getContext().drawItem(armorComponent, 0, 0);
                                        context.getContext().drawItemInSlot(mc.textRenderer, armorComponent, 0, 0);
                                    }
                                }
                                case Armor.Item -> {
                                    DiffuseLighting.disableGuiDepthLighting();
                                    context.getContext().drawItem(armorComponent, 0, 0);
                                    context.getContext().drawItemInSlot(mc.textRenderer, armorComponent, 0, 0);
                                }
                                case Armor.Full -> {
                                    DiffuseLighting.disableGuiDepthLighting();
                                    context.getContext().drawItem(armorComponent, 0, 0);
                                    context.getContext().drawItemInSlot(mc.textRenderer, armorComponent, 0, 0);
                                    if (armorComponent.getMaxDamage() > 0) {
                                        context.getContext().drawText(mc.textRenderer, String.valueOf(percent), 9 - mc.textRenderer.getWidth(String.valueOf(percent)) / 2, -mc.textRenderer.fontHeight + 1, color.getRGB(), true);
                                    }
                                }
                                case Durability -> {
                                    context.getContext().drawItemInSlot(mc.textRenderer, armorComponent, 0, 0);
                                    if (armorComponent.getMaxDamage() > 0) {
                                        if (!armorComponent.isItemBarVisible()) {
                                            int i = armorComponent.getItemBarStep();
                                            int j = armorComponent.getItemBarColor();
                                            int k = 2;
                                            int l = 13;
                                            context.getContext().fill(RenderLayer.getGuiOverlay(), k, l, k + 13, l + 2, -16777216);
                                            context.getContext().fill(RenderLayer.getGuiOverlay(), k, l, k + i, l + 1, j | -16777216);
                                        }
                                        context.getContext().drawText(mc.textRenderer, String.valueOf(percent), 9 - mc.textRenderer.getWidth(String.valueOf(percent)) / 2, 5, color.getRGB(), true);
                                    }
                                }
                                default -> throw new IllegalStateException("Unexpected value: " + armorMode.getValue());
                            }
                            context.getContext().getMatrices().pop();

                            if (this.enchants.getValue()) {
                                AtomicReference<Float> enchantmentY = new AtomicReference<>((float) 0);
                                Object2IntMap<RegistryEntry<Enchantment>> enchantments = new Object2IntArrayMap<>();
                                getEnchantments(armorComponent, enchantments);
                                float finalItem_offset = item_offset;
                                enchantments.forEach((enchantmentRegistryEntry, i) -> {
                                    String id = enchantmentRegistryEntry.getIdAsString();
                                    int level = i;
                                    String encName;
                                    switch (id) {
                                        case "minecraft:blast_protection" -> encName = "B" + level;
                                        case "minecraft:protection" -> encName = "P" + level;
                                        case "minecraft:thorns" -> encName = "T" + level;
                                        case "minecraft:sharpness" -> encName = "S" + level;
                                        case "minecraft:efficiency" -> encName = "E" + level;
                                        case "minecraft:unbreaking" -> encName = "U" + level;
                                        case "minecraft:power" -> encName = "PO" + level;
                                        default -> {
                                            return;
                                        }
                                    }

                                    context.getContext().getMatrices().push();
                                    context.getContext().getMatrices().translate((posX - 50f + finalItem_offset), (posY - 45f + enchantmentY.get()), 0);
                                    context.getContext().drawText(mc.textRenderer, encName, 0, 0, -1, true);
                                    context.getContext().getMatrices().pop();
                                    enchantmentY.updateAndGet(v -> v - 8);
                                });
                            }
                        }
                        item_offset += 18f;
                    }
                    // broken
//                    Renderer2d.renderQuad(context.getContext().getMatrices(), rect.getValue(), tagX - 2, (float) (posY - 14f), textWidth + 4, 13);
//                    Renderer2d.renderQuad(context.getContext().getMatrices(), outline.getValue(),tagX - 3, (float) (posY - 14f), textWidth + 6, 1);
//                    Renderer2d.renderQuad(context.getContext().getMatrices(), outline.getValue(), tagX - 3, (float) (posY - 2f), textWidth + 6, 1);
//                    Renderer2d.renderQuad(context.getContext().getMatrices(), outline.getValue(), tagX - 3, (float) (posY - 14f), 1, 12);
//                    Renderer2d.renderQuad(context.getContext().getMatrices(), outline.getValue(), tagX + textWidth + 2, (float) (posY - 14f), 1, 12);
                }
//                if (font.getValue() == Font.Fancy) {
//                    Color c = Opium.FRIEND_MANAGER.isFriend(ent.getGameProfile().getName()) ? friendColor.getValue() : this.color.getValue();
//                    RenderUtils.getFontRenderer().drawString(context.getContext().getMatrices(), final_string, tagX, (float) posY - 10, c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha());
//                } else {
                    context.getContext().getMatrices().push();
                    context.getContext().getMatrices().translate(tagX, ((float) posY - 11), 0);
                    context.getContext().drawText(mc.textRenderer, final_string, 0, 0, Opium.FRIEND_MANAGER.isFriend(ent.getGameProfile().getName()) ? friendColor.getValue().getRGB() : this.color.getValue().getRGB(), true);
                    context.getContext().getMatrices().pop();
//                }
                context.getContext().getMatrices().pop();
            }
        }
    }

    static void getEnchantments(ItemStack itemStack, Object2IntMap<RegistryEntry<Enchantment>> enchantments) {
        enchantments.clear();
        if (!itemStack.isEmpty()) {
            Set<Object2IntMap.Entry<RegistryEntry<Enchantment>>> itemEnchantments = itemStack.getItem() == Items.ENCHANTED_BOOK
                    ? Objects.requireNonNull(itemStack.get(DataComponentTypes.STORED_ENCHANTMENTS)).getEnchantmentEntries()
                    : itemStack.getEnchantments().getEnchantmentEntries();

            for (Object2IntMap.Entry<RegistryEntry<Enchantment>> entry : itemEnchantments) {
                enchantments.put(entry.getKey(), entry.getIntValue());
            }
        }
    }

    public static String getEntityPing(PlayerEntity entity) {
        if (mc.getNetworkHandler() == null) return "-1";
        PlayerListEntry playerListEntry = mc.getNetworkHandler().getPlayerListEntry(entity.getUuid());
        if (playerListEntry == null) return "-1";
        int ping = playerListEntry.getLatency();
        Formatting color = Formatting.GREEN;
        if (ping >= 100) {
            color = Formatting.YELLOW;
        }
        if (ping >= 250) {
            color = Formatting.RED;
        }
        return color.toString() + ping;
    }

    public static GameMode getEntityGamemode(PlayerEntity entity) {
        if (entity == null) return null;
        PlayerListEntry playerListEntry = mc.getNetworkHandler().getPlayerListEntry(entity.getUuid());
        return playerListEntry == null ? null : playerListEntry.getGameMode();
    }

    private String translateGamemode(GameMode gamemode) {
        if (gamemode == null) return "§7[BOT]";
        return switch (gamemode) {
            case SURVIVAL -> "§b[S]";
            case CREATIVE -> "§c[C]";
            case SPECTATOR -> "§7[SP]";
            case ADVENTURE -> "§e[A]";
        };
    }

    private Formatting getHealthColor(@NotNull PlayerEntity entity) {
        int health = (int) ((int) entity.getHealth() + entity.getAbsorptionAmount());
        if (health >= 18) {
            return Formatting.GREEN;
        }
        if (health >= 12) {
            return Formatting.YELLOW;
        }
        if (health >= 6) {
            return Formatting.RED;
        }
        return Formatting.DARK_RED;
    }

    public static float round2(double value) {
        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(1, RoundingMode.HALF_UP);
        return bd.floatValue();
    }

    public static Vec3d worldSpaceToScreenSpace(Vec3d pos) {
        Camera camera = mc.getEntityRenderDispatcher().camera;
        int displayHeight = mc.getWindow().getHeight();
        int[] viewport = new int[4];
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
        Vector3f target = new Vector3f();

        double deltaX = pos.x - camera.getPos().x;
        double deltaY = pos.y - camera.getPos().y;
        double deltaZ = pos.z - camera.getPos().z;

        Vector4f transformedCoordinates = new Vector4f((float) deltaX, (float) deltaY, (float) deltaZ, 1.f).mul(lastWorldSpaceMatrix);
        Matrix4f matrixProj = new Matrix4f(lastProjMat);
        Matrix4f matrixModel = new Matrix4f(lastModMat);
        matrixProj.mul(matrixModel).project(transformedCoordinates.x(), transformedCoordinates.y(), transformedCoordinates.z(), viewport, target);
        return new Vec3d(target.x / mc.getWindow().getScaleFactor(), (displayHeight - target.y) / mc.getWindow().getScaleFactor(), target.z);
    }
    
    public enum Font {
        Fancy, Fast
    }

    public enum Armor {
        None, Full, Durability, Item, OnlyArmor
    }
    
}
