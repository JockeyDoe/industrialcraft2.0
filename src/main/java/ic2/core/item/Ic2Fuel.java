package ic2.core.item;

import com.mojang.serialization.MapCodec;
import ic2.core.IC2;
import ic2.core.init.NativeRegistration;
import ic2.core.ref.BlockName;
import ic2.core.ref.ItemName;
import java.util.List;
import java.util.Set;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CookingFuel;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootContextUser;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.providers.number.floats.ResolvableFloat.Constant;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProvider;
import net.minecraft.world.level.storage.loot.providers.number.ints.ResolvableInt;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.loot.NeoForgeLootContextParams;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;

@EventBusSubscriber(modid = "ic2")
public final class Ic2Fuel implements ContextIntProvider, LootContextUser {
   public static final ResourceKey<ContextIntProvider> KEY = ResourceKey.create(Registries.CONTEXT_INT_PROVIDER, IC2.getIdentifier("fuel"));
   public static final MapCodec<Ic2Fuel> CODEC = MapCodec.unit(Ic2Fuel::new);

   public static void init() {
      NativeRegistration.add(Registries.CONTEXT_INT_PROVIDER_TYPE, IC2.getIdentifier("fuel"), CODEC);
   }

   public int getIntUnsafe(LootContext context) {
      ItemInstance instance = (ItemInstance)context.getOptional(NeoForgeLootContextParams.QUERIED_STACK);
      if (instance == null) {
         return 0;
      } else if (instance instanceof ItemStack stack) {
         return IC2.getInstance().getBurnTime(stack);
      } else {
         ItemStack stack = new ItemStack(instance.typeHolder(), instance.count());
         BuiltInRegistries.DATA_COMPONENT_TYPE.forEach(type -> copyComponent(instance, stack, type));
         return IC2.getInstance().getBurnTime(stack);
      }
   }

   private static <T> void copyComponent(ItemInstance source, ItemStack destination, DataComponentType<T> type) {
      destination.set(type, source.get(type));
   }

   public MapCodec<? extends ContextIntProvider> codec() {
      return CODEC;
   }

   public Set<ContextKey<?>> getReferencedContextParams() {
      return Set.of(NeoForgeLootContextParams.QUERIED_STACK);
   }

   public void validate(ValidationContext context) {
      context.validateContextUsage(this);
   }

   @SubscribeEvent
   public static void components(ModifyDefaultComponentsEvent event) {
      CookingFuel fuel = new CookingFuel(ResolvableInt.fromKey(KEY), new Constant(1.0F));

      for (Item item : List.of(
         BlockName.sapling.getInstance().asItem(),
         Items.SUGAR_CANE,
         Items.CACTUS,
         ItemName.crafting.getInstance(),
         ItemName.cell.getInstance(),
         ItemName.fluid_cell.getInstance()
      )) {
         event.modify(item, (builder, context, target) -> builder.set(DataComponents.COOKING_FUEL, fuel));
      }
   }
}
