package we.devs.opium.asm.mixins;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.sound.MusicTracker;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import we.devs.opium.Opium;
import we.devs.opium.api.manager.music.CustomSoundEngineManager;
import we.devs.opium.api.manager.music.MusicStateManager;
import we.devs.opium.api.utilities.IMinecraft;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin implements IMinecraft {

    @Shadow @Final private static Logger LOGGER;

    // Injects custom behavior at the end of the TitleScreen initialization.
    @Inject(method = "init()V", at = @At("TAIL"))
    void init(CallbackInfo ci) {
        // Get the Music Tracker
        MusicTracker musicTracker = mc.getMusicTracker();

        // Stop all base Minecraft music
        musicTracker.stop();

        // Check if the user-defined music is already playing
        if (!MusicStateManager.isPlayingCustomMusic()) {
            MusicStateManager.setPlayingCustomMusic(true); // Set the flag
            playNextTrack(); // Play the next custom track
        }
    }

    // Method to play a random track from the custom music pool
    @Unique
    private void playNextTrack() {
        // Choose a random music track
        String randomMusic = MusicStateManager.getRandomMusicTrack();

        // Play the track using the custom sound engine
        CustomSoundEngineManager.getSoundEngine().loadSound("opium/mod_music/" + randomMusic);
        CustomSoundEngineManager.getSoundEngine().play();
    }

    // Injects custom behavior when the TitleScreen is removed
    @Inject(method = "removed()V", at = @At("HEAD"))
    void removed(CallbackInfo ci) {
        LOGGER.info("Title Screen Closed");
    }

    // Injects custom behavior after rendering the panorama background
    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/TitleScreen;renderPanoramaBackground(Lnet/minecraft/client/gui/DrawContext;F)V", shift = At.Shift.AFTER))
    public void render(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        // Check if the current song is still playing
        float musicVolume = mc.options.getSoundVolume(SoundCategory.MUSIC);
        float masterVolume = mc.options.getSoundVolume(SoundCategory.MASTER);

        // If the current song is not playing, play the next track
        if (!CustomSoundEngineManager.getSoundEngine().isPlaying()) {
            playNextTrack(); // Start the next track
        }

        // Get screen dimensions and set text position
        int screenWidth = mc.getWindow().getScaledWidth();
        int screenHeight = mc.getWindow().getScaledHeight();
        int x = 2; // Left lower corner
        int y = screenHeight - 20;
        String baseText = "Playing: ";
        String trackName = CustomSoundEngineManager.getSoundEngine().getCurrentTrackName();
        String fullText = baseText + trackName; // Full text "Playing: <Trackname>"

        // Draw the current track information on the left lower corner
        if (musicVolume > 0.0F && masterVolume > 0.0F) {
            // Draw the base text
            context.drawTextWithShadow(mc.textRenderer, fullText, x, y, 0xFF808080);
        } else {
            context.drawTextWithShadow(mc.textRenderer, "Not Playing Music At The Moment", x, y, 0xFF808080);
        }

        // Highlight characters in the track name (for visual effect)
        if (musicVolume > 0.0F && masterVolume > 0.0F) {
            long time = System.currentTimeMillis();
            int charIndex = (int) ((time / 100) % fullText.length());
            int glintColor = 0xFFFFFF; // Glint effect color

            // Highlight 4 characters
            for (int i = 0; i < 4; i++) {
                int currentIndex = (charIndex + i) % fullText.length();
                int glintCharX = x + mc.textRenderer.getWidth(fullText.substring(0, currentIndex));
                context.drawTextWithShadow(mc.textRenderer, String.valueOf(fullText.charAt(currentIndex)), glintCharX, y, glintColor);
            }
        }

        // Optional: Draw the background image
        context.drawTexture(
                Identifier.of("opium", "textures/kenbg.png"),
                0, 0, 0, 0,
                mc.getWindow().getScaledWidth(),
                mc.getWindow().getScaledHeight(),
                mc.getWindow().getScaledWidth(),
                mc.getWindow().getScaledHeight()
        );
    }
}