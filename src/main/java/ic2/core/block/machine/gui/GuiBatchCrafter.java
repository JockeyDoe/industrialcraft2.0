package ic2.core.block.machine.gui;

import com.google.common.base.Supplier;
import ic2.core.GuiIC2;
import ic2.core.block.machine.container.ContainerBatchCrafter;
import ic2.core.gui.EnergyGauge;
import ic2.core.gui.Gauge;
import ic2.core.gui.ItemStackImage;
import ic2.core.gui.LinkedGauge;
import ic2.core.util.StackUtil;
import java.util.Objects;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GuiBatchCrafter extends GuiIC2<ContainerBatchCrafter> {
   private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("ic2", "textures/gui/guibatchcrafter.png");

   public GuiBatchCrafter(ContainerBatchCrafter container) {
      super(container, 206);
      this.addElement(EnergyGauge.asBolt(this, 12, 45, container.base));
      this.addElement(new LinkedGauge(this, 90, 35, container.base, "progress", Gauge.GaugeStyle.ProgressArrow));
      this.addElement(new ItemStackImage(this, 94, 14, new Supplier<ItemStack>() {
         {
            Objects.requireNonNull(GuiBatchCrafter.this);
         }

         public ItemStack get() {
            return StackUtil.wrapEmpty(GuiBatchCrafter.this.container.base.recipeOutput);
         }
      }));
   }

   @Override
   protected Identifier getTexture() {
      return TEXTURE;
   }
}
