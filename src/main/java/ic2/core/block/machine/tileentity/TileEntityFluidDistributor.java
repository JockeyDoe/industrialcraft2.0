package ic2.core.block.machine.tileentity;

import ic2.api.network.INetworkClientTileEntityEventListener;
import ic2.core.ContainerBase;
import ic2.core.IHasGui;
import ic2.core.block.TileEntityInventory;
import ic2.core.block.comp.Fluids;
import ic2.core.block.invslot.InvSlot;
import ic2.core.block.invslot.InvSlotConsumableLiquid;
import ic2.core.block.invslot.InvSlotConsumableLiquidByTank;
import ic2.core.block.invslot.InvSlotOutput;
import ic2.core.block.machine.container.ContainerFluidDistributor;
import ic2.core.block.machine.gui.GuiFluidDistributor;
import ic2.core.network.GuiSynced;
import ic2.core.profile.NotClassic;
import ic2.core.util.LiquidUtil;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.fluids.FluidStack;

@NotClassic
public class TileEntityFluidDistributor extends TileEntityInventory implements IHasGui, INetworkClientTileEntityEventListener {
   public final InvSlotConsumableLiquidByTank inputSlot;
   public final InvSlotOutput OutputSlot;
   @GuiSynced
   public final Fluids.InternalFluidTank fluidTank;
   protected final Fluids fluids = this.addComponent(new Fluids(this));

   public TileEntityFluidDistributor() {
      this.fluidTank = this.fluids.addTank("fluidTank", 1000);
      this.inputSlot = new InvSlotConsumableLiquidByTank(
         this, "inputSlot", InvSlot.Access.I, 1, InvSlot.InvSide.BOTTOM, InvSlotConsumableLiquid.OpType.Fill, this.fluidTank
      );
      this.OutputSlot = new InvSlotOutput(this, "OutputSlot", 1);
   }

   @Override
   protected void onLoaded() {
      super.onLoaded();
      this.updateConnectivity();
   }

   @Override
   public void setActive(boolean val) {
      super.setActive(val);
      this.updateConnectivity();
   }

   @Override
   public void setFacing(Direction facing) {
      super.setFacing(facing);
      this.updateConnectivity();
   }

   protected void updateConnectivity() {
      EnumSet<Direction> acceptingSides = EnumSet.of(this.getFacing());
      if (this.getActive()) {
         acceptingSides = EnumSet.complementOf(acceptingSides);
      }

      this.fluids.changeConnectivity(this.fluidTank, acceptingSides, Collections.emptySet());
   }

   @Override
   protected void updateEntityServer() {
      super.updateEntityServer();
      this.inputSlot.processFromTank(this.fluidTank, this.OutputSlot);
      if (this.fluidTank.getFluidAmount() > 0) {
         this.moveFluid();
      }
   }

   protected void moveFluid() {
      Level world = this.getLevel();
      if (this.getActive()) {
         BlockEntity target = world.getBlockEntity(this.getBlockPos().relative(this.getFacing()));
         Direction side = this.getFacing().getOpposite();
         if (LiquidUtil.isFluidTile(target, side)) {
            int amount = LiquidUtil.fillTile(target, side, this.fluidTank.getFluid(), false);
            if (amount > 0) {
               this.fluidTank.drainInternal(amount, true);
            }
         }
      } else {
         Map<Direction, BlockEntity> acceptingNeighbors = new EnumMap<>(Direction.class);
         int acceptedVolume = 0;

         for (Direction dir : Direction.values()) {
            if (dir != this.getFacing()) {
               BlockEntity target = world.getBlockEntity(this.getBlockPos().relative(dir));
               Direction side = dir.getOpposite();
               if (LiquidUtil.isFluidTile(target, side)) {
                  int amount = LiquidUtil.fillTile(target, side, this.fluidTank.getFluid(), true);
                  if (amount > 0) {
                     acceptingNeighbors.put(dir, target);
                     acceptedVolume += amount;
                  }
               }
            }
         }

         while (!acceptingNeighbors.isEmpty()) {
            int amount = Math.min(acceptedVolume, this.fluidTank.getFluidAmount());
            if (amount <= 0) {
               break;
            }

            amount /= acceptingNeighbors.size();
            if (amount <= 0) {
               for (Entry<Direction, BlockEntity> entry : acceptingNeighbors.entrySet()) {
                  BlockEntity target = entry.getValue();
                  Direction side = entry.getKey().getOpposite();
                  FluidStack fs = this.fluidTank.getFluid();
                  if (fs == null) {
                     return;
                  }

                  fs = fs.copy();
                  fs.setAmount(Math.min(acceptedVolume, fs.getAmount()));
                  if (fs.getAmount() <= 0) {
                     return;
                  }

                  int cAmount = LiquidUtil.fillTile(target, side, fs, false);
                  this.fluidTank.drainInternal(cAmount, true);
                  acceptedVolume -= cAmount;
               }
               break;
            }

            Iterator<Entry<Direction, BlockEntity>> it = acceptingNeighbors.entrySet().iterator();

            while (it.hasNext()) {
               Entry<Direction, BlockEntity> entry = it.next();
               BlockEntity targetx = entry.getValue();
               Direction sidex = entry.getKey().getOpposite();
               FluidStack fsx = this.fluidTank.getFluid();
               if (fsx == null) {
                  break;
               }

               fsx = fsx.copy();
               if (fsx.getAmount() <= 0) {
                  break;
               }

               fsx.setAmount(Math.min(amount, fsx.getAmount()));
               int cAmount = LiquidUtil.fillTile(targetx, sidex, fsx, false);
               this.fluidTank.drainInternal(cAmount, true);
               acceptedVolume -= cAmount;
               if (cAmount < fsx.getAmount()) {
                  it.remove();
               }
            }
         }
      }
   }

   @Override
   public void onNetworkEvent(Player player, int event) {
      this.setActive(!this.getActive());
   }

   @Override
   public ContainerBase<?> getGuiContainer(Player player) {
      return new ContainerFluidDistributor(player, this);
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public Screen getGui(Player player, boolean isAdmin) {
      return new GuiFluidDistributor(new ContainerFluidDistributor(player, this));
   }

   @Override
   public void onGuiClosed(Player player) {
   }
}
