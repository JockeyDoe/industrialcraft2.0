package ic2.core.block.machine.tileentity;

import gnu.trove.TIntCollection;
import ic2.core.ContainerBase;
import ic2.core.IHasGui;
import ic2.core.block.IInventorySlotHolder;
import ic2.core.block.TileEntityInventory;
import ic2.core.block.invslot.InvSlot;
import ic2.core.block.invslot.InvSlotConsumable;
import ic2.core.block.invslot.InvSlotConsumableOreDict;
import ic2.core.block.machine.container.ContainerIndustrialWorkbench;
import ic2.core.block.machine.gui.GuiIndustrialWorkbench;
import ic2.core.profile.NotClassic;
import ic2.core.recipe.NativeCrafting;
import ic2.core.ref.ItemName;
import ic2.core.util.IInventoryInvSlot;
import ic2.core.util.InventorySlotCrafting;
import ic2.core.util.ItemData;
import ic2.core.util.StackUtil;
import ic2.core.util.Tuple;
import java.util.List;
import java.util.Objects;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Direction;
import net.minecraft.nbt.ByteTag;
import net.minecraft.world.Container;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@NotClassic
public class TileEntityIndustrialWorkbench extends TileEntityInventory implements IHasGui {
   public final InvSlot craftingGrid = new InvSlot(this, "crafting", InvSlot.Access.NONE, 9);
   public final InvSlot craftingStorage = new InvSlot(this, "craftingStorage", InvSlot.Access.I, 18);
   public final TileEntityIndustrialWorkbench.InvSlotCraftingCombo leftCrafting = new TileEntityIndustrialWorkbench.InvSlotCraftingCombo(
      this, "left", "craftingToolForgeHammer"
   );
   public final TileEntityIndustrialWorkbench.InvSlotCraftingCombo rightCrafting = new TileEntityIndustrialWorkbench.InvSlotCraftingCombo(
      this, "right", "craftingToolWireCutter"
   );

   @Override
   public void onPlaced(ItemStack stack, LivingEntity placer, Direction facing) {
      super.onPlaced(stack, placer, facing);
      if (!ItemData.has(stack) || !ItemData.read(stack).contains("PLACED")) {
         this.leftCrafting.tool.put(ItemName.forge_hammer.getItemStack());
         this.rightCrafting.tool.put(ItemName.cutter.getItemStack());
      }
   }

   @Override
   protected ItemStack adjustDrop(ItemStack drop, boolean wrench) {
      drop = super.adjustDrop(drop, wrench);
      ItemData.put(drop, "PLACED", ByteTag.valueOf(true));
      return drop;
   }

   public void rebalance() {
      if (!this.craftingGrid.isEmpty()) {
         boolean changed = false;
         Container crafting = new IInventoryInvSlot(this.craftingGrid);
         int index = 0;

         for (int size = this.craftingStorage.size(); index < size; index++) {
            if (!this.craftingStorage.isEmpty(index)) {
               Tuple.T2<List<ItemStack>, ? extends TIntCollection> changes = StackUtil.balanceStacks(crafting, this.craftingStorage.get(index));
               if (!changes.b.isEmpty()) {
                  changed = true;
                  ItemStack toPut = changes.a.isEmpty() ? StackUtil.emptyStack : changes.a.get(0);
                  this.craftingStorage.put(index, toPut);
               }
            }
         }

         if (changed) {
            this.setChanged();
         }
      }
   }

   private static int getPossible(int max, ItemStack existing, ItemStack in) {
      int amount = Math.min(max, in.isStackable() ? in.getMaxStackSize() : 1);
      if (!StackUtil.isEmpty(existing)) {
         if (!StackUtil.checkItemEqualityStrict(existing, in)) {
            return 0;
         }

         amount -= StackUtil.getSize(existing);
      }

      return Math.min(amount, StackUtil.getSize(in));
   }

   private static ItemStack transfer(InvSlot slot, ItemStack gridItem, boolean allowEmpty) {
      for (int index = 0; index < slot.size(); index++) {
         ItemStack stack = slot.get(index);
         int amount = getPossible(slot.getStackSizeLimit(), stack, gridItem);
         if (amount >= 1) {
            if (StackUtil.isEmpty(stack)) {
               if (!allowEmpty) {
                  continue;
               }

               slot.put(index, StackUtil.copyWithSize(gridItem, amount));
            } else {
               slot.put(index, StackUtil.incSize(stack, amount));
            }

            gridItem = StackUtil.decSize(gridItem, amount);
            if (StackUtil.isEmpty(gridItem)) {
               break;
            }
         }
      }

      return gridItem;
   }

