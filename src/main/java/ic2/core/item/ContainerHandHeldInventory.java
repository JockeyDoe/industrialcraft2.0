package ic2.core.item;

import ic2.core.ContainerBase;
import ic2.core.item.tool.HandHeldInventory;
import ic2.core.util.ItemData;
import ic2.core.util.StackUtil;
import net.minecraft.network.protocol.game.ClientboundSetHeldSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class ContainerHandHeldInventory<T extends HandHeldInventory> extends ContainerBase<T> {
   public ContainerHandHeldInventory(T inventory) {
      super(inventory);
   }

   @Override
   public ItemStack func_184996_a(int slot, int button, ContainerInput type, Player player) {
      boolean closeGUI;
      closeGUI = false;
      label84:
      switch (type) {
         case CLONE:
         case PICKUP_ALL:
         case QUICK_CRAFT:
            break;
         case PICKUP:
            if (slot >= 0 && slot < this.slots.size()) {
               closeGUI = this.base.isThisContainer(((Slot)this.slots.get(slot)).getItem());
            }
            break;
         case QUICK_MOVE:
            if (slot >= 0 && slot < this.slots.size() && this.base.isThisContainer(((Slot)this.slots.get(slot)).getItem())) {
               return StackUtil.emptyStack;
            }
            break;
         case SWAP:
            assert slot >= 0 && slot < this.slots.size();

            assert this.findSlot(player.getInventory(), button) != null;

            boolean swapOut = this.base.isThisContainer(player.getInventory().getItem(button));
            boolean swapTo = this.base.isThisContainer(((Slot)this.slots.get(slot)).getItem());
            if (swapOut || swapTo) {
               for (int i = 0; i < 9; i++) {
                  if (swapOut && slot == this.findSlot(player.getInventory(), i).orElse(-1) || swapTo && button == i) {
                     if (player instanceof ServerPlayer) {
                        ((ServerPlayer)player).connection.send(new ClientboundSetHeldSlotPacket(i));
                     }
                     break label84;
                  }
               }
            }
            break;
         case THROW:
            if (slot >= 0 && slot < this.slots.size()) {
               closeGUI = this.base.isThisContainer(((Slot)this.slots.get(slot)).getItem());
            }
            break;
         default:
            throw new RuntimeException("Unexpected ClickType: " + type);
      }

      ItemStack stack = super.func_184996_a(slot, button, type, player);
      if (closeGUI && !player.level().isClientSide()) {
         this.base.saveAsThrown(stack);
         player.closeContainer();
      } else if (type == ContainerInput.CLONE) {
         ItemStack held = player.containerMenu.getCarried();
         if (this.base.isThisContainer(held)) {
            ItemData.remove(held, "uid");
         }
      }

      return stack;
   }

   @Override
   public void removed(Player player) {
      this.base.onGuiClosed(player);
      super.removed(player);
   }
}
