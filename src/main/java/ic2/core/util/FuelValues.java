package ic2.core.util;

import java.util.Optional;
import net.minecraft.core.Holder.Reference;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.context.ContextKeySet;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CookingFuel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams.Builder;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.providers.number.ints.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProvider;
import net.minecraft.world.level.storage.loot.providers.number.ints.ResolvableInt;
import net.minecraft.world.level.storage.loot.providers.number.ints.ResolvableInt.Constant;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.loot.NeoForgeLootContextParams;

public final class FuelValues {
   private FuelValues() {
   }

   public static int withoutWorld(ItemStack stack, Provider registries) {
      CookingFuel fuel = (CookingFuel)stack.get(DataComponents.COOKING_FUEL);
      if (fuel == null) {
         return 0;
      } else if (fuel.burnTime() instanceof Constant constant) {
         return constant.value();
      } else {
         if (fuel.burnTime() instanceof net.minecraft.world.level.storage.loot.providers.number.ints.ResolvableInt.Reference reference) {
            Optional<Reference<ContextIntProvider>> provider = registries.lookup(Registries.CONTEXT_INT_PROVIDER)
               .flatMap(registry -> registry.get(reference.key()));
            if (provider.isPresent() && provider.get().value() instanceof ConstantValue constant) {
               return constant.value();
            }
         }

         return 0;
      }
   }

   public static int inWorld(ItemStack stack, ServerLevel level, BlockEntity owner, Container container) {
      Builder parameters = new Builder(level).withParameter(NeoForgeLootContextParams.QUERIED_STACK, stack);
      ContextKeySet set = new net.minecraft.util.context.ContextKeySet.Builder().required(NeoForgeLootContextParams.QUERIED_STACK).build();
      if (owner != null && container != null) {
         parameters.withParameter(LootContextParams.BLOCK_ENTITY, owner)
            .withParameter(LootContextParams.BLOCK_STATE, owner.getBlockState())
            .withParameter(LootContextParams.CONTAINER, container)
            .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(owner.getBlockPos()));
         set = LootContextParamSets.CONTAINER_PROCESS;
      }

      LootContext context = new net.minecraft.world.level.storage.loot.LootContext.Builder(parameters.create(set)).create(Optional.empty());
      return ResolvableInt.getFromItem(stack, DataComponents.COOKING_FUEL, CookingFuel::burnTime, context, 0);
   }
}
