package ic2.core.util;

import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.Nameable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;

public class DelegatingInventory implements Container, InventoryFields {
   private final Container parent;
   private final ContainerData fields;

   public DelegatingInventory(Container parent) {
      this.parent = parent;
      this.fields = InventoryFields.of(parent);
   }

   public String getName() {
      return InventoryNames.get(this.parent);
   }

   public boolean hasCustomName() {
      return this.parent instanceof Nameable named && named.hasCustomName();
   }

   public Component getDisplayName() {
      return (Component)(this.parent instanceof Nameable named ? named.getDisplayName() : Component.translatable(this.getName()));
   }

   public int getContainerSize() {
      return this.parent.getContainerSize();
   }

   public boolean isEmpty() {
      return this.parent.isEmpty();
   }

   public ItemStack getItem(int index) {
      return this.parent.getItem(index);
   }

   public ItemStack removeItem(int index, int count) {
      return this.parent.removeItem(index, count);
   }

   public ItemStack removeItemNoUpdate(int index) {
      return this.parent.removeItemNoUpdate(index);
   }

   public void setItem(int index, ItemStack stack) {
      this.parent.setItem(index, stack);
   }

   public int getMaxStackSize() {
      return this.parent.getMaxStackSize();
   }

   public void setChanged() {
      this.parent.setChanged();
   }

   public boolean stillValid(Player player) {
      return this.parent.stillValid(player);
   }

   public void startOpen(Player player) {
      this.parent.startOpen(player);
   }

   public void stopOpen(Player player) {
      this.parent.stopOpen(player);
   }

   public boolean canPlaceItem(int index, ItemStack stack) {
      return this.parent.canPlaceItem(index, stack);
   }

   @Override
   public int func_174887_a_(int id) {
      return this.fields.get(id);
   }

   @Override
   public void func_174885_b(int id, int value) {
      this.fields.set(id, value);
   }

   @Override
   public int func_174890_g() {
      return this.fields.getCount();
   }

   public void clearContent() {
      this.parent.clearContent();
   }
}
