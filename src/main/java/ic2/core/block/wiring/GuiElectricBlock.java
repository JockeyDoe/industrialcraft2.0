package ic2.core.block.wiring;

import com.google.common.base.Supplier;
import ic2.core.GuiIC2;
import ic2.core.gui.EnergyGauge;
import ic2.core.gui.VanillaButton;
import ic2.core.init.Localization;
import java.util.Objects;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GuiElectricBlock extends GuiIC2<ContainerElectricBlock> {
   private static final Identifier background = Identifier.fromNamespaceAndPath("ic2", "textures/gui/guielectricblock.png");

   public GuiElectricBlock(final ContainerElectricBlock container) {
      super(container, 196);
      this.addElement(EnergyGauge.asBar(this, 79, 38, container.base));
      this.addElement(new VanillaButton(this, 152, 4, 20, 20, this.createEventSender(0)).withIcon(new Supplier<ItemStack>() {
         {
            Objects.requireNonNull(GuiElectricBlock.this);
         }

         public ItemStack get() {
            return new ItemStack(Items.REDSTONE);
         }
      }).withTooltip(new Supplier<String>() {
         {
            Objects.requireNonNull(GuiElectricBlock.this);
         }

         public String get() {
            return container.base.getRedstoneMode();
         }
      }));
   }

   @Override
   protected void drawForegroundLayer(int mouseX, int mouseY) {
      super.drawForegroundLayer(mouseX, mouseY);
      this.getGraphics().text(this.getFont(), Localization.translate("ic2.EUStorage.gui.info.armor"), 8, this.imageHeight - 126 + 3, -12566464, false);
      this.getGraphics().text(this.getFont(), Localization.translate("ic2.EUStorage.gui.info.level"), 79, 25, -12566464, false);
      int e = (int)Math.min(this.container.base.energy.getEnergy(), this.container.base.energy.getCapacity());
      this.getGraphics().text(this.getFont(), " " + e, 110, 35, -12566464, false);
      this.getGraphics().text(this.getFont(), "/" + (int)this.container.base.energy.getCapacity(), 110, 45, -12566464, false);
      String output = Localization.translate("ic2.EUStorage.gui.info.output", this.container.base.output);
      this.getGraphics().text(this.getFont(), output, 85, 60, -12566464, false);
   }

   @Override
   protected Identifier getTexture() {
      return background;
   }
}
