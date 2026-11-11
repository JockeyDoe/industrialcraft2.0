package ic2.core.block.wiring;

import ic2.api.energy.EnergyNet;
import ic2.api.energy.event.EnergyTileLoadEvent;
import ic2.api.energy.event.EnergyTileUnloadEvent;
import ic2.api.energy.tile.IColoredEnergyTile;
import ic2.api.energy.tile.IEnergyAcceptor;
import ic2.api.energy.tile.IEnergyConductor;
import ic2.api.energy.tile.IEnergyEmitter;
import ic2.api.energy.tile.IEnergyTile;
import ic2.api.network.INetworkTileEntityEventListener;
import ic2.core.IC2;
import ic2.core.IWorldTickCallback;
import ic2.core.block.BlockFoam;
import ic2.core.block.BlockWall;
import ic2.core.block.ITeBlock;
import ic2.core.block.NativeBlockEntities;
import ic2.core.block.TileEntityBlock;
import ic2.core.block.TileEntityWall;
import ic2.core.block.comp.Obscuration;
import ic2.core.block.state.Ic2BlockState;
import ic2.core.block.state.UnlistedProperty;
import ic2.core.item.block.ItemCable;
import ic2.core.item.tool.ItemToolCutter;
import ic2.core.ref.BlockName;
import ic2.core.ref.ItemName;
import ic2.core.ref.TeBlock;
import ic2.core.util.Ic2Color;
import ic2.core.util.LogCategory;
import ic2.core.util.StackUtil;
import ic2.core.util.Util;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.common.NeoForge;

@TeBlock.Delegated(current = TileEntityCable.class, old = TileEntityClassicCable.class)
public class TileEntityCable extends TileEntityBlock implements IEnergyConductor, INetworkTileEntityEventListener, IColoredEnergyTile {
   public static final float insulationThickness = 0.0625F;
   public static final UnlistedProperty<TileEntityCable.CableRenderState> renderStateProperty = new UnlistedProperty<>(
      "renderstate", TileEntityCable.CableRenderState.class
   );
   protected CableType cableType = CableType.copper;
   protected int insulation;
   private Ic2Color color = Ic2Color.black;
   private CableFoam foam = CableFoam.None;
   private Ic2Color foamColor = BlockWall.defaultColor;
   private final Obscuration obscuration;
   private byte connectivity = 0;
   private volatile TileEntityCable.CableRenderState renderState;
   private volatile TileEntityWall.WallRenderState wallRenderState;
   public boolean addedToEnergyNet = false;
   private IWorldTickCallback continuousUpdate = null;
   private static final int EventRemoveConductor = 0;

   public static Class<? extends TileEntityCable> delegate() {
      return IC2.version.isClassic() ? TileEntityClassicCable.class : TileEntityCable.class;
   }

   public static TileEntityCable delegate(CableType cableType, int insulation) {
      return (TileEntityCable)(IC2.version.isClassic() ? new TileEntityClassicCable(cableType, insulation) : new TileEntityCable(cableType, insulation));
   }

   public static TileEntityCable delegate(CableType cableType, int insulation, Ic2Color color) {
      return (TileEntityCable)(IC2.version.isClassic()
         ? new TileEntityClassicCable(cableType, insulation, color)
         : new TileEntityCable(cableType, insulation, color));
   }

   public TileEntityCable(CableType cableType, int insulation) {
      this();
      this.cableType = cableType;
      this.insulation = insulation;
   }

   public TileEntityCable(CableType cableType, int insulation, Ic2Color color) {
      this(cableType, insulation);
      if (this.canBeColored(color)) {
         this.color = color;
      }
   }

   public TileEntityCable() {
      this.obscuration = this.addComponent(new Obscuration(this, new Runnable() {
         {
            Objects.requireNonNull(TileEntityCable.this);
         }

         @Override
         public void run() {
            IC2.network.get(true).updateTileEntityField(TileEntityCable.this, "obscuration");
         }
      }));
   }

   @Override
   public void loadAdditional(CompoundTag nbt) {
      super.loadAdditional(nbt);
      this.cableType = CableType.values[nbt.getByteOr("cableType", (byte)0) & 255];
      this.insulation = nbt.getByteOr("insulation", (byte)0) & 255;
      this.color = Ic2Color.values[nbt.getByteOr("color", (byte)0) & 255];
      this.foam = CableFoam.values[nbt.getByteOr("foam", (byte)0) & 255];
      this.foamColor = Ic2Color.values[nbt.getByteOr("foamColor", (byte)0) & 255];
   }

