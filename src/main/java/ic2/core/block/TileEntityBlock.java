package ic2.core.block;

import ic2.api.network.INetworkDataProvider;
import ic2.api.network.INetworkUpdateListener;
import ic2.core.IC2;
import ic2.core.IHasGui;
import ic2.core.IWorldTickCallback;
import ic2.core.block.comp.BasicRedstoneComponent;
import ic2.core.block.comp.ComparatorEmitter;
import ic2.core.block.comp.Components;
import ic2.core.block.comp.Energy;
import ic2.core.block.comp.Redstone;
import ic2.core.block.comp.RedstoneEmitter;
import ic2.core.block.comp.TileEntityComponent;
import ic2.core.block.state.Ic2BlockState;
import ic2.core.block.state.MaterialProperty;
import ic2.core.block.type.ResourceBlock;
import ic2.core.entity.EntityTagData;
import ic2.core.gui.dynamic.IGuiConditionProvider;
import ic2.core.init.Localization;
import ic2.core.model.ModelComparator;
import ic2.core.ref.BlockMaterial;
import ic2.core.ref.BlockName;
import ic2.core.ref.MetaTeBlock;
import ic2.core.ref.MetaTeBlockProperty;
import ic2.core.ref.TeBlock;
import ic2.core.util.AabbUtil;
import ic2.core.util.LogCategory;
import ic2.core.util.NbtChecks;
import ic2.core.util.Util;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.capabilities.BlockCapability;
import org.apache.commons.lang3.mutable.MutableObject;

