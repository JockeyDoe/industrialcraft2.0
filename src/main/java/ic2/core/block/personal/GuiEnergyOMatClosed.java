package ic2.core.block.personal;

import ic2.core.GuiIC2;
import ic2.core.init.Localization;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GuiEnergyOMatClosed extends GuiIC2<ContainerEnergyOMatClosed> {
   private static final Identifier background = Identifier.fromNamespaceAndPath("ic2", "textures/gui/guienergyomatclosed.png");

   public GuiEnergyOMatClosed(ContainerEnergyOMatClosed container) {
      super(container);
   }

   @Override
   protected void drawForegroundLayer(int mouseX, int mouseY) {
      super.drawForegroundLayer(mouseX, mouseY);
      this.getGraphics().text(this.getFont(), Localization.translate("container.inventory"), 8, this.imageHeight - 96 + 2, -12566464, false);
      this.getGraphics().text(this.getFont(), Localization.translate("ic2.container.personalTrader.want"), 12, 21, -12566464, false);
      this.getGraphics().text(this.getFont(), Localization.translate("ic2.container.personalTrader.offer"), 12, 39, -12566464, false);
      this.getGraphics().text(this.getFont(), this.container.base.euOffer + " EU", 50, 39, -12566464, false);
      this.getGraphics()
         .text(this.getFont(), Localization.translate("ic2.container.personalTraderEnergy.paidFor", this.container.base.paidFor), 12, 57, -12566464, false);
   }

   @Override
   protected Identifier getTexture() {
      return background;
   }
}
