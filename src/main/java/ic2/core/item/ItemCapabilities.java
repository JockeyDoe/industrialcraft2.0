package ic2.core.item;

import ic2.core.block.BlockTileEntity;
import ic2.core.block.TileEntityBlock;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.BiFunction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.ItemCapability;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.capabilities.Capabilities.Fluid;
import net.neoforged.neoforge.transfer.access.ItemAccess;

@EventBusSubscriber(modid = "ic2")
public final class ItemCapabilities {
   private static final Map<Item, Map<ItemCapability<?, ItemAccess>, BiFunction<ItemStack, ItemAccess, ?>>> providers = new IdentityHashMap<>();
   private static int revision;

   public static int revision() {
      return revision;
   }

   public static <T> void add(Item item, ItemCapability<T, ItemAccess> capability, BiFunction<ItemStack, ItemAccess, T> provider) {
      Map<ItemCapability<?, ItemAccess>, BiFunction<ItemStack, ItemAccess, ?>> map = providers.computeIfAbsent(item, key -> new IdentityHashMap<>());
      if (map.putIfAbsent(capability, provider) != null) {
         throw new IllegalStateException("Duplicate item capability: " + capability.name());
      } else {
         revision++;
      }
   }

   public static <T> T get(ItemStack stack, ItemAccess access, ItemCapability<T, ItemAccess> capability) {
      Map<ItemCapability<?, ItemAccess>, BiFunction<ItemStack, ItemAccess, ?>> map = providers.get(stack.getItem());
      BiFunction<ItemStack, ItemAccess, ?> provider = map == null ? null : map.get(capability);
      return (T)(provider == null ? null : capability.typeClass().cast(provider.apply(stack, access)));
   }

   private static <T> void register(RegisterCapabilitiesEvent event, Item item, ItemCapability<T, ItemAccess> capability) {
      event.registerItem(capability, (stack, access) -> get(stack, access, capability), new ItemLike[]{item});
   }

   @SubscribeEvent
   public static void register(RegisterCapabilitiesEvent event) {
      providers.forEach((item, map) -> map.keySet().forEach(capability -> register(event, item, capability)));
      Block[] blocks = BuiltInRegistries.BLOCK.stream().filter(block -> block instanceof BlockTileEntity).toArray(Block[]::new);
      event.registerBlock(
         net.neoforged.neoforge.capabilities.Capabilities.Item.BLOCK,
         (level, pos, state, entity, side) -> entity instanceof TileEntityBlock machine
            ? machine.getCapability(net.neoforged.neoforge.capabilities.Capabilities.Item.BLOCK, side)
            : null,
         blocks
      );
      event.registerBlock(
         Fluid.BLOCK, (level, pos, state, entity, side) -> entity instanceof TileEntityBlock machine ? machine.getCapability(Fluid.BLOCK, side) : null, blocks
      );
   }
}