   @Override
   public CompoundTag saveAdditional(CompoundTag nbt) {
      super.saveAdditional(nbt);
      nbt.putByte("cableType", (byte)this.cableType.ordinal());
      nbt.putByte("insulation", (byte)this.insulation);
      nbt.putByte("color", (byte)this.color.ordinal());
      nbt.putByte("foam", (byte)this.foam.ordinal());
      nbt.putByte("foamColor", (byte)this.foamColor.ordinal());
      return nbt;
   }

   @Override
   protected void onLoaded() {
      super.onLoaded();
      if (this.getLevel().isClientSide()) {
         this.updateRenderState();
      } else {
         if (this.getClass() == TileEntityCable.class && (this.cableType == CableType.detector || this.cableType == CableType.splitter)) {
            IC2.log.debug(LogCategory.Block, "Fixing incorrect cable TE %s.", Util.toString(this));
            TileEntityCable newTe = (TileEntityCable)(this.cableType == CableType.detector ? new TileEntityCableDetector() : new TileEntityCableSplitter());
            CompoundTag nbt = new CompoundTag();
            this.saveAdditional(nbt);
            this.getLevel().setBlockEntity(NativeBlockEntities.at(newTe, this.getBlockPos()));
            newTe.loadAdditional(nbt);
            return;
         }

         NeoForge.EVENT_BUS.post(new EnergyTileLoadEvent(this));
         this.addedToEnergyNet = true;
         this.updateConnectivity();
         if (this.foam == CableFoam.Soft) {
            this.changeFoam(this.foam, true);
         }
      }
   }

   @Override
   protected void onUnloaded() {
      if (IC2.platform.isSimulating() && this.addedToEnergyNet) {
         NeoForge.EVENT_BUS.post(new EnergyTileUnloadEvent(this));
         this.addedToEnergyNet = false;
      }

      if (this.continuousUpdate != null) {
         IC2.tickHandler.removeContinuousWorldTick(this.getLevel(), this.continuousUpdate);
         this.continuousUpdate = null;
      }

      super.onUnloaded();
   }

   @Override
   protected SoundType getBlockSound(Entity entity) {
      return SoundType.METAL;
   }

   @Override
   public void onPlaced(ItemStack stack, LivingEntity placer, Direction facing) {
      this.updateRenderState();
      super.onPlaced(stack, placer, facing);
   }

   @Override
   protected ItemStack getPickBlock(Player player, HitResult target) {
      return ItemCable.getCable(this.cableType, this.insulation);
   }

   @Override
   protected List<AABB> getAabbs(boolean forCollision) {
      if (this.foam != CableFoam.Hardened && (this.foam != CableFoam.Soft || forCollision)) {
         float th = this.cableType.thickness + this.insulation * 2 * 0.0625F;
         float sp = (1.0F - th) / 2.0F;
         List<AABB> ret = new ArrayList<>(7);
         ret.add(new AABB(sp, sp, sp, sp + th, sp + th, sp + th));

         for (Direction facing : Direction.values()) {
            boolean hasConnection = (this.connectivity & 1 << facing.ordinal()) != 0;
            if (hasConnection) {
               float zS = sp;
               float yS = sp;
               float xS = sp;
               float yE;
               float zE;
               float xE = yE = zE = sp + th;
               switch (facing) {
                  case DOWN:
                     yS = 0.0F;
                     yE = sp;
                     break;
                  case UP:
                     yS = sp + th;
                     yE = 1.0F;
                     break;
                  case NORTH:
                     zS = 0.0F;
                     zE = sp;
                     break;
                  case SOUTH:
                     zS = sp + th;
                     zE = 1.0F;
                     break;
                  case WEST:
                     xS = 0.0F;
                     xE = sp;
                     break;
                  case EAST:
                     xS = sp + th;
                     xE = 1.0F;
                     break;
                  default:
                     throw new RuntimeException();
               }

               ret.add(new AABB(xS, yS, zS, xE, yE, zE));
            }
         }

         return ret;
      } else {
         return super.getAabbs(forCollision);
      }
   }

   @Override
   protected boolean isNormalCube() {
      return this.foam == CableFoam.Hardened || this.foam == CableFoam.Soft;
   }

   @Override
   protected boolean isSideSolid(Direction side) {
      return this.foam == CableFoam.Hardened;
   }

   @Override
   protected boolean doesSideBlockRendering(Direction side) {
      return this.foam == CableFoam.Hardened;
   }

