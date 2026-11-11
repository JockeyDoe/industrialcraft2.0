package ic2.core.block.machine.gui;

import ic2.core.GuiIC2;
import ic2.core.block.machine.container.ContainerClassicCropmatron;
import ic2.core.gui.EnergyGauge;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GuiClassicCropmatron extends GuiIC2<ContainerClassicCropmatron> {
   private static final Identifier background = Identifier.fromNamespaceAndPath("ic2", "textures/gui/gui_cropmatron_classic.png");

   public GuiClassicCropmatron(ContainerClassicCropmatron container) {
      super(container);
      this.addElement(EnergyGauge.asBolt(this, 29, 39, container.base));
   }

   @Override
   protected Identifier getTexture() {
      return background;
   }
}
