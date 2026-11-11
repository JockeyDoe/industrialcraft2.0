package ic2.core.item.armor;

import ic2.api.item.IItemHudInfo;
import ic2.api.item.IItemHudProvider;
import ic2.core.block.comp.Ic2FluidTank;
import ic2.core.init.Localization;
import ic2.core.item.ItemDefaults;
import ic2.core.item.capability.CapabilityFluidHandlerItem;
import ic2.core.ref.ItemName;
import ic2.core.util.FluidTransfer;
import ic2.core.util.ItemData;
import ic2.core.util.ItemStacks;
import ic2.core.util.StackUtil;
import ic2.core.util.Util;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;

public abstract class ItemArmorFluidTank extends ItemArmorUtility implements IItemHudInfo, IItemHudProvider.IItemHudBarProvider {
   protected final int capacity;
   protected final Fluid allowfluid;

   public ItemArmorFluidTank(ItemName name, String armorName, Fluid allowfluid, int capacity) {
      super(name, armorName, EquipmentSlot.CHEST);
      ItemDefaults.durability(this, 27);
      ItemDefaults.stackSize(this, 1);
      this.capacity = capacity;
      this.allowfluid = allowfluid;
      this.addCapability(net.neoforged.neoforge.capabilities.Capabilities.Fluid.ITEM, new BiFunction<ItemStack, ItemAccess, ResourceHandler<FluidResource>>() {
         {
            Objects.requireNonNull(ItemArmorFluidTank.this);
         }

         public ResourceHandler<FluidResource> apply(@Nullable ItemStack stack, ItemAccess access) {
            return new CapabilityFluidHandlerItem(stack, access, ItemArmorFluidTank.this.capacity) {
               {
                  Objects.requireNonNull(<VAR_NAMELESS_ENCLOSURE>);
               }

               @Override
               public boolean canFillFluidType(FluidStack fluid) {
                  return fluid != null && fluid.getFluid() == ItemArmorFluidTank.this.allowfluid;
               }

               @Override
               public boolean canDrainFluidType(FluidStack fluid) {
                  return fluid != null && fluid.getFluid() == ItemArmorFluidTank.this.allowfluid;
               }

               @Override
               public ItemStack getContainer() {
                  ItemStack ret = super.getContainer();
                  ItemArmorFluidTank.this.Updatedamage(ret);
                  return ret;
               }
            };
         }
      });
   }

   public void filltank(ItemStack stack) {
      CompoundTag nbtTagCompound = StackUtil.getOrCreateNbtData(stack);
      CompoundTag fluidTag = nbtTagCompound.getCompoundOrEmpty("Fluid");
      FluidStack fs = new FluidStack(this.allowfluid, this.capacity);
      new Ic2FluidTank(fs, this.capacity).writeToNBT(fluidTag);
      nbtTagCompound.put("Fluid", fluidTag);
      ItemData.commit(stack, nbtTagCompound);
   }

   public double getCharge(ItemStack stack) {
      FluidStack fs = FluidTransfer.getFluidContained(stack);
      if (fs == null) {
         return 0.0;
      } else {
         double ret = fs.getAmount();
         return ret > 0.0 ? ret : 0.0;
      }
   }

   public double getMaxCharge(ItemStack stack) {
      return this.capacity;
   }

   protected void Updatedamage(ItemStack stack) {
      ItemStacks.setRawMeta(stack, stack.getMaxDamage() - 1 - (int)Util.map(this.getCharge(stack), this.getMaxCharge(stack), stack.getMaxDamage() - 2));
   }

   public boolean isEmpty(ItemStack stack) {
      return FluidTransfer.getFluidContained(stack) == null;
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
   public int getBarPercent(ItemStack stack) {
      return this.getMaxDamage(stack) - this.getDamage(stack) * 100 / this.getMaxDamage(stack);
   }

   @Override
   public List<String> getHudInfo(ItemStack stack, boolean advanced) {
      List<String> info = new LinkedList<>();
      FluidStack fs = FluidTransfer.getFluidContained(stack);
      if (fs != null) {
         info.add("< " + fs.getHoverName().getString() + ", " + fs.getAmount() + " mB >");
      } else {
         info.add(Localization.translate("ic2.item.FluidContainer.Empty"));
      }

      return info;
   }

   @Override
   public boolean func_82789_a(ItemStack par1ItemStack, ItemStack par2ItemStack) {
      return false;
   }
}
