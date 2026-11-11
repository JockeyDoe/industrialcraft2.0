package ic2.core.block;

import ic2.api.energy.tile.IHeatSource;
import ic2.core.IC2;
import ic2.core.network.GuiSynced;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;

public abstract class TileEntityHeatSourceInventory extends TileEntityInventory implements IHeatSource {
   @GuiSynced
   protected int transmitHeat;
   @GuiSynced
   protected int maxHeatEmitpeerTick;
   protected int HeatBuffer;

   @Override
   protected void updateEntityServer() {
      super.updateEntityServer();
      int amount = this.getMaxHeatEmittedPerTick() - this.HeatBuffer;
      if (amount > 0) {
         this.addtoHeatBuffer(this.fillHeatBuffer(amount));
      }
   }

   @Override
   public int maxrequestHeatTick(Direction directionFrom) {
      return this.getConnectionBandwidth(directionFrom);
   }

   @Override
   public int getConnectionBandwidth(Direction side) {
      return this.facingMatchesDirection(side) ? this.getMaxHeatEmittedPerTick() : 0;
   }

   @Override
   public int requestHeat(Direction directionFrom, int requestheat) {
      return this.drawHeat(directionFrom, requestheat, false);
   }

   @Override
   public int drawHeat(Direction side, int request, boolean simulate) {
      if (this.facingMatchesDirection(side)) {
         int heatBuffer = this.getHeatBuffer();
         if (heatBuffer >= request) {
            if (!simulate) {
               this.setHeatBuffer(heatBuffer - request);
               this.transmitHeat = request;
            }

            return request;
         } else {
            if (!simulate) {
               this.transmitHeat = heatBuffer;
               this.setHeatBuffer(0);
            }

            return heatBuffer;
         }
      } else {
         return 0;
      }
   }

   @Override
   public void loadAdditional(CompoundTag nbtTagCompound) {
      super.loadAdditional(nbtTagCompound);
      this.HeatBuffer = nbtTagCompound.getIntOr("HeatBuffer", 0);
   }

   @Override
   public CompoundTag saveAdditional(CompoundTag nbt) {
      super.saveAdditional(nbt);
      nbt.putInt("HeatBuffer", this.HeatBuffer);
      return nbt;
   }

   @Override
   public void setChanged() {
      super.setChanged();
      if (IC2.platform.isSimulating()) {
         this.maxHeatEmitpeerTick = this.getMaxHeatEmittedPerTick();
      }
   }

   @Override
   protected void onLoaded() {
      super.onLoaded();
      if (IC2.platform.isSimulating()) {
         this.maxHeatEmitpeerTick = this.getMaxHeatEmittedPerTick();
      }
   }

   public boolean facingMatchesDirection(Direction direction) {
      return direction == this.getFacing();
   }

   public int getHeatBuffer() {
      return this.HeatBuffer;
   }

   public void setHeatBuffer(int HeatBuffer) {
      this.HeatBuffer = HeatBuffer;
   }

   public void addtoHeatBuffer(int heat) {
      this.setHeatBuffer(this.getHeatBuffer() + heat);
   }

   public int gettransmitHeat() {
      return this.transmitHeat;
   }

   public String getOutput() {
      return this.gettransmitHeat() + " / " + this.getMaxHeatEmittedPerTick();
   }

   protected abstract int fillHeatBuffer(int var1);

   public abstract int getMaxHeatEmittedPerTick();
}
