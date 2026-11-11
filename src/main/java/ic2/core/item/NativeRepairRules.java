package ic2.core.item;

import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AnvilUpdateEvent;

@EventBusSubscriber(modid = "ic2")
public final class NativeRepairRules {
   private NativeRepairRules() {
   }

   @SubscribeEvent
   public static void anvil(AnvilUpdateEvent event) {
      ItemStack left = event.getLeft();
      ItemStack right = event.getRight();
      if (!left.isEmpty()
         && !right.isEmpty()
         && Math.max(event.getMaterialCost(), event.getVanillaResult().materialCost()) > 0
         && left.getItem() != right.getItem()) {
         if (left.getItem() instanceof RepairMatcher matcher && !matcher.func_82789_a(left, right)) {
            event.setCanceled(true);
         }
      }
   }
}
