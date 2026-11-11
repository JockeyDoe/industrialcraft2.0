package ic2.core.block;

import ic2.core.block.comp.ComparatorEmitter;
import ic2.core.block.invslot.InvSlot;
import ic2.core.block.invslot.InvSlotUpgrade;
import ic2.core.util.BlockCapabilities;
import ic2.core.util.InventoryFields;
import ic2.core.util.StackUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities.Item;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper;
import net.neoforged.neoforge.transfer.item.WorldlyContainerWrapper;

public abstract class TileEntityInventory extends TileEntityBlock implements WorldlyContainer, IInventorySlotHolder<TileEntityInventory>, InventoryFields {
   private final List<InvSlot> invSlots = new ArrayList<>();
   private final ResourceHandler<ItemResource>[] itemHandler = new ResourceHandler[Direction.values().length + 1];
   protected final ComparatorEmitter comparator = this.addComponent(new ComparatorEmitter(this));

   public TileEntityInventory() {
      this.comparator.setUpdate(this::calcRedstoneFromInvSlots);
   }

   @Override
   public void loadAdditional(CompoundTag nbtTagCompound) {
      super.loadAdditional(nbtTagCompound);
      CompoundTag invSlotsTag = nbtTagCompound.getCompoundOrEmpty("InvSlots");

      for (InvSlot invSlot : this.invSlots) {
         invSlot.readFromNbt(invSlotsTag.getCompoundOrEmpty(invSlot.name));
      }
   }

   @Override
   public CompoundTag saveAdditional(CompoundTag nbt) {
      super.saveAdditional(nbt);
      CompoundTag invSlotsTag = new CompoundTag();

      for (InvSlot invSlot : this.invSlots) {
         CompoundTag invSlotTag = new CompoundTag();
         invSlot.writeToNbt(invSlotTag);
         invSlotsTag.put(invSlot.name, invSlotTag);
      }

      nbt.put("InvSlots", invSlotsTag);
      return nbt;
   }

   public int getContainerSize() {
      int ret = 0;

      for (InvSlot invSlot : this.invSlots) {
         ret += invSlot.size();
      }

      return ret;
   }

   public boolean isEmpty() {
      for (InvSlot invSlot : this.invSlots) {
         if (!invSlot.isEmpty()) {
            return false;
         }
      }

      return true;
   }

   public ItemStack getItem(int index) {
      int loc = this.locateInvSlot(index);
      return loc == -1 ? StackUtil.emptyStack : this.getStackAt(loc);
   }

   public ItemStack removeItem(int index, int amount) {
      int loc = this.locateInvSlot(index);
      if (loc == -1) {
         return StackUtil.emptyStack;
      } else {
         ItemStack stack = this.getStackAt(loc);
         if (StackUtil.isEmpty(stack)) {
            return StackUtil.emptyStack;
         } else if (amount >= StackUtil.getSize(stack)) {
            this.putStackAt(loc, StackUtil.emptyStack);
            return stack;
         } else {
            if (amount != 0) {
               if (amount < 0) {
                  int space = Math.min(this.getAt(loc).getStackSizeLimit(), stack.getMaxStackSize()) - StackUtil.getSize(stack);
                  amount = Math.max(amount, -space);
               }

               this.putStackAt(loc, StackUtil.decSize(stack, amount));
            }

            ItemStack ret = stack.copy();
            return StackUtil.setSize(ret, amount);
         }
      }
   }

   public ItemStack removeItemNoUpdate(int index) {
      int loc = this.locateInvSlot(index);
      if (loc == -1) {
         return StackUtil.emptyStack;
      } else {
         ItemStack ret = this.getStackAt(loc);
         if (!StackUtil.isEmpty(ret)) {
            this.putStackAt(loc, StackUtil.emptyStack);
         }

         return ret;
      }
   }

