package ic2.core.block.machine.container;

import gnu.trove.TIntCollection;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import ic2.core.block.machine.tileentity.TileEntityBatchCrafter;
import ic2.core.slot.SlotHologramSlot;
import ic2.core.slot.SlotInvSlot;
import ic2.core.util.StackUtil;
import ic2.core.util.Tuple;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class ContainerBatchCrafter extends ContainerElectricMachine<TileEntityBatchCrafter> {
   protected final TIntIntMap indexToSlot = new TIntIntHashMap();
   public static final short HEIGHT = 206;

   public ContainerBatchCrafter(Player player, TileEntityBatchCrafter tileEntity) {
      super(player, tileEntity, 206, 8, 62);

      for (int y = 0; y < 3; y++) {
         for (int x = 0; x < 3; x++) {
            this.addSlot(new SlotHologramSlot(tileEntity.craftingGrid, x + y * 3, 30 + x * 18, 17 + y * 18, 1, new SlotHologramSlot.ChangeCallback() {
               {
                  Objects.requireNonNull(ContainerBatchCrafter.this);
               }

               @Override
               public void onChanged(int index) {
                  if (ContainerBatchCrafter.this.base.hasLevel() && !ContainerBatchCrafter.this.base.getLevel().isClientSide()) {
                     ContainerBatchCrafter.this.base.matrixChange(index);
                  }
               }
            }));
         }
      }

      this.addSlot(new SlotInvSlot(tileEntity.craftingOutput, 0, 124, 35));

      for (int slot = 0; slot < 9; slot++) {
         this.indexToSlot.put(slot, this.addSlot(new SlotInvSlot(tileEntity.ingredientsRow[slot], 0, 8 + slot * 18, 84)).index);
         this.addSlot(new SlotInvSlot(tileEntity.containerOutput, slot, 8 + slot * 18, 102));
      }

      for (int slot = 0; slot < 4; slot++) {
         this.addSlot(new SlotInvSlot(tileEntity.upgradeSlot, slot, 152, 8 + slot * 18));
      }
   }

   @Override
   protected ItemStack handlePlayerSlotShiftClick(Player player, ItemStack sourceItemStack) {
      Tuple.T2<List<ItemStack>, ? extends TIntCollection> changes = StackUtil.balanceStacks(
         this.base.ingredients, this.base.acceptPredicate, StackUtil.getSlotsFromInv(this.base.ingredients), Collections.singleton(sourceItemStack)
      );
      TIntIterator iter = changes.b.iterator();

      while (iter.hasNext()) {
         int currentSlot = iter.next();
         ((Slot)this.slots.get(this.indexToSlot.get(currentSlot))).setChanged();
      }

      return changes.a.isEmpty() ? StackUtil.emptyStack : changes.a.get(0);
   }

   @Override
   public List<String> getNetworkedFields() {
      List<String> fields = super.getNetworkedFields();
      fields.add("guiProgress");
      fields.add("recipeOutput");
      return fields;
   }
}