   @Override
   public Ic2BlockState.Ic2BlockStateInstance getExtendedState(Ic2BlockState.Ic2BlockStateInstance state) {
      state = super.getExtendedState(state);
      TileEntityCable.CableRenderState cableRenderState = this.renderState;
      if (cableRenderState != null) {
         state = state.withProperties(renderStateProperty, cableRenderState);
      }

      TileEntityWall.WallRenderState wallRenderState = this.wallRenderState;
      if (wallRenderState != null) {
         state = state.withProperties(TileEntityWall.renderStateProperty, wallRenderState);
      }

      return state;
   }

   @Override
   public void onNeighborChange(Block neighbor, BlockPos neighborPos) {
      super.onNeighborChange(neighbor, neighborPos);
      if (!this.getLevel().isClientSide()) {
         this.updateConnectivity();
      }
   }

   private void updateConnectivity() {
      Level world = this.getLevel();
      byte newConnectivity = 0;
      int mask = 1;

      for (Direction dir : Direction.values()) {
         IEnergyTile tile = EnergyNet.instance.getSubTile(world, this.getBlockPos().relative(dir));
         if ((
               tile instanceof IEnergyAcceptor && ((IEnergyAcceptor)tile).acceptsEnergyFrom(this, dir.getOpposite())
                  || tile instanceof IEnergyEmitter && ((IEnergyEmitter)tile).emitsEnergyTo(this, dir.getOpposite())
            )
            && this.canInteractWith(tile, dir)) {
            newConnectivity = (byte)(newConnectivity | mask);
         }

         mask *= 2;
      }

      if (this.connectivity != newConnectivity) {
         this.connectivity = newConnectivity;
         IC2.network.get(true).updateTileEntityField(this, "connectivity");
      }
   }

   @Override
   protected boolean onActivated(Player player, InteractionHand hand, Direction side, float hitX, float hitY, float hitZ) {
      if (this.foam == CableFoam.Soft && StackUtil.consume(player, hand, StackUtil.sameItem(Blocks.SAND), 1)) {
         this.changeFoam(CableFoam.Hardened, false);
         return true;
      } else if (this.foam == CableFoam.None && StackUtil.consume(player, hand, StackUtil.sameStack(BlockName.foam.getItemStack(BlockFoam.FoamType.normal)), 1)
         )
       {
         this.foam();
         return true;
      } else {
         return super.onActivated(player, hand, side, hitX, hitY, hitZ);
      }
   }

   @Override
   protected void onClicked(Player player) {
      super.onClicked(player);
      ItemToolCutter cutter = ItemName.cutter.getInstance();
      if (!cutter.removeInsulation(player, InteractionHand.MAIN_HAND, this)) {
         cutter.removeInsulation(player, InteractionHand.OFF_HAND, this);
      }
   }

   @Override
   protected float getHardness() {
      switch (this.foam) {
         case Soft:
            return BlockName.foam.getInstance().defaultBlockState().getDestroySpeed(this.getLevel(), this.getBlockPos());
         case Hardened:
            return BlockName.wall.getInstance().defaultBlockState().getDestroySpeed(this.getLevel(), this.getBlockPos());
         case None:
         default:
            return super.getHardness();
      }
   }

   @Override
   protected float getExplosionResistance(Entity exploder, Explosion explosion) {
      switch (this.foam) {
         case Soft:
         case None:
         default:
            return super.getHardness();
         case Hardened:
            return BlockName.wall.getInstance().defaultBlockState().getExplosionResistance(this.getLevel(), this.getBlockPos(), explosion);
      }
   }

   @Override
   protected int getLightOpacity() {
      return this.foam == CableFoam.Hardened ? 255 : 0;
   }

   private boolean canBeColored(Ic2Color newColor) {
      switch (this.foam) {
         case Soft:
         default:
            return false;
         case Hardened:
            return this.color != newColor;
         case None:
            return this.color != newColor && this.cableType.minColoredInsulation <= this.insulation;
      }
   }

