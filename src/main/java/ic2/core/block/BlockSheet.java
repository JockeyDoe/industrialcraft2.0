package ic2.core.block;

import ic2.core.IC2;
import ic2.core.block.state.IIdProvider;
import ic2.core.block.type.IExtBlockType;
import ic2.core.item.block.ItemBlockSheet;
import ic2.core.item.type.MiscResourceType;
import ic2.core.profile.NotClassic;
import ic2.core.ref.BlockMaterial;
import ic2.core.ref.BlockName;
import ic2.core.ref.ItemName;
import ic2.core.util.BlockSupport;
import ic2.core.util.DirectionOrder;
import ic2.core.util.Ic2BlockPos;
import ic2.core.util.ItemStacks;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BlockSheet extends BlockMultiID<BlockSheet.SheetType> {
   private static final AABB aabb = new AABB(0.0, 0.0, 0.0, 1.0, 0.125, 1.0);
   private static final Direction[] positiveHorizontalFacings = new Direction[]{Direction.EAST, Direction.SOUTH};

   public static BlockSheet create() {
      return BlockMultiID.create(BlockSheet.class, BlockSheet.SheetType.class);
   }

   public BlockSheet() {
      super(BlockName.sheet, BlockMaterial.CIRCUITS, ItemBlockSheet.class);
   }

   @Override
   public boolean func_149686_d(BlockState state) {
      return false;
   }

   public boolean func_149662_c(BlockState state) {
      return false;
   }

   @Override
   public AABB func_185496_a(BlockState state, BlockGetter source, BlockPos pos) {
      return aabb;
   }

   public void func_185477_a(
      BlockState state, Level worldIn, BlockPos pos, AABB entityBox, List<AABB> collidingBoxes, @Nullable Entity entityIn, boolean isActualState
   ) {
      if (this.getType(state) != BlockSheet.SheetType.wool
         || !(entityIn instanceof Player)
         || !entityIn.isShiftKeyDown() && !(entityIn.getY() < pos.getY() + aabb.maxY - entityIn.maxUpStep())) {
         AABB box = this.func_180646_a(state, worldIn, pos);
         if (box != null && box.move(pos).intersects(entityBox)) {
            collidingBoxes.add(box.move(pos));
         }
      }
   }

   @Override
   public AABB func_180646_a(BlockState state, BlockGetter world, BlockPos pos) {
      AABB aabb = super.func_180646_a(state, world, pos);
      switch ((BlockSheet.SheetType)this.getType(state)) {
         case resin:
            return null;
         default:
            return aabb;
      }
   }

   @Override
   protected VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
      return !(this.getType(state) == BlockSheet.SheetType.wool && context instanceof EntityCollisionContext collision)
            || !(collision.getEntity() instanceof Player player && (player.isShiftKeyDown() || player.getY() < pos.getY() + aabb.maxY - player.maxUpStep()))
         ? super.getCollisionShape(state, world, pos, context)
         : Shapes.empty();
   }

   protected void entityInside(BlockState state, Level world, BlockPos pos, Entity entity, InsideBlockEffectApplier effects, boolean newlyEntered) {
      this.func_180634_a(world, pos, state, entity);
   }

   protected void neighborChanged(BlockState state, Level world, BlockPos pos, Block block, Orientation orientation, boolean piston) {
      this.func_189540_a(state, world, pos, block, pos);
   }

   public boolean canReplace(Level world, BlockPos pos, Direction side, ItemStack stack) {
      return this.isValidPosition(world, pos, this.func_176203_a(ItemStacks.rawMeta(stack)));
   }

   private boolean isValidPosition(Level world, BlockPos pos, BlockState state) {
      switch ((BlockSheet.SheetType)this.getType(state)) {
         case resin:
            return this.isNormalCubeBelow(world, pos);
         case rubber:
            for (Direction facing : DirectionOrder.HORIZONTAL) {
               state = world.getBlockState(pos.relative(facing));
               if (state == BlockName.sheet.getBlockState(BlockSheet.SheetType.rubber) || state.isSolidRender()) {
                  return true;
               }
            }

            return this.isNormalCubeBelow(world, pos);
         case wool:
            return true;
         default:
            return false;
      }
   }

   private boolean isNormalCubeBelow(Level world, BlockPos pos) {
      pos = pos.below();
      BlockState state = world.getBlockState(pos);
      return state.isSolidRender();
   }

   public void func_189540_a(BlockState state, Level world, BlockPos pos, Block neighborBlock, BlockPos neighborPos) {
      if (!this.isValidPosition(world, pos, state)) {
         world.removeBlock(pos, false);
         this.func_176226_b(world, pos, state, 0);
      }
   }

   public void func_180634_a(Level world, BlockPos pos, BlockState state, Entity entity) {
      switch ((BlockSheet.SheetType)this.getType(state)) {
         case resin:
            entity.fallDistance = (float)(entity.fallDistance * 0.75);
            entity.setDeltaMovement(new Vec3(entity.getDeltaMovement().x * 0.6, entity.getDeltaMovement().y, entity.getDeltaMovement().z));
            entity.setDeltaMovement(new Vec3(entity.getDeltaMovement().x, entity.getDeltaMovement().y * 0.85, entity.getDeltaMovement().z));
            entity.setDeltaMovement(new Vec3(entity.getDeltaMovement().x, entity.getDeltaMovement().y, entity.getDeltaMovement().z * 0.6));
            break;
         case rubber:
            if (BlockSupport.top(world, pos.below(), false)) {
               return;
            }

            if (entity instanceof LivingEntity && !canSupportWeight(world, pos)) {
               world.removeBlock(pos, false);
               return;
            }

            if (entity.getDeltaMovement().y <= -0.4) {
               entity.fallDistance = 0.0;
               entity.setDeltaMovement(new Vec3(entity.getDeltaMovement().x * 1.1, entity.getDeltaMovement().y, entity.getDeltaMovement().z));
               entity.setDeltaMovement(new Vec3(entity.getDeltaMovement().x, entity.getDeltaMovement().y, entity.getDeltaMovement().z * 1.1));
               if (entity instanceof LivingEntity) {
                  if (entity instanceof Player && IC2.keyboard.isJumpKeyDown((Player)entity)) {
                     entity.setDeltaMovement(new Vec3(entity.getDeltaMovement().x, entity.getDeltaMovement().y * -1.3, entity.getDeltaMovement().z));
                  } else if (entity instanceof Player && ((Player)entity).isShiftKeyDown()) {
                     entity.setDeltaMovement(new Vec3(entity.getDeltaMovement().x, entity.getDeltaMovement().y * -0.1, entity.getDeltaMovement().z));
                  } else {
                     entity.setDeltaMovement(new Vec3(entity.getDeltaMovement().x, entity.getDeltaMovement().y * -0.8, entity.getDeltaMovement().z));
                  }
               } else {
                  entity.setDeltaMovement(new Vec3(entity.getDeltaMovement().x, entity.getDeltaMovement().y * -0.8, entity.getDeltaMovement().z));
               }
            }
            break;
         case wool:
            entity.fallDistance = (float)(entity.fallDistance * 0.95);
      }
   }

   private static boolean canSupportWeight(Level world, BlockPos pos) {
      int maxRange = 16;
      Ic2BlockPos cPos = new Ic2BlockPos();

      for (Direction axis : positiveHorizontalFacings) {
         for (int dir = -1; dir <= 1; dir += 2) {
            cPos.set(pos);
            boolean supported = false;

            for (int i = 0; i < 16; i++) {
               cPos.move(axis, dir);
               BlockState state = cPos.getBlockState(world);
               if (state.isSolidRender()) {
                  supported = true;
                  break;
               }

               if (state != BlockName.sheet.getBlockState(BlockSheet.SheetType.rubber)) {
                  break;
               }

               cPos.moveDown();
               BlockState baseState = cPos.getBlockState(world);
               if (baseState.isSolidRender()) {
                  supported = true;
                  break;
               }

               cPos.moveUp();
            }

            if (!supported) {
               break;
            }

            if (dir == 1) {
               return true;
            }
         }
      }

      return false;
   }

   @Override
   public List<ItemStack> getDrops(BlockGetter world, BlockPos pos, BlockState state, int fortune) {
      switch ((BlockSheet.SheetType)this.getType(state)) {
         case resin:
            if (IC2.random.nextInt(5) != 0) {
               List<ItemStack> ret = new ArrayList<>();
               ret.add(ItemName.misc_resource.getItemStack(MiscResourceType.resin));
               return ret;
            }

            return new ArrayList<>();
         default:
            return super.getDrops(world, pos, state, fortune);
      }
   }

   public static enum SheetType implements IIdProvider, IExtBlockType {
      resin(1.6F, 0.5F),
      rubber(0.8F, 2.0F),
      @NotClassic
      wool(0.8F, 0.8F);

      public static BlockSheet.SheetType[] values = values();
      private final float hardness;
      private final float explosionResistance;

      private SheetType(float hardness, float explosionResistance) {
         this.hardness = hardness;
         this.explosionResistance = explosionResistance;
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
   }
}
