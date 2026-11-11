package ic2.core.block.machine.container;

import gnu.trove.TIntCollection;
import gnu.trove.iterator.TIntIterator;
import ic2.core.ContainerFullInv;
import ic2.core.IC2;
import ic2.core.block.machine.tileentity.TileEntityIndustrialWorkbench;
import ic2.core.recipe.NativeCrafting;
import ic2.core.slot.SlotInvSlot;
import ic2.core.util.InventorySlotCrafting;
import ic2.core.util.StackUtil;
import ic2.core.util.Tuple;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class ContainerIndustrialWorkbench extends ContainerFullInv<TileEntityIndustrialWorkbench> {
   protected final CraftingContainer craftMatrix = new InventorySlotCrafting(3, 3) {
      {
         Objects.requireNonNull(ContainerIndustrialWorkbench.this);
      }

      @Override
      protected ItemStack get(int index) {
         return ContainerIndustrialWorkbench.this.base.craftingGrid.get(index);
      }

      @Override
      protected void put(int index, ItemStack stack) {
         ContainerIndustrialWorkbench.this.base.craftingGrid.put(index, stack);
         ContainerIndustrialWorkbench.this.slotsChanged(this);
      }

      @Override
      public boolean isEmpty() {
         return ContainerIndustrialWorkbench.this.base.craftingGrid.isEmpty();
      }

      @Override
      public void clearContent() {
         ContainerIndustrialWorkbench.this.base.craftingGrid.clear();
      }
   };
   protected final Container craftResult = new ResultContainer();
   protected final Slot[] outputs = new Slot[3];
   public final Player player;
   public final int indexOutput;
   public final int indexGridStart;
   public final int indexGridEnd;
   public final int indexBufferStart;
   public final int indexBufferEnd;
   public final int indexOutputHammer;
   public final int indexOutputCutter;
   public static final int WIDTH = 194;
   public static final int HEIGHT = 228;

   public ContainerIndustrialWorkbench(Player player, TileEntityIndustrialWorkbench tileEntity) {
      super(player, tileEntity, 228);
      this.player = player;
      this.indexOutput = this.slots.size();
      this.outputs[0] = this.addSlot(new ResultSlot(player, this.craftMatrix, this.craftResult, 0, 124, 61) {
         {
            Objects.requireNonNull(ContainerIndustrialWorkbench.this);
         }

         protected void checkTakeAchievements(ItemStack stack) {
            if (IC2.platform.isRendering()) {
               IC2.network.get(false).sendContainerEvent(ContainerIndustrialWorkbench.this, "craft");
            } else {
               ContainerIndustrialWorkbench.this.onContainerEvent("craft");
            }

            super.checkTakeAchievements(stack);
         }

         public void onTake(Player thePlayer, ItemStack stack) {
            if (thePlayer.level().isClientSide() || NativeCrafting.find(ContainerIndustrialWorkbench.this.craftMatrix, thePlayer.level()) != null) {
               super.onTake(thePlayer, stack);
            }
         }
      });
      this.indexGridStart = this.slots.size();

      for (int y = 0; y < 3; y++) {
         for (int x = 0; x < 3; x++) {
            this.addSlot(new SlotInvSlot(tileEntity.craftingGrid, x + y * 3, 30 + x * 18, 43 + y * 18) {
               {
                  Objects.requireNonNull(ContainerIndustrialWorkbench.this);
               }

               public void setChanged() {
                  super.setChanged();
                  ContainerIndustrialWorkbench.this.slotsChanged(ContainerIndustrialWorkbench.this.craftMatrix);
               }
            });
         }
      }

      this.indexGridEnd = this.slots.size();
      this.indexBufferStart = this.slots.size();

      for (int y = 0; y < 2; y++) {
         for (int x = 0; x < 9; x++) {
            this.addSlot(new SlotInvSlot(tileEntity.craftingStorage, x + y * 9, 8 + x * 18, 106 + y * 18));
         }
      }

      this.indexBufferEnd = this.slots.size();
      this.addSlot(new SlotInvSlot(tileEntity.leftCrafting.tool, 0, 7, 17));
      this.addSlot(new SlotInvSlot(tileEntity.leftCrafting.input, 0, 25, 17));
      this.indexOutputHammer = this.slots.size();
      this.outputs[1] = this.addSlot(new ResultSlot(player, tileEntity.leftCrafting.crafting, tileEntity.leftCrafting.resultInv, 0, 69, 17));
      this.addSlot(new SlotInvSlot(tileEntity.rightCrafting.tool, 0, 91, 17));
      this.addSlot(new SlotInvSlot(tileEntity.rightCrafting.input, 0, 109, 17));
      this.indexOutputCutter = this.slots.size();
      this.outputs[2] = this.addSlot(new ResultSlot(player, tileEntity.rightCrafting.crafting, tileEntity.rightCrafting.resultInv, 0, 153, 17));
      this.slotsChanged(this.craftMatrix);
   }

   @Override
   public void onContainerEvent(String event) {
      if ("craft".equals(event)) {
         this.broadcastChanges();
         this.base.rebalance();
         this.broadcastChanges();
      } else if ("clear".equals(event)) {
         this.broadcastChanges();
         this.base.clear(this.player);
         this.broadcastChanges();
      }

      super.onContainerEvent(event);
   }

   public void slotsChanged(Container inventory) {
      this.craftResult.setItem(0, NativeCrafting.result(this.craftMatrix, this.base.getLevel()));
   }

   public boolean func_94530_a(ItemStack stack, Slot slot) {
      for (Slot output : this.outputs) {
         if (slot.container == output.container) {
            return false;
         }
      }

      return super.canTakeItemForPickAll(stack, slot);
   }

   @Override
   protected ItemStack handlePlayerSlotShiftClick(Player player, ItemStack sourceItemStack) {
      Tuple.T2<List<ItemStack>, ? extends TIntCollection> changes = StackUtil.balanceStacks(this.craftMatrix, sourceItemStack);
      TIntIterator iter = changes.b.iterator();

      while (iter.hasNext()) {
         int currentSlot = iter.next();
         ((Slot)this.slots.get(currentSlot + 37)).setChanged();
      }

      return !changes.a.isEmpty() ? super.handlePlayerSlotShiftClick(player, changes.a.get(0)) : StackUtil.emptyStack;
   }

   @Override
   protected ItemStack handleGUISlotShiftClick(Player player, ItemStack sourceItemStack) {
      ItemStack start = sourceItemStack.copy();
      Slot craftingSlot = null;

      for (Slot slot : this.outputs) {
         if (slot.getItem() == sourceItemStack) {
            craftingSlot = slot;
            break;
         }
      }

      boolean isOutput = craftingSlot != null;
      boolean isBuffer = false;

      for (int i = this.indexBufferStart; i < this.indexBufferEnd; i++) {
         Slot slotx = (Slot)this.slots.get(i);
         if (slotx.getItem() == sourceItemStack) {
            isBuffer = true;
            break;
         }
      }

      for (int run = 0; run < 2 && !StackUtil.isEmpty(sourceItemStack); run++) {
         ListIterator<Slot> it = this.slots.listIterator(this.slots.size());

         while (it.hasPrevious()) {
            Slot targetSlot = it.previous();
            if ((
                  targetSlot.container == player.getInventory()
                     || !isBuffer && targetSlot.index >= this.indexBufferStart && targetSlot.index < this.indexBufferEnd
               )
               && isValidTargetSlot(targetSlot, sourceItemStack, run == 1, false)) {
               sourceItemStack = this.transfer(sourceItemStack, targetSlot);
               if (StackUtil.isEmpty(sourceItemStack)) {
                  if (isOutput) {
                     craftingSlot.onQuickCraft(sourceItemStack, start);
                     craftingSlot.onTake(player, start);
                     if (craftingSlot.hasItem() && StackUtil.checkItemEquality(craftingSlot.getItem(), start)) {
                        sourceItemStack = craftingSlot.getItem();
                        start = sourceItemStack.copy();

                        assert it.hasNext();

                        it.next();
                        continue;
                     }
                  }
                  break;
               }
            }
         }
      }

      return sourceItemStack;
   }
}
