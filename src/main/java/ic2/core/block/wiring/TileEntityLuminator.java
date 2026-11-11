package ic2.core.block.wiring;

import ic2.api.energy.EnergyNet;
import ic2.api.energy.tile.IEnergyEmitter;
import ic2.api.energy.tile.IEnergyTile;
import ic2.api.item.ElectricItem;
import ic2.core.IC2;
import ic2.core.IWorldTickCallback;
import ic2.core.Ic2Player;
import ic2.core.block.TileEntityBlock;
import ic2.core.block.comp.ComparatorEmitter;
import ic2.core.block.comp.Energy;
import ic2.core.block.comp.Redstone;
import ic2.core.util.StackUtil;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;

public class TileEntityLuminator extends TileEntityBlock {
   private static final int manualChargeCapacity = 10000;
   private static final Map<Direction, List<AABB>> aabbMap = getAabbMap();
   private final Energy energy = this.addComponent(Energy.asBasicSink(this, 5.0));
   private final Redstone redstone = this.addComponent(new Redstone(this));
   private final ComparatorEmitter comparator = this.addComponent(new ComparatorEmitter(this));
   private boolean invertRedstone;
   public static boolean ignoreBlockStay = false;

   public TileEntityLuminator() {
      this.comparator.setUpdate(this.energy::getComparatorValue);
   }

   @Override
   public void loadAdditional(CompoundTag nbt) {
      super.loadAdditional(nbt);
      this.invertRedstone = nbt.getBooleanOr("invert", false);
   }

   @Override
   public CompoundTag saveAdditional(CompoundTag nbt) {
      super.saveAdditional(nbt);
      nbt.putBoolean("invert", this.invertRedstone);
      return nbt;
   }

   @Override
   public void onLoaded() {
      this.energy.setDirections(Collections.singleton(this.getFacing().getOpposite()), Collections.emptySet());
      super.onLoaded();
      IC2.tickHandler.requestSingleWorldTick(this.getLevel(), new IWorldTickCallback() {
         {
            Objects.requireNonNull(TileEntityLuminator.this);
         }

         @Override
         public void onTick(Level world) {
            TileEntityLuminator.this.checkPlacement();
         }
      });
   }

   @Override
   protected Direction getPlacementFacing(LivingEntity placer, Direction facing) {
      return facing;
   }

   @Override
   protected void updateEntityServer() {
      super.updateEntityServer();
      boolean lit = this.isLit() && this.energy.useEnergy(0.25);
      if (this.getActive() != lit) {
         this.setActive(lit);
         this.updateLight();
      }
   }

   private boolean isLit() {
      return this.redstone.hasRedstoneInput() != this.invertRedstone;
   }

   @Override
   protected boolean onActivated(Player player, InteractionHand hand, Direction side, float hitX, float hitY, float hitZ) {
      if (!this.getLevel().isClientSide()) {
         ItemStack stack = StackUtil.get(player, hand);
         double amount = 10000.0 - this.energy.getEnergy();
         if (stack != null && amount > 0.0 && (amount = ElectricItem.manager.discharge(stack, amount, this.energy.getSinkTier(), true, true, false)) > 0.0) {
            this.energy.forceAddEnergy(amount);
         } else {
            this.invertRedstone = !this.invertRedstone;
            IC2.network.get(true).updateTileEntityField(this, "invertRedstone");
         }
      }

      return true;
   }

   @Override
   protected void onNeighborChange(Block neighbor, BlockPos neighborPos) {
      super.onNeighborChange(neighbor, neighborPos);
      this.checkPlacement();
   }

   private void checkPlacement() {
      Level world = this.getLevel();
      if (!isValidPosition(world, this.getBlockPos().relative(this.getFacing().getOpposite()), this.getFacing())) {
         this.getBlockType()
            .playerDestroy(
               (ServerLevel)world, (ServerPlayer)Ic2Player.get(world), this.getBlockPos(), world.getBlockState(this.getBlockPos()), this, StackUtil.emptyStack
            );
         world.removeBlock(this.getBlockPos(), false);
      }
   }

   public static boolean isValidPosition(Level world, BlockPos pos, Direction side) {
      if (!world.isClientSide() && !ignoreBlockStay) {
         if (world.getBlockState(pos).isFaceSturdy(world, pos, side)) {
            return true;
         } else {
            IEnergyTile tile = EnergyNet.instance.getSubTile(world, pos);
            return tile instanceof IEnergyEmitter;
         }
      } else {
         return true;
      }
   }

   @Override
   protected List<AABB> getAabbs(boolean forCollision) {
      return aabbMap.get(this.getFacing());
   }

   @Override
   public int getLightValue() {
      return this.getActive() ? 15 : 0;
   }

   @Override
   protected void onEntityCollision(Entity entity) {
      super.onEntityCollision(entity);
      if (this.getActive() && entity instanceof Monster) {
         boolean isUndead = entity instanceof LivingEntity && entity.getType().builtInRegistryHolder().is(EntityTypeTags.UNDEAD);
         entity.igniteForSeconds(isUndead ? 20.0F : 10.0F);
      }
   }

   @Override
   protected boolean canSetFacingWrench(Direction facing, Player player) {
      return true;
   }

   @Override
   protected boolean setFacingWrench(Direction facing, Player player) {
      this.invertRedstone = !this.invertRedstone;
      return true;
   }

   @Override
   public boolean wrenchCanRemove(Player player) {
      return false;
   }

   @Override
   public void onNetworkUpdate(String field) {
      super.onNetworkUpdate(field);
      if (field.equals("active")) {
         this.updateLight();
      }
   }

   private void updateLight() {
      this.getLevel().getChunkSource().getLightEngine().checkBlock(this.getBlockPos());
   }

   private static Map<Direction, List<AABB>> getAabbMap() {
      Map<Direction, List<AABB>> ret = new EnumMap<>(Direction.class);
      double height = 0.0625;
      double remHeight = 0.9375;

      for (Direction side : Direction.values()) {
         int dx = side.getStepX();
         int dy = side.getStepY();
         int dz = side.getStepZ();
         double xS = (dx + 1) / 2 * 0.9375;
         double yS = (dy + 1) / 2 * 0.9375;
         double zS = (dz + 1) / 2 * 0.9375;
         double xE = 0.0625 + (dx + 2) / 2 * 0.9375;
         double yE = 0.0625 + (dy + 2) / 2 * 0.9375;
         double zE = 0.0625 + (dz + 2) / 2 * 0.9375;
         ret.put(side.getOpposite(), Arrays.asList(new AABB(xS, yS, zS, xE, yE, zE)));
      }

      return ret;
   }
}
