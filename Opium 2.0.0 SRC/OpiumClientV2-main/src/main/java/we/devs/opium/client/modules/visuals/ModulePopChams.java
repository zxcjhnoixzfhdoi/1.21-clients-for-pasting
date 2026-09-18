package we.devs.opium.client.modules.visuals;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.block.Blocks;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Vector3f;
import we.devs.opium.api.manager.module.Module;
import we.devs.opium.api.manager.module.RegisterModule;
import we.devs.opium.api.utilities.AnimateUtil;
import we.devs.opium.asm.ducks.ILimbAnimator;
import we.devs.opium.client.events.EventPacketReceive;
import we.devs.opium.client.events.EventRender3D;
import we.devs.opium.client.values.impl.ValueColor;
import we.devs.opium.client.values.impl.ValueNumber;

import java.awt.*;
import java.util.concurrent.CopyOnWriteArrayList;

@RegisterModule(name = "Pop Chams", description = "Pop Chams", tag = "Pop Chams", category = Module.Category.VISUALS)
public final class ModulePopChams extends Module {
    private final ValueColor color = new ValueColor("Color", "Color", "Color", new Color(255, 255, 255));
    private final ValueNumber alphaSpeed = new ValueNumber("AlphaSpeed", "AlphaSpeed", "AlphaSpeed", 0.2, 0, 1);

    private final CopyOnWriteArrayList<Person> popList = new CopyOnWriteArrayList<>();
    public static ModulePopChams INSTANCE;

    public ModulePopChams() {
        INSTANCE = this;
    }

    @Override
    public void onUpdate() {
        popList.forEach(person -> person.update(popList));
    }

    @Override
    public void onRender3D(EventRender3D e) {
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(770, 771, 0, 1);

        popList.forEach(person -> {
            person.modelPlayer.leftPants.visible = false;
            person.modelPlayer.rightPants.visible = false;
            person.modelPlayer.leftSleeve.visible = false;
            person.modelPlayer.rightSleeve.visible = false;
            person.modelPlayer.jacket.visible = false;
            person.modelPlayer.hat.visible = false;
            renderEntity(e.getMatrices(), person.player, person.modelPlayer, person.getAlpha());
        });

        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
    }

    @Override
    public void onPacketReceive(EventPacketReceive event) {
        EntityStatusS2CPacket packet;
        if (nullCheck()) {
            return;
        }
        if (event.getPacket() instanceof EntityStatusS2CPacket && (packet = (EntityStatusS2CPacket) event.getPacket()).getStatus() == 35) {
            Entity e = packet.getEntity(mc.world);
            if (!(e instanceof PlayerEntity player) || player == mc.player) {
                return;
            }
            
            PlayerEntity entity = new PlayerEntity(mc.world, BlockPos.ORIGIN, player.bodyYaw, new GameProfile(player.getUuid(), player.getName().getString())) {
                @Override
                public boolean isSpectator() {
                    return false;
                }

                @Override
                public boolean isCreative() {
                    return false;
                }
            };

            entity.copyPositionAndRotation(player);
            entity.bodyYaw = player.bodyYaw;
            entity.headYaw = player.headYaw;
            entity.handSwingProgress = player.handSwingProgress;
            entity.handSwingTicks = player.handSwingTicks;
            entity.setSneaking(player.isSneaking());
            entity.limbAnimator.setSpeed(player.limbAnimator.getSpeed());
            ((ILimbAnimator) entity.limbAnimator).cheetOpium$setPos(player.limbAnimator.getPos());
            popList.add(new Person(entity));
        }
    }

    private void renderEntity(MatrixStack matrices, LivingEntity entity, BipedEntityModel<PlayerEntity> modelBase, int alpha) {
        if(!(entity instanceof PlayerEntity)) return;
        double x = entity.getX() - mc.getEntityRenderDispatcher().camera.getPos().getX();
        double y = entity.getY() - mc.getEntityRenderDispatcher().camera.getPos().getY();
        double z = entity.getZ() - mc.getEntityRenderDispatcher().camera.getPos().getZ();

        matrices.push();
        matrices.translate((float) x, (float) y, (float) z);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotation(rad(180 - entity.bodyYaw)));
        prepareScale(matrices);

        modelBase.animateModel((PlayerEntity) entity, entity.limbAnimator.getPos(), entity.limbAnimator.getSpeed(), mc.getRenderTickCounter().getTickDelta(true));
        modelBase.setAngles((PlayerEntity) entity, entity.limbAnimator.getPos(), entity.limbAnimator.getSpeed(), entity.age, entity.headYaw - entity.bodyYaw, entity.getPitch());

        RenderSystem.enableBlend();
        RenderSystem.disableDepthTest();
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        RenderSystem.blendFuncSeparate(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ZERO);
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        modelBase.render(matrices, buffer, 10, 0, color.getValue().getRGB());
        BufferRenderer.drawWithGlobalProgram(buffer.end());
        RenderSystem.disableBlend();
        RenderSystem.enableDepthTest();
        matrices.pop();
    }

    static float rad(float angle) {
        return (float) (angle * Math.PI / 180);
    }

    private static void prepareScale(MatrixStack matrixStack) {
        matrixStack.scale(-1.0F, -1.0F, 1.0F);
        matrixStack.scale(1.6f, 1.8f, 1.6f);
        matrixStack.translate(0.0F, -1.501F, 0.0F);
    }

    private class Person {
        private final PlayerEntity player;
        private final PlayerEntityModel<PlayerEntity> modelPlayer;
        private int alpha;

        public Person(PlayerEntity player) {
            this.player = player;
            modelPlayer = new PlayerEntityModel<>(new EntityRendererFactory.Context(mc.getEntityRenderDispatcher(), mc.getItemRenderer(), mc.getBlockRenderManager(), mc.getEntityRenderDispatcher().getHeldItemRenderer(), mc.getResourceManager(), mc.getEntityModelLoader(), mc.textRenderer).getPart(EntityModelLayers.PLAYER), false);
            modelPlayer.getHead().scale(new Vector3f(-0.3f, -0.3f, -0.3f));
            alpha = color.getValue().getAlpha();
        }

        public void update(CopyOnWriteArrayList<Person> arrayList) {
            if (alpha <= 0) {
                arrayList.remove(this);
                player.kill();
                player.remove(Entity.RemovalReason.KILLED);
                player.onRemoved();
                return;
            }
            alpha = (int) (AnimateUtil.animate(alpha, 0, alphaSpeed.getValue().doubleValue()) - 0.2);
        }

        public int getAlpha() {
            return MathHelper.clamp(alpha, 0, 255);
        }
    }
}