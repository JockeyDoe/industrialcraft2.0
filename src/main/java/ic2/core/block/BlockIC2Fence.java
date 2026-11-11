package ic2.core.block;

import ic2.api.item.ItemWrapper;
import ic2.core.IC2;
import ic2.core.block.machine.tileentity.TileEntityMagnetizer;
import ic2.core.block.state.BlockStates;
import ic2.core.block.state.IIdProvider;
import ic2.core.block.type.IExtBlockType;
import ic2.core.model.ModelKey;
import ic2.core.model.ModelRegistry;
import ic2.core.ref.BlockMaterial;
import ic2.core.ref.BlockName;
import ic2.core.slot.ArmorSlot;
import ic2.core.util.DirectionOrder;
import ic2.core.util.Ic2BlockPos;
import ic2.core.util.Util;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class BlockIC2Fence extends BlockMultiID<BlockIC2Fence.IC2FenceType> {
   public static final Map<Direction, Property<Boolean>> connectProperties = getConnectProperties();
   private static final double halfThickness = 0.125;
   private static final double height = 1.5;
   private static final Map<Property<Boolean>, AABB> aabbs = getAabbs();

   public static BlockIC2Fence create() {
      return BlockMultiID.create(BlockIC2Fence.class, BlockIC2Fence.IC2FenceType.class);
   }

   private BlockIC2Fence() {
      super(BlockName.fence, BlockMaterial.IRON);
      BlockState defaultState = BlockStates.with((BlockState)this.stateDefinition.any(), this.typeProperty, this.typeProperty.getDefault());

      for (Property<Boolean> property : connectProperties.values()) {
         defaultState = (BlockState)defaultState.setValue(property, false);
      }

      this.registerDefaultState(defaultState);
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public void registerModels(BlockName name) {
      Item item = Item.byBlock(this);
      if (item != null && item != Items.AIR) {
         Identifier loc = Util.getName(item);
         if (loc != null) {
            for (BlockState state : this.getTypeStates()) {
               ModelRegistry.setCustomModelResourceLocation(
                  item,
                  this.func_176201_c(state),
                  new ModelKey(loc.toString() + "/" + BlockStates.<BlockIC2Fence.IC2FenceType>value(state, this.typeProperty).getName(), null)
               );
            }
         }
      }
   }

   @Override
   public boolean func_149686_d(BlockState state) {
      return false;
   }

   @Override
   protected StateDefinition func_180661_e() {
      List<Property<?>> properties = new ArrayList<>();
      properties.add(this.getTypeProperty());
      properties.addAll(connectProperties.values());
      return BlockStates.definition(this, properties.toArray(new Property[0]));
   }

   public BlockState func_176221_a(BlockState state, BlockGetter world, BlockPos pos) {
      boolean isPole = true;
      boolean magnetizerConnected = false;
      BlockState ret = state;

      for (Direction facing : DirectionOrder.HORIZONTAL) {
         BlockState neighborState = world.getBlockState(pos.relative(facing));
         if (isFence(neighborState)) {
            isPole = false;
            if (magnetizerConnected) {
               break;
            }

            ret = (BlockState)ret.setValue(connectProperties.get(facing), true);
         } else if (isPole && getMagnetizer(world, pos.relative(facing), facing, world.getBlockState(pos.relative(facing)), false) != null) {
            magnetizerConnected = true;
            ret = (BlockState)ret.setValue(connectProperties.get(facing), true);
         }
      }

      if (!isPole && magnetizerConnected) {
         ret = state;

         for (Direction facingx : DirectionOrder.HORIZONTAL) {
            BlockState neighborState = world.getBlockState(pos.relative(facingx));
            if (isFence(neighborState)) {
               ret = (BlockState)ret.setValue(connectProperties.get(facingx), true);
            }
         }
      }

      return ret;
   }

   public boolean func_149662_c(BlockState state) {
      return false;
   }

   public boolean func_149721_r(BlockState state) {
      return false;
   }

   public boolean isSideSolid(BlockState state, BlockGetter world, BlockPos blockPos, Direction side) {
      return side.getAxis() == Axis.Y;
   }

   public boolean canPlaceTorchOnTop(BlockState state, BlockGetter world, BlockPos pos) {
      return true;
   }

   protected VoxelShape getBlockSupportShape(BlockState state, BlockGetter world, BlockPos pos) {
      return Shapes.box(0.375, 0.0, 0.375, 0.625, 1.0, 0.625);
   }

   public void func_180634_a(Level world, BlockPos pos, BlockState state, Entity rawEntity) {
      if (rawEntity instanceof Player) {
         boolean powered = this.isPowered(world, pos, BlockStates.value(state, this.typeProperty));
         Player player = (Player)rawEntity;
         boolean metalShoes = hasMetalShoes(player);
         boolean descending = player.isShiftKeyDown();
         boolean slow = player.getDeltaMovement().y >= -0.25 || player.getDeltaMovement().y < 1.6;
         if (slow) {
            player.fallDistance = 0.0;
         }

         if (!powered) {
            if (descending && !slow && metalShoes) {
               player.setDeltaMovement(new Vec3(player.getDeltaMovement().x, player.getDeltaMovement().y * 0.9, player.getDeltaMovement().z));
            }
         } else if (descending) {
            if (!slow) {
               player.setDeltaMovement(new Vec3(player.getDeltaMovement().x, player.getDeltaMovement().y * 0.8, player.getDeltaMovement().z));
            }
         } else {
            player.setDeltaMovement(new Vec3(player.getDeltaMovement().x, player.getDeltaMovement().y + 0.075, player.getDeltaMovement().z));
            if (player.getDeltaMovement().y > 0.0) {
               player.setDeltaMovement(new Vec3(player.getDeltaMovement().x, player.getDeltaMovement().y * 1.03, player.getDeltaMovement().z));
            }

            double maxSpeed = IC2.keyboard.isAltKeyDown(player) ? 0.1 : (metalShoes ? 1.5 : 0.5);
            player.setDeltaMovement(new Vec3(player.getDeltaMovement().x, Math.min(player.getDeltaMovement().y, maxSpeed), player.getDeltaMovement().z));
         }

         if (!world.isClientSide()) {
            for (TileEntityMagnetizer magnetizer : this.getMagnetizers(world, pos, false)) {
               IC2.network.get(true).updateTileEntityField(magnetizer, "energy");
            }
         }
      }
   }

   public void func_185477_a(BlockState state, Level world, BlockPos pos, AABB mask, List<AABB> result, Entity collidingEntity, boolean isActualState) {
      if (!isActualState) {
         state = this.func_176221_a(state, world, pos);
      }

      func_185492_a(pos, mask, result, aabbs.get(null));

      for (Property<Boolean> property : connectProperties.values()) {
         if ((Boolean)state.getValue(property)) {
            func_185492_a(pos, mask, result, aabbs.get(property));
         }
      }
   }

   @Override
   public AABB func_185496_a(BlockState state, BlockGetter world, BlockPos pos) {
      AABB ret = aabbs.get(null);
      double xS = ret.minX;
      double yS = 0.0;
      double zS = ret.minZ;
      double xE = ret.maxX;
      double yE = 1.0;
      double zE = ret.maxZ;
      state = this.func_176221_a(state, world, pos);

      for (Property<Boolean> property : connectProperties.values()) {
         if ((Boolean)state.getValue(property)) {
            AABB aabb = aabbs.get(property);
            xS = Math.min(xS, aabb.minX);
            zS = Math.min(zS, aabb.minZ);
            xE = Math.max(xE, aabb.maxX);
            zE = Math.max(zE, aabb.maxZ);
         }
      }

      return new AABB(xS, 0.0, zS, xE, 1.0, zE);
   }

   private static boolean isFence(BlockState state) {
      return state.getBlock() instanceof BlockIC2Fence || state.getBlock() instanceof FenceBlock;
   }

   private static TileEntityMagnetizer getMagnetizer(BlockGetter world, BlockPos pos, Direction side, BlockState state, boolean checkPower) {
      if (state.getBlock() != BlockName.te.getInstance()) {
         return null;
      } else {
         if (world.getBlockEntity(pos) instanceof TileEntityMagnetizer ret) {
            if (side != null && !side.getOpposite().equals(ret.getFacing())) {
               return null;
            }

            if (!checkPower || ret.canBoost()) {
               return ret;
            }
         }

         return null;
      }
   }

   public static boolean hasMetalShoes(Player player) {
      ItemStack shoes = ArmorSlot.getEquipment(player.getInventory()).get(0);
      if (shoes != null) {
         Item item = shoes.getItem();
         if (item == Items.IRON_BOOTS || item == Items.GOLDEN_BOOTS || item == Items.CHAINMAIL_BOOTS || ItemWrapper.isMetalArmor(shoes, player)) {
            return true;
         }
      }

      return false;
   }

   private boolean isPowered(Level world, BlockPos start, BlockIC2Fence.IC2FenceType type) {
      if (!type.canBoost) {
         return false;
      } else {
         List<TileEntityMagnetizer> magnetizers = this.getMagnetizers(world, start, true);
         if (magnetizers.isEmpty()) {
            return false;
         } else {
            double multiplier = 1.0 / magnetizers.size();

            for (TileEntityMagnetizer magnetizer : magnetizers) {
               magnetizer.boost(multiplier);
            }

            return true;
         }
      }
   }

   private List<TileEntityMagnetizer> getMagnetizers(BlockGetter world, BlockPos start, boolean checkPower) {
      int maxRange = 20;
      List<TileEntityMagnetizer> ret = new ArrayList<>();
      Ic2BlockPos center = new Ic2BlockPos(start);
      Ic2BlockPos tmp = new Ic2BlockPos();

      for (Direction facing : DirectionOrder.HORIZONTAL) {
         Ic2BlockPos nPos = tmp.set(center).move(facing);
         BlockState state = nPos.getBlockState(world);
         if (isFence(state)) {
            return Collections.emptyList();
         }

         TileEntityMagnetizer te;
         if ((te = getMagnetizer(world, nPos, facing, state, checkPower)) != null) {
            ret.add(te);
         }
      }

      if (!ret.isEmpty()) {
         return ret;
      } else {
         int minDir = 0;
         int maxDir = 2;

         for (int dy = 1; dy <= 20; dy++) {
            boolean abort = false;
            int dir = minDir;

            label79:
            while (dir < maxDir) {
               int offset = dir * 2 - 1;
               center.setY(start.getY() + offset * dy);
               BlockState centerState = center.getBlockState(world);
               if (centerState.getBlock() instanceof BlockIC2Fence && BlockStates.value(centerState, this.typeProperty).canBoost) {
                  int oldSize = ret.size();
                  Direction[] var16 = DirectionOrder.HORIZONTAL;
                  int var17 = var16.length;
                  int var18 = 0;

                  while (true) {
                     if (var18 < var17) {
                        Direction facing = var16[var18];
                        Ic2BlockPos nPosx = tmp.set(center).move(facing);
                        BlockState statex = nPosx.getBlockState(world);
                        if (!isFence(statex)) {
                           TileEntityMagnetizer te;
                           if ((te = getMagnetizer(world, nPosx, facing, statex, checkPower)) != null) {
                              abort = true;
                              ret.add(te);
                           }

                           var18++;
                           continue;
                        }

                        if (dir == 0) {
                           minDir = 1;
                        } else {
                           maxDir = 1;
                        }

                        if (minDir == maxDir) {
                           abort = true;
                        }

                        while (ret.size() > oldSize) {
                           ret.remove(ret.size() - 1);
                        }
                     }

                     dir++;
                     continue label79;
                  }
               }

               if (dir == 0) {
                  minDir = 1;
               } else {
                  maxDir = 1;
               }

               if (minDir == maxDir) {
                  abort = true;
               }
               break;
            }

            if (abort) {
               break;
            }
         }

         return ret;
      }
   }

   private static Map<Direction, Property<Boolean>> getConnectProperties() {
      Map<Direction, Property<Boolean>> ret = new EnumMap<>(Direction.class);

      for (Direction facing : DirectionOrder.HORIZONTAL) {
         ret.put(facing, BooleanProperty.create(facing.getName()));
      }

      return ret;
   }

   private static Map<Property<Boolean>, AABB> getAabbs() {
      Map<Property<Boolean>, AABB> ret = new IdentityHashMap<>(connectProperties.size() + 1);
      double spaceL = 0.375;
      double spaceR = 0.625;
      ret.put(null, new AABB(0.375, 0.0, 0.375, 0.625, 1.5, 0.625));

      for (Direction facing : DirectionOrder.HORIZONTAL) {
         double start;
         double end;
         if (facing.getAxisDirection() == AxisDirection.NEGATIVE) {
            start = 0.0;
            end = 0.375;
         } else {
            start = 0.625;
            end = 1.0;
         }

         AABB aabb;
         if (facing.getAxis() == Axis.X) {
            aabb = new AABB(start, 0.0, 0.375, end, 1.5, 0.625);
         } else {
            aabb = new AABB(0.375, 0.0, start, 0.625, 1.5, end);
         }

         ret.put(connectProperties.get(facing), aabb);
      }

      return ret;
   }

   public static enum IC2FenceType implements IIdProvider, IExtBlockType {
      iron(true, 5.0F, 10.0F);

      public final boolean canBoost;
      private final float hardness;
      private final float explosionResistance;

      private IC2FenceType(boolean canBoost, float hardness, float explosionResistance) {
         this.canBoost = canBoost;
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
