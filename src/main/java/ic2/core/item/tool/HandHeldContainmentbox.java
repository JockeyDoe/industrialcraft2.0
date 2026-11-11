package ic2.core.item.tool;

import ic2.core.ContainerBase;
import ic2.core.item.reactor.ItemReactorMOX;
import ic2.core.item.reactor.ItemReactorUranium;
import ic2.core.ref.ItemName;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class HandHeldContainmentbox extends HandHeldInventory {
   public HandHeldContainmentbox(Player player, ItemStack stack1, int inventorySize) {
      super(player, stack1, inventorySize);
   }

   @Override
   public ContainerBase<HandHeldContainmentbox> getGuiContainer(Player player) {
      return new ContainerContainmentbox(player, this);
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public Screen getGui(Player player, boolean isAdmin) {
      return new GuiContainmentbox(new ContainerContainmentbox(player, this));
   }

   @Override
   public String getName() {
      return "ic2.containment_box";
   }

   public boolean hasCustomName() {
      return false;
   }

   @Override
   public boolean canPlaceItem(int index, ItemStack stack) {
      return stack == null
         ? false
         : stack.getItem() == ItemName.nuclear.getInstance() || stack.getItem() instanceof ItemReactorMOX || stack.getItem() instanceof ItemReactorUranium;
   }
}
