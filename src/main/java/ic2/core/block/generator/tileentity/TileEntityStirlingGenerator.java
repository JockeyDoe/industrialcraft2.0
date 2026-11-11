package ic2.core.block.generator.tileentity;

import ic2.api.energy.tile.IHeatSource;
import ic2.core.init.MainConfig;
import ic2.core.profile.NotClassic;
import ic2.core.util.ConfigUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

@NotClassic
public class TileEntityStirlingGenerator extends TileEntityConversionGenerator {
   private final double productionpeerheat = (double)(0.5F * ConfigUtil.getFloat(MainConfig.get(), "balance/energy/generator/Stirling"));
   protected IHeatSource source;

   @Override
   protected void onLoaded() {
      super.onLoaded();
      this.updateSource();
   }

   @Override
   protected void setFacing(Direction facing) {
      super.setFacing(facing);
      this.updateSource();
   }

   @Override
   protected void onNeighborChange(Block neighbor, BlockPos neighborPos) {
      super.onNeighborChange(neighbor, neighborPos);
      if (this.getBlockPos().relative(this.getFacing()).equals(neighborPos)) {
         this.updateSource();
      }
   }

   protected void updateSource() {
      if (this.source == null || ((BlockEntity)this.source).isRemoved()) {
         BlockEntity te = this.getLevel().getBlockEntity(this.getBlockPos().relative(this.getFacing()));
         if (te instanceof IHeatSource) {
            this.source = (IHeatSource)te;
         } else {
            this.source = null;
         }
      }
   }

   @Override
   protected int getEnergyAvailable() {
      if (this.source == null) {
         return 0;
      } else {
         assert !((BlockEntity)this.source).isRemoved();

         return this.source.drawHeat(this.getFacing().getOpposite(), this.source.getConnectionBandwidth(this.getFacing().getOpposite()), true);
      }
   }

   @Override
   protected void drawEnergyAvailable(int amount) {
      if (this.source != null) {
         assert !((BlockEntity)this.source).isRemoved();

         this.source.drawHeat(this.getFacing().getOpposite(), amount, false);
      } else {
         assert false;
      }
   }

   @Override
   protected double getMultiplier() {
      return this.productionpeerheat;
   }
}
