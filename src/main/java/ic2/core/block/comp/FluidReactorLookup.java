package ic2.core.block.comp;

import ic2.core.block.TileEntityBlock;
import ic2.core.block.reactor.tileentity.TileEntityNuclearReactorElectric;
import ic2.core.util.WorldSearchUtil;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public class FluidReactorLookup extends TileEntityComponent {
   private TileEntityNuclearReactorElectric reactor;
   private long lastReactorUpdate;

   public FluidReactorLookup(TileEntityBlock parent) {
      super(parent);
   }

   public TileEntityNuclearReactorElectric getReactor() {
      long time = this.parent.getLevel().getGameTime();
      if (time != this.lastReactorUpdate) {
         this.updateReactor();
         this.lastReactorUpdate = time;
      } else if (this.reactor != null && (this.reactor.isRemoved() || !this.reactor.isFluidCooled())) {
         this.reactor = null;
      }

      return this.reactor;
   }

   private void updateReactor() {
      int dist = 2;
      Level world = this.parent.getLevel();
      BlockPos pos = this.parent.getBlockPos();
      if (!world.isAreaLoaded(pos, 2)) {
         this.reactor = null;
      } else {
         if (this.reactor != null
            && !this.reactor.isRemoved()
            && this.reactor.isFluidCooled()
            && this.reactor.getLevel() == world
            && world.getBlockEntity(this.reactor.getBlockPos()) == this.reactor) {
            BlockPos reactorPos = this.reactor.getBlockPos();
            int dx = Math.abs(pos.getX() - reactorPos.getX());
            int dy = Math.abs(pos.getY() - reactorPos.getY());
            int dz = Math.abs(pos.getZ() - reactorPos.getZ());
            if (dx <= 2 && dy <= 2 && dz <= 2 && (dx == 2 || dy == 2 || dz == 2)) {
               return;
            }
         }

         this.reactor = null;
         WorldSearchUtil.findTileEntities(world, pos, 2, new WorldSearchUtil.ITileEntityResultHandler() {
            {
               Objects.requireNonNull(FluidReactorLookup.this);
            }

            @Override
            public boolean onMatch(BlockEntity te) {
               if (te instanceof TileEntityNuclearReactorElectric cReactor && cReactor.isFluidCooled()) {
                  FluidReactorLookup.this.reactor = cReactor;
                  return true;
               } else {
                  return false;
               }
            }
         });
      }
   }
}
