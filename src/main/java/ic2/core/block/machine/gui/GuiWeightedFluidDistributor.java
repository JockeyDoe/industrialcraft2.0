package ic2.core.block.machine.gui;

import ic2.core.block.machine.container.ContainerWeightedFluidDistributor;
import ic2.core.gui.TankGauge;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GuiWeightedFluidDistributor extends GuiWeightedDistributor<ContainerWeightedFluidDistributor> {
   private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("ic2", "textures/gui/guiweightedfluiddistributor.png");

   public GuiWeightedFluidDistributor(ContainerWeightedFluidDistributor container) {
      super(container, 211);
      this.addElement(TankGauge.createPlain(this, 33, 111, 110, 10, container.base.fluidTank));
   }

   @Override
   protected Identifier getTexture() {
      return TEXTURE;
   }
}
