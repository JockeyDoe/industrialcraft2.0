package ic2.core.block.comp;

import ic2.core.block.TileEntityBlock;

public class RedstoneEmitter extends BasicRedstoneComponent {
   public RedstoneEmitter(TileEntityBlock parent) {
      super(parent);
   }

   @Override
   public void onChange() {
      this.parent.getLevel().updateNeighborsAt(this.parent.getBlockPos(), this.parent.getBlockType(), null);
   }
}
