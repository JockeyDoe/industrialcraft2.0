package ic2.core.gui.dynamic;

import ic2.core.item.tool.HandHeldInventory;
import ic2.core.slot.SlotHologramSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class DynamicHandHeldContainer<T extends HandHeldInventory> extends DynamicContainer<T> {
   public static <T extends HandHeldInventory> DynamicHandHeldContainer<T> create(T base, Player player, GuiParser.GuiNode guiNode) {
      return new DynamicHandHeldContainer<>(base, player, guiNode);
   }

   protected DynamicHandHeldContainer(T base, Player player, GuiParser.GuiNode guiNode) {
      super(base, player, guiNode);
   }

   @Override
   protected SlotHologramSlot.ChangeCallback getCallback() {
      return this.base.makeSaveCallback();
   }

   @Override
   public void onContainerEvent(String event) {
      this.base.onEvent(event);
      super.onContainerEvent(event);
   }

   @Override
   public ItemStack func_184996_a(int slot, int button, ContainerInput type, Player player) {
      boolean thrown = false;
      Slot realSlot = null;
      if (!player.level().isClientSide() && slot >= 0 && slot < this.slots.size()) {
         realSlot = (Slot)this.slots.get(slot);
         thrown = this.base.isThisContainer(realSlot.getItem());
      }

      ItemStack stack = super.func_184996_a(slot, button, type, player);
      if (thrown && !realSlot.hasItem()) {
         this.base.saveAsThrown(stack);
         player.closeContainer();
      }

      return stack;
   }

   @Override
   public void removed(Player player) {
      this.base.onGuiClosed(player);
      super.removed(player);
   }
}
