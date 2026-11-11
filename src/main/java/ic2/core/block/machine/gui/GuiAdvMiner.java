package ic2.core.block.machine.gui;

import com.google.common.base.Supplier;
import ic2.core.GuiIC2;
import ic2.core.block.machine.container.ContainerAdvMiner;
import ic2.core.gui.BasicButton;
import ic2.core.gui.EnergyGauge;
import ic2.core.init.Localization;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GuiAdvMiner extends GuiIC2<ContainerAdvMiner> {
   public GuiAdvMiner(final ContainerAdvMiner container) {
      super(container, 203);
      this.addElement(EnergyGauge.asBolt(this, 12, 55, container.base));
      this.addElement(
         BasicButton.create(this, 133, 101, this.createEventSender(0), BasicButton.ButtonStyle.AdvMinerReset).withTooltip("ic2.AdvMiner.gui.switch.reset")
      );
      this.addElement(
         BasicButton.create(this, 123, 27, this.createEventSender(1), BasicButton.ButtonStyle.AdvMinerMode).withTooltip("ic2.AdvMiner.gui.switch.mode")
      );
      this.addElement(
         BasicButton.create(this, 129, 45, this.createEventSender(2), BasicButton.ButtonStyle.AdvMinerSilkTouch).withTooltip(new Supplier<String>() {
            {
               Objects.requireNonNull(GuiAdvMiner.this);
            }

            public String get() {
               return Localization.translate("ic2.AdvMiner.gui.switch.silktouch", container.base.silkTouch);
            }
         })
      );
   }

   @Override
   protected void drawForegroundLayer(int mouseX, int mouseY) {
      BlockPos target = this.container.base.getMineTarget();
      if (target != null) {
         BlockPos pos = this.container.base.getBlockPos();
         this.getGraphics()
            .text(
               this.getFont(),
               Localization.translate("ic2.AdvMiner.gui.info.minelevel", target.getX() - pos.getX(), target.getZ() - pos.getZ(), target.getY() - pos.getY()),
               28,
               105,
               -14619842,
               false
            );
      }

      if (this.container.base.blacklist) {
         this.getGraphics().text(this.getFont(), Localization.translate("ic2.AdvMiner.gui.mode.blacklist"), 40, 31, -14619842, false);
      } else {
         this.getGraphics().text(this.getFont(), Localization.translate("ic2.AdvMiner.gui.mode.whitelist"), 40, 31, -14619842, false);
      }

      super.drawForegroundLayer(mouseX, mouseY);
   }

   @Override
   public Identifier getTexture() {
      return Identifier.fromNamespaceAndPath("ic2", "textures/gui/guiadvminer.png");
   }
}
