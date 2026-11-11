package ic2.core.item.tool;

import ic2.core.GuiIC2;
import ic2.core.gui.CustomButton;
import ic2.core.gui.IClickHandler;
import ic2.core.gui.MouseButton;
import ic2.core.init.Localization;
import ic2.core.util.Util;
import java.io.IOException;
import java.util.Objects;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GuiToolMeter extends GuiIC2<ContainerMeter> {
   public GuiToolMeter(ContainerMeter container) {
      super(container, 217);
      this.addElement(
         new CustomButton(this, 112, 55, 20, 20, this.createModeSetter(ContainerMeter.Mode.EnergyIn))
            .withTooltip("ic2.itemToolMEter.mode.switch\nic2.itemToolMEter.mode.EnergyIn")
      );
      this.addElement(
         new CustomButton(this, 132, 55, 20, 20, this.createModeSetter(ContainerMeter.Mode.EnergyOut))
            .withTooltip("ic2.itemToolMEter.mode.switch\nic2.itemToolMEter.mode.EnergyOut")
      );
      this.addElement(
         new CustomButton(this, 112, 75, 20, 20, this.createModeSetter(ContainerMeter.Mode.EnergyGain))
            .withTooltip("ic2.itemToolMEter.mode.switch\nic2.itemToolMEter.mode.EnergyGain")
      );
      this.addElement(
         new CustomButton(this, 132, 75, 20, 20, this.createModeSetter(ContainerMeter.Mode.Voltage))
            .withTooltip("ic2.itemToolMEter.mode.switch\nic2.itemToolMEter.mode.Voltage")
      );
   }

   private IClickHandler createModeSetter(final ContainerMeter.Mode mode) {
      return new IClickHandler() {
         {
            Objects.requireNonNull(GuiToolMeter.this);
         }

         @Override
         public void onClick(MouseButton button) {
            GuiToolMeter.this.container.setMode(mode);
         }
      };
   }

   @Override
   protected void func_73864_a(int i, int j, int k) throws IOException {
      super.func_73864_a(i, j, k);
      int xMin = (this.width - this.imageWidth) / 2;
      int yMin = (this.height - this.imageHeight) / 2;
      int x = i - xMin;
      int y = j - yMin;
      if (x >= 26 && y >= 111 && x <= 83 && y <= 123) {
         this.container.reset();
      }
   }

   @Override
   protected void drawForegroundLayer(int mouseX, int mouseY) {
      super.drawForegroundLayer(mouseX, mouseY);
      String unit = this.container.getMode() == ContainerMeter.Mode.Voltage ? "ic2.generic.text.v" : "ic2.generic.text.EUt";
      unit = Localization.translate(unit);
      this.getGraphics().text(this.getFont(), Localization.translate("ic2.itemToolMEter.mode"), 115, 43, -14619842, false);
      this.getGraphics().text(this.getFont(), Localization.translate("ic2.itemToolMEter.avg"), 15, 41, -14619842, false);
      this.getGraphics().text(this.getFont(), Util.toSiString(this.container.getResultAvg(), 6) + unit, 15, 51, -14619842, false);
      this.getGraphics().text(this.getFont(), Localization.translate("ic2.itemToolMEter.max/min"), 15, 64, -14619842, false);
      this.getGraphics().text(this.getFont(), Util.toSiString(this.container.getResultMax(), 6) + unit, 15, 74, -14619842, false);
      this.getGraphics().text(this.getFont(), Util.toSiString(this.container.getResultMin(), 6) + unit, 15, 84, -14619842, false);
      this.getGraphics()
         .text(this.getFont(), Localization.translate("ic2.itemToolMEter.cycle", this.container.getResultCount() / 20), 15, 100, -14619842, false);
      this.getGraphics().text(this.getFont(), Localization.translate("ic2.itemToolMEter.mode.reset"), 39, 114, -14619842, false);
      switch (this.container.getMode()) {
         case EnergyIn:
            this.getGraphics().text(this.getFont(), Localization.translate("ic2.itemToolMEter.mode.EnergyIn"), 105, 100, -14619842, false);
            break;
         case EnergyOut:
            this.getGraphics().text(this.getFont(), Localization.translate("ic2.itemToolMEter.mode.EnergyOut"), 105, 100, -14619842, false);
            break;
         case EnergyGain:
            this.getGraphics().text(this.getFont(), Localization.translate("ic2.itemToolMEter.mode.EnergyGain"), 105, 100, -14619842, false);
            break;
         case Voltage:
            this.getGraphics().text(this.getFont(), Localization.translate("ic2.itemToolMEter.mode.Voltage"), 105, 100, -14619842, false);
      }
   }

   @Override
   protected void func_146976_a(float f, int x, int y) {
      super.func_146976_a(f, x, y);
      this.bindTexture();
      switch (this.container.getMode()) {
         case EnergyIn:
            this.drawTexturedRect(112.0, 55.0, 40.0, 40.0, 176.0, 0.0);
            break;
         case EnergyOut:
            this.drawTexturedRect(112.0, 55.0, 40.0, 40.0, 176.0, 40.0);
            break;
         case EnergyGain:
            this.drawTexturedRect(112.0, 55.0, 40.0, 40.0, 176.0, 120.0);
            break;
         case Voltage:
            this.drawTexturedRect(112.0, 55.0, 40.0, 40.0, 176.0, 80.0);
      }
   }

   @Override
   protected Identifier getTexture() {
      return Identifier.fromNamespaceAndPath("ic2", "textures/gui/guitooleumeter.png");
   }
}
