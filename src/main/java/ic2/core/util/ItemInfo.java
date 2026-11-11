package ic2.core.util;

import ic2.api.info.IInfoProvider;
import ic2.core.IC2;
import ic2.core.block.TileEntityInventory;
import ic2.core.init.MainConfig;
import ic2.core.item.Ic2Fuel;
import ic2.core.item.type.CraftingItemType;
import ic2.core.item.type.DustResourceType;
import ic2.core.ref.ItemName;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CookingFuel;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.loot.providers.number.ints.ResolvableInt.Reference;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

public class ItemInfo implements IInfoProvider {
   @Override
   public double getEnergyValue(ItemStack stack) {
      if (StackUtil.isEmpty(stack)) {
         return 0.0;
      } else if (StackUtil.checkItemEquality(stack, ItemName.single_use_battery.getItemStack())) {
         return 1200.0;
      } else if (StackUtil.checkItemEquality(stack, Items.REDSTONE)) {
         return 800.0;
      } else {
         return StackUtil.checkItemEquality(stack, ItemName.dust.getItemStack(DustResourceType.energium)) ? 16000.0 : 0.0;
      }
   }

   @Override
   public int getFuelValue(ItemStack stack, boolean allowLava) {
      return this.getFuelValue(stack, allowLava, null);
   }

   public int getFuelValue(ItemStack stack, boolean allowLava, TileEntityInventory machine) {
      if (StackUtil.isEmpty(stack)) {
         return 0;
      } else if ((
            StackUtil.checkItemEquality(stack, ItemName.crafting.getItemStack(CraftingItemType.scrap))
               || StackUtil.checkItemEquality(stack, ItemName.crafting.getItemStack(CraftingItemType.scrap_box))
         )
         && !ConfigUtil.getBool(MainConfig.get(), "misc/allowBurningScrap")) {
         return 0;
      } else {
         FluidStack liquid = FluidTransfer.getFluidContained(stack);
         boolean isLava = liquid != null && liquid.getAmount() > 0 && liquid.getFluid() == Fluids.LAVA;
         if (isLava && !allowLava) {
            return 0;
         } else {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            ServerLevel level = machine != null && machine.getLevel() instanceof ServerLevel world ? world : (server == null ? null : server.overworld());
            int ret;
            if (level != null) {
               ret = FuelValues.inWorld(stack, level, machine, machine);
            } else {
               CookingFuel fuel = (CookingFuel)stack.get(DataComponents.COOKING_FUEL);
               if (fuel != null && fuel.burnTime() instanceof Reference ref && ref.key().equals(Ic2Fuel.KEY)) {
                  ret = IC2.getInstance().getBurnTime(stack);
               } else {
                  ret = FuelValues.withoutWorld(stack, ItemStacks.registries());
               }
            }

            return isLava ? ret / 10 : ret;
         }
      }
   }
}
