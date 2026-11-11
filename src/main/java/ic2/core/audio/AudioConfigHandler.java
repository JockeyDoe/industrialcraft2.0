package ic2.core.audio;

import ic2.core.IC2;
import ic2.core.init.Localization;
import ic2.core.init.MainConfig;
import java.util.Locale;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.screens.options.SoundOptionsScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ScreenEvent.Init.Post;

public class AudioConfigHandler {
   @SubscribeEvent
   public static void guiCreate(Post event) {
      if (event.getScreen() instanceof SoundOptionsScreen gui) {
         byte var3 = 11;
         event.addListener(
            new AbstractSliderButton(
               gui.width / 2 - 155 + var3 % 2 * 160, gui.height / 6 - 12 + 24 * (var3 >> 1), 150, 20, Component.empty(), IC2.audioManager.getMasterVolume()
            ) {
               {
                  this.updateMessage();
               }

               protected void updateMessage() {
                  this.setMessage(Component.literal("IC2: " + (this.value == 0.0 ? Localization.translate("options.off") : (int)(this.value * 100.0) + "%")));
               }

               protected void applyValue() {
                  ((AudioManagerClient)IC2.audioManager).masterVolume = (float)this.value;
                  MainConfig.get().set("audio/volume", String.format(Locale.ROOT, "%.2f", this.value));
                  MainConfig.save();
               }
            }
         );
      }
   }
}
