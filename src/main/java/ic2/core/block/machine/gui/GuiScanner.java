package ic2.core.block.machine.gui;

import ic2.core.GuiIC2;
import ic2.core.block.machine.container.ContainerScanner;
import ic2.core.block.machine.tileentity.TileEntityScanner;
import ic2.core.gui.CustomButton;
import ic2.core.gui.EnergyGauge;
import ic2.core.gui.IEnableHandler;
import ic2.core.init.Localization;
import ic2.core.util.Util;
import java.util.Objects;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GuiScanner extends GuiIC2<ContainerScanner> {
   private static final Identifier background = Identifier.fromNamespaceAndPath("ic2", "textures/gui/guiscanner.png");
   private final String[] info = new String[9];

   public GuiScanner(final ContainerScanner container) {
      super(container);
      this.addElement(EnergyGauge.asBolt(this, 12, 25, container.base));
      this.addElement(
         new CustomButton(this, 102, 49, 12, 12, 176, 57, background, this.createEventSender(0))
            .withEnableHandler(
               new IEnableHandler() {
                  {
                     Objects.requireNonNull(GuiScanner.this);
                  }

                  @Override
                  public boolean isEnabled() {
                     return container.base.getState() == TileEntityScanner.State.COMPLETED
                        || container.base.getState() == TileEntityScanner.State.TRANSFER_ERROR
                        || container.base.getState() == TileEntityScanner.State.FAILED;
                  }
               }
            )
            .withTooltip("ic2.Scanner.gui.button.delete")
      );
      this.addElement(new CustomButton(this, 143, 49, 24, 12, 176, 69, background, this.createEventSender(1)).withEnableHandler(new IEnableHandler() {
         {
            Objects.requireNonNull(GuiScanner.this);
         }

         @Override
         public boolean isEnabled() {
            return container.base.getState() == TileEntityScanner.State.COMPLETED || container.base.getState() == TileEntityScanner.State.TRANSFER_ERROR;
         }
      }).withTooltip("ic2.Scanner.gui.button.save"));
      this.info[1] = Localization.translate("ic2.Scanner.gui.info1");
      this.info[2] = Localization.translate("ic2.Scanner.gui.info2");
      this.info[3] = Localization.translate("ic2.Scanner.gui.info3");
      this.info[4] = Localization.translate("ic2.Scanner.gui.info4");
      this.info[5] = Localization.translate("ic2.Scanner.gui.info5");
      this.info[6] = Localization.translate("ic2.Scanner.gui.info6");
      this.info[7] = Localization.translate("ic2.Scanner.gui.info7");
      this.info[8] = Localization.translate("ic2.Scanner.gui.info8");
   }

   @Override
   protected void drawForegroundLayer(int mouseX, int mouseY) {
      super.drawForegroundLayer(mouseX, mouseY);
      this.getGraphics().text(this.getFont(), this.info[5] + ":", 105, 6, -12566464, false);
      TileEntityScanner te = this.container.base;
      switch (te.getState()) {
         case IDLE:
            this.getGraphics().text(this.getFont(), Localization.translate("ic2.Scanner.gui.idle"), 10, 69, -1316064, false);
            break;
         case NO_STORAGE:
            this.getGraphics().text(this.getFont(), this.info[2], 10, 69, -1316064, false);
            break;
         case SCANNING:
            this.getGraphics().text(this.getFont(), this.info[1], 10, 69, -14619842, false);
            this.getGraphics().text(this.getFont(), te.getPercentageDone() + "%", 125, 69, -14619842, false);
            break;
         case NO_ENERGY:
            this.getGraphics().text(this.getFont(), this.info[3], 10, 69, -2682864, false);
            break;
         case ALREADY_RECORDED:
            this.getGraphics().text(this.getFont(), this.info[8], 10, 69, -2682864, false);
            break;
         case FAILED:
            this.getGraphics().text(this.getFont(), this.info[4], 10, 69, -14619842, false);
            this.getGraphics().text(this.getFont(), this.info[6], 110, 30, -2682864, false);
            break;
         case COMPLETED:
         case TRANSFER_ERROR:
            if (te.getState() == TileEntityScanner.State.COMPLETED) {
               this.getGraphics().text(this.getFont(), this.info[4], 10, 69, -14619842, false);
            }

            if (te.getState() == TileEntityScanner.State.TRANSFER_ERROR) {
               this.getGraphics().text(this.getFont(), this.info[7], 10, 69, -2682864, false);
            }

            this.getGraphics().text(this.getFont(), Util.toSiString(te.patternUu, 4) + "B UUM", 105, 25, -1, false);
            this.getGraphics().text(this.getFont(), Util.toSiString(te.patternEu, 4) + "EU", 105, 36, -1, false);
      }
   }

   @Override
   protected void func_146976_a(float partialTicks, int mouseX, int mouseY) {
      super.func_146976_a(partialTicks, mouseX, mouseY);
      this.bindTexture();
      TileEntityScanner te = this.container.base;
      int scanningloop = te.getSubPercentageDoneScaled(66);
      if (scanningloop > 0) {
         this.func_73729_b(this.leftPos + 30, this.topPos + 20, 176, 14, scanningloop, 43);
      }
   }

   @Override
   protected Identifier getTexture() {
      return background;
   }
}
