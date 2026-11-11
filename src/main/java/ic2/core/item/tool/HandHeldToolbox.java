package ic2.core.item.tool;

import ic2.api.item.ItemWrapper;
import ic2.core.ContainerBase;
import ic2.core.util.StackUtil;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class HandHeldToolbox extends HandHeldInventory {
   public HandHeldToolbox(Player player, ItemStack stack, int inventorySize) {
      super(player, stack, inventorySize);
   }

   @Override
   public ContainerBase<HandHeldToolbox> getGuiContainer(Player player) {
      return new ContainerToolbox(player, this);
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public Screen getGui(Player player, boolean isAdmin) {
      return new GuiToolbox(new ContainerToolbox(player, this));
   }

   @Override
   public String getName() {
      return "toolbox";
   }

   public boolean hasCustomName() {
      return false;
   }

   @Override
   public boolean canPlaceItem(int i, ItemStack itemstack) {
      return StackUtil.isEmpty(itemstack) ? false : ItemWrapper.canBeStoredInToolbox(itemstack);
   }
}
