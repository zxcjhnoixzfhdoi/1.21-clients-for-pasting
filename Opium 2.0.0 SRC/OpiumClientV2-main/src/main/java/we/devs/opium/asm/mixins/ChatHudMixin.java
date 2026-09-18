package we.devs.opium.asm.mixins;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import we.devs.opium.Opium;
import we.devs.opium.api.utilities.RenderUtils;
import we.devs.opium.client.modules.client.ModuleFont;

import java.awt.*;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

@Mixin(ChatHud.class)
public abstract class ChatHudMixin {

    @Shadow @Final private MinecraftClient client;

    @Shadow private boolean hasUnreadNewMessages;

    @Shadow private int scrolledLines;

    @Shadow protected abstract int getLineHeight();

    @Shadow protected abstract void drawIndicatorIcon(DrawContext context, int x, int y, MessageIndicator.Icon icon);

    @Shadow protected abstract int getIndicatorX(ChatHudLine.Visible line);

    @Shadow @Final private List<ChatHudLine.Visible> visibleMessages;

    @Shadow protected abstract int getMessageIndex(double chatLineX, double chatLineY);

    @Shadow protected abstract double toChatLineX(double x);

    @Shadow protected abstract double toChatLineY(double y);

    @Shadow public abstract int getWidth();

    @Shadow public abstract double getChatScale();

    @Shadow public abstract int getVisibleLineCount();

    @Shadow protected abstract boolean isChatHidden();

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    void render(DrawContext context, int currentTick, int mouseX, int mouseY, boolean focused, CallbackInfo ci) {
        if (!this.isChatHidden() && ModuleFont.INSTANCE.customChatFont.getValue() && RenderUtils.getFontRenderer() != null) {
            Opium.LOGGER.info("Drawing text");
            int i = this.getVisibleLineCount();
            int j = this.visibleMessages.size();
            if (j > 0) {
                this.client.getProfiler().push("chat");
                float f = (float)this.getChatScale();
                int k = MathHelper.ceil((float)this.getWidth() / f);
                int l = context.getScaledWindowHeight();
                context.getMatrices().push();
                context.getMatrices().scale(f, f, 1.0F);
                context.getMatrices().translate(4.0F, 0.0F, 0.0F);
                int m = MathHelper.floor((float)(l - 40) / f);
                int n = this.getMessageIndex(this.toChatLineX((double)mouseX), this.toChatLineY((double)mouseY));
                double d = (Double)this.client.options.getChatOpacity().getValue() * (double)0.9F + (double)0.1F;
                double e = (Double)this.client.options.getTextBackgroundOpacity().getValue();
                double g = (Double)this.client.options.getChatLineSpacing().getValue();
                int o = this.getLineHeight();
                int p = (int)Math.round((double)-8.0F * (g + (double)1.0F) + (double)4.0F * g);
                int q = 0;

                for(int r = 0; r + this.scrolledLines < this.visibleMessages.size() && r < i; ++r) {
                    int s = r + this.scrolledLines;
                    ChatHudLine.Visible visible = (ChatHudLine.Visible)this.visibleMessages.get(s);
                    if (visible != null) {
                        int t = currentTick - visible.addedTime();
                        if (t < 200 || focused) {
                            double opacity = (double)t / (double)200.0F;
                            opacity = (double)1.0F - opacity;
                            opacity *= 10.0F;
                            opacity = MathHelper.clamp(opacity, 0.0F, 1.0F);
                            opacity *= opacity;
                            double h = focused ? (double)1.0F : opacity;
                            int u = (int)((double)255.0F * h * d);
                            int v = (int)((double)255.0F * h * e);
                            ++q;
                            if (u > 3) {
                                int w = 0;
                                int x = m - r * o;
                                int y = x + p;
                                context.fill(-4, x - o, 0 + k + 4 + 4, x, v << 24);
                                MessageIndicator messageIndicator = visible.indicator();
                                if (messageIndicator != null) {
                                    int z = messageIndicator.indicatorColor() | u << 24;
                                    context.fill(-4, x - o, -2, x, z);
                                    if (s == n && messageIndicator.icon() != null) {
                                        int aa = this.getIndicatorX(visible);
                                        Objects.requireNonNull(this.client.textRenderer);
                                        int ab = y + 9;
                                        this.drawIndicatorIcon(context, aa, ab, messageIndicator.icon());
                                    }
                                }

                                context.getMatrices().push();
                                context.getMatrices().translate(0.0F, 0.0F, 50.0F);
                                AtomicReference<String> prevText = new AtomicReference<>("");
                                visible.content().accept((index, style, codePoint) -> {
                                    String text = new StringBuilder().appendCodePoint(codePoint).toString();
                                    float newX = prevText.get() == "" ? 0 : RenderUtils.getFontRenderer().getStringWidth(prevText.get());
                                    RenderUtils.drawString(context.getMatrices(), text, newX, y, -1); // todo: color, bold / italic / underlined / strikethrough (formatting)
                                    prevText.getAndSet(prevText.get() + text);
                                    return true;
                                });
                                context.getMatrices().pop();
                            }
                        }
                    }
                }

                long ac = this.client.getMessageHandler().getUnprocessedMessageCount();
                if (ac > 0L) {
                    int ad = (int)((double)128.0F * d);
                    int t = (int)((double)255.0F * e);
                    context.getMatrices().push();
                    context.getMatrices().translate(0.0F, (float)m, 0.0F);
                    context.fill(-2, 0, k + 4, 9, t << 24);
                    context.getMatrices().translate(0.0F, 0.0F, 50.0F);
//                    context.drawTextWithShadow(this.client.textRenderer, Text.translatable("chat.queue", new Object[]{ac}).asOrderedText(), 0, 1, 16777215 + (ad << 24));
                    AtomicReference<String> prevText = new AtomicReference<>("");
                    Text.translatable("chat.queue", new Object[]{ac}).asOrderedText().accept((index, style, codePoint) -> {
                        String text = new StringBuilder().appendCodePoint(codePoint).toString();
                        float newX = prevText.get() == "" ? 0 : RenderUtils.getFontRenderer().getStringWidth(prevText.get());
                        RenderUtils.drawString(context.getMatrices(), text, newX, 1, (style == null || style.getColor() == null) ? Color.white.getRGB() : style.getColor().getRgb());
                        prevText.getAndSet(prevText.get() + text);
                        return true;
                    });
                    context.getMatrices().pop();
                }

                if (focused) {
                    int ad = this.getLineHeight();
                    int t = j * ad;
                    int ae = q * ad;
                    int af = this.scrolledLines * ae / j - m;
                    int u = ae * ae / t;
                    if (t != ae) {
                        int v = af > 0 ? 170 : 96;
                        int w = this.hasUnreadNewMessages ? 13382451 : 3355562;
                        int x = k + 4;
                        context.fill(x, -af, x + 2, -af - u, 100, w + (v << 24));
                        context.fill(x + 2, -af, x + 1, -af - u, 100, 13421772 + (v << 24));
                    }
                }

                context.getMatrices().pop();
                this.client.getProfiler().pop();
                ci.cancel();
            }
        }
    }

}