   public void clear(Player player) {
      if (!this.craftingGrid.isEmpty()) {
         label39:
         for (int index = 0; index < this.craftingGrid.size(); index++) {
            if (!this.craftingGrid.isEmpty(index)) {
               ItemStack stack = this.craftingGrid.get(index);

               for (int pass = 0; pass < 2; pass++) {
                  stack = transfer(this.craftingStorage, stack, pass == 1);
                  if (StackUtil.isEmpty(stack)) {
                     this.craftingGrid.clear(index);
                     continue label39;
                  }
               }

               if (StackUtil.storeInventoryItem(stack, player, false)) {
                  this.craftingGrid.clear(index);
               } else {
                  this.craftingGrid.put(stack);
               }
            }
         }
      }
   }

   @Override
   public ContainerBase<TileEntityIndustrialWorkbench> getGuiContainer(Player player) {
      return new ContainerIndustrialWorkbench(player, this);
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public Screen getGui(Player player, boolean isAdmin) {
      return new GuiIndustrialWorkbench(new ContainerIndustrialWorkbench(player, this));
   }

   @Override
   public void onGuiClosed(Player player) {
   }

   public static class InvSlotCraftingCombo {
      protected Recipe recipe;
      public final InvSlotConsumable input;
      public final InvSlotConsumableOreDict tool;
      public final CraftingContainer crafting = new InventorySlotCrafting(2, 1) {
         {
            Objects.requireNonNull(InvSlotCraftingCombo.this);
         }

         private InvSlot getCraftingSlot(int index) {
            switch (index) {
               case 0:
                  return InvSlotCraftingCombo.this.tool;
               case 1:
                  return InvSlotCraftingCombo.this.input;
               default:
                  throw new IllegalArgumentException("Invalid index: " + index);
            }
         }

         @Override
         protected ItemStack get(int index) {
            return this.getCraftingSlot(index).get();
         }

         @Override
         protected void put(int index, ItemStack stack) {
            this.getCraftingSlot(index).put(stack);
         }

         @Override
         public boolean isEmpty() {
            return InvSlotCraftingCombo.this.input.isEmpty() && InvSlotCraftingCombo.this.tool.isEmpty();
         }

         @Override
         public void clearContent() {
            InvSlotCraftingCombo.this.input.clear();
            InvSlotCraftingCombo.this.tool.clear();
         }
      };
      public final ResultContainer resultInv = new ResultContainer();

      public InvSlotCraftingCombo(TileEntityInventory base, String name, String tool) {
         this.input = new InvSlotConsumable(base, name + "Input", InvSlot.Access.I, 1, InvSlot.InvSide.ANY) {
            {
               Objects.requireNonNull(InvSlotCraftingCombo.this);
            }

            @Override
            public boolean accepts(ItemStack stack) {
               ItemStack prev = this.get();

               boolean var3;
               try {
                  this.put(stack);
                  var3 = InvSlotCraftingCombo.this.canProcess();
               } finally {
                  this.put(prev);
               }

               return var3;
            }

            @Override
            public void onChanged() {
               InvSlotCraftingCombo.this.resultInv.setItem(0, InvSlotCraftingCombo.this.getOutputStack());
            }
         };
         this.tool = new InvSlotConsumableOreDict(base, name + "Tool", InvSlot.Access.I, 1, InvSlot.InvSide.ANY, tool) {
            {
               Objects.requireNonNull(InvSlotCraftingCombo.this);
            }

            @Override
            public void onChanged() {
               InvSlotCraftingCombo.this.resultInv.setItem(0, InvSlotCraftingCombo.this.getOutputStack());
            }
         };
      }

      protected boolean canProcess() {
         if (!this.crafting.isEmpty()) {
            if (this.recipe != null && NativeCrafting.matches(this.recipe, this.crafting, this.tool.base.getParent().getLevel())) {
               return true;
            } else {
               this.recipe = NativeCrafting.find(this.crafting, this.tool.base.getParent().getLevel());
               return this.recipe != null;
            }
         } else {
            return false;
         }
      }

      public ItemStack getOutputStack() {
         return !this.canProcess() ? StackUtil.emptyStack : NativeCrafting.assemble(this.recipe, this.crafting);
      }
   }
}
