package ic2.core.block;

import ic2.core.block.state.IIdProvider;
import ic2.core.block.type.ResourceBlock;
import ic2.core.ref.BlockMaterial;
import ic2.core.ref.BlockName;
import ic2.core.util.StackUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.SpawnPlacementType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.AABB;

public class BlockFoam extends BlockMultiID<BlockFoam.FoamType> {
   public static BlockFoam create() {
      return BlockMultiID.create(BlockFoam.class, BlockFoam.FoamType.class);
   }

   private BlockFoam() {
      super(BlockName.foam, BlockMaterial.CLOTH);
      this.func_149675_a(true);
      this.func_149711_c(0.01F);
      this.func_149752_b(10.0F);
      this.func_149672_a(SoundType.WOOL);
   }

   public boolean func_149662_c(BlockState state) {
      return false;
   }

   public boolean isNormalCube(BlockState state, BlockGetter world, BlockPos pos) {
      return true;
   }

   @Nullable
   @Override
   public AABB func_180646_a(BlockState blockState, BlockGetter world, BlockPos pos) {
      return null;
   }

   public boolean isSideSolid(BlockState state, BlockGetter world, BlockPos pos, Direction side) {
      return false;
   }

   @Override
   public void func_180650_b(Level world, BlockPos pos, BlockState state, Random random) {
      int tickSpeed = (Integer)((ServerLevel)world).getGameRules().get(GameRules.RANDOM_TICK_SPEED);
      if (tickSpeed <= 0) {
         throw new IllegalStateException("Foam was randomly ticked when world " + world + " isn't ticking?");
      } else {
         BlockFoam.FoamType type = (BlockFoam.FoamType)state.getValue(this.typeProperty);
         float chance = getHardenChance(world, pos, state, type) * 4096.0F / tickSpeed;
         if (random.nextFloat() < chance) {
            world.setBlock(pos, ((BlockFoam.FoamType)state.getValue(this.typeProperty)).getResult(), 3);
         }
      }
   }

   public static float getHardenChance(Level world, BlockPos pos, BlockState state, BlockFoam.FoamType type) {
      int light = world.getMaxLocalRawBrightness(pos);
      if (!state.isSolidRender() && state.getLightDampening() == 0) {
         for (Direction side : Direction.values()) {
            light = Math.max(light, world.getMaxLocalRawBrightness(pos.relative(side)));
         }
      }

      int avgTime = type.hardenTime * (16 - light);
      return 1.0F / (avgTime * 20);
   }

   public boolean func_180639_a(
      Level world, BlockPos pos, BlockState state, Player player, InteractionHand hand, Direction side, float hitX, float hitY, float hitZ
   ) {
      if (StackUtil.consume(player, hand, StackUtil.sameItem(Blocks.SAND), 1)) {
         world.setBlock(pos, ((BlockFoam.FoamType)state.getValue(this.typeProperty)).getResult(), 3);
         return true;
      } else {
         return false;
      }
   }

   @Override
   public List<ItemStack> getDrops(BlockGetter world, BlockPos pos, BlockState state, int fortune) {
      return ((BlockFoam.FoamType)state.getValue(this.typeProperty)).getDrops();
   }

   public boolean canCreatureSpawn(BlockState state, BlockGetter world, BlockPos pos, SpawnPlacementType type) {
      return false;
   }

   public static enum FoamType implements IIdProvider {
      normal(300),
      reinforced(600);

      public final int hardenTime;

      private FoamType(int hardenTime) {
         this.hardenTime = hardenTime;
      }

      @Override
      public String getName() {
         return this.name();
      }

      @Override
      public int getId() {
         return this.ordinal();
      }

      public List<ItemStack> getDrops() {
         switch (this) {
            case normal:
               return new ArrayList<>();
            case reinforced:
               List<ItemStack> ret = new ArrayList<>();
               ret.add(BlockName.scaffold.getItemStack(BlockScaffold.ScaffoldType.iron));
               return ret;
            default:
               throw new UnsupportedOperationException();
         }
      }

      public BlockState getResult() {
         switch (this) {
            case normal:
               return BlockName.wall.getBlockState(BlockWall.defaultColor);
            case reinforced:
               return BlockName.resource.getBlockState(ResourceBlock.reinforced_stone);
            default:
               throw new UnsupportedOperationException();
         }
      }
   }
}
