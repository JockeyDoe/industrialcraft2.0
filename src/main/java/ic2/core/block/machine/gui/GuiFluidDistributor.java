package ic2.core.block.machine.gui;

import ic2.core.GuiIC2;
import ic2.core.IC2;
import ic2.core.block.machine.container.ContainerFluidDistributor;
import ic2.core.gui.TankGauge;
import ic2.core.init.Localization;
import java.io.IOException;
import net.minecraft.resources.Identifier;

public class GuiFluidDistributor extends GuiIC2<ContainerFluidDistributor> {
   public GuiFluidDistributor(ContainerFluidDistributor container) {
      super(container, 184);
      this.addElement(TankGauge.createPlain(this, 29, 38, 55, 47, container.base.fluidTank));
   }

   @Override
   protected void drawForegroundLayer(int mouseX, int mouseY) {
      super.drawForegroundLayer(mouseX, mouseY);
      this.getGraphics().text(this.getFont(), Localization.translate("ic2.FluidDistributor.gui.mode.info"), 112, 47, -11025190, false);
      if (this.container.base.getActive()) {
         this.getGraphics().text(this.getFont(), Localization.translate("ic2.FluidDistributor.gui.mode.concentrate"), 95, 71, -11025190, false);
      } else {
         this.getGraphics().text(this.getFont(), Localization.translate("ic2.FluidDistributor.gui.mode.distribute"), 95, 71, -11025190, false);
      }
   }

   @Override
   protected void func_73864_a(int mouseX, int mouseY, int mouseButton) throws IOException {
      super.func_73864_a(mouseX, mouseY, mouseButton);
      mouseX -= this.leftPos;
      mouseY -= this.topPos;
      if (mouseX >= 117 && mouseY >= 58 && mouseX <= 135 && mouseY <= 66) {
         IC2.network.get(false).initiateClientTileEntityEvent(this.container.base, 1);
      }
   }

   @Override
   protected Identifier getTexture() {
      return Identifier.fromNamespaceAndPath("ic2", "textures/gui/guifluiddistributor.png");
   }
}
