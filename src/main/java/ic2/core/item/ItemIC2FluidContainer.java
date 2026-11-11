package ic2.core.item;

import ic2.api.item.IItemHudInfo;
import ic2.core.init.Localization;
import ic2.core.item.capability.CapabilityFluidHandlerItem;
import ic2.core.ref.FluidName;
import ic2.core.ref.IMultiItem;
import ic2.core.ref.ItemName;
import ic2.core.util.FluidKeys;
import ic2.core.util.FluidTransfer;
import ic2.core.util.StackUtil;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import javax.annotation.Nullable;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.capabilities.Capabilities.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;

public abstract class ItemIC2FluidContainer extends ItemIC2 implements IMultiItem<FluidName>, IItemHudInfo {
   protected final int capacity;

   public ItemIC2FluidContainer(ItemName name, int capacity) {
      super(name);
      this.capacity = capacity;
      ItemDefaults.variants(this, true);
      this.addCapability(Fluid.ITEM, new BiFunction<ItemStack, ItemAccess, ResourceHandler<FluidResource>>() {
         {
            Objects.requireNonNull(ItemIC2FluidContainer.this);
         }

         public ResourceHandler<FluidResource> apply(@Nullable ItemStack stack, ItemAccess access) {
            return new CapabilityFluidHandlerItem(stack, access, ItemIC2FluidContainer.this.capacity) {
               {
                  Objects.requireNonNull(<VAR_NAMELESS_ENCLOSURE>);
               }

               @Override
               public boolean canFillFluidType(FluidStack fluid) {
                  return fluid != null && ItemIC2FluidContainer.this.canfill(fluid.getFluid());
               }

               @Override
               public boolean canDrainFluidType(FluidStack fluid) {
                  return fluid != null && ItemIC2FluidContainer.this.canfill(fluid.getFluid());
               }
            };
         }
      });
   }

   public ItemStack getItemStack(FluidName type) {
      return this.getItemStack(type.getInstance());
   }

   public ItemStack getItemStack(net.minecraft.world.level.material.Fluid fluid) {
      ItemStack ret = new ItemStack(this);
      if (fluid == null) {
         return ret;
      } else {
         FluidTransfer handler = FluidTransfer.getFluidHandler(ret);
         if (handler == null) {
            return null;
         } else {
            return handler.fill(new FluidStack(fluid, Integer.MAX_VALUE), true) > 0 ? handler.getContainer() : null;
         }
      }
   }

   @Override
   public ItemStack getItemStack(String variant) {
      if (variant != null && !variant.isEmpty()) {
         net.minecraft.world.level.material.Fluid fluid = FluidKeys.find(variant);
         return fluid == null ? null : this.getItemStack(fluid);
      } else {
         return new ItemStack(this);
      }
   }

   @Override
   public String getVariant(ItemStack stack) {
      if (stack == null) {
         throw new NullPointerException("null stack");
      } else if (stack.getItem() != this) {
         throw new IllegalArgumentException("The stack " + stack + " doesn't match " + this);
      } else {
         FluidStack fs = FluidTransfer.getFluidContained(stack);
         return fs != null && fs.getFluid() != null ? FluidKeys.name(fs.getFluid()) : null;
      }
   }

   @Override
   public Set<FluidName> getAllTypes() {
      return EnumSet.allOf(FluidName.class);
   }

   @Override
   public Set<ItemStack> getAllStacks() {
      Set<ItemStack> ret = new HashSet<>();
      ret.add(new ItemStack(this));

      for (net.minecraft.world.level.material.Fluid fluid : BuiltInRegistries.FLUID.stream().toList()) {
         ItemStack add = this.getItemStack(fluid);
         if (add != null) {
            ret.add(add);
         }
      }

      return ret;
   }

   @Override
   public boolean hasContainerItem(ItemStack stack) {
      return FluidTransfer.getFluidContained(stack) != null;
   }

   @Override
   public ItemStack getContainerItem(ItemStack stack) {
      if (!this.hasContainerItem(stack)) {
         return super.getContainerItem(stack);
      } else {
         ItemStack ret = StackUtil.copyWithSize(stack, 1);
         FluidTransfer handler = FluidTransfer.getFluidHandler(ret);
         handler.drain(Integer.MAX_VALUE, true);
         return handler.getContainer();
      }
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public void func_77624_a(ItemStack stack, Level world, List<String> tooltip, TooltipFlag advanced) {
      super.func_77624_a(stack, world, tooltip, advanced);
      FluidStack fs = FluidTransfer.getFluidContained(stack);
      if (fs != null) {
         tooltip.add("< " + fs.getHoverName().getString() + ", " + fs.getAmount() + " mB >");
      } else {
         tooltip.add(Localization.translate("ic2.item.FluidContainer.Empty"));
      }
   }

   @Override
   public List<String> getHudInfo(ItemStack stack, boolean advanced) {
      List<String> info = new LinkedList<>();
      FluidStack fs = FluidTransfer.getFluidContained(stack);
      if (fs != null) {
         info.add("< " + FluidKeys.name(fs) + ", " + fs.getAmount() + " mB >");
      } else {
         info.add(Localization.translate("ic2.item.FluidContainer.Empty"));
      }

      return info;
   }

   public abstract boolean canfill(net.minecraft.world.level.material.Fluid var1);
}