public abstract class TileEntityBlock extends BlockEntity implements INetworkDataProvider, INetworkUpdateListener, IGuiConditionProvider {
   public static final String teBlockName = "teBlk";
   public static final String oldMarker = "Old-";
   protected static final int lightOpacityTranslucent = 0;
   protected static final int lightOpacityOpaque = 255;
   private static final CompoundTag emptyNbt = new CompoundTag();
   private static final List<AABB> defaultAabbs = Arrays.asList(new AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0));
   private static final List<TileEntityComponent> emptyComponents = Collections.emptyList();
   private static final Map<Class<?>, TileEntityBlock.TickSubscription> tickSubscriptions = new HashMap<>();
   private static final byte loadStateInitial = 0;
   private static final byte loadStateQueued = 1;
   private static final byte loadStateLoaded = 2;
   private static final byte loadStateUnloaded = 3;
   private static final boolean debugLoad = System.getProperty("ic2.te.debugload") != null;
   protected final ITeBlock teBlock;
   private final BlockTileEntity block;
   private Map<Class<? extends TileEntityComponent>, TileEntityComponent> components;
   private Map<BlockCapability<?, Direction>, TileEntityComponent> capabilityComponents;
   private List<TileEntityComponent> updatableComponents;
   private boolean active = false;
   private byte facing = (byte)Direction.DOWN.ordinal();
   private byte loadState = 0;
   private boolean enableWorldTick;

   public Level getWorld() {
      return this.getLevel();
   }

   public BlockPos getPos() {
      return this.getBlockPos();
   }

   public BlockPos getLocation() {
      return this.getBlockPos();
   }

   public boolean isInvalid() {
      return this.isRemoved();
   }

   public static <T extends TileEntityBlock> T instantiate(Class<T> cls) {
      try {
         return cls.newInstance();
      } catch (Exception var2) {
         throw new RuntimeException(var2);
      }
   }

   public TileEntityBlock() {
      super(NativeBlockEntities.constructionType(), NativeBlockEntities.constructionPos(), NativeBlockEntities.constructionState());
      ITeBlock teb = TeBlockRegistry.get((Class<? extends TileEntityBlock>)this.getClass());
      this.teBlock = (ITeBlock)(teb == null ? TeBlock.invalid : teb);
      this.block = TeBlockRegistry.get(this.teBlock.getIdentifier());
   }

   public BlockEntityType<?> getType() {
      return NativeBlockEntities.type((Class<? extends TileEntityBlock>)this.getClass());
   }

   protected final void loadAdditional(ValueInput input) {
      super.loadAdditional(input);
      this.loadAdditional(EntityTagData.read(input));
   }

   protected final void saveAdditional(ValueOutput output) {
      super.saveAdditional(output);
      EntityTagData.write(output, this.saveAdditional(new CompoundTag()));
   }

   public final BlockTileEntity getBlockType() {
      return this.block;
   }

   public final BlockState getBlockState() {
      return (BlockState)((BlockState)((BlockState)((BlockState)this.block
                  .defaultBlockState()
                  .setValue(this.block.materialProperty, MaterialProperty.WrappedMaterial.get(this.teBlock.getMaterial())))
               .setValue(this.block.typeProperty, MetaTeBlockProperty.getState(this.teBlock, this.getActive())))
            .setValue(BlockTileEntity.facingProperty, this.getFacing()))
         .setValue(BlockTileEntity.transparentProperty, this.teBlock.isTransparent());
   }

   public final void setRemoved() {
      if (this.loadState == 2) {
         if (debugLoad) {
            IC2.log.debug(LogCategory.Block, "TE onUnloaded (invalidate) for %s at %s.", this, Util.formatPosition(this));
         }

         this.onUnloaded();
      } else {
         if (debugLoad) {
            IC2.log.debug(LogCategory.Block, "Skipping TE onUnloaded (invalidate) for %s at %s, state: %d.", this, Util.formatPosition(this), this.loadState);
         }

         this.loadState = 3;
      }

      super.setRemoved();
   }

   public final void onChunkUnload() {
      if (this.loadState == 2) {
         if (debugLoad) {
            IC2.log.debug(LogCategory.Block, "TE onUnloaded (chunk unload) for %s at %s.", this, Util.formatPosition(this));
         }

         this.onUnloaded();
      } else {
         if (debugLoad) {
            IC2.log.debug(LogCategory.Block, "Skipping TE onUnloaded (chunk unload) for %s at %s, state: %d.", this, Util.formatPosition(this), this.loadState);
         }

         this.loadState = 3;
      }
   }

   public final void func_145829_t() {
      super.onLoad();
      Level world = this.getLevel();
      if (world == null || this.getBlockPos() == null) {
         throw new IllegalStateException("no world/pos");
      } else if (this.loadState != 0 && this.loadState != 3) {
         throw new IllegalStateException("invalid load state: " + this.loadState);
      } else {
         this.loadState = 1;
         IC2.tickHandler
            .requestSingleWorldTick(
               world,
               new IWorldTickCallback() {
                  {
                     Objects.requireNonNull(TileEntityBlock.this);
                  }

                  @Override
                  public void onTick(Level world) {
                     BlockState state;
                     if (world == TileEntityBlock.this.getLevel()
                        && TileEntityBlock.this.getBlockPos() != null
                        && !TileEntityBlock.this.isRemoved()
                        && TileEntityBlock.this.loadState == 1
                        && world.hasChunkAt(TileEntityBlock.this.getBlockPos())
                        && (state = world.getBlockState(TileEntityBlock.this.getBlockPos())).getBlock() == TileEntityBlock.this.block
                        && world.getBlockEntity(TileEntityBlock.this.getBlockPos()) == TileEntityBlock.this) {
                        BlockMaterial expectedMaterial = TileEntityBlock.this.teBlock.getMaterial();
                        if (((MaterialProperty.WrappedMaterial)state.getValue(TileEntityBlock.this.block.materialProperty)).getMaterial() != expectedMaterial) {
                           if (TileEntityBlock.debugLoad) {
                              IC2.log
                                 .debug(LogCategory.Block, "Adjusting material for %s at %s.", TileEntityBlock.this, Util.formatPosition(TileEntityBlock.this));
                           }

                           world.setBlock(
                              TileEntityBlock.this.getBlockPos(),
                              (BlockState)state.setValue(TileEntityBlock.this.block.materialProperty, MaterialProperty.WrappedMaterial.get(expectedMaterial)),
                              0
                           );

                           assert world.getBlockEntity(TileEntityBlock.this.getBlockPos()) == TileEntityBlock.this;
                        }

                        if (TileEntityBlock.debugLoad) {
                           IC2.log.debug(LogCategory.Block, "TE onLoaded for %s at %s.", TileEntityBlock.this, Util.formatPosition(TileEntityBlock.this));
                        }

                        TileEntityBlock.this.onLoaded();
                     } else if (TileEntityBlock.debugLoad) {
                        IC2.log.debug(LogCategory.Block, "Skipping TE init for %s at %s.", TileEntityBlock.this, Util.formatPosition(TileEntityBlock.this));
                     }
                  }
               }
            );
      }
   }

   public final void onLoad() {
      this.func_145829_t();
   }

   public final void onChunkUnloaded() {
      this.onChunkUnload();
   }

   protected void onLoaded() {
      if (this.loadState != 1) {
         throw new IllegalStateException("invalid load state: " + this.loadState);
      } else {
         this.loadState = 2;
         this.enableWorldTick = this.requiresWorldTick();
         if (this.components != null) {
            for (TileEntityComponent component : this.components.values()) {
               component.onLoaded();
               if (component.enableWorldTick()) {
                  if (this.updatableComponents == null) {
                     this.updatableComponents = new ArrayList<>(4);
                  }

                  this.updatableComponents.add(component);
               }
            }
         }
      }
   }

   protected void onUnloaded() {
      if (this.loadState == 3) {
         throw new IllegalStateException("invalid load state: " + this.loadState);
      } else {
         this.loadState = 3;
         if (this.components != null) {
            for (TileEntityComponent component : this.components.values()) {
               component.onUnloaded();
            }
         }
      }
   }

   public void loadAdditional(CompoundTag nbt) {
      if (!this.getSupportedFacings().isEmpty()) {
         byte facingValue = nbt.getByteOr("facing", (byte)0);
         if (facingValue >= 0 && facingValue < Direction.values().length && this.getSupportedFacings().contains(Direction.values()[facingValue])) {
            this.facing = facingValue;
         } else if (!this.getSupportedFacings().isEmpty()) {
            this.facing = (byte)this.getSupportedFacings().iterator().next().ordinal();
         } else {
            this.facing = (byte)Direction.DOWN.ordinal();
         }
      }

      this.active = nbt.getBooleanOr("active", false);
      if (this.components != null && NbtChecks.hasType(nbt, "components", 10)) {
         CompoundTag componentsNbt = nbt.getCompoundOrEmpty("components");

         for (String name : componentsNbt.keySet()) {
            Class<? extends TileEntityComponent> cls = Components.getClass(name);
            TileEntityComponent component;
            if (cls != null && (component = this.getComponent(cls)) != null) {
               CompoundTag componentNbt = componentsNbt.getCompoundOrEmpty(name);
               component.readFromNbt(componentNbt);
            } else {
               IC2.log.warn(LogCategory.Block, "Can't find component %s while loading %s.", name, this);
            }
         }
      }
   }

   public CompoundTag saveAdditional(CompoundTag nbt) {
      if (!this.getSupportedFacings().isEmpty()) {
         nbt.putByte("facing", this.facing);
      }

      nbt.putBoolean("active", this.active);
      if (this.components != null) {
         CompoundTag componentsNbt = null;

         for (TileEntityComponent component : this.components.values()) {
            CompoundTag componentNbt = component.writeToNbt();
            if (componentNbt != null) {
               if (componentsNbt == null) {
                  componentsNbt = new CompoundTag();
                  nbt.put("components", componentsNbt);
               }

               componentsNbt.put(Components.getId((Class<? extends TileEntityComponent>)component.getClass()), componentNbt);
            }
         }
      }

      return nbt;
   }

   public CompoundTag func_189517_E_() {
      IC2.network.get(true).sendInitialData(this);
      return emptyNbt;
   }

   public ClientboundBlockEntityDataPacket func_189518_D_() {
      IC2.network.get(true).sendInitialData(this);
      return null;
   }

   public final void func_73660_a() {
      if (this.loadState == 2) {
         if (this.updatableComponents != null) {
            for (TileEntityComponent component : this.updatableComponents) {
               component.onWorldTick();
            }
         }

         if (this.enableWorldTick) {
            if (this.getLevel().isClientSide()) {
               this.updateEntityClient();
            } else {
               this.updateEntityServer();
            }
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   protected void updateEntityClient() {
   }

   protected void updateEntityServer() {
   }

   @Override
   public List<String> getNetworkedFields() {
      List<String> ret = new ArrayList<>(3);
      ret.add("teBlk=" + (this.isOldVersion() ? "Old-" : "") + this.teBlock.getName());
      ret.add("active");
      ret.add("facing");
      return ret;
   }

   private boolean isOldVersion() {
      assert this.hasLevel() && !this.getLevel().isClientSide();

      return this.teBlock.getTeClass() != this.getClass();
   }

   @Override
   public void onNetworkUpdate(String field) {
      if (field.equals("active") && this.hasActiveTexture() || field.equals("facing")) {
         this.rerender();
      }
   }

   @OnlyIn(Dist.CLIENT)
   private boolean hasActiveTexture() {
      if (!this.teBlock.hasActive()) {
         return false;
      } else {
         BlockState stateA = this.getBlockState();
         BlockState stateB = (BlockState)stateA.setValue(
            this.block.typeProperty, MetaTeBlockProperty.getState(this.teBlock, !((MetaTeBlock)stateA.getValue(this.block.typeProperty)).active)
         );
         return !ModelComparator.isEqual(stateA, stateB, this.getLevel(), this.getBlockPos());
      }
   }

   protected Ic2BlockState.Ic2BlockStateInstance getExtendedState(Ic2BlockState.Ic2BlockStateInstance state) {
      return state;
   }

   public void onPlaced(ItemStack stack, LivingEntity placer, Direction facing) {
      Level world = this.getLevel();
      if (!world.isClientSide()) {
      }

      facing = this.getPlacementFacing(placer, facing);
      if (facing != this.getFacing()) {
         this.setFacing(facing);
      }

      if (world.isClientSide()) {
         this.rerender();
      }
   }

   protected HitResult collisionRayTrace(Vec3 start, Vec3 end) {
      Vec3 startNormalized = start.subtract(this.getBlockPos().getX(), this.getBlockPos().getY(), this.getBlockPos().getZ());
      double lengthSq = Util.square(end.x - start.x) + Util.square(end.y - start.y) + Util.square(end.z - start.z);
      double lengthInv = 1.0 / Math.sqrt(lengthSq);
      Vec3 direction = new Vec3((end.x - start.x) * lengthInv, (end.y - start.y) * lengthInv, (end.z - start.z) * lengthInv);
      double minDistanceSq = lengthSq;
      Vec3 minIntersection = null;
      Direction minIntersectionSide = null;
      MutableObject<Vec3> intersectionOut = new MutableObject();

      for (AABB aabb : this.getAabbs(false)) {
         Direction side = AabbUtil.getIntersection(startNormalized, direction, aabb, intersectionOut);
         if (side != null) {
            Vec3 intersection = (Vec3)intersectionOut.getValue();
            double distanceSq = Util.square(intersection.x - startNormalized.x)
               + Util.square(intersection.y - startNormalized.y)
               + Util.square(intersection.z - startNormalized.z);
            if (distanceSq < minDistanceSq) {
               minDistanceSq = distanceSq;
               minIntersection = intersection;
               minIntersectionSide = side;
            }
         }
      }

      return minIntersection == null
         ? null
         : new BlockHitResult(
            minIntersection.add(this.getBlockPos().getX(), this.getBlockPos().getY(), this.getBlockPos().getZ()),
            minIntersectionSide,
            this.getBlockPos(),
            false
         );
   }

   public AABB getVisualBoundingBox() {
      return this.getAabb(false);
   }

   protected AABB getPhysicsBoundingBox() {
      return this.getAabb(true);
   }

   protected AABB getOutlineBoundingBox() {
      return this.getVisualBoundingBox();
   }

   protected void addCollisionBoxesToList(AABB mask, List<AABB> list, Entity collidingEntity) {
      AABB maskNormalized = mask.move(-this.getBlockPos().getX(), -this.getBlockPos().getY(), -this.getBlockPos().getZ());

      for (AABB aabb : this.getAabbs(true)) {
         if (aabb.intersects(maskNormalized)) {
            list.add(aabb.move(this.getBlockPos()));
         }
      }
   }

   private AABB getAabb(boolean forCollision) {
      List<AABB> aabbs = this.getAabbs(forCollision);
      if (aabbs.isEmpty()) {
         throw new RuntimeException("No AABBs for " + this);
      } else if (aabbs.size() == 1) {
         return aabbs.get(0);
      } else {
         double zS = Double.POSITIVE_INFINITY;
         double yS = Double.POSITIVE_INFINITY;
         double xS = Double.POSITIVE_INFINITY;
         double zE = Double.NEGATIVE_INFINITY;
         double yE = Double.NEGATIVE_INFINITY;
         double xE = Double.NEGATIVE_INFINITY;

         for (AABB aabb : aabbs) {
            xS = Math.min(xS, aabb.minX);
            yS = Math.min(yS, aabb.minY);
            zS = Math.min(zS, aabb.minZ);
            xE = Math.max(xE, aabb.maxX);
            yE = Math.max(yE, aabb.maxY);
            zE = Math.max(zE, aabb.maxZ);
         }

         return new AABB(xS, yS, zS, xE, yE, zE);
      }
   }

   protected void onEntityCollision(Entity entity) {
   }

   @OnlyIn(Dist.CLIENT)
   protected boolean shouldSideBeRendered(Direction side, BlockPos otherPos) {
      AABB aabb = this.getVisualBoundingBox();
      if (aabb != defaultAabbs) {
         switch (side) {
            case DOWN:
               if (aabb.minY > 0.0) {
                  return true;
               }
               break;
            case UP:
               if (aabb.maxY < 1.0) {
                  return true;
               }
               break;
            case NORTH:
               if (aabb.minZ > 0.0) {
                  return true;
               }
               break;
            case SOUTH:
               if (aabb.maxZ < 1.0) {
                  return true;
               }
               break;
            case WEST:
               if (aabb.minX > 0.0) {
                  return true;
               }
               break;
            case EAST:
               if (aabb.maxX < 1.0) {
                  return true;
               }
         }
      }

      Level world = this.getLevel();
      return !world.getBlockState(otherPos).isFaceSturdy(world, otherPos, side.getOpposite());
   }

   protected boolean doesSideBlockRendering(Direction side) {
      return checkSide(this.getAabbs(false), side, false);
   }

   private static boolean checkSide(List<AABB> aabbs, Direction side, boolean strict) {
      if (aabbs == defaultAabbs) {
         return true;
      } else {
         int dx = side.getStepX();
         int dy = side.getStepY();
         int dz = side.getStepZ();
         int xS = (dx + 1) / 2;
         int yS = (dy + 1) / 2;
         int zS = (dz + 1) / 2;
         int xE = (dx + 2) / 2;
         int yE = (dy + 2) / 2;
         int zE = (dz + 2) / 2;
         if (strict) {
            for (AABB aabb : aabbs) {
               switch (side) {
                  case DOWN:
                     if (aabb.minY < 0.0) {
                        return false;
                     }
                     break;
                  case UP:
                     if (aabb.maxY > 1.0) {
                        return false;
                     }
                     break;
                  case NORTH:
                     if (aabb.minZ < 0.0) {
                        return false;
                     }
                     break;
                  case SOUTH:
                     if (aabb.maxZ > 1.0) {
                        return false;
                     }
                     break;
                  case WEST:
                     if (aabb.minX < 0.0) {
                        return false;
                     }
                     break;
                  case EAST:
                     if (aabb.maxX > 1.0) {
                        return false;
                     }
               }
            }
         }

         for (AABB aabb : aabbs) {
            if (aabb.minX <= xS && aabb.minY <= yS && aabb.minZ <= zS && aabb.maxX >= xE && aabb.maxY >= yE && aabb.maxZ >= zE) {
               return true;
            }
         }

         return false;
      }
   }

   protected boolean isNormalCube() {
      List<AABB> aabbs = this.getAabbs(false);
      if (aabbs == defaultAabbs) {
         return true;
      } else if (aabbs.size() != 1) {
         return false;
      } else {
         AABB aabb = aabbs.get(0);
         return aabb.minX <= 0.0 && aabb.minY <= 0.0 && aabb.minZ <= 0.0 && aabb.maxX >= 1.0 && aabb.maxY >= 1.0 && aabb.maxZ >= 1.0;
      }
   }

   protected boolean isSideSolid(Direction side) {
      return checkSide(this.getAabbs(false), side, true);
   }

   protected int getLightOpacity() {
      return this.isNormalCube() ? 255 : 0;
   }

   protected int getLightValue() {
      return 0;
   }

   protected boolean onActivated(Player player, InteractionHand hand, Direction side, float hitX, float hitY, float hitZ) {
      if (this instanceof IHasGui) {
         return !this.getLevel().isClientSide() ? IC2.platform.launchGui(player, (IHasGui)this) : true;
      } else {
         return false;
      }
   }

   protected void onClicked(Player player) {
   }

   protected void onNeighborChange(Block neighbor, BlockPos neighborPos) {
      if (this.components != null) {
         for (TileEntityComponent component : this.components.values()) {
            component.onNeighborChange(neighbor, neighborPos);
         }
      }
   }

   protected int getWeakPower(Direction side) {
      BasicRedstoneComponent component = this.getComponent(RedstoneEmitter.class);
      return component == null ? 0 : component.getLevel();
   }

   protected boolean canConnectRedstone(Direction side) {
      return this.hasComponent(RedstoneEmitter.class) || this.hasComponent(Redstone.class);
   }

   protected int getComparatorInputOverride() {
      BasicRedstoneComponent component = this.getComponent(ComparatorEmitter.class);
      return component == null ? 0 : component.getLevel();
   }

   protected boolean recolor(Direction side, DyeColor mcColor) {
      return false;
   }

   protected void onExploded(Explosion explosion) {
   }

   protected void onBlockBreak() {
   }

   protected boolean onRemovedByPlayer(Player player, boolean willHarvest) {
      return true;
   }

   protected boolean isFlammable(Direction face) {
      return true;
   }

   protected ItemStack getPickBlock(Player player, HitResult target) {
      return this.block.getItemStack(this.teBlock);
   }

   protected boolean canHarvest(Player player, boolean defaultValue) {
      return defaultValue;
   }

   protected List<ItemStack> getSelfDrops(int fortune, boolean wrench) {
      ItemStack drop = this.getPickBlock(null, null);
      drop = this.adjustDrop(drop, wrench);
      return drop == null ? Collections.emptyList() : Arrays.asList(drop);
   }

   protected List<ItemStack> getAuxDrops(int fortune) {
      return Collections.emptyList();
   }

   protected float getHardness() {
      return this.teBlock.getHardness();
   }

   protected float getExplosionResistance(Entity exploder, Explosion explosion) {
      return this.teBlock.getExplosionResistance();
   }

   protected boolean canEntityDestroy(Entity entity) {
      return true;
   }

   public Direction getFacing() {
      return Direction.values()[this.facing];
   }

   protected boolean canSetFacingWrench(Direction facing, Player player) {
      if (!this.teBlock.allowWrenchRotating()) {
         return false;
      } else {
         return facing == this.getFacing() ? false : this.getSupportedFacings().contains(facing);
      }
   }

   protected boolean setFacingWrench(Direction facing, Player player) {
      if (!this.canSetFacingWrench(facing, player)) {
         return false;
      } else {
         this.setFacing(facing);
         return true;
      }
   }

   protected boolean wrenchCanRemove(Player player) {
      return true;
   }

   protected List<ItemStack> getWrenchDrops(Player player, int fortune) {
      List<ItemStack> ret = new ArrayList<>();
      ret.addAll(this.getSelfDrops(fortune, true));
      ret.addAll(this.getAuxDrops(fortune));
      return ret;
   }

   protected boolean isCrop() {
      return false;
   }

   protected SoundType getBlockSound(Entity entity) {
      return SoundType.STONE;
   }

   protected Direction getPlacementFacing(LivingEntity placer, Direction facing) {
      Set<Direction> supportedFacings = this.getSupportedFacings();
      if (supportedFacings.isEmpty()) {
         return Direction.DOWN;
      } else if (placer != null) {
         Vec3 dir = placer.getLookAngle();
         Direction bestFacing = null;
         double maxMatch = Double.NEGATIVE_INFINITY;

         for (Direction cFacing : supportedFacings) {
            double match = dir.dot(cFacing.getOpposite().getUnitVec3());
            if (match > maxMatch) {
               maxMatch = match;
               bestFacing = cFacing;
            }
         }

         return bestFacing;
      } else {
         return facing != null && supportedFacings.contains(facing.getOpposite()) ? facing.getOpposite() : this.getSupportedFacings().iterator().next();
      }
   }

   protected List<AABB> getAabbs(boolean forCollision) {
      return defaultAabbs;
   }

   protected ItemStack adjustDrop(ItemStack drop, boolean wrench) {
      if (!wrench) {
         switch (this.teBlock.getDefaultDrop()) {
            case Self:
            default:
               break;
            case None:
               drop = null;
               break;
            case Generator:
               drop = BlockName.te.getItemStack(TeBlock.generator);
               break;
            case Machine:
               drop = BlockName.resource.getItemStack(ResourceBlock.machine);
               break;
            case AdvMachine:
               drop = BlockName.resource.getItemStack(ResourceBlock.advanced_machine);
         }
      }

      return drop;
   }

   protected Set<Direction> getSupportedFacings() {
      return this.teBlock.getSupportedFacings();
   }

   protected void setFacing(Direction facing) {
      if (facing == null) {
         throw new NullPointerException("null facing");
      } else if (this.facing == facing.ordinal()) {
         throw new IllegalArgumentException("unchanged facing");
      } else if (!this.getSupportedFacings().contains(facing)) {
         throw new IllegalArgumentException("invalid facing: " + facing + ", supported: " + this.getSupportedFacings());
      } else {
         this.facing = (byte)facing.ordinal();
         if (!this.getLevel().isClientSide()) {
            IC2.network.get(true).updateTileEntityField(this, "facing");
         }
      }
   }

   public boolean getActive() {
      return this.active;
   }

   public void setActive(boolean active) {
      if (this.active != active) {
         this.active = active;
         IC2.network.get(true).updateTileEntityField(this, "active");
      }
   }

   @Override
   public boolean getGuiState(String name) {
      if ("active".equals(name)) {
         return this.getActive();
      } else {
         throw new IllegalArgumentException("Unexpected GUI value requested: " + name);
      }
   }

   @OnlyIn(Dist.CLIENT)
   public void addInformation(ItemStack stack, List<String> tooltip, TooltipFlag advanced) {
      if (this.hasComponent(Energy.class)) {
         Energy energy = this.getComponent(Energy.class);
         if (!energy.getSourceDirs().isEmpty()) {
            tooltip.add(Localization.translate("ic2.item.tooltip.PowerTier", energy.getSourceTier()));
         } else if (!energy.getSinkDirs().isEmpty()) {
            tooltip.add(Localization.translate("ic2.item.tooltip.PowerTier", energy.getSinkTier()));
         }
      }
   }

   public boolean hasCapability(BlockCapability<?, Direction> capability, Direction facing) {
      if (this.capabilityComponents == null) {
         return false;
      } else {
         TileEntityComponent comp = this.capabilityComponents.get(capability);
         return comp == null ? false : comp.getProvidedCapabilities(facing).contains(capability);
      }
   }

   public <T> T getCapability(BlockCapability<T, Direction> capability, Direction facing) {
      if (this.capabilityComponents == null) {
         return null;
      } else {
         TileEntityComponent comp = this.capabilityComponents.get(capability);
         return comp == null ? null : comp.getCapability(capability, facing);
      }
   }

   protected final <T extends TileEntityComponent> T addComponent(T component) {
      if (component == null) {
         throw new NullPointerException("null component");
      } else {
         if (this.components == null) {
            this.components = new IdentityHashMap<>(4);
         }

         TileEntityComponent prev = this.components.put((Class<? extends TileEntityComponent>)component.getClass(), component);
         if (prev != null) {
            throw new RuntimeException("conflicting component while adding " + component + ", already used by " + prev + ".");
         } else {
            for (BlockCapability<?, Direction> cap : component.getProvidedCapabilities(null)) {
               this.addComponentCapability(cap, component);
            }

            return component;
         }
      }
   }

   public boolean hasComponent(Class<? extends TileEntityComponent> cls) {
      return this.components == null ? false : this.components.containsKey(cls);
   }

   public <T extends TileEntityComponent> T getComponent(Class<T> cls) {
      return (T)(this.components == null ? null : this.components.get(cls));
   }

   public final Iterable<? extends TileEntityComponent> getComponents() {
      return (Iterable<? extends TileEntityComponent>)(this.components == null ? emptyComponents : this.components.values());
   }

   private void addComponentCapability(BlockCapability<?, Direction> cap, TileEntityComponent component) {
      if (this.capabilityComponents == null) {
         this.capabilityComponents = new IdentityHashMap<>();
      }

      TileEntityComponent prev = this.capabilityComponents.put(cap, component);

      assert prev == null;
   }

   protected final void rerender() {
      BlockState state = this.getBlockState();
      this.getLevel().sendBlockUpdated(this.getBlockPos(), state, state, 2);
   }

   protected boolean clientNeedsExtraModelInfo() {
      return false;
   }

   public boolean shouldRefresh(Level world, BlockPos pos, BlockState oldState, BlockState newState) {
      return oldState.getBlock() != newState.getBlock();
   }

   private final synchronized boolean requiresWorldTick() {
      Class<?> cls = this.getClass();
      TileEntityBlock.TickSubscription subscription = tickSubscriptions.get(cls);
      if (subscription == null) {
         boolean hasUpdateClient = false;
         boolean hasUpdateServer = false;

         for (boolean isClient = FMLEnvironment.getDist().isClient();
            cls != TileEntityBlock.class && (!hasUpdateClient && isClient || !hasUpdateServer);
            cls = cls.getSuperclass()
         ) {
            if (!hasUpdateClient && isClient) {
               boolean found = true;

               try {
                  cls.getDeclaredMethod("updateEntityClient");
               } catch (NoSuchMethodException var9) {
                  found = false;
               }

               if (found) {
                  hasUpdateClient = true;
               }
            }

            if (!hasUpdateServer) {
               boolean found = true;

               try {
                  cls.getDeclaredMethod("updateEntityServer");
               } catch (NoSuchMethodException var8) {
                  found = false;
               }

               if (found) {
                  hasUpdateServer = true;
               }
            }
         }

         if (hasUpdateClient) {
            if (hasUpdateServer) {
               subscription = TileEntityBlock.TickSubscription.Both;
            } else {
               subscription = TileEntityBlock.TickSubscription.Client;
            }
         } else if (hasUpdateServer) {
            subscription = TileEntityBlock.TickSubscription.Server;
         } else {
            subscription = TileEntityBlock.TickSubscription.None;
         }

         tickSubscriptions.put(this.getClass(), subscription);
      }

      return this.getLevel().isClientSide()
         ? subscription == TileEntityBlock.TickSubscription.Both || subscription == TileEntityBlock.TickSubscription.Client
         : subscription == TileEntityBlock.TickSubscription.Both || subscription == TileEntityBlock.TickSubscription.Server;
   }

   private static enum TickSubscription {
      None,
      Client,
      Server,
      Both;
   }
}
