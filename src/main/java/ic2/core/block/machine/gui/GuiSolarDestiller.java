package ic2.core.block.machine.gui;

import ic2.core.GuiIC2;
import ic2.core.block.machine.container.ContainerSolarDestiller;
import ic2.core.gui.TankGauge;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GuiSolarDestiller extends GuiIC2<ContainerSolarDestiller> {
   public GuiSolarDestiller(ContainerSolarDestiller container) {
      super(container, 184);
      this.addElement(TankGauge.createPlain(this, 37, 43, 53, 18, container.base.inputTank));
      this.addElement(TankGauge.createPlain(this, 115, 55, 17, 43, container.base.outputTank));
   }

   @Override
   protected void func_146976_a(float f, int x, int y) {
      super.func_146976_a(f, x, y);
      this.bindTexture();
      if (this.container.base.canWork()) {
         this.func_73729_b(this.leftPos + 36, this.topPos + 26, 0, 184, 97, 29);
      }
   }

   @Override
   protected Identifier getTexture() {
      return Identifier.fromNamespaceAndPath("ic2", "textures/gui/guisolardestiller.png");
   }
}
