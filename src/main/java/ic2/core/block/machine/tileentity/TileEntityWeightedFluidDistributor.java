package ic2.core.block.machine.tileentity;

import ic2.api.network.ClientModifiable;
import ic2.core.ContainerBase;
import ic2.core.IC2;
import ic2.core.block.machine.container.ContainerWeightedFluidDistributor;
import ic2.core.block.machine.gui.GuiWeightedFluidDistributor;
import ic2.core.profile.NotClassic;
import ic2.core.util.LiquidUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@NotClassic
public class TileEntityWeightedFluidDistributor extends TileEntityFluidDistributor implements IWeightedDistributor {
   @ClientModifiable
   protected List<Direction> priority = new ArrayList<>(5);

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
   protected void updateConnectivity() {
      if (!this.getLevel().isClientSide() && !this.priority.isEmpty() && this.priority.remove(this.getFacing())) {
         this.updatePriority(true);
      }

      this.fluids.changeConnectivity(this.fluidTank, Collections.singleton(this.getFacing()), Collections.emptySet());
   }

   @Override
   protected void moveFluid() {
      if (!this.priority.isEmpty()) {
         int tankAmount = this.fluidTank.getFluidAmount();

         for (Direction dir : this.priority) {
            assert dir != this.getFacing();

            BlockEntity target = this.getLevel().getBlockEntity(this.getBlockPos().relative(dir));
            Direction side = dir.getOpposite();
            if (LiquidUtil.isFluidTile(target, side)) {
               int amount = LiquidUtil.fillTile(target, side, this.fluidTank.getFluid(), false);
               if (amount > 0) {
                  tankAmount -= amount;
                  this.fluidTank.drainInternal(amount, true);
                  if (tankAmount <= 0) {
                     break;
                  }
               }
            }
         }
      }
   }

   @Override
   public ContainerBase<?> getGuiContainer(Player player) {
      return new ContainerWeightedFluidDistributor(player, this);
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public Screen getGui(Player player, boolean isAdmin) {
      return new GuiWeightedFluidDistributor(new ContainerWeightedFluidDistributor(player, this));
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
   public void onNetworkEvent(Player player, int event) {
      int position = event / 10;
      Direction facing = Direction.from3DDataValue(event % 10 & 6);

      assert position >= 0 && position <= this.priority.size() : "Position was " + position;

      assert facing != this.getFacing();

      if (position == this.priority.size()) {
         this.priority.add(facing);
      } else {
         this.priority.set(position, facing);
      }
   }
}
