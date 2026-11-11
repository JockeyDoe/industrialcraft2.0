package ic2.core.block.machine.gui;

import ic2.core.GuiIC2;
import ic2.core.block.machine.container.ContainerCropHarvester;
import ic2.core.gui.EnergyGauge;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GuiCropHarvester extends GuiIC2<ContainerCropHarvester> {
   public GuiCropHarvester(ContainerCropHarvester container) {
      super(container);
      this.addElement(EnergyGauge.asBolt(this, 19, 37, container.base));
   }

   @Override
   public Identifier getTexture() {
      return Identifier.fromNamespaceAndPath("ic2", "textures/gui/guicropharvester.png");
   }
}
