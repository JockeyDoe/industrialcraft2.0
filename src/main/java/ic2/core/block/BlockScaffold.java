package ic2.core.block;

import ic2.api.recipe.IRecipeInput;
import ic2.api.recipe.Recipes;
import ic2.core.IC2;
import ic2.core.block.state.IIdProvider;
import ic2.core.block.type.IBlockSound;
import ic2.core.block.type.IExtBlockType;
import ic2.core.ref.BlockMaterial;
import ic2.core.ref.BlockName;
import ic2.core.util.BlockSupport;
import ic2.core.util.DirectionOrder;
import ic2.core.util.StackUtil;
import ic2.core.util.Util;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class BlockScaffold extends BlockMultiID<BlockScaffold.ScaffoldType> {
   private static final IRecipeInput stickInput = Recipes.inputFactory.forOreDict("stickWood");
   private static final Direction[] supportedFacings = new Direction[]{Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};
   private static final double border = 0.03125;
   private static final AABB aabb = new AABB(0.03125, 0.0, 0.03125, 0.96875, 1.0, 0.96875);

   public static BlockScaffold create() {
      return BlockMultiID.create(BlockScaffold.class, BlockScaffold.ScaffoldType.class);
   }

   private BlockScaffold() {
      super(BlockName.scaffold, BlockMaterial.WOOD);
      this.func_149675_a(true);
   }

   @OnlyIn(Dist.CLIENT)
   public ChunkSectionLayer func_180664_k() {
      return ChunkSectionLayer.CUTOUT;
   }

   @Override
   public BlockMaterial getMaterialCategory(BlockState state) {
      BlockScaffold.ScaffoldType type = this.getType(state);
      if (type == null) {
         return super.getMaterialCategory(state);
      } else {
         switch (type) {
            case wood:
            case reinforced_wood:
               return BlockMaterial.WOOD;
            case iron:
            case reinforced_iron:
               return BlockMaterial.IRON;
            default:
               throw new IllegalStateException("Invalid scaffold type: " + type);
         }
      }
   }

   public boolean func_149662_c(BlockState state) {
      return false;
   }

   public boolean func_149721_r(BlockState state) {
      return false;
   }

   public boolean isLadder(BlockState state, BlockGetter world, BlockPos pos, LivingEntity entity) {
      return true;
   }

   public void func_180634_a(Level world, BlockPos pos, BlockState state, Entity rawEntity) {
      if (rawEntity instanceof LivingEntity entity) {
         entity.fallDistance = 0.0;
         double limit = 0.15;
         entity.setDeltaMovement(new Vec3(Util.limit(entity.getDeltaMovement().x, -limit, limit), entity.getDeltaMovement().y, entity.getDeltaMovement().z));
         entity.setDeltaMovement(new Vec3(entity.getDeltaMovement().x, entity.getDeltaMovement().y, Util.limit(entity.getDeltaMovement().z, -limit, limit)));
         if (entity.isShiftKeyDown() && entity instanceof Player) {
            if (entity.isInWater()) {
               entity.setDeltaMovement(new Vec3(entity.getDeltaMovement().x, 0.02, entity.getDeltaMovement().z));
            } else {
               entity.setDeltaMovement(new Vec3(entity.getDeltaMovement().x, 0.08, entity.getDeltaMovement().z));
            }
         } else if (entity.horizontalCollision) {
            entity.setDeltaMovement(new Vec3(entity.getDeltaMovement().x, 0.2, entity.getDeltaMovement().z));
         } else {
            entity.setDeltaMovement(new Vec3(entity.getDeltaMovement().x, Math.max(entity.getDeltaMovement().y, -0.07), entity.getDeltaMovement().z));
         }
      }
   }

   @Override
   public AABB func_185496_a(BlockState state, BlockGetter world, BlockPos pos) {
      return aabb;
   }

   public AABB func_180640_a(BlockState state, Level worldIn, BlockPos pos) {
      return new AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0).move(pos);
   }

   public boolean isSideSolid(BlockState state, BlockGetter world, BlockPos pos, Direction side) {
      return side.getAxis() == Axis.Y;
   }

   @Override
   public List<ItemStack> getDrops(BlockGetter world, BlockPos pos, BlockState state, int fortune) {
      if (state.getBlock() != this) {
         return Collections.emptyList();
      } else {
         List<ItemStack> ret = new ArrayList<>();
         BlockScaffold.ScaffoldType type = (BlockScaffold.ScaffoldType)state.getValue(this.typeProperty);
         switch (type) {
            case wood:
            case iron:
               ret.add(this.getItemStack(type));
               break;
            case reinforced_wood:
               ret.add(this.getItemStack(BlockScaffold.ScaffoldType.wood));
               ret.add(new ItemStack(Items.STICK, 2));
               break;
            case reinforced_iron:
               ret.add(this.getItemStack(BlockScaffold.ScaffoldType.iron));
               ret.add(BlockName.fence.getItemStack(BlockIC2Fence.IC2FenceType.iron));
               break;
            default:
               throw new IllegalStateException();
         }

         return ret;
      }
   }

   public boolean func_180639_a(
      Level world, BlockPos pos, BlockState state, Player player, InteractionHand hand, Direction side, float hitX, float hitY, float hitZ
   ) {
      if (player.isShiftKeyDown()) {
         return false;
      } else {
         ItemStack stack = player.getItemInHand(hand);
         if (StackUtil.isEmpty(stack)) {
            return false;
         } else {
            BlockScaffold.ScaffoldType type = this.getType(state);
            if (type == null) {
               return false;
            } else {
               int stickCount = 2;
               int fenceCount = 1;
               switch (type) {
                  case wood:
                     if (!stickInput.matches(stack) || StackUtil.getSize(stack) < 2) {
                        return false;
                     }
                     break;
                  case reinforced_wood:
                  case reinforced_iron:
                     return false;
                  case iron:
                     if (!StackUtil.checkItemEquality(stack, BlockName.fence.getItemStack(BlockIC2Fence.IC2FenceType.iron)) || StackUtil.getSize(stack) < 1) {
                        return false;
                     }
                     break;
                  default:
                     throw new IllegalStateException();
               }

               if (!this.isPillar(world, pos)) {
                  return false;
               } else {
                  world.setBlock(pos, (BlockState)state.setValue(this.typeProperty, switch (type) {
                     case wood -> {
                        StackUtil.consumeOrError(player, hand, StackUtil.recipeInput(stickInput), 2);
                        yield BlockScaffold.ScaffoldType.reinforced_wood;
                     }
                     case iron -> {
                        StackUtil.consumeOrError(player, hand, StackUtil.sameStack(BlockName.fence.getItemStack(BlockIC2Fence.IC2FenceType.iron)), 1);
                        yield BlockScaffold.ScaffoldType.reinforced_iron;
                     }
                     default -> throw new IllegalStateException();
                  }), 3);
                  return true;
               }
            }
         }
      }
   }

   public void func_180649_a(Level world, BlockPos pos, Player player) {
      InteractionHand hand = InteractionHand.MAIN_HAND;
      ItemStack stack = player.getItemInHand(hand);
      if (!StackUtil.isEmpty(stack) && StackUtil.checkItemEquality(stack, Item.byBlock(this))) {
         while (world.getBlockState(pos).getBlock() == this) {
            pos = pos.above();
         }

         if (this.func_176196_c(world, pos) && pos.getY() < IC2.getWorldHeight(world)) {
            boolean isCreative = player.getAbilities().instabuild;
            ItemStack prev = isCreative ? StackUtil.copy(stack) : null;
            stack.useOn(
               new UseOnContext(player, hand, new BlockHitResult(new Vec3(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5), Direction.UP, pos.below(), false))
            );
            if (!isCreative) {
               StackUtil.clearEmpty(player, hand);
            } else {
               StackUtil.set(player, hand, prev);
            }
         }
      }
   }

   protected void entityInside(BlockState state, Level world, BlockPos pos, Entity entity, InsideBlockEffectApplier effects, boolean newlyEntered) {
      this.func_180634_a(world, pos, state, entity);
   }

   protected void neighborChanged(BlockState state, Level world, BlockPos pos, Block block, Orientation orientation, boolean piston) {
      this.func_189540_a(state, world, pos, block, pos);
   }

   @Override
   protected void randomTick(BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {
      this.randomTick(world, pos, state, new Random(random.nextLong()));
   }

   @Override
   public boolean func_176196_c(Level world, BlockPos pos) {
      return super.func_176196_c(world, pos) && this.hasSupport(world, pos, BlockScaffold.ScaffoldType.wood);
   }

   public void func_189540_a(BlockState state, Level world, BlockPos pos, Block neighborBlock, BlockPos neighborPos) {
      this.checkSupport(world, pos);
   }

   public void randomTick(Level world, BlockPos pos, BlockState state, Random random) {
      if (random.nextInt(8) == 0) {
         this.checkSupport(world, pos);
      }
   }

   private boolean isPillar(Level world, BlockPos pos) {
      while (world.getBlockState(pos).getBlock() == this) {
         pos = pos.below();
      }

      return BlockSupport.top(world, pos, false);
   }

   public int getFireSpreadSpeed(BlockGetter world, BlockPos pos, Direction face) {
      BlockScaffold.ScaffoldType type = this.getType(world, pos);
      if (type == null) {
         return 0;
      } else {
         switch (type) {
            case wood:
            case reinforced_wood:
               return 8;
            case iron:
            case reinforced_iron:
               return 0;
            default:
               throw new IllegalStateException();
         }
      }
   }

   public int getFlammability(BlockGetter world, BlockPos pos, Direction face) {
      BlockScaffold.ScaffoldType type = this.getType(world, pos);
      if (type == null) {
         return 0;
      } else {
         switch (type) {
            case wood:
            case reinforced_wood:
               return 20;
            case iron:
            case reinforced_iron:
               return 0;
            default:
               throw new IllegalStateException();
         }
      }
   }

   private boolean hasSupport(BlockGetter world, BlockPos start, BlockScaffold.ScaffoldType type) {
      return this.calculateSupport(world, start, type).get(start).strength >= 0;
   }

   private void checkSupport(Level world, BlockPos start) {
      BlockState state = world.getBlockState(start);
      if (state.getBlock() == this) {
         Map<BlockPos, BlockScaffold.Support> results = this.calculateSupport(world, start, (BlockScaffold.ScaffoldType)state.getValue(this.typeProperty));
         boolean droppedAny = false;

         for (BlockScaffold.Support support : results.values()) {
            if (support.strength < 0) {
               world.setBlock(support.pos, Blocks.AIR.defaultBlockState(), 2);
               this.func_176226_b(world, support.pos, (BlockState)this.defaultBlockState().setValue(this.typeProperty, support.type), 0);
               droppedAny = true;
            }
         }

         if (droppedAny) {
            for (BlockScaffold.Support supportx : results.values()) {
               if (supportx.strength < 0) {
                  world.updateNeighborsAt(supportx.pos, this);
               }
            }
         }
      }
   }

   private Map<BlockPos, BlockScaffold.Support> calculateSupport(BlockGetter world, BlockPos start, BlockScaffold.ScaffoldType type) {
      Map<BlockPos, BlockScaffold.Support> results = new HashMap<>();
      Queue<BlockScaffold.Support> queue = new ArrayDeque<>();
      Set<BlockPos> groundSupports = new HashSet<>();
      BlockScaffold.Support support = new BlockScaffold.Support(start, type, -1);
      results.put(start, support);
      queue.add(support);

      while ((support = queue.poll()) != null) {
         for (Direction dir : Direction.values()) {
            BlockPos pos = support.pos.relative(dir);
            if (!results.containsKey(pos)) {
               BlockState state = world.getBlockState(pos);
               Block block = state.getBlock();
               if (block == this) {
                  type = (BlockScaffold.ScaffoldType)state.getValue(this.typeProperty);
                  BlockScaffold.Support cSupport = new BlockScaffold.Support(pos, type, -1);
                  results.put(pos, cSupport);
                  queue.add(cSupport);
               } else if (state.isSolidRender()) {
                  groundSupports.add(pos);
               }
            }
         }
      }

      for (BlockPos groundPos : groundSupports) {
         BlockPos pos = groundPos.above();
         int propagatedStrength = 0;

         while (true) {
            support = results.get(pos);
            if (support == null) {
               break;
            }

            int strength;
            if (support.type.strength >= propagatedStrength) {
               strength = support.type.strength;
               propagatedStrength = strength - 1;
            } else {
               strength = propagatedStrength--;
            }

            if (support.strength < strength) {
               support.strength = strength;

               for (Direction dirx : DirectionOrder.HORIZONTAL) {
                  BlockPos nPos = pos.relative(dirx);
                  BlockScaffold.Support nSupport = results.get(nPos);
                  if (nSupport != null && nSupport.strength < strength) {
                     nSupport.strength = strength - 1;
                     queue.add(nSupport);
                  }
               }
            }

            pos = pos.above();
         }
      }

      while ((support = queue.poll()) != null) {
         for (Direction dirxx : supportedFacings) {
            BlockPos pos = support.pos.relative(dirxx);
            BlockScaffold.Support nSupport = results.get(pos);
            if (nSupport != null && nSupport.strength < support.strength) {
               nSupport.strength = support.strength - 1;
               if (nSupport.strength > 0) {
                  queue.add(nSupport);
               }
            }
         }
      }

      return results;
   }

   public static enum ScaffoldType implements IIdProvider, IExtBlockType, IBlockSound {
      wood(2, 0.5F, 0.12F, SoundType.WOOD),
      reinforced_wood(5, 0.6F, 0.24F, SoundType.WOOD),
      iron(5, 0.8F, 6.0F, SoundType.METAL),
      reinforced_iron(12, 1.0F, 8.0F, SoundType.METAL);

      public final int strength;
      private final float hardness;
      private final float explosionResistance;
      private final SoundType sound;

      private ScaffoldType(int strength, float hardness, float explosionResistance, SoundType sound) {
         if (strength < 1) {
            throw new IllegalArgumentException();
         } else {
            this.strength = strength;
            this.hardness = hardness;
            this.explosionResistance = explosionResistance;
            this.sound = sound;
         }
      }

      @Override
      public String getName() {
         return this.name();
      }

      @Override
      public int getId() {
         return this.ordinal();
      }

      @Override
      public float getHardness() {
         return this.hardness;
      }

      @Override
      public float getExplosionResistance() {
         return this.explosionResistance;
      }

      @Override
      public SoundType getSound() {
         return this.sound;
      }
   }

   private static class Support {
      final BlockPos pos;
      final BlockScaffold.ScaffoldType type;
      int strength;

      Support(BlockPos pos, BlockScaffold.ScaffoldType type, int strength) {
         this.pos = pos;
         this.type = type;
         this.strength = strength;
      }
   }
}
