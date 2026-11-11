package ic2.core.block.wiring;

import com.google.common.base.Supplier;
import ic2.core.GuiIC2;
import ic2.core.gui.EnergyGauge;
import ic2.core.gui.Text;
import ic2.core.gui.VanillaButton;
import ic2.core.gui.dynamic.TextProvider;
import java.util.Objects;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GuiChargepadBlock extends GuiIC2<ContainerChargepadBlock> {
   private static final Identifier background = Identifier.fromNamespaceAndPath("ic2", "textures/gui/guichargepadblock.png");

   public GuiChargepadBlock(final ContainerChargepadBlock container) {
      super(container, 161);
      this.addElement(EnergyGauge.asBar(this, 79, 38, container.base));
      this.addElement(new VanillaButton(this, 152, 4, 20, 20, this.createEventSender(0)).withIcon(new Supplier<ItemStack>() {
         {
            Objects.requireNonNull(GuiChargepadBlock.this);
         }

         public ItemStack get() {
            return new ItemStack(Items.REDSTONE);
         }
      }).withTooltip(new Supplier<String>() {
         {
            Objects.requireNonNull(GuiChargepadBlock.this);
         }

         public String get() {
            return container.base.getRedstoneMode();
         }
      }));
      this.addElement(Text.create(this, 79, 25, TextProvider.ofTranslated("ic2.EUStorage.gui.info.level"), 4210752, false));
      this.addElement(Text.create(this, 110, 35, TextProvider.of(new Supplier<String>() {
         {
            Objects.requireNonNull(GuiChargepadBlock.this);
         }

         public String get() {
            return " " + (int)Math.min(container.base.energy.getEnergy(), container.base.energy.getCapacity());
         }
      }), 4210752, false));
      this.addElement(Text.create(this, 110, 45, TextProvider.of(new Supplier<String>() {
         {
            Objects.requireNonNull(GuiChargepadBlock.this);
         }

         public String get() {
            return "/" + (int)container.base.energy.getCapacity();
         }
      }), 4210752, false));
   }

   @Override
   protected Identifier getTexture() {
      return background;
   }
}
