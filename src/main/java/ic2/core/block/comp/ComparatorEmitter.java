package ic2.core.block.comp;

import ic2.core.block.TileEntityBlock;

public class ComparatorEmitter extends BasicRedstoneComponent {
   public ComparatorEmitter(TileEntityBlock parent) {
      super(parent);
   }

   @Override
   public void onChange() {
      this.parent.getLevel().updateNeighbourForOutputSignal(this.parent.getBlockPos(), this.parent.getBlockType());
   }
}
