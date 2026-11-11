package ic2.core.item.tool;

import ic2.core.ContainerBase;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class HandHeldMeter extends HandHeldInventory {
   public HandHeldMeter(Player player, ItemStack stack) {
      super(player, stack, 0);
   }

   @Override
   public ContainerBase<?> getGuiContainer(Player player) {
      return new ContainerMeter(player, this);
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public Screen getGui(Player player, boolean isAdmin) {
      return new GuiToolMeter(new ContainerMeter(player, this));
   }

   @Override
   public String getName() {
      return "ic2.meter";
   }

   public boolean hasCustomName() {
      return false;
   }

   void closeGUI() {
      this.player.closeContainer();
   }
}
