package ic2.core.item;

import ic2.api.item.ElectricItem;
import ic2.core.gui.GuiInput;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

@OnlyIn(Dist.CLIENT)
public class ElectricItemTooltipHandler {
   public ElectricItemTooltipHandler() {
      NeoForge.EVENT_BUS.register(this);
   }

   @SubscribeEvent
   public void drawTooltips(ItemTooltipEvent event) {
      ItemStack stack = event.getItemStack();
      if (stack != null && ElectricItem.manager.getMaxCharge(stack) > 0.0) {
         String tooltip = ElectricItem.manager.getToolTip(stack);
         if (tooltip != null && !tooltip.trim().isEmpty()) {
            event.getToolTip().add(Component.literal(tooltip));
            if (GuiInput.shift()) {
               event.getToolTip().add(Component.translatable("ic2.item.tooltip.PowerTier", new Object[]{ElectricItem.manager.getTier(stack)}));
            }
         }
      }
   }
}