   @Override
   protected boolean recolor(Direction side, DyeColor mcColor) {
      Ic2Color newColor = Ic2Color.get(mcColor);
      if (!this.canBeColored(newColor)) {
         return false;
      } else {
         if (!this.getLevel().isClientSide()) {
            if (this.foam == CableFoam.None) {
               if (this.addedToEnergyNet) {
                  NeoForge.EVENT_BUS.post(new EnergyTileUnloadEvent(this));
               }

               this.addedToEnergyNet = false;
               this.color = newColor;
               NeoForge.EVENT_BUS.post(new EnergyTileLoadEvent(this));
               this.addedToEnergyNet = true;
               IC2.network.get(true).updateTileEntityField(this, "color");
               this.updateConnectivity();
            } else {
               this.foamColor = newColor;
               IC2.network.get(true).updateTileEntityField(this, "foamColor");
               this.obscuration.clear();
            }

            this.setChanged();
         }

         return true;
      }
   }

   @Override
   protected boolean onRemovedByPlayer(Player player, boolean willHarvest) {
      return this.changeFoam(CableFoam.None, false) ? false : super.onRemovedByPlayer(player, willHarvest);
   }

   public boolean isFoamed() {
      return this.foam != CableFoam.None;
   }

   public boolean foam() {
      return this.changeFoam(CableFoam.Soft, false);
   }

   public boolean tryAddInsulation() {
      if (this.insulation >= this.cableType.maxInsulation) {
         return false;
      } else {
         this.insulation++;
         if (!this.getLevel().isClientSide()) {
            IC2.network.get(true).updateTileEntityField(this, "insulation");
         }

         return true;
      }
   }

   public boolean tryRemoveInsulation(boolean simulate) {
      if (this.insulation <= 0) {
         return false;
      } else if (simulate) {
         return true;
      } else {
         if (this.insulation == this.cableType.minColoredInsulation) {
            CableFoam foam = this.foam;
            this.foam = CableFoam.None;
            this.recolor(this.getFacing(), DyeColor.BLACK);
            this.foam = foam;
         }

         this.insulation--;
         if (!this.getLevel().isClientSide()) {
            IC2.network.get(true).updateTileEntityField(this, "insulation");
         }

         return true;
      }
   }

   @Override
   public boolean wrenchCanRemove(Player player) {
      return false;
   }

   @Override
   public boolean acceptsEnergyFrom(IEnergyEmitter emitter, Direction direction) {
      return this.canInteractWith(emitter, direction);
   }

   @Override
   public boolean emitsEnergyTo(IEnergyAcceptor receiver, Direction direction) {
      return this.canInteractWith(receiver, direction);
   }

   public boolean canInteractWith(IEnergyTile tile, Direction side) {
      if (!(tile instanceof IColoredEnergyTile other)) {
         return true;
      } else {
         DyeColor thisColor = this.getColor(side);
         DyeColor otherColor = other.getColor(side.getOpposite());
         return thisColor == null || otherColor == null || thisColor == otherColor;
      }
   }

   @Override
   public double getConductionLoss() {
      return this.cableType.loss;
   }

   @Override
   public double getInsulationEnergyAbsorption() {
      if (this.cableType.maxInsulation == 0) {
         return 2.147483647E9;
      } else {
         return this.cableType == CableType.tin
            ? EnergyNet.instance.getPowerFromTier(this.insulation)
            : EnergyNet.instance.getPowerFromTier(this.insulation + 1);
      }
   }

   @Override
   public double getInsulationBreakdownEnergy() {
      return 9001.0;
   }

   @Override
   public double getConductorBreakdownEnergy() {
      return this.cableType.capacity + 1;
   }

   @Override
   public void removeInsulation() {
      this.tryRemoveInsulation(false);
   }

   @Override
   public void removeConductor() {
      this.getLevel().removeBlock(this.getBlockPos(), false);
      IC2.network.get(true).initiateTileEntityEvent(this, 0, true);
   }

   @Override
   public DyeColor getColor(Direction side) {
      return this.color == Ic2Color.black ? null : this.color.mcColor;
   }

   @Override
   public List<String> getNetworkedFields() {
      List<String> ret = new ArrayList<>();
      ret.add("cableType");
      ret.add("insulation");
      ret.add("color");
      ret.add("foam");
      ret.add("connectivity");
      ret.add("obscuration");
      ret.addAll(super.getNetworkedFields());
      return ret;
   }

   @Override
   public void onNetworkUpdate(String field) {
      this.updateRenderState();
      if (field.equals("foam") && (this.foam == CableFoam.None || this.foam == CableFoam.Hardened)) {
         this.relight();
      }

      this.rerender();
      super.onNetworkUpdate(field);
   }

   private void relight() {
   }

