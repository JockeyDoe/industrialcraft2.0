package ic2.core.slot;

import ic2.core.util.StackUtil;
import ic2.core.util.Util;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class SlotHologramSlot extends Slot {
   protected final ItemStack[] stacks;
   protected final int index;
   protected final int stackSizeLimit;
   protected final SlotHologramSlot.ChangeCallback changeCallback;

   public SlotHologramSlot(ItemStack[] stacks, int index, int x, int y, int stackSizeLimit, SlotHologramSlot.ChangeCallback changeCallback) {
      super(null, 0, x, y);
      if (index >= stacks.length) {
         throw new ArrayIndexOutOfBoundsException(index);
      } else {
         this.stacks = stacks;
         this.index = index;
         this.stackSizeLimit = stackSizeLimit;
         this.changeCallback = changeCallback;
      }
   }

   public boolean mayPickup(Player player) {
      return false;
   }

   public int getMaxStackSize() {
      return this.stackSizeLimit;
   }

   public boolean mayPlace(ItemStack stack) {
      return false;
   }

   public ItemStack getItem() {
      return StackUtil.wrapEmpty(this.stacks[this.index]);
   }

   public void set(ItemStack stack) {
      this.stacks[this.index] = stack;
   }

   public void setChanged() {
      if (Util.inDev()) {
         System.out.println(StackUtil.toStringSafe(this.stacks));
      }

      if (this.changeCallback != null) {
         this.changeCallback.onChanged(this.index);
      }
   }

   public ItemStack func_75209_a(int amount) {
      return StackUtil.emptyStack;
   }

   public boolean func_75217_a(Container inventory, int index) {
      return false;
   }

   public ItemStack slotClick(int dragType, ContainerInput clickType, Player player) {
      if (Util.inDev() && player.level().isClientSide()) {
         System.out.printf("dragType=%d clickType=%s stack=%s%n", dragType, clickType, player.containerMenu.getCarried());
      }

      if (clickType == ContainerInput.PICKUP && (dragType == 0 || dragType == 1)) {
         ItemStack playerStack = player.containerMenu.getCarried();
         ItemStack slotStack = this.stacks[this.index];
         if (!StackUtil.isEmpty(playerStack)) {
            int curSize = StackUtil.getSize(slotStack);
            int extraSize = dragType == 0 ? StackUtil.getSize(playerStack) : 1;
            int limit = Math.min(playerStack.getMaxStackSize(), this.stackSizeLimit);
            if (curSize + extraSize > limit) {
               extraSize = Math.max(0, limit - curSize);
            }

            if (curSize == 0) {
               this.stacks[this.index] = StackUtil.copyWithSize(playerStack, extraSize);
            } else if (StackUtil.checkItemEquality(playerStack, slotStack)) {
               if (Util.inDev()) {
                  System.out.println("add " + extraSize + " to " + slotStack + " -> " + (curSize + extraSize));
               }

               this.stacks[this.index] = StackUtil.incSize(slotStack, extraSize);
            } else {
               this.stacks[this.index] = StackUtil.copyWithSize(playerStack, Math.min(StackUtil.getSize(playerStack), limit));
            }
         } else if (!StackUtil.isEmpty(slotStack)) {
            if (dragType == 0) {
               this.stacks[this.index] = StackUtil.emptyStack;
            } else {
               int newSize = StackUtil.getSize(slotStack) / 2;
               if (newSize <= 0) {
                  this.stacks[this.index] = StackUtil.emptyStack;
               } else {
                  this.stacks[this.index] = StackUtil.setSize(slotStack, newSize);
               }
            }
         }

         this.setChanged();
      }

      return StackUtil.emptyStack;
   }

   public interface ChangeCallback {
      void onChanged(int var1);
   }
}
