package ic2.core.block;

import ic2.core.block.state.IIdProvider;
import ic2.core.ref.BlockMaterial;
import ic2.core.ref.BlockName;
import java.util.Random;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.SpawnPlacementType;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class BlockTexGlass extends BlockMultiID<BlockTexGlass.GlassType> {
   public static BlockTexGlass create() {
      return BlockMultiID.create(BlockTexGlass.class, BlockTexGlass.GlassType.class);
   }

   private BlockTexGlass() {
      super(BlockName.glass, BlockMaterial.GLASS);
      this.func_149711_c(5.0F);
      this.func_149752_b(180.0F);
      this.func_149672_a(SoundType.GLASS);
   }

   public int func_149745_a(Random random) {
      return 0;
   }

   public boolean func_149662_c(BlockState state) {
      return false;
   }

   public boolean func_149730_j(BlockState state) {
      return true;
   }

   @Override
   public boolean func_149686_d(BlockState state) {
      return false;
   }

   public boolean func_185481_k(BlockState state) {
      return false;
   }

   @OnlyIn(Dist.CLIENT)
   public ChunkSectionLayer func_180664_k() {
      return ChunkSectionLayer.CUTOUT;
   }

   public boolean canCreatureSpawn(BlockState state, BlockGetter world, BlockPos pos, SpawnPlacementType type) {
      return false;
   }

   @Override
   public boolean func_176225_a(BlockState state, BlockGetter world, BlockPos pos, Direction side) {
      return world.getBlockState(pos.relative(side)).getBlock() == this ? false : super.func_176225_a(state, world, pos, side);
   }

   public static enum GlassType implements IIdProvider {
      reinforced;

      @Override
      public String getName() {
         return this.name();
      }

      @Override
      public int getId() {
         return this.ordinal();
      }
   }
}
