package ic2.core.block.machine.gui;

import ic2.core.GuiIC2;
import ic2.core.block.machine.container.ContainerMiner;
import ic2.core.gui.EnergyGauge;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GuiMiner extends GuiIC2<ContainerMiner> {
   public GuiMiner(ContainerMiner container) {
      super(container);
      this.addElement(EnergyGauge.asBolt(this, 155, 41, container.base));
   }

   @Override
   protected Identifier getTexture() {
      return Identifier.fromNamespaceAndPath("ic2", "textures/gui/guiminer.png");
   }
}
