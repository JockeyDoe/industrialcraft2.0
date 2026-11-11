package ic2.core.util;

import ic2.core.block.TileEntityInventory;
import ic2.core.block.invslot.InvSlot;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class IInventoryInvSlot implements Container, InventoryFields {
   public final InvSlot slot;

   public IInventoryInvSlot(InvSlot slot) {
      this.slot = slot;
   }

   public int getContainerSize() {
      return this.slot.size();
   }

   public int getMaxStackSize() {
      return this.slot.getStackSizeLimit();
   }

   public boolean isEmpty() {
      return this.slot.isEmpty();
   }

   public boolean canPlaceItem(int index, ItemStack stack) {
      return this.slot.accepts(stack);
   }

   public ItemStack getItem(int index) {
      return this.slot.get(index);
   }

   public ItemStack removeItem(int index, int count) {
      ItemStack stack = this.getItem(index);
      if (!StackUtil.isEmpty(stack)) {
         int amount = Math.min(StackUtil.getSize(stack), count);
         ItemStack out = StackUtil.copyWithSize(stack, amount);
         this.setItem(index, StackUtil.decSize(stack, amount));
         return out;
      } else {
         return StackUtil.emptyStack;
      }
   }

   public void setItem(int index, ItemStack stack) {
      this.slot.put(index, stack);
   }

   public ItemStack removeItemNoUpdate(int index) {
      ItemStack stack = this.getItem(index);
      this.setItem(index, StackUtil.emptyStack);
      return stack;
   }

   public void clearContent() {
      this.slot.clear();
   }

   public void setChanged() {
      this.slot.onChanged();
   }

   public boolean stillValid(Player player) {
      return true;
   }

   public void startOpen(Player player) {
   }

   public void stopOpen(Player player) {
   }

   public boolean hasCustomName() {
      assert this.slot.base != null;

      return ((TileEntityInventory)this.slot.base.getParent()).hasCustomName();
   }

   public String getName() {
      assert this.slot.base != null;

      return ((TileEntityInventory)this.slot.base.getParent()).getName();
   }

   public Component getDisplayName() {
      assert this.slot.base != null;

      return ((TileEntityInventory)this.slot.base.getParent()).getDisplayName();
   }

   @Override
   public int func_174890_g() {
      assert this.slot.base != null;

      return ((TileEntityInventory)this.slot.base.getParent()).func_174890_g();
   }

   @Override
   public int func_174887_a_(int id) {
      assert this.slot.base != null;

      return ((TileEntityInventory)this.slot.base.getParent()).func_174887_a_(id);
   }

   @Override
   public void func_174885_b(int id, int value) {
      assert this.slot.base != null;

      ((TileEntityInventory)this.slot.base.getParent()).func_174885_b(id, value);
   }
}
