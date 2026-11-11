package ic2.core.block;

import com.google.common.base.Function;
import ic2.core.block.state.BlockStates;
import ic2.core.ref.BlockName;
import ic2.core.util.BlockSupport;
import ic2.core.util.TorchGeometry;
import java.util.Random;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class BlockDynamite extends BlockBase {
   public static final Property<Boolean> linked = BooleanProperty.create("linked");

   public BlockDynamite() {
      super(BlockName.dynamite, MaterialIC2TNT.instance, (Function)null);
      this.func_149675_a(true);
      this.func_149711_c(0.0F);
      this.func_149672_a(SoundType.GRASS);
      this.func_149647_a(null);
      this.registerDefaultState((BlockState)((BlockState)this.defaultBlockState().setValue(linked, false)).setValue(BlockStateProperties.FACING, Direction.UP));
   }

   @Override
   protected StateDefinition func_180661_e() {
      return BlockStates.definition(this, new Property[]{BlockStateProperties.FACING, linked});
   }

   @Override
   public AABB func_185496_a(BlockState state, BlockGetter source, BlockPos pos) {
      return TorchGeometry.box((Direction)state.getValue(BlockStateProperties.FACING));
   }

   public boolean func_149662_c(BlockState state) {
      return false;
   }

   @Override
   public boolean func_149686_d(BlockState state) {
      return false;
   }

   @OnlyIn(Dist.CLIENT)
   public ChunkSectionLayer func_180664_k() {
      return ChunkSectionLayer.CUTOUT;
   }

   @Override
   public boolean func_176196_c(Level world, BlockPos pos) {
      for (Direction dir : BlockStateProperties.FACING.getPossibleValues()) {
         if (BlockSupport.top(world, pos.relative(dir.getOpposite()), false)) {
            return true;
         }
      }

      return false;
   }

   @Override
   public BlockState func_180642_a(Level world, BlockPos pos, Direction facing, float hitX, float hitY, float hitZ, int meta, LivingEntity placer) {
      if (facing == Direction.DOWN || !BlockSupport.top(world, pos.relative(facing.getOpposite()), false)) {
         for (Direction facing2 : BlockStateProperties.FACING.getPossibleValues()) {
            if (BlockSupport.top(world, pos.relative(facing2.getOpposite()), false)) {
               facing = facing2;
               break;
            }
         }
      }

      return (BlockState)this.defaultBlockState().setValue(BlockStateProperties.FACING, facing);
   }

   @Override
   public int func_176201_c(BlockState state) {
      return ((Direction)state.getValue(BlockStateProperties.FACING)).ordinal() << 1 | (state.getValue(linked) ? 1 : 0);
   }

   @Override
   public BlockState func_176203_a(int meta) {
      return (BlockState)((BlockState)this.defaultBlockState().setValue(linked, (meta & 1) != 0))
         .setValue(BlockStateProperties.FACING, Direction.values()[meta >> 1]);
   }

   public void func_180633_a(Level world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
      this.checkPlacement(world, pos, state);
   }

   public void randomTick(Level world, BlockPos pos, BlockState state, Random random) {
      this.checkPlacement(world, pos, state);
   }

   public void func_189540_a(BlockState state, Level world, BlockPos pos, Block neighborBlock, BlockPos neighborPos) {
      this.checkPlacement(world, pos, state);
   }

   public int func_149745_a(Random random) {
      return 0;
   }

   public int func_180651_a(BlockState state) {
      return 0;
   }

   public void func_180652_a(Level world, BlockPos pos, Explosion explosion) {
      this.explode(world, pos, explosion != null ? explosion.getIndirectSourceEntity() : null, true);
   }

   public boolean removedByPlayer(BlockState state, Level world, BlockPos pos, Player player, boolean willHarvest) {
      if (!world.isClientSide()) {
         this.explode(world, pos, player, false);
      }

      return false;
   }

   private void checkPlacement(Level world, BlockPos pos, BlockState state) {
      if (!world.isClientSide()) {
         if (world.hasNeighborSignal(pos)) {
            this.explode(world, pos, null, false);
         } else if (!BlockSupport.top(world, pos.relative(((Direction)state.getValue(BlockStateProperties.FACING)).getOpposite()), false)) {
            world.removeBlock(pos, false);
            this.func_176226_b(world, pos, state, 0);
         }
      }
   }

   private void explode(Level world, BlockPos pos, LivingEntity player, boolean byExplosion) {
      world.removeBlock(pos, false);
      EntityDynamite entity = new EntityStickyDynamite(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5F);
      entity.owner = player;
      entity.fuse = byExplosion ? 5 : 40;
      world.addFreshEntity(entity);
      world.playSound(null, pos, SoundEvents.TNT_PRIMED, SoundSource.BLOCKS, 1.0F, 1.0F);
   }

   public HitResult func_180636_a(BlockState state, Level world, BlockPos pos, Vec3 start, Vec3 end) {
      return Shapes.create(this.func_185496_a(state, world, pos)).clip(start, end, pos);
   }
}
