package ic2.core.block;

import ic2.core.block.state.BlockStates;
import ic2.core.item.type.MiscResourceType;
import ic2.core.ref.BlockMaterial;
import ic2.core.ref.BlockName;
import ic2.core.ref.ItemName;
import ic2.core.util.ItemStacks;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.PushReaction;

public class BlockRubWood extends BlockBase {
   public static final EnumProperty<BlockRubWood.RubberWoodState> stateProperty = EnumProperty.create("state", BlockRubWood.RubberWoodState.class);

   public BlockRubWood() {
      super(BlockName.rubber_wood, BlockMaterial.WOOD);
      this.func_149675_a(true);
      this.func_149711_c(1.0F);
      this.func_149672_a(SoundType.WOOD);
      this.registerDefaultState((BlockState)((BlockState)this.stateDefinition.any()).setValue(stateProperty, BlockRubWood.RubberWoodState.plain_y));
   }

   @Override
   protected StateDefinition func_180661_e() {
      return BlockStates.definition(this, new Property[]{stateProperty});
   }

   @Override
   public BlockState func_176203_a(int meta) {
      return meta >= 0 && meta < BlockRubWood.RubberWoodState.values.length
         ? (BlockState)this.defaultBlockState().setValue(stateProperty, BlockRubWood.RubberWoodState.values[meta])
         : this.defaultBlockState();
   }

   @Override
   public int func_176201_c(BlockState state) {
      return ((BlockRubWood.RubberWoodState)state.getValue(stateProperty)).ordinal();
   }

   @Override
   public BlockState func_180642_a(Level world, BlockPos pos, Direction facing, float hitX, float hitY, float hitZ, int meta, LivingEntity placer) {
      BlockState state = super.func_180642_a(world, pos, facing, hitX, hitY, hitZ, meta, placer);
      return (BlockState)state.setValue(stateProperty, getPlainAxisState(facing.getAxis()));
   }

   private static BlockRubWood.RubberWoodState getPlainAxisState(Axis axis) {
      switch (axis) {
         case X:
            return BlockRubWood.RubberWoodState.plain_x;
         case Y:
            return BlockRubWood.RubberWoodState.plain_y;
         case Z:
            return BlockRubWood.RubberWoodState.plain_z;
         default:
            throw new IllegalArgumentException("invalid axis: " + axis);
      }
   }

   public void func_180653_a(Level world, BlockPos pos, BlockState state, float chance, int fortune) {
      if (!world.isClientSide()) {
         int count = 1;

         for (int j1 = 0; j1 < count; j1++) {
            if (!(world.getRandom().nextFloat() > chance)) {
               Item item = Item.byBlock(this);
               if (item != null) {
                  Block.popResource(world, pos, ItemStacks.create(item, 1, 0));
               }

               if (!((BlockRubWood.RubberWoodState)state.getValue(stateProperty)).isPlain() && world.getRandom().nextInt(6) == 0) {
                  Block.popResource(world, pos, ItemName.misc_resource.getItemStack(MiscResourceType.resin));
               }
            }
         }
      }
   }

   public void func_180663_b(Level world, BlockPos pos, BlockState state) {
      int range = 4;
      MutableBlockPos cPos = new MutableBlockPos();

      for (int y = -range; y <= range; y++) {
         for (int z = -range; z <= range; z++) {
            for (int x = -range; x <= range; x++) {
               cPos.set(pos.getX() + x, pos.getY() + y, pos.getZ() + z);
               BlockState cState = world.getBlockState(cPos);
               Block cBlock = cState.getBlock();
               if (cState.is(BlockTags.LEAVES)) {
                  world.scheduleTick(cPos.immutable(), cBlock, 1);
               }
            }
         }
      }
   }

   @Override
   public List<ItemStack> getDrops(BlockGetter world, BlockPos pos, BlockState state, int fortune) {
      ArrayList<ItemStack> drops = new ArrayList<>();
      drops.add(new ItemStack(this));
      if (!((BlockRubWood.RubberWoodState)state.getValue(stateProperty)).isPlain() && world instanceof Level level && level.getRandom().nextInt(6) == 0) {
         drops.add(ItemName.misc_resource.getItemStack(MiscResourceType.resin));
      }

      return drops;
   }

   @Override
   protected void randomTick(BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {
      this.randomTick(world, pos, state, new Random(random.nextLong()));
   }

   public void randomTick(Level world, BlockPos pos, BlockState state, Random random) {
      if (random.nextInt(7) == 0) {
         BlockRubWood.RubberWoodState rwState = (BlockRubWood.RubberWoodState)state.getValue(stateProperty);
         if (!rwState.canRegenerate()) {
            return;
         }

         world.setBlock(pos, (BlockState)state.setValue(stateProperty, rwState.getWet()), 3);
      }
   }

   public PushReaction func_149656_h(BlockState state) {
      BlockRubWood.RubberWoodState rstate = (BlockRubWood.RubberWoodState)state.getValue(stateProperty);
      return rstate != BlockRubWood.RubberWoodState.plain_x && rstate != BlockRubWood.RubberWoodState.plain_y && rstate != BlockRubWood.RubberWoodState.plain_z
         ? PushReaction.IMMOVEABLE
         : PushReaction.PUSH_PULL;
   }

   public boolean canSustainLeaves(BlockState state, BlockGetter world, BlockPos pos) {
      return true;
   }

   public boolean isWood(BlockGetter world, BlockPos pos) {
      return true;
   }

   public int getFireSpreadSpeed(BlockGetter world, BlockPos pos, Direction face) {
      return 4;
   }

   public int getFlammability(BlockGetter world, BlockPos pos, Direction face) {
      return 20;
   }

   public static enum RubberWoodState implements StringRepresentable {
      plain_y(Axis.Y, null, false),
      plain_x(Axis.X, null, false),
      plain_z(Axis.Z, null, false),
      dry_north(Axis.Y, Direction.NORTH, false),
      dry_south(Axis.Y, Direction.SOUTH, false),
      dry_west(Axis.Y, Direction.WEST, false),
      dry_east(Axis.Y, Direction.EAST, false),
      wet_north(Axis.Y, Direction.NORTH, true),
      wet_south(Axis.Y, Direction.SOUTH, true),
      wet_west(Axis.Y, Direction.WEST, true),
      wet_east(Axis.Y, Direction.EAST, true);

      public final Axis axis;
      public final Direction facing;
      public final boolean wet;
      private static final BlockRubWood.RubberWoodState[] values = values();

      private RubberWoodState(Axis axis, Direction facing, boolean wet) {
         this.axis = axis;
         this.facing = facing;
         this.wet = wet;
      }

      public String getSerializedName() {
         return this.name();
      }

      public String getName() {
         return this.name();
      }

      public boolean isPlain() {
         return this.facing == null;
      }

      public boolean canRegenerate() {
         return !this.isPlain() && !this.wet;
      }

      public BlockRubWood.RubberWoodState getWet() {
         if (this.isPlain()) {
            return null;
         } else {
            return this.wet ? this : values[this.ordinal() + 4];
         }
      }

      public BlockRubWood.RubberWoodState getDry() {
         return !this.isPlain() && this.wet ? values[this.ordinal() - 4] : this;
      }

      public static BlockRubWood.RubberWoodState getWet(Direction facing) {
         switch (facing) {
            case NORTH:
               return wet_north;
            case SOUTH:
               return wet_south;
            case WEST:
               return wet_west;
            case EAST:
               return wet_east;
            default:
               throw new IllegalArgumentException("incompatible facing: " + facing);
         }
      }
   }
}
