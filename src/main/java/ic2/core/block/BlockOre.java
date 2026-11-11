package ic2.core.block;

import ic2.core.block.type.ResourceBlock;
import ic2.core.ref.BlockMaterial;
import ic2.core.ref.BlockName;
import net.minecraft.world.level.block.state.BlockState;

public class BlockOre extends BlockMultiID<ResourceBlock> {
   public static BlockOre create() {
      return BlockMultiID.create(BlockOre.class, ResourceBlock.class);
   }

   public BlockOre() {
      super(BlockName.resource, BlockMaterial.ROCK);
   }

   public int func_180651_a(BlockState state) {
      return this.func_176201_c(state);
   }
}
