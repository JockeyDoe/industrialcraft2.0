package ic2.core.block.machine.gui;

import ic2.core.GuiIC2;
import ic2.core.block.machine.container.ContainerFluidBottler;
import ic2.core.gui.EnergyGauge;
import ic2.core.gui.TankGauge;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GuiFluidBottler extends GuiIC2<ContainerFluidBottler> {
   public GuiFluidBottler(ContainerFluidBottler container) {
      super(container, 184);
      this.addElement(EnergyGauge.asBolt(this, 12, 35, container.base));
      this.addElement(TankGauge.createNormal(this, 78, 34, container.base.fluidTank));
   }

   @Override
   protected void func_146976_a(float f, int x, int y) {
      super.func_146976_a(f, x, y);
      this.bindTexture();
      int progressSize = Math.round(this.container.base.getProgress() * 16.0F);
      if (progressSize > 0) {
         this.func_73729_b(this.leftPos + 61, this.topPos + 36, 198, 0, progressSize, 13);
         this.func_73729_b(this.leftPos + 61, this.topPos + 73, 198, 0, progressSize, 13);
         this.func_73729_b(this.leftPos + 99, this.topPos + 55, 198, 0, progressSize, 13);
      }
   }

   @Override
   public Identifier getTexture() {
      return Identifier.fromNamespaceAndPath("ic2", "textures/gui/guibottler.png");
   }
}
