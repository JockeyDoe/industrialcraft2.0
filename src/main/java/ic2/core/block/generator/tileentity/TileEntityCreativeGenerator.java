package ic2.core.block.generator.tileentity;

import ic2.api.energy.event.EnergyTileLoadEvent;
import ic2.api.energy.event.EnergyTileUnloadEvent;
import ic2.api.energy.tile.IEnergyAcceptor;
import ic2.api.energy.tile.IMultiEnergySource;
import ic2.core.block.TileEntityBlock;
import ic2.core.profile.NotClassic;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.common.NeoForge;

@NotClassic
public class TileEntityCreativeGenerator extends TileEntityBlock implements IMultiEnergySource {
   @Override
   public double getOfferedEnergy() {
      return Double.POSITIVE_INFINITY;
   }

   @Override
   public void drawEnergy(double amount) {
   }

   @Override
   public int getSourceTier() {
      return 1;
   }

   @Override
   public boolean emitsEnergyTo(IEnergyAcceptor receiver, Direction side) {
      return true;
   }

   @Override
   public boolean sendMultipleEnergyPackets() {
      return true;
   }

   @Override
   public int getMultipleEnergyPacketAmount() {
      return 10;
   }

   @Override
   protected void onLoaded() {
      super.onLoaded();
      if (!this.getLevel().isClientSide()) {
         NeoForge.EVENT_BUS.post(new EnergyTileLoadEvent(this));
      }
   }

   @Override
   protected void onUnloaded() {
      if (!this.getLevel().isClientSide()) {
         NeoForge.EVENT_BUS.post(new EnergyTileUnloadEvent(this));
      }

      super.onUnloaded();
   }
}
