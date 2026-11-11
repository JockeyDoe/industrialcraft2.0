package ic2.core.item.type;

import ic2.core.block.state.IIdProvider;
import ic2.core.crop.TileEntityCrop;
import ic2.core.profile.NotExperimental;
import ic2.core.ref.FluidName;
import ic2.core.ref.ItemName;
import ic2.core.util.FluidTransfer;
import ic2.core.util.ItemData;
import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.transfer.ItemAccessResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

@NotExperimental
public enum CellType implements IIdProvider {
   empty(0),
   water(1, Fluids.WATER),
   lava(2, Fluids.LAVA),
   air(3, FluidName.air.getInstance()),
   electrolyzed_water(4),
   biofuel(5),
   coalfuel(6),
   bio(7),
   hydrated_coal(8),
   weed_ex(9),
   hydration(10);

   private final int id;
   final Fluid fluid;

   private CellType(int id) {
      this(id, null);
   }

   private CellType(int id, Fluid fluid) {
      this.id = id;
      this.fluid = fluid;
   }

   @Override
   public String getName() {
      return this.name();
   }

   @Override
   public int getId() {
      return this.id;
   }

   public int getStackSize() {
      return this != weed_ex && this != hydration ? 64 : 1;
   }

   public boolean isFluidContainer() {
      return this.fluid != null || this == empty;
   }

   public boolean hasCropAction() {
      return this == water || this == weed_ex || this == hydration;
   }

   public int getUsage(ItemStack stack) {
      switch (this) {
         case weed_ex:
            return ItemData.has(stack) ? ItemData.read(stack).getIntOr("weedEX", 0) : 0;
         case hydration:
            return ItemData.has(stack) ? ItemData.read(stack).getIntOr("hydration", 0) : 0;
         default:
            return 0;
      }
   }

   public int getMaximum(ItemStack stack) {
      switch (this) {
         case weed_ex:
            return 64;
         case hydration:
            return 10000;
         default:
            return 0;
      }
   }

   public InteractionResult doCropAction(ItemStack stack, Consumer<ItemStack> result, TileEntityCrop crop, boolean manual) {
      assert this.hasCropAction();

      switch (this) {
         case water:
            if (crop.getStorageWater() < 10) {
               crop.setStorageWater(10);
               return InteractionResult.SUCCESS;
            }

            return InteractionResult.FAIL;
         case weed_ex:
            FluidTransfer handler = chargeHandler(stack, false, true);
            if (crop.applyWeedEx(handler, manual)) {
               result.accept(handler.getContainer());
               return InteractionResult.SUCCESS;
            }

            return InteractionResult.FAIL;
         case hydration:
            FluidTransfer hydrationHandler = chargeHandler(stack, manual, false);
            if (crop.applyHydration(hydrationHandler)) {
               result.accept(hydrationHandler.getContainer());
               return InteractionResult.SUCCESS;
            }

            return InteractionResult.FAIL;
         default:
            throw new IllegalStateException("Type was " + this);
      }
   }

   private static FluidTransfer chargeHandler(ItemStack stack, boolean manual, boolean weedEx) {
      ItemAccess access = ItemAccess.forStack(stack);
      return new FluidTransfer(new CellType.ChargeHandler(access, manual, weedEx), access);
   }

   public static class CellFluidHandler extends ItemAccessResourceHandler<FluidResource> {
      private final Function<ItemStack, CellType> typeGetter;

      public CellFluidHandler(ItemStack stack, Function<ItemStack, CellType> typeGetter) {
         this(stack, ItemAccess.forStack(stack), typeGetter);
      }

      public CellFluidHandler(ItemStack stack, ItemAccess access, Function<ItemStack, CellType> typeGetter) {
         super(access, 1);
         this.typeGetter = typeGetter;
      }

      protected FluidResource getResourceFrom(ItemResource item, int index) {
         if (item.isEmpty()) {
            return FluidResource.EMPTY;
         } else {
            CellType type = this.typeGetter.apply(item.toStack());
            return type != null && type.fluid != null ? FluidResource.of(type.fluid) : FluidResource.EMPTY;
         }
      }

      protected int getAmountFrom(ItemResource item, int index) {
         return this.getResourceFrom(item, index).isEmpty() ? 0 : 1000;
      }

      protected int getCapacity(int index, FluidResource resource) {
         return 1000;
      }

      public boolean isValid(int index, FluidResource resource) {
         return !this.itemAccess.getResource().isEmpty()
            && this.typeGetter.apply(this.itemAccess.getResource().toStack()) == CellType.empty
            && Arrays.stream(CellType.values()).anyMatch(type -> type.fluid == resource.getFluid());
      }

      protected ItemResource update(ItemResource item, int index, FluidResource resource, int amount) {
         CellType result = CellType.empty;
         if (amount > 0) {
            result = Arrays.stream(CellType.values()).filter(type -> type.fluid == resource.getFluid()).findFirst().orElseThrow();
         }

         return ItemResource.of(ItemName.cell.getItemStack(result));
      }

      public int insert(int index, FluidResource resource, int amount, TransactionContext tx) {
         return amount < 1000 ? 0 : super.insert(index, resource, 1000, tx);
      }

      public int extract(int index, FluidResource resource, int amount, TransactionContext tx) {
         return amount < 1000 ? 0 : super.extract(index, resource, 1000, tx);
      }
   }

   private static final class ChargeHandler extends ItemAccessResourceHandler<FluidResource> {
      private final boolean manual;
      private final boolean weedEx;

      ChargeHandler(ItemAccess access, boolean manual, boolean weedEx) {
         super(access, 1);
         this.manual = manual;
         this.weedEx = weedEx;
      }

      private String key() {
         return this.weedEx ? "weedEX" : "hydration";
      }

      private int maximum() {
         return this.weedEx ? 3200 : 10000;
      }

      protected FluidResource getResourceFrom(ItemResource item, int index) {
         return this.getAmountFrom(item, index) == 0
            ? FluidResource.EMPTY
            : FluidResource.of((Fluid)(this.weedEx ? FluidName.weed_ex.getInstance() : Fluids.WATER));
      }

      protected int getAmountFrom(ItemResource item, int index) {
         if (item.isEmpty()) {
            return 0;
         } else {
            CompoundTag tag = ((CustomData)item.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)).copyTag();
            return Math.max(0, this.maximum() - tag.getIntOr(this.key(), 0) * (this.weedEx ? 50 : 1));
         }
      }

      protected int getCapacity(int index, FluidResource resource) {
         return this.maximum();
      }

      public boolean isValid(int index, FluidResource resource) {
         return false;
      }

      public int insert(int index, FluidResource resource, int amount, TransactionContext tx) {
         return 0;
      }

      public int extract(int index, FluidResource resource, int amount, TransactionContext tx) {
         return this.weedEx && amount < 50 ? 0 : super.extract(index, resource, this.weedEx ? 50 : (this.manual ? amount : Math.min(amount, 180)), tx);
      }

      protected ItemResource update(ItemResource item, int index, FluidResource resource, int amount) {
         if (amount <= 0) {
            return ItemResource.EMPTY;
         } else {
            CompoundTag tag = ((CustomData)item.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)).copyTag();
            tag.putInt(this.key(), (this.maximum() - amount) / (this.weedEx ? 50 : 1));
            return item.with(DataComponents.CUSTOM_DATA, CustomData.of(tag));
         }
      }
   }
}
