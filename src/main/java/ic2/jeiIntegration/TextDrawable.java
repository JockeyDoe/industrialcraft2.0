package ic2.jeiIntegration;

import ic2.core.gui.Text;
import ic2.core.init.Localization;
import mezz.jei.api.gui.drawable.IDrawable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class TextDrawable implements IDrawable {
   private final String text;
   private final Text.TextAlignment alignment;
   private final int color;

   public TextDrawable(String text, Text.TextAlignment alignment, int color) {
      this.text = text;
      this.alignment = alignment;
      this.color = color;
   }

   public void draw(GuiGraphicsExtractor graphics, int x, int y) {
      graphics.text(Minecraft.getInstance().font, Localization.translate(this.text), x, y, this.color, false);
   }

   public int getHeight() {
      return 12;
   }

   public int getWidth() {
      return Minecraft.getInstance().font.width(Localization.translate(this.text));
   }
}
