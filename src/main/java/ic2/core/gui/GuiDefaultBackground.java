package ic2.core.gui;

import ic2.core.ContainerBase;
import ic2.core.GuiIC2;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;

public abstract class GuiDefaultBackground<T extends ContainerBase<? extends Container>> extends GuiIC2<T> {
   public GuiDefaultBackground(T container) {
      super(container);
   }

   public GuiDefaultBackground(T container, int ySize) {
      super(container, ySize);
   }

   public GuiDefaultBackground(T container, int xSize, int ySize) {
      super(container, xSize, ySize);
   }

   @Override
   protected void drawBackgroundAndTitle(float partialTicks, int mouseX, int mouseY) {
      GuiElement.bindCommonTexture();
      this.drawTexturedRect(-16.0, -16.0, 32.0, 32.0, 0.0, 0.0);
      this.drawTexturedRect(this.imageWidth - 16, -16.0, 32.0, 32.0, 64.0, 0.0);
      this.drawTexturedRect(-16.0, this.imageHeight - 16, 32.0, 32.0, 0.0, 64.0);
      this.drawTexturedRect(this.imageWidth - 16, this.imageHeight - 16, 32.0, 32.0, 64.0, 64.0);

      for (int side = 0; side < 2; side++) {
         int y = this.imageHeight * side - 16;
         int v = 64 * side;

         for (int x = 16; x < this.imageWidth - 16; x += 32) {
            int width = Math.min(32, this.imageWidth - 16 - x);
            this.drawTexturedRect(x, y, width, 32.0, 32.0, v);
         }
      }

      for (int side = 0; side < 2; side++) {
         int x = this.imageWidth * side - 16;
         int u = 64 * side;

         for (int y = 16; y < this.imageHeight - 16; y += 32) {
            int height = Math.min(32, this.imageHeight - 16 - y);
            this.drawTexturedRect(x, y, 32.0, height, u, 32.0);
         }
      }

      for (int y = 16; y < this.imageHeight - 16; y += 32) {
         int height = Math.min(32, this.imageHeight - 16 - y);

         for (int x = 16; x < this.imageWidth - 16; x += 32) {
            int width = Math.min(32, this.imageWidth - 16 - x);
            this.drawTexturedRect(x, y, width, height, 32.0, 32.0);
         }
      }
   }

   @Override
   protected Identifier getTexture() {
      return null;
   }
}
