package ic2.core.block.comp;

import ic2.core.util.FluidKeys;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

public class Ic2FluidTank extends FluidStacksResourceHandler implements FluidTankView {
   public Ic2FluidTank(int capacity) {
      super(1, capacity);
      if (capacity < 0) {
         throw new IllegalArgumentException("Negative tank capacity");
      }
   }

   public Ic2FluidTank(FluidStack stack, int capacity) {
      this(capacity);
      this.setFluid(stack);
   }

   @Override
   public FluidStack getFluid() {
      int amount = this.getFluidAmount();
      return amount == 0 ? null : ((FluidResource)this.getResource(0)).toStack(amount);
   }

   @Override
   public int getFluidAmount() {
      return this.getAmountAsInt(0);
   }

   @Override
   public int getCapacity() {
      return this.capacity;
   }

   public void setCapacity(int capacity) {
      if (capacity < 0) {
         throw new IllegalArgumentException("Negative tank capacity");
      } else {
         this.capacity = capacity;
      }
   }

   public void setFluid(FluidStack stack) {
      this.set(0, stack == null ? FluidResource.EMPTY : FluidResource.of(stack), stack == null ? 0 : stack.getAmount());
   }

   public boolean canFillFluidType(FluidStack stack) {
      return stack != null && !stack.isEmpty();
   }

   public boolean canDrainFluidType(FluidStack stack) {
      return stack != null && !stack.isEmpty();
   }

   public boolean isValid(int index, FluidResource resource) {
      return super.isValid(index, resource) && this.canFillFluidType(resource.toStack(1));
   }

   public int fill(FluidStack stack, boolean execute) {
      return !this.canFillFluidType(stack) ? 0 : this.fillInternal(stack, execute);
   }

   public int fillInternal(FluidStack stack, boolean execute) {
      if (stack != null && !stack.isEmpty()) {
         Transaction transaction = Transaction.open(null);

         int var5;
         try {
            int inserted = this.insert(0, FluidResource.of(stack), stack.getAmount(), transaction);
            if (execute) {
               transaction.commit();
            }

            var5 = inserted;
         } catch (Throwable var7) {
            if (transaction != null) {
               try {
                  transaction.close();
               } catch (Throwable var6) {
                  var7.addSuppressed(var6);
               }
            }

            throw var7;
         }

         if (transaction != null) {
            transaction.close();
         }

         return var5;
      } else {
         return 0;
      }
   }

   public FluidStack drain(int amount, boolean execute) {
      FluidStack stored = this.getFluid();
      return !this.canDrainFluidType(stored) ? null : this.drainInternal(amount, execute);
   }

   public FluidStack drain(FluidStack stack, boolean execute) {
      FluidStack stored = this.getFluid();
      return stack != null && stored != null && FluidStack.isSameFluidSameComponents(stack, stored) ? this.drain(stack.getAmount(), execute) : null;
   }

   public FluidStack drainInternal(int amount, boolean execute) {
      if (amount > 0 && this.getFluidAmount() != 0) {
         FluidResource resource = (FluidResource)this.getResource(0);
         Transaction transaction = Transaction.open(null);

         FluidStack var6;
         try {
            int extracted = this.extract(0, resource, amount, transaction);
            if (execute) {
               transaction.commit();
            }

            var6 = extracted == 0 ? null : resource.toStack(extracted);
         } catch (Throwable var8) {
            if (transaction != null) {
               try {
                  transaction.close();
               } catch (Throwable var7) {
                  var8.addSuppressed(var7);
               }
            }

            throw var8;
         }

         if (transaction != null) {
            transaction.close();
         }

         return var6;
      } else {
         return null;
      }
   }

   public Ic2FluidTank readFromNBT(CompoundTag tag) {
      RegistryOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY));
      if (tag.contains("Contents")) {
         this.setFluid((FluidStack)FluidStack.OPTIONAL_CODEC.parse(ops, tag.get("Contents")).getOrThrow());
      } else {
         Fluid fluid = FluidKeys.find(tag.getStringOr("FluidName", ""));
         int amount = Math.max(0, tag.getIntOr("Amount", 0));
         this.setFluid(fluid != null && amount != 0 ? new FluidStack(fluid, amount) : null);
      }

      return this;
   }

   public CompoundTag writeToNBT(CompoundTag tag) {
      RegistryOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY));
      FluidStack stored = this.getFluid();
      tag.put("Contents", (Tag)FluidStack.OPTIONAL_CODEC.encodeStart(ops, stored == null ? FluidStack.EMPTY : stored).getOrThrow());
      return tag;
   }
}
