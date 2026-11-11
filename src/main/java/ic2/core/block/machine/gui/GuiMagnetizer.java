package ic2.core.block.machine.gui;

import ic2.core.GuiIC2;
import ic2.core.block.BlockIC2Fence;
import ic2.core.block.machine.container.ContainerMagnetizer;
import ic2.core.gui.EnergyGauge;
import ic2.core.init.Localization;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GuiMagnetizer extends GuiIC2<ContainerMagnetizer> {
   public GuiMagnetizer(ContainerMagnetizer container) {
      super(container);
      this.addElement(EnergyGauge.asBolt(this, 11, 28, container.base));
   }

   @Override
   protected Identifier getTexture() {
      return Identifier.fromNamespaceAndPath("ic2", "textures/gui/guimagnetizer.png");
   }

   @Override
   protected void drawForegroundLayer(int mouseX, int mouseY) {
      super.drawForegroundLayer(mouseX, mouseY);
      if (BlockIC2Fence.hasMetalShoes(this.container.player)) {
         this.getGraphics().text(this.getFont(), Localization.translate("ic2.Magnetizer.gui.hasMetalShoes"), 18, 66, -12517568, false);
      } else {
         this.getGraphics().text(this.getFont(), Localization.translate("ic2.Magnetizer.gui.noMetalShoes"), 18, 66, -49088, false);
      }
   }
}
