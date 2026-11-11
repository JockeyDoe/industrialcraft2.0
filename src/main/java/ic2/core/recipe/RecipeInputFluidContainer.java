package ic2.core.recipe;

import ic2.api.recipe.IRecipeInput;
import ic2.core.IC2;
import ic2.core.item.ItemCapabilities;
import ic2.core.util.FluidKeys;
import ic2.core.util.FluidTransfer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;

public class RecipeInputFluidContainer extends RecipeInputBase implements IRecipeInput {
   final Fluid fluid;
   final int amount;
   private static volatile RecipeInputFluidContainer.FluidHandlerInfo fluidHandlerInfo = new RecipeInputFluidContainer.FluidHandlerInfo(
      Collections.emptyList(), -1, -1
   );

   RecipeInputFluidContainer(Fluid fluid) {
      this(fluid, 1000);
   }

   RecipeInputFluidContainer(Fluid fluid, int amount) {
      this.fluid = fluid;
      this.amount = amount;
   }

   @Override
   public boolean matches(ItemStack subject) {
      FluidStack fs = FluidTransfer.getFluidContained(subject);
      return fs == null && this.fluid == null || fs != null && fs.getFluid() == this.fluid && fs.getAmount() >= this.amount;
   }

   @Override
   public int getAmount() {
      return 1;
   }

   @Override
   public List<ItemStack> getInputs() {
      return getFluidContainer(this.fluid);
   }

   @Override
   public String toString() {
      return "RInputFluidContainer<" + this.amount + "x" + FluidKeys.name(this.fluid) + ">";
   }

   @Override
   public boolean equals(Object obj) {
      RecipeInputFluidContainer other;
      return obj != null && this.getClass() == obj.getClass() && (other = (RecipeInputFluidContainer)obj).fluid == this.fluid && other.amount == this.amount;
   }

   public static List<ItemStack> getFluidContainer(Fluid fluid) {
      RecipeInputFluidContainer.FluidHandlerInfo info = fluidHandlerInfo;
      if (info.itemCount != BuiltInRegistries.ITEM.size() || info.providerRevision != ItemCapabilities.revision()) {
         List<ItemStack> list = new ArrayList<>();

         for (Item item : BuiltInRegistries.ITEM) {
            ItemStack stack = new ItemStack(item);
            FluidTransfer handler = FluidTransfer.getFluidHandler(stack);
            if (handler != null) {
               handler.drain(Integer.MAX_VALUE, true);
               ItemStack container = handler.getContainer();
               if (container == null) {
                  IC2.platform
                     .displayError(
                        "Null container stack!\nItem: %s\nRegistry: %s\nUnlocalised: %s\nHandler: %s (%s)",
                        item,
                        BuiltInRegistries.ITEM.getKey(item),
                        item.getDescriptionId(),
                        handler,
                        handler.getClass()
                     );
               }

               if (FluidTransfer.getFluidContained(container) == null) {
                  list.add(stack);
               }
            }
         }

         fluidHandlerInfo = info = new RecipeInputFluidContainer.FluidHandlerInfo(
            Collections.unmodifiableList(list), BuiltInRegistries.ITEM.size(), ItemCapabilities.revision()
         );
      }

      if (fluid == null) {
         return info.items;
      } else {
         List<ItemStack> ret = new ArrayList<>();

         for (ItemStack stack : info.items) {
            FluidTransfer handler = FluidTransfer.getFluidHandler(stack.copy());
            if (handler != null && handler.fill(new FluidStack(fluid, Integer.MAX_VALUE), true) > 0) {
               ItemStack containerx = handler.getContainer();
               if (FluidTransfer.getFluidContained(containerx) != null) {
                  ret.add(containerx);
               }
            }
         }

         return ret;
      }
   }

   private static class FluidHandlerInfo {
      final List<ItemStack> items;
      final int itemCount;
      final int providerRevision;

      FluidHandlerInfo(List<ItemStack> items, int itemCount, int providerRevision) {
         this.items = items;
         this.itemCount = itemCount;
         this.providerRevision = providerRevision;
      }
   }
}
