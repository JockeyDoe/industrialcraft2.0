package ic2.core.block.machine.gui;

import ic2.core.GuiIC2;
import ic2.core.block.machine.container.ContainerClassicCanner;
import ic2.core.gui.EnergyGauge;
import ic2.core.gui.Gauge;
import ic2.core.gui.LinkedGauge;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GuiClassicCanner extends GuiIC2<ContainerClassicCanner> {
   public static final Identifier background = Identifier.fromNamespaceAndPath("ic2", "textures/gui/gui_canner_classic.png");

   public GuiClassicCanner(ContainerClassicCanner container) {
      super(container);
      this.addElement(new LinkedGauge(this, 74, 36, container.base, "progress", Gauge.GaugeStyle.ProgressLongArrow));
      this.addElement(EnergyGauge.asBolt(this, 34, 28, container.base));
   }

   @Override
   protected Identifier getTexture() {
      return background;
   }
}
