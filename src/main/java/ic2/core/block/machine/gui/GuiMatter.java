package ic2.core.block.machine.gui;

import ic2.core.GuiIC2;
import ic2.core.block.machine.container.ContainerMatter;
import ic2.core.gui.TankGauge;
import ic2.core.init.Localization;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GuiMatter extends GuiIC2<ContainerMatter> {
   public String progressLabel;
   public String amplifierLabel;

   public GuiMatter(ContainerMatter container) {
      super(container);
      this.addElement(TankGauge.createNormal(this, 96, 22, container.base.fluidTank));
      this.progressLabel = Localization.translate("ic2.Matter.gui.info.progress");
      this.amplifierLabel = Localization.translate("ic2.Matter.gui.info.amplifier");
   }

   @Override
   protected void drawForegroundLayer(int mouseX, int mouseY) {
      super.drawForegroundLayer(mouseX, mouseY);
      this.getGraphics().text(this.getFont(), this.progressLabel, 8, 22, -12566464, false);
      this.getGraphics().text(this.getFont(), this.container.base.getProgressAsString(), 18, 31, -12566464, false);
      if (this.container.base.scrap > 0) {
         this.getGraphics().text(this.getFont(), this.amplifierLabel, 8, 46, -12566464, false);
         this.getGraphics().text(this.getFont(), this.container.base.scrap + "", 8, 58, -12566464, false);
      }
   }

   @Override
   public Identifier getTexture() {
      return Identifier.fromNamespaceAndPath("ic2", "textures/gui/guimatter.png");
   }
}
