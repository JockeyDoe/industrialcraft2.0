package ic2.core.block.heatgenerator.gui;

import ic2.core.GuiIC2;
import ic2.core.block.heatgenerator.container.ContainerFluidHeatGenerator;
import ic2.core.gui.TankGauge;
import ic2.core.init.Localization;
import net.minecraft.resources.Identifier;

public class GuiFluidHeatGenerator extends GuiIC2<ContainerFluidHeatGenerator> {
   private static final Identifier background = Identifier.fromNamespaceAndPath("ic2", "textures/gui/guifluidheatgenerator.png");

   public GuiFluidHeatGenerator(ContainerFluidHeatGenerator container) {
      super(container);
      this.addElement(TankGauge.createNormal(this, 70, 20, container.base.getFluidTank()));
   }

   @Override
   protected void drawForegroundLayer(int mouseX, int mouseY) {
      super.drawForegroundLayer(mouseX, mouseY);
      this.getGraphics()
         .text(this.getFont(), Localization.translate("ic2.FluidHeatGenerator.gui.info.Emit") + this.container.base.gettransmitHeat(), 96, 33, -11025190, false);
      this.getGraphics()
         .text(
            this.getFont(),
            Localization.translate("ic2.FluidHeatGenerator.gui.info.MaxEmit") + this.container.base.getMaxHeatEmittedPerTick(),
            96,
            52,
            -11025190,
            false
         );
   }

   @Override
   protected Identifier getTexture() {
      return background;
   }
}
