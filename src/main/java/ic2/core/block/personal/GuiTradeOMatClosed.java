package ic2.core.block.personal;

import ic2.core.GuiIC2;
import ic2.core.init.Localization;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GuiTradeOMatClosed extends GuiIC2<ContainerTradeOMatClosed> {
   private static final Identifier background = Identifier.fromNamespaceAndPath("ic2", "textures/gui/guitradeomatclosed.png");

   public GuiTradeOMatClosed(ContainerTradeOMatClosed container) {
      super(container);
   }

   @Override
   protected void drawForegroundLayer(int mouseX, int mouseY) {
      super.drawForegroundLayer(mouseX, mouseY);
      this.getGraphics().text(this.getFont(), Localization.translate("container.inventory"), 8, this.imageHeight - 96 + 2, -12566464, false);
      this.getGraphics().text(this.getFont(), Localization.translate("ic2.container.personalTrader.want"), 12, 23, -12566464, false);
      this.getGraphics().text(this.getFont(), Localization.translate("ic2.container.personalTrader.offer"), 12, 42, -12566464, false);
      this.getGraphics().text(this.getFont(), Localization.translate("ic2.container.personalTrader.stock"), 12, 60, -12566464, false);
      this.getGraphics()
         .text(
            this.getFont(),
            this.container.base.stock < 0 ? "Ã¢Ë†Å¾" : this.container.base.stock + "",
            50,
            60,
            (this.container.base.stock != 0 ? 4210752 : 16733525) | 0xFF000000,
            false
         );
   }

   @Override
   protected Identifier getTexture() {
      return background;
   }
}
