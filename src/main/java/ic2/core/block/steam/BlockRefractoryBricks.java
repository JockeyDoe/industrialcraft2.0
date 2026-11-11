package ic2.core.block.steam;

import ic2.core.block.BlockBase;
import ic2.core.block.state.BlockStates;
import ic2.core.ref.BlockMaterial;
import ic2.core.ref.BlockName;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;

public class BlockRefractoryBricks extends BlockBase {
   public BlockRefractoryBricks() {
      super(BlockName.refractory_bricks, BlockMaterial.ROCK);
      this.func_149711_c(2.0F);
      this.func_149752_b(10.0F);
      this.setHarvestLevel("pickaxe", 0);
   }

   @Override
   protected StateDefinition func_180661_e() {
      return BlockStates.definition(this, new Property[0]);
   }

   public boolean isFlammable(BlockGetter world, BlockPos pos, Direction face) {
      return false;
   }
}