   public void setItem(int index, ItemStack stack) {
      int loc = this.locateInvSlot(index);
      if (loc == -1) {
         assert false;
      } else {
         if (StackUtil.isEmpty(stack)) {
            stack = StackUtil.emptyStack;
         }

         this.putStackAt(loc, stack);
      }
   }

   public void setChanged() {
      super.setChanged();

      for (InvSlot invSlot : this.invSlots) {
         invSlot.onChanged();
      }
   }

   public String getName() {
      ITeBlock teBlock = TeBlockRegistry.get((Class<? extends TileEntityBlock>)this.getClass());
      String name = teBlock == null ? "invalid" : teBlock.getName();
      return this.getBlockType().getDescriptionId() + "." + name;
   }

   public boolean hasCustomName() {
      return false;
   }

   public Component getDisplayName() {
      return Component.literal(this.getName());
   }

   public int getMaxStackSize() {
      int max = 0;

      for (InvSlot slot : this.invSlots) {
         max = Math.max(max, slot.getStackSizeLimit());
      }

      return max;
   }

   public boolean stillValid(Player player) {
      return !this.isRemoved() && player.distanceToSqr(Vec3.atCenterOf(this.getBlockPos())) <= 64.0;
   }

   public void startOpen(Player player) {
   }

   public void stopOpen(Player player) {
   }

   public boolean canPlaceItem(int index, ItemStack stack) {
      if (stack.isEmpty()) {
         return false;
      } else {
         InvSlot invSlot = this.getInventorySlot(index);
         return invSlot != null && invSlot.canInput() && invSlot.accepts(stack);
      }
   }

   public int[] getSlotsForFace(Direction side) {
      int[] ret = new int[this.getContainerSize()];
      int i = 0;

      while (i < ret.length) {
         ret[i] = i++;
      }

      return ret;
   }

   public boolean canPlaceItemThroughFace(int index, ItemStack stack, Direction side) {
      if (StackUtil.isEmpty(stack)) {
         return false;
      } else {
         InvSlot targetSlot = this.getInventorySlot(index);
         if (targetSlot == null) {
            return false;
         } else if (targetSlot.canInput() && targetSlot.accepts(stack)) {
            if (targetSlot.preferredSide != InvSlot.InvSide.ANY && targetSlot.preferredSide.matches(side)) {
               return true;
            } else {
               for (InvSlot invSlot : this.invSlots) {
                  if (invSlot != targetSlot
                     && invSlot.preferredSide != InvSlot.InvSide.ANY
                     && invSlot.preferredSide.matches(side)
                     && invSlot.canInput()
                     && invSlot.accepts(stack)) {
                     return false;
                  }
               }

               return true;
            }
         } else {
            return false;
         }
      }
   }

