package ic2.core.block.machine.tileentity;

import ic2.api.network.INetworkClientTileEntityEventListener;
import ic2.core.ContainerBase;
import ic2.core.IC2;
import ic2.core.IHasGui;
import ic2.core.block.comp.Fluids;
import ic2.core.block.comp.Ic2FluidTank;
import ic2.core.block.invslot.InvSlot;
import ic2.core.block.invslot.InvSlotConsumableLiquid;
import ic2.core.block.invslot.InvSlotConsumableLiquidByTank;
import ic2.core.block.invslot.InvSlotOutput;
import ic2.core.block.machine.container.ContainerFluidRegulator;
import ic2.core.block.machine.gui.GuiFluidRegulator;
import ic2.core.init.Localization;
import ic2.core.network.GuiSynced;
import ic2.core.profile.NotClassic;
import ic2.core.util.LiquidUtil;
import java.util.Collections;
import java.util.EnumSet;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@NotClassic
public class TileEntityFluidRegulator extends TileEntityElectricMachine implements IHasGui, INetworkClientTileEntityEventListener {
   private int mode;
   private int updateTicker;
   private int outputmb;
   private boolean newActive;
   public final InvSlotOutput wasseroutputSlot;
   public final InvSlotConsumableLiquidByTank wasserinputSlot;
   @GuiSynced
   protected final Fluids.InternalFluidTank fluidTank;
   protected final Fluids fluids = this.addComponent(new Fluids(this));

   public TileEntityFluidRegulator() {
      super(10000, 4);
      this.fluidTank = this.fluids.addTank("fluidTank", 10000, InvSlot.Access.NONE);
      this.wasserinputSlot = new InvSlotConsumableLiquidByTank(
         this, "wasserinputSlot", InvSlot.Access.I, 1, InvSlot.InvSide.TOP, InvSlotConsumableLiquid.OpType.Drain, this.fluidTank
      );
      this.wasseroutputSlot = new InvSlotOutput(this, "wasseroutputSlot", 1);
      this.newActive = false;
      this.outputmb = 0;
      this.mode = 0;
      this.updateTicker = IC2.random.nextInt(this.getTickRate());
   }

   @Override
   public void loadAdditional(CompoundTag nbt) {
      super.loadAdditional(nbt);
      this.outputmb = nbt.getIntOr("outputmb", 0);
      this.mode = nbt.getIntOr("mode", 0);
   }

   @Override
   public CompoundTag saveAdditional(CompoundTag nbt) {
      super.saveAdditional(nbt);
      nbt.putInt("outputmb", this.outputmb);
      nbt.putInt("mode", this.mode);
      return nbt;
   }

   @Override
   protected void onLoaded() {
      super.onLoaded();
      this.updateConnectivity();
   }

   @Override
   public void setFacing(Direction side) {
      super.setFacing(side);
      this.updateConnectivity();
   }

   private void updateConnectivity() {
      this.fluids.changeConnectivity(this.fluidTank, EnumSet.complementOf(EnumSet.of(this.getFacing())), Collections.emptySet());
   }

   @Override
   protected void updateEntityServer() {
      super.updateEntityServer();
      this.wasserinputSlot.processIntoTank(this.fluidTank, this.wasseroutputSlot);
      if (this.updateTicker++ % this.getTickRate() == 0 || this.mode != 0) {
         this.newActive = this.work();
         if (this.getActive() != this.newActive) {
            this.setActive(this.newActive);
         }
      }
   }

   private boolean work() {
      if (this.outputmb == 0) {
         return false;
      } else if (this.energy.getEnergy() < 10.0) {
         return false;
      } else if (this.fluidTank.getFluidAmount() <= 0) {
         return false;
      } else {
         Direction dir = this.getFacing();
         BlockEntity te = this.getLevel().getBlockEntity(this.getBlockPos().relative(dir));
         Direction side = dir.getOpposite();
         if (LiquidUtil.isFluidTile(te, side)) {
            int amount = LiquidUtil.fillTile(te, side, this.fluidTank.drainInternal(this.outputmb, false), false);
            if (amount > 0) {
               this.fluidTank.drainInternal(this.outputmb, true);
               this.energy.useEnergy(10.0);
               return true;
            }
         }

         return false;
      }
   }

   @Override
   public void onNetworkEvent(Player player, int event) {
      if (event != 1001 && event != 1002) {
         this.outputmb += event;
         if (this.outputmb > 1000) {
            this.outputmb = 1000;
         }

         if (this.outputmb < 0) {
            this.outputmb = 0;
         }
      } else {
         if (event == 1001 && this.mode == 0) {
            this.mode = 1;
         }

         if (event == 1002 && this.mode == 1) {
            this.mode = 0;
         }
      }
   }

   public int getTickRate() {
      return 20;
   }

   @Override
   public ContainerBase<TileEntityFluidRegulator> getGuiContainer(Player player) {
      return new ContainerFluidRegulator(player, this);
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public Screen getGui(Player player, boolean isAdmin) {
      return new GuiFluidRegulator(new ContainerFluidRegulator(player, this));
   }

   @Override
   public void onGuiClosed(Player player) {
   }

   public int gaugeLiquidScaled(int i, int tank) {
      switch (tank) {
         case 0:
            if (this.fluidTank.getFluidAmount() <= 0) {
               return 0;
            }

            return this.fluidTank.getFluidAmount() * i / this.fluidTank.getCapacity();
         default:
            return 0;
      }
   }

   public int getoutputmb() {
      return this.outputmb;
   }

   public String getmodegui() {
      switch (this.mode) {
         case 0:
            return Localization.translate("ic2.generic.text.sec");
         case 1:
            return Localization.translate("ic2.generic.text.tick");
         default:
            return "";
      }
   }

   public Ic2FluidTank getFluidTank() {
      return this.fluidTank;
   }
}
