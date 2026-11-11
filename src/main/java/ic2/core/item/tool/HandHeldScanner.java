package ic2.core.item.tool;

import ic2.core.ContainerBase;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class HandHeldScanner extends HandHeldInventory {
   ItemStack itemScanner;
   Player player;

   public HandHeldScanner(Player player, ItemStack itemScanner) {
      super(player, itemScanner, 0);
      this.itemScanner = itemScanner;
      this.player = player;
   }

   @Override
   public ContainerBase<HandHeldScanner> getGuiContainer(Player player) {
      return new ContainerToolScanner(player, this);
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public Screen getGui(Player player, boolean isAdmin) {
      return new GuiToolScanner(new ContainerToolScanner(player, this));
   }

   @Override
   public String getName() {
      return this.itemScanner.getItem().getDescriptionId();
   }

   public boolean hasCustomName() {
      return false;
   }
}
