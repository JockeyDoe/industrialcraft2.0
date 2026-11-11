package ic2.core.block.machine.tileentity;

import ic2.api.network.ClientModifiable;
import ic2.core.ContainerBase;
import ic2.core.IC2;
import ic2.core.IHasGui;
import ic2.core.block.TileEntityInventory;
import ic2.core.block.invslot.InvSlot;
import ic2.core.block.machine.container.ContainerWeightedItemDistributor;
import ic2.core.block.machine.gui.GuiWeightedItemDistributor;
import ic2.core.profile.NotClassic;
import ic2.core.util.StackUtil;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@NotClassic
public class TileEntityWeightedItemDistributor extends TileEntityInventory implements IHasGui, IWeightedDistributor {
   @ClientModifiable
   protected List<Direction> priority = new ArrayList<>(5);
   public final InvSlot buffer = new InvSlot(this, "buffer", InvSlot.Access.I, 9);

   @Override
   public CompoundTag saveAdditional(CompoundTag nbt) {
      super.saveAdditional(nbt);
      if (!this.priority.isEmpty()) {
         int[] indexes = new int[this.priority.size()];

         for (int i = 0; i < indexes.length; i++) {
            indexes[i] = this.priority.get(i).get3DDataValue();
         }

         nbt.putIntArray("priority", indexes);
      }

      return nbt;
   }

   @Override
   public void loadAdditional(CompoundTag nbt) {
      super.loadAdditional(nbt);
      int[] indexes = nbt.getIntArray("priority").orElseGet(() -> new int[0]);
      if (indexes.length > 0) {
         for (int index : indexes) {
            this.priority.add(Direction.from3DDataValue(index));
         }
      }
   }

   @Override
   public List<String> getNetworkedFields() {
      List<String> ret = super.getNetworkedFields();
      ret.add("priority");
      return ret;
   }

   @Override
   protected void onLoaded() {
      super.onLoaded();
      this.updateConnectivity();
   }

   @Override
   protected void setFacing(Direction facing) {
      super.setFacing(facing);
      this.updateConnectivity();
   }

   protected void updateConnectivity() {
      if (!this.getLevel().isClientSide() && !this.priority.isEmpty() && this.priority.remove(this.getFacing())) {
         this.updatePriority(true);
      }
   }

   @Override
   protected void updateEntityServer() {
      super.updateEntityServer();
      if (!this.priority.isEmpty() && !this.buffer.isEmpty()) {
         Level world = this.getLevel();
         boolean hasChanged = false;

         for (Direction facing : this.priority) {
            BlockEntity te = world.getBlockEntity(this.getBlockPos().relative(facing));
            Direction side = facing.getOpposite();
            if (StackUtil.isInventoryTile(te, side)) {
               boolean empty = true;

               for (int index = 0; index < this.buffer.size(); index++) {
                  if (!this.buffer.isEmpty(index)) {
                     ItemStack stack = this.buffer.get(index);
                     ItemStack transferStack = StackUtil.copy(stack);
                     int amount = StackUtil.putInInventory(te, side, transferStack, true);
                     if (amount > 0) {
                        amount = StackUtil.putInInventory(te, side, transferStack, false);
                        stack = StackUtil.decSize(stack, amount);
                        this.buffer.put(index, stack);
                        hasChanged = true;
                        empty &= StackUtil.isEmpty(stack);
                     }
                  }
               }

               if (hasChanged && empty) {
                  break;
               }
            }
         }

         if (hasChanged) {
            this.setChanged();
         }
      }
   }

   @Override
   public ContainerBase<?> getGuiContainer(Player player) {
      return new ContainerWeightedItemDistributor(player, this);
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public Screen getGui(Player player, boolean isAdmin) {
      return new GuiWeightedItemDistributor(new ContainerWeightedItemDistributor(player, this));
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public List<Direction> getPriority() {
      return this.priority;
   }

   @Override
   public void updatePriority(boolean server) {
      IC2.network.get(server).updateTileEntityField(this, "priority");
   }

   @Override
   public void onGuiClosed(Player player) {
   }
}