   public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction side) {
      InvSlot targetSlot = this.getInventorySlot(index);
      if (targetSlot != null && targetSlot.canOutput()) {
         boolean correctSide = targetSlot.preferredSide.matches(side);
         if (targetSlot.preferredSide != InvSlot.InvSide.ANY && correctSide) {
            return true;
         } else {
            for (InvSlot invSlot : this.invSlots) {
               if (invSlot != targetSlot
                  && (invSlot.preferredSide != InvSlot.InvSide.ANY || !correctSide)
                  && invSlot.preferredSide.matches(side)
                  && invSlot.canOutput()
                  && !invSlot.isEmpty()) {
                  return false;
               }
            }

            return true;
         }
      } else {
         return false;
      }
   }

   @Override
   public int func_174887_a_(int id) {
      return 0;
   }

   @Override
   public void func_174885_b(int id, int value) {
   }

   @Override
   public int func_174890_g() {
      return 0;
   }

   public void clearContent() {
      for (InvSlot invSlot : this.invSlots) {
         invSlot.clear();
      }
   }

   @Override
   public int getBaseIndex(InvSlot invSlot) {
      int ret = 0;

      for (InvSlot slot : this.invSlots) {
         if (slot == invSlot) {
            return ret;
         }

         ret += slot.size();
      }

      return -1;
   }

   public TileEntityInventory getParent() {
      return this;
   }

   @Override
   public InvSlot getInventorySlot(String name) {
      for (InvSlot invSlot : this.invSlots) {
         if (invSlot.name.equals(name)) {
            return invSlot;
         }
      }

      return null;
   }

   @Override
   public void addInventorySlot(InvSlot inventorySlot) {
      assert this.invSlots.stream().noneMatch(slot -> slot.name.equals(inventorySlot.name));

      this.invSlots.add(inventorySlot);
   }

   private int locateInvSlot(int extIndex) {
      if (extIndex < 0) {
         return -1;
      } else {
         for (int i = 0; i < this.invSlots.size(); i++) {
            int size = this.invSlots.get(i).size();
            if (extIndex < size) {
               return i << 16 | extIndex;
            }

            extIndex -= size;
         }

         return -1;
      }
   }

   private static int getIndex(int loc) {
      return loc >>> 16;
   }

   private static int getOffset(int loc) {
      return loc & 65535;
   }

   private InvSlot getAt(int loc) {
      assert loc != -1;

      return this.invSlots.get(getIndex(loc));
   }

   private ItemStack getStackAt(int loc) {
      return this.getAt(loc).get(getOffset(loc));
   }

   private void putStackAt(int loc, ItemStack stack) {
      this.getAt(loc).put(getOffset(loc), stack);
      super.setChanged();
   }

   private InvSlot getInventorySlot(int extIndex) {
      int loc = this.locateInvSlot(extIndex);
      return loc == -1 ? null : this.getAt(loc);
   }

   @Override
   protected List<ItemStack> getAuxDrops(int fortune) {
      List<ItemStack> ret = new ArrayList<>(super.getAuxDrops(fortune));

      for (InvSlot slot : this.invSlots) {
         for (ItemStack stack : slot) {
            if (!StackUtil.isEmpty(stack)) {
               ret.add(stack);
            }
         }
      }

      return ret;
   }

   @Override
   public <T> T getCapability(BlockCapability<T, Direction> capability, Direction facing) {
      if (capability == Item.BLOCK) {
         if (facing == null) {
            if (this.itemHandler[this.itemHandler.length - 1] == null) {
               this.itemHandler[this.itemHandler.length - 1] = VanillaContainerWrapper.of(this);
            }

            return BlockCapabilities.cast(Item.BLOCK, this.itemHandler[this.itemHandler.length - 1]);
         } else {
            if (this.itemHandler[facing.ordinal()] == null) {
               this.itemHandler[facing.ordinal()] = new WorldlyContainerWrapper(this, facing);
            }

            return BlockCapabilities.cast(Item.BLOCK, this.itemHandler[facing.ordinal()]);
         }
      } else {
         return super.getCapability(capability, facing);
      }
   }

   @Override
   public boolean hasCapability(BlockCapability<?, Direction> capability, Direction facing) {
      return capability == Item.BLOCK || super.hasCapability(capability, facing);
   }

   protected static int calcRedstoneFromInvSlots(InvSlot... slots) {
      return calcRedstoneFromInvSlots(Arrays.asList(slots));
   }

   protected int calcRedstoneFromInvSlots() {
      return calcRedstoneFromInvSlots(this.invSlots);
   }

   protected static int calcRedstoneFromInvSlots(Iterable<InvSlot> invSlots) {
      int space = 0;
      int used = 0;

      for (InvSlot slot : invSlots) {
         if (!(slot instanceof InvSlotUpgrade)) {
            int size = slot.size();
            int limit = slot.getStackSizeLimit();
            space += size * limit;

            for (int i = 0; i < size; i++) {
               ItemStack stack = slot.get(i);
               if (!StackUtil.isEmpty(stack)) {
                  used += Math.min(limit, stack.getCount() * limit / stack.getMaxStackSize());
               }
            }
         }
      }

      return used != 0 && space != 0 ? 1 + used * 14 / space : 0;
   }
}
