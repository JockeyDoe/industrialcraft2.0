package ic2.core;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;

public abstract class ContainerFullInv<T extends Container> extends ContainerBase<T> {
   public ContainerFullInv(Player player, T base, int height) {
      super(base);
      this.addPlayerInventorySlots(player, height);
   }

   public ContainerFullInv(Player player, T base, int width, int height) {
      super(base);
      this.addPlayerInventorySlots(player, width, height);
   }
}