   @Override
   public void onNetworkEvent(int event) {
      Level world = this.getLevel();
      switch (event) {
         case 0:
            world.playLocalSound(
               this.getBlockPos(),
               SoundEvents.GENERIC_BURN,
               SoundSource.BLOCKS,
               0.5F,
               2.6F + (world.getRandom().nextFloat() - world.getRandom().nextFloat()) * 0.8F,
               false
            );

            for (int l = 0; l < 8; l++) {
               world.addParticle(
                  ParticleTypes.LARGE_SMOKE,
                  this.getBlockPos().getX() + Math.random(),
                  this.getBlockPos().getY() + 1.2,
                  this.getBlockPos().getZ() + Math.random(),
                  0.0,
                  0.0,
                  0.0
               );
            }
            break;
         default:
            IC2.platform
               .displayError(
                  "An unknown event type was received over multiplayer.\nThis could happen due to corrupted data or a bug.\n\n(Technical information: event ID "
                     + event
                     + ", tile entity below)\nT: "
                     + this
                     + " ("
                     + this.getBlockPos()
                     + ")"
               );
      }
   }

   private boolean changeFoam(CableFoam foam, boolean duringLoad) {
      if (this.foam == foam && !duringLoad) {
         return false;
      } else {
         Level world = this.getLevel();
         if (world.isClientSide()) {
            return true;
         } else {
            this.foam = foam;
            if (this.continuousUpdate != null) {
               IC2.tickHandler.removeContinuousWorldTick(world, this.continuousUpdate);
               this.continuousUpdate = null;
            }

            if (foam != CableFoam.Hardened) {
               this.obscuration.clear();
               if (this.foamColor != BlockWall.defaultColor) {
                  this.foamColor = BlockWall.defaultColor;
                  if (!duringLoad) {
                     IC2.network.get(true).updateTileEntityField(this, "foamColor");
                  }
               }
            }

            if (foam == CableFoam.Soft) {
               this.continuousUpdate = new IWorldTickCallback() {
                  {
                     Objects.requireNonNull(TileEntityCable.this);
                  }

                  @Override
                  public void onTick(Level world) {
                     if (world.getRandom().nextFloat()
                        < BlockFoam.getHardenChance(
                           world,
                           TileEntityCable.this.getBlockPos(),
                           TileEntityCable.this.getBlockType().getState((ITeBlock)TeBlock.cable),
                           BlockFoam.FoamType.normal
                        )) {
                        TileEntityCable.this.changeFoam(CableFoam.Hardened, false);
                     }
                  }
               };
               IC2.tickHandler.requestContinuousWorldTick(world, this.continuousUpdate);
            }

            if (!duringLoad) {
               IC2.network.get(true).updateTileEntityField(this, "foam");
               world.updateNeighborsAt(this.getBlockPos(), this.getBlockType());
               this.setChanged();
            }

            return true;
         }
      }
   }

   @Override
   protected boolean clientNeedsExtraModelInfo() {
      return true;
   }

   private void updateRenderState() {
      this.renderState = new TileEntityCable.CableRenderState(this.cableType, this.insulation, this.color, this.foam, this.connectivity, this.getActive());
      this.wallRenderState = new TileEntityWall.WallRenderState(this.foamColor, this.obscuration.getRenderState());
   }

   public static class CableRenderState {
      public final CableType type;
      public final int insulation;
      public final Ic2Color color;
      public final CableFoam foam;
      public final int connectivity;
      public final boolean active;

      public CableRenderState(CableType type, int insulation, Ic2Color color, CableFoam foam, int connectivity, boolean active) {
         this.type = type;
         this.insulation = insulation;
         this.color = color;
         this.foam = foam;
         this.connectivity = connectivity;
         this.active = active;
      }

      @Override
      public int hashCode() {
         int ret = this.type.hashCode();
         ret = ret * 31 + this.insulation;
         ret = ret * 31 + this.color.hashCode();
         ret = ret * 31 + this.foam.hashCode();
         ret = ret * 31 + this.connectivity;
         return ret << 1 | (this.active ? 1 : 0);
      }

      @Override
      public boolean equals(Object obj) {
         if (obj == this) {
            return true;
         } else {
            return !(obj instanceof TileEntityCable.CableRenderState o)
               ? false
               : o.type == this.type
                  && o.insulation == this.insulation
                  && o.color == this.color
                  && o.foam == this.foam
                  && o.connectivity == this.connectivity
                  && o.active == this.active;
         }
      }

      @Override
      public String toString() {
         return "CableState<" + this.type + ", " + this.insulation + ", " + this.color + ", " + this.foam + ", " + this.connectivity + ", " + this.active + ">";
      }
   }
}
