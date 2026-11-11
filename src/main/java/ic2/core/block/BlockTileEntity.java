package ic2.core.block;

import com.google.common.base.Function;
import com.google.common.collect.UnmodifiableIterator;
import ic2.api.item.ITeBlockSpecialItem;
import ic2.api.tile.IWrenchable;
import ic2.core.IC2;
import ic2.core.block.state.Ic2BlockState;
import ic2.core.block.state.MaterialProperty;
import ic2.core.block.state.SkippedBooleanProperty;
import ic2.core.init.MainConfig;
import ic2.core.item.block.ItemBlockTileEntity;
import ic2.core.item.tool.ItemToolWrench;
import ic2.core.model.ModelKey;
import ic2.core.model.ModelRegistry;
import ic2.core.model.ModelUtil;
import ic2.core.ref.BlockMaterial;
import ic2.core.ref.BlockName;
import ic2.core.ref.IMultiBlock;
import ic2.core.ref.MetaTeBlock;
import ic2.core.ref.MetaTeBlockProperty;
import ic2.core.ref.TeBlock;
import ic2.core.util.ConfigUtil;
import ic2.core.util.ItemStacks;
import ic2.core.util.LogCategory;
import ic2.core.util.ParticleUtil;
import ic2.core.util.StackUtil;
import ic2.core.util.Util;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Position;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.loot.LootParams.Builder;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public final class BlockTileEntity extends BlockBase implements IMultiBlock<ITeBlock>, IWrenchable, EntityBlock {
   public final Property<MetaTeBlock> typeProperty = this.getTypeProperty();
   public final MaterialProperty materialProperty = this.getMaterialProperty();
   public static final Property<Direction> facingProperty = EnumProperty.create("facing", Direction.class);
   private static final ThreadLocal<Property<MetaTeBlock>> currentTypeProperty = new UnstartingThreadLocal<>();
   private static final ThreadLocal<MaterialProperty> currentMaterialProperty = new UnstartingThreadLocal<>();
   public static final Property<Boolean> transparentProperty = new SkippedBooleanProperty("transparent");
   private final ItemBlockTileEntity item;
   private static final int removedTesToKeep = 4;
   private static final WeakReference<TileEntityBlock>[] removedTes = new WeakReference[4];
   private static int nextRemovedTeIndex;
   private static final ThreadLocal<Player> harvesters = new ThreadLocal<>();

   static BlockTileEntity create(BlockName name, Collection<BlockMaterial> materials) {
      BlockTileEntity ret = create(name.name(), TeBlock.invalid.getIdentifier(), materials);
      name.setInstance(ret);
      return ret;
   }

   static BlockTileEntity create(String name, Identifier identifier, Collection<BlockMaterial> materials) {
      currentTypeProperty.set(new MetaTeBlockProperty(identifier));
      currentMaterialProperty.set(new MaterialProperty(materials));
      BlockTileEntity ret = new BlockTileEntity(name, identifier);
      currentMaterialProperty.remove();
      currentTypeProperty.remove();
      return ret;
   }

   private BlockTileEntity(String name, final Identifier identifier) {
      super(null, TeBlockRegistry.getInfo(identifier).getDefaultMaterial());
      this.register(name, identifier, new Function<Block, Item>() {
         {
            Objects.requireNonNull(BlockTileEntity.this);
         }

         public Item apply(Block input) {
            return new ItemBlockTileEntity(input, identifier);
         }
      });
      this.registerDefaultState(
         (BlockState)((BlockState)((BlockState)((BlockState)((BlockState)this.stateDefinition.any())
                     .setValue(this.materialProperty, MaterialProperty.WrappedMaterial.get(this.getMaterialCategory(this.defaultBlockState()))))
                  .setValue(this.typeProperty, MetaTeBlockProperty.invalid))
               .setValue(facingProperty, Direction.DOWN))
            .setValue(transparentProperty, Boolean.FALSE)
      );
      this.item = (ItemBlockTileEntity)Item.byBlock(this);
      IC2.log.debug(LogCategory.Block, "Successfully built BlockTileEntity for identity " + identifier + ".");
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public void registerModels(BlockName name) {
      final ModelKey invalidLocation = ModelUtil.getTEBlockModelLocation(
         Util.getName(BlockName.te.getInstance()),
         (BlockState)((BlockState)((BlockState)((BlockState)((BlockState)this.stateDefinition.any())
                     .setValue(this.materialProperty, MaterialProperty.WrappedMaterial.get(this.getMaterialCategory(this.defaultBlockState()))))
                  .setValue(this.typeProperty, MetaTeBlockProperty.invalid))
               .setValue(facingProperty, Direction.NORTH))
            .setValue(transparentProperty, Boolean.FALSE)
      );
      IC2.log.debug(LogCategory.Block, "Preparing to set models for " + this.item.identifier + ".");
      IC2.log.debug(LogCategory.Block, "Mapping " + this.getStateDefinition().getPossibleStates().size() + " states.");
      ModelRegistry.setCustomStateMapper(
         this,
         new ModelRegistry.IStateMapper() {
            {
               Objects.requireNonNull(BlockTileEntity.this);
            }

            @Override
            public Map<BlockState, ModelKey> func_178130_a(Block block) {
               Map<BlockState, ModelKey> ret = new IdentityHashMap<>();
               UnmodifiableIterator var3 = block.getStateDefinition().getPossibleStates().iterator();

               while (var3.hasNext()) {
                  BlockState state = (BlockState)var3.next();
                  MetaTeBlock metaTeBlock = (MetaTeBlock)state.getValue(BlockTileEntity.this.typeProperty);
                  Direction facing = (Direction)state.getValue(BlockTileEntity.facingProperty);
                  if (metaTeBlock.teBlock.getSupportedFacings().contains(facing)
                     || facing == Direction.DOWN && metaTeBlock.teBlock.getSupportedFacings().isEmpty()) {
                     ret.put(state, ModelUtil.getTEBlockModelLocation(metaTeBlock.teBlock.getIdentifier(), state));
                  } else {
                     ret.put(state, invalidLocation);
                  }
               }

               return ret;
            }
         }
      );
      ModelRegistry.setCustomMeshDefinition(
         this.item,
         new ModelRegistry.ItemMeshDefinition() {
            {
               Objects.requireNonNull(BlockTileEntity.this);
            }

            @Override
            public ModelKey func_178113_a(ItemStack stack) {
               ITeBlock teBlock = TeBlockRegistry.get(BlockTileEntity.this.item.identifier, ItemStacks.rawMeta(stack));
               if (teBlock == null) {
                  return invalidLocation;
               } else if (teBlock instanceof ITeBlockSpecialItem && ((ITeBlockSpecialItem)teBlock).doesOverrideDefault(stack)) {
                  ModelKey location = ((ITeBlockSpecialItem)teBlock).getModelLocation(stack);
                  return location == null ? invalidLocation : location;
               } else {
                  BlockState state = (BlockState)((BlockState)BlockTileEntity.this.defaultBlockState()
                        .setValue(BlockTileEntity.this.typeProperty, MetaTeBlockProperty.getState(teBlock)))
                     .setValue(BlockTileEntity.facingProperty, BlockTileEntity.getItemFacing(teBlock));
                  return ModelUtil.getTEBlockModelLocation(teBlock.getIdentifier(), state);
               }
            }
         }
      );
      boolean checkSpecialModels = TeBlockRegistry.getInfo(this.item.identifier).hasSpecialModels();

      for (MetaTeBlockProperty.MetaTePair block : MetaTeBlockProperty.getAllStates(this.item.identifier)) {
         if (block.hasItem()) {
            ModelKey model = checkSpecialModels ? this.getSpecialModel(block) : null;
            if (model == null) {
               BlockState state = (BlockState)((BlockState)((BlockState)this.stateDefinition.any()).setValue(this.typeProperty, block.inactive))
                  .setValue(facingProperty, getItemFacing(block.getBlock()));
               model = ModelUtil.getTEBlockModelLocation(block.getIdentifier(), state);
            }

            assert model != null;

            ModelRegistry.registerItemVariants(this.item, new ModelKey[]{model});
         }

         IC2.log.debug(LogCategory.Block, "Done item for " + this.item.identifier + ":" + block.getName() + ".");
      }
   }

   private static Direction getItemFacing(ITeBlock teBlock) {
      Set<Direction> supported = teBlock.getSupportedFacings();
      if (supported.contains(Direction.NORTH)) {
         return Direction.NORTH;
      } else {
         return !supported.isEmpty() ? supported.iterator().next() : Direction.DOWN;
      }
   }

   @OnlyIn(Dist.CLIENT)
   private ModelKey getSpecialModel(MetaTeBlockProperty.MetaTePair blockTextures) {
      assert blockTextures.getBlock() instanceof ITeBlockSpecialItem;

      ITeBlockSpecialItem block = (ITeBlockSpecialItem)blockTextures.getBlock();
      ItemStack stack = ItemStacks.create(this.item, 1, blockTextures.getBlock().getId());
      return block.doesOverrideDefault(stack) ? block.getModelLocation(stack) : null;
   }

   public boolean canRenderInLayer(BlockState state, ChunkSectionLayer layer) {
      return state.getValue(transparentProperty) ? layer == ChunkSectionLayer.CUTOUT : layer == ChunkSectionLayer.SOLID;
   }

   public boolean func_149716_u() {
      return true;
   }

   public boolean hasTileEntity(BlockState state) {
      return true;
   }

   @Override
   protected StateDefinition func_180661_e() {
      return new Ic2BlockState(this, this.getTypeProperty(), this.getMaterialProperty(), facingProperty, transparentProperty);
   }

   @Override
   public int func_176201_c(BlockState state) {
      int ret = this.materialProperty.getId((MaterialProperty.WrappedMaterial)state.getValue(this.materialProperty));
      if (ret >= 0 && ret < 8) {
         return ret | (state.getValue(transparentProperty) ? 8 : 0);
      } else {
         throw new IllegalStateException("invalid material id: " + ret);
      }
   }

   @Override
   public BlockState func_176203_a(int meta) {
      boolean isTransparent = (meta & 8) != 0;
      int materialId = meta & 7;
      return (BlockState)((BlockState)this.defaultBlockState().setValue(this.materialProperty, this.materialProperty.getMaterial(materialId)))
         .setValue(transparentProperty, isTransparent);
   }

   public BlockState func_176221_a(BlockState state, BlockGetter world, BlockPos pos) {
      TileEntityBlock te = getTe(world, pos);
      return te == null ? state : te.getBlockState();
   }

   protected String getDescriptionIdForBlock() {
      return !this.isIC2() ? this.item.identifier.getNamespace() + "." + this.item.identifier.getPath() : super.getDescriptionId();
   }

   @Override
   public void func_149666_a(CreativeModeTab tabs, NonNullList<ItemStack> list) {
      TeBlockRegistry.TeBlockInfo<?> info = TeBlockRegistry.getInfo(this.item.identifier);
      if (info.hasCreativeRegisterer()) {
         info.getCreativeRegisterer().addSubBlocks(list, this, this.item, tabs);
      } else if (tabs == IC2.tabIC2 || tabs == BuiltInRegistries.CREATIVE_MODE_TAB.getValue(CreativeModeTabs.SEARCH)) {
         for (ITeBlock type : info.getTeBlocks()) {
            if (type.hasItem()) {
               list.add(this.getItemStack(type));
            }
         }
      }
   }

   @Override
   public Set<ITeBlock> getAllTypes() {
      return Collections.unmodifiableSet(TeBlockRegistry.getAll(this.item.identifier));
   }

   @Override
   public ItemStack func_185473_a(Level world, BlockPos pos, BlockState state) {
      TileEntityBlock te = getTe(world, pos);
      return te == null ? StackUtil.emptyStack : te.getPickBlock(null, null);
   }

   public ItemStack getCloneItemStack(LevelReader reader, BlockPos pos, BlockState state, boolean includeData, Player player) {
      return reader instanceof Level world
         ? this.getPickBlock(state, new BlockHitResult(Vec3.atCenterOf(pos), Direction.DOWN, pos, false), world, pos, player)
         : super.getCloneItemStack(reader, pos, state, includeData, player);
   }

   public ItemStack getPickBlock(BlockState state, HitResult target, Level world, BlockPos pos, Player player) {
      TileEntityBlock te = getTe(world, pos);
      return te == null ? StackUtil.emptyStack : te.getPickBlock(player, target);
   }

   public BlockState getState(ITeBlock variant) {
      if (variant == null) {
         throw new IllegalArgumentException("invalid type: " + variant);
      } else {
         Set<Direction> supportedFacings = variant.getSupportedFacings();
         Direction facing;
         if (supportedFacings.isEmpty()) {
            facing = Direction.DOWN;
         } else if (supportedFacings.contains(Direction.NORTH)) {
            facing = Direction.NORTH;
         } else {
            facing = supportedFacings.iterator().next();
         }

         return (BlockState)((BlockState)((BlockState)((BlockState)this.defaultBlockState()
                     .setValue(this.materialProperty, MaterialProperty.WrappedMaterial.get(variant.getMaterial())))
                  .setValue(this.typeProperty, MetaTeBlockProperty.getState(variant)))
               .setValue(facingProperty, facing))
            .setValue(transparentProperty, variant.isTransparent());
      }
   }

   @Override
   public BlockState getState(String variant) {
      return this.getState(TeBlockRegistry.get(variant));
   }

   public ItemStack getItemStack(ITeBlock type) {
      if (type == null) {
         throw new IllegalArgumentException("invalid type: null");
      } else {
         int id = type.getId();
         return id != -1 ? ItemStacks.create(this.item, 1, id) : null;
      }
   }

   @Override
   public ItemStack getItemStack(String variant) {
      if (variant == null) {
         throw new IllegalArgumentException("Invalid ITeBlock type: null");
      } else {
         ITeBlock type = TeBlockRegistry.get(variant);
         if (type == null) {
            throw new IllegalArgumentException("Invalid ITeBlock type: " + variant);
         } else {
            return this.getItemStack(type);
         }
      }
   }

   @Override
   public String getVariant(ItemStack stack) {
      if (stack == null) {
         throw new NullPointerException("null stack");
      } else if (stack.getItem() != this.item) {
         throw new IllegalArgumentException("The stack " + stack + " doesn't match " + this.item + " (" + this + ")");
      } else {
         ITeBlock type = TeBlockRegistry.get(this.item.identifier, ItemStacks.rawMeta(stack));
         if (type == null) {
            throw new IllegalArgumentException("The stack " + stack + " doesn't reference any valid subtype");
         } else {
            return type.getName();
         }
      }
   }

   @Override
   public boolean func_149686_d(BlockState state) {
      return false;
   }

   public boolean func_149662_c(BlockState state) {
      return !(Boolean)state.getValue(transparentProperty);
   }

   public boolean canReplace(Level world, BlockPos pos, Direction side, ItemStack stack) {
      if (StackUtil.isEmpty(stack)) {
         return true;
      } else if (stack.getItem() != this.item) {
         return false;
      } else {
         ITeBlock type = TeBlockRegistry.get(this.item.identifier, ItemStacks.rawMeta(stack));
         if (type == null) {
            return false;
         } else {
            TeBlock.ITePlaceHandler handler = type.getPlaceHandler();
            return handler == null || handler.canReplace(world, pos, side, stack);
         }
      }
   }

   public boolean addLandingEffects(BlockState state, ServerLevel world, BlockPos pos, BlockState state2, LivingEntity entity, int numberOfParticles) {
      if (world.isClientSide()) {
         throw new IllegalStateException();
      } else {
         TileEntityBlock te = getTe(world, pos);
         if (te == null) {
            return super.addLandingEffects(state, world, pos, state2, entity, numberOfParticles);
         } else {
            if (te.clientNeedsExtraModelInfo()) {
               IC2.network.get(true).initiateTeblockLandEffect(world, pos, entity.getX(), entity.getY(), entity.getZ(), numberOfParticles, te.teBlock);
            } else {
               IC2.network.get(true).initiateTeblockLandEffect(world, entity.getX(), entity.getY(), entity.getZ(), numberOfParticles, te.teBlock);
            }

            return true;
         }
      }
   }

   public boolean addRunningEffects(BlockState state, Level world, BlockPos pos, Entity entity) {
      if (world.isClientSide()) {
         return true;
      } else {
         TileEntityBlock te = getTe(world, pos);
         if (te == null) {
            return super.addRunningEffects(state, world, pos, entity);
         } else {
            if (te.clientNeedsExtraModelInfo()) {
               IC2.network.get(true).initiateTeblockRunEffect(world, pos, entity, te.teBlock);
            } else {
               IC2.network.get(true).initiateTeblockRunEffect(world, entity, te.teBlock);
            }

            return true;
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   public boolean addHitEffects(BlockState state, Level world, HitResult target, ParticleEngine manager) {
      BlockPos pos = ((BlockHitResult)target).getBlockPos();
      TileEntityBlock te = getTe(world, pos);
      if (te == null) {
         return false;
      } else {
         ParticleUtil.spawnBlockHitParticles(te, ((BlockHitResult)target).getDirection(), te.clientNeedsExtraModelInfo());
         return true;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public boolean addDestroyEffects(Level world, BlockPos pos, ParticleEngine manager) {
      TileEntityBlock te = getTe(world, pos);
      if (te != null && te.clientNeedsExtraModelInfo()) {
         ParticleUtil.spawnBlockBreakParticles(te);
         return true;
      } else {
         return false;
      }
   }

   @Override
   public BlockMaterial getMaterialCategory(BlockState state) {
      return ((MaterialProperty.WrappedMaterial)state.getValue(this.materialProperty)).getMaterial();
   }

   public boolean func_176214_u(BlockState state) {
      return this.getMaterialCategory(state).blocksMotion() && !state.isAir();
   }

   public boolean func_176205_b(BlockGetter world, BlockPos pos) {
      return !this.getMaterialCategory(world.getBlockState(pos)).blocksMotion();
   }

   public boolean func_181623_g() {
      return false;
   }

   public PushReaction func_149656_h(BlockState state) {
      return this.getMaterialCategory(state).getPushReaction();
   }

   public boolean func_149751_l(BlockState state) {
      return !this.getMaterialCategory(state).isSolid();
   }

   public MapColor func_180659_g(BlockState state, BlockGetter world, BlockPos pos) {
      return this.getMaterialCategory(state).getMapColor();
   }

   public BlockState getExtendedState(BlockState state, BlockGetter world, BlockPos pos) {
      TileEntityBlock te = getTe(world, pos);
      return (BlockState)(te == null ? state : te.getExtendedState(Ic2BlockState.wrap(state)));
   }

   public void func_180633_a(Level world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
      TileEntityBlock te = getTe(world, pos);
      if (te != null) {
         te.onPlaced(stack, placer, Direction.UP);
      }
   }

   public HitResult func_180636_a(BlockState state, Level world, BlockPos pos, Vec3 start, Vec3 end) {
      TileEntityBlock te = getTe(world, pos);
      return (HitResult)(te == null ? AABB.clip(List.of(new AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0)), start, end, pos) : te.collisionRayTrace(start, end));
   }

   @Override
   public AABB func_185496_a(BlockState state, BlockGetter world, BlockPos pos) {
      TileEntityBlock te = getTe(world, pos);
      return te == null ? new AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0) : te.getVisualBoundingBox();
   }

   public AABB func_180640_a(BlockState state, Level world, BlockPos pos) {
      TileEntityBlock te = getTe(world, pos);
      return te == null ? new AABB(pos) : te.getOutlineBoundingBox().move(pos);
   }

   @Override
   public AABB func_180646_a(BlockState state, BlockGetter world, BlockPos pos) {
      TileEntityBlock te = getTe(world, pos);
      return te == null ? new AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0) : te.getPhysicsBoundingBox();
   }

   public void func_185477_a(BlockState state, Level world, BlockPos pos, AABB mask, List<AABB> list, Entity collidingEntity, boolean isActualState) {
      TileEntityBlock te = getTe(world, pos);
      if (te == null) {
         AABB box = new AABB(pos);
         if (box.intersects(mask)) {
            list.add(box);
         }
      } else {
         te.addCollisionBoxesToList(mask, list, collidingEntity);
      }
   }

   public void func_180634_a(Level world, BlockPos pos, BlockState state, Entity entity) {
      TileEntityBlock te = getTe(world, pos);
      if (te != null) {
         te.onEntityCollision(entity);
      }
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public boolean func_176225_a(BlockState state, BlockGetter world, BlockPos pos, Direction side) {
      TileEntityBlock te = getTe(world, pos);
      return te == null
         ? !world.getBlockState(pos.relative(side)).isFaceSturdy(world, pos.relative(side), side.getOpposite())
         : te.shouldSideBeRendered(side, pos.relative(side));
   }

   public boolean doesSideBlockRendering(BlockState state, BlockGetter world, BlockPos pos, Direction face) {
      TileEntityBlock te = getTe(world, pos);
      return te == null ? false : te.doesSideBlockRendering(face);
   }

   public boolean isNormalCube(BlockState state, BlockGetter world, BlockPos pos) {
      TileEntityBlock te = getTe(world, pos);
      return te == null ? false : te.isNormalCube();
   }

   public boolean isSideSolid(BlockState state, BlockGetter world, BlockPos pos, Direction side) {
      TileEntityBlock te = getTe(world, pos);
      return te == null ? false : te.isSideSolid(side);
   }

   public boolean isFaceSolid(BlockGetter world, BlockState state, BlockPos pos, Direction face) {
      return this.isSideSolid(state, world, pos, face);
   }

   public int getLightOpacity(BlockState state, BlockGetter world, BlockPos pos) {
      TileEntityBlock te = getTe(world, pos);
      return te == null ? state.getLightDampening() : te.getLightOpacity();
   }

   public int getLightValue(BlockState state, BlockGetter world, BlockPos pos) {
      TileEntityBlock te = getTe(world, pos);
      return te == null ? 0 : te.getLightValue();
   }

   public boolean func_180639_a(
      Level world, BlockPos pos, BlockState state, Player player, InteractionHand hand, Direction side, float hitX, float hitY, float hitZ
   ) {
      if (player.isShiftKeyDown()) {
         return false;
      } else {
         TileEntityBlock te = getTe(world, pos);
         return te == null ? false : te.onActivated(player, hand, side, hitX, hitY, hitZ);
      }
   }

   public void func_180649_a(Level world, BlockPos pos, Player player) {
      TileEntityBlock te = getTe(world, pos);
      if (te != null) {
         te.onClicked(player);
      }
   }

   public void func_189540_a(BlockState state, Level world, BlockPos pos, Block neighborBlock, BlockPos neighborPos) {
      TileEntityBlock te = getTe(world, pos);
      if (te != null) {
         te.onNeighborChange(neighborBlock, neighborPos);
      }
   }

   public int func_180656_a(BlockState state, BlockGetter world, BlockPos pos, Direction side) {
      TileEntityBlock te = getTe(world, pos);
      return te == null ? 0 : te.getWeakPower(side);
   }

   public boolean canConnectRedstone(BlockState state, BlockGetter world, BlockPos pos, Direction side) {
      TileEntityBlock te = getTe(world, pos);
      return te == null ? false : te.canConnectRedstone(side);
   }

   public boolean func_149740_M(BlockState state) {
      return true;
   }

   public int func_180641_l(BlockState state, Level world, BlockPos pos) {
      TileEntityBlock te = getTe(world, pos);
      return te == null ? 0 : te.getComparatorInputOverride();
   }

   public boolean recolorBlock(Level world, BlockPos pos, Direction side, DyeColor color) {
      TileEntityBlock te = getTe(world, pos);
      return te == null ? false : te.recolor(side, color);
   }

   public void onBlockExploded(Level world, BlockPos pos, Explosion explosion) {
      TileEntityBlock te = getTe(world, pos);
      if (te != null) {
         te.onExploded(explosion);
      }

      if (world instanceof ServerLevel server) {
         super.onBlockExploded(world.getBlockState(pos), server, pos, explosion);
      }
   }

   public void func_180663_b(Level world, BlockPos pos, BlockState state) {
      TileEntityBlock te = getTe(world, pos);
      if (te != null) {
         te.onBlockBreak();
      }

      world.removeBlockEntity(pos);
   }

   public boolean removedByPlayer(BlockState state, Level world, BlockPos pos, Player player, boolean willHarvest) {
      TileEntityBlock te = getTe(world, pos);
      if (te != null) {
         if (!te.onRemovedByPlayer(player, willHarvest)) {
            return false;
         }

         if (willHarvest && !world.isClientSide()) {
            removedTes[nextRemovedTeIndex] = new WeakReference<>(te);
            nextRemovedTeIndex = (nextRemovedTeIndex + 1) % 4;
         }
      }

      return super.onDestroyedByPlayer(state, world, pos, player, player.getMainHandItem(), willHarvest, state.getFluidState());
   }

   public boolean isFlammable(BlockGetter world, BlockPos pos, Direction face) {
      TileEntityBlock tile = getTe(world, pos);
      return tile == null ? false : tile.isFlammable(face);
   }

   public float func_180647_a(BlockState state, Player player, Level world, BlockPos pos) {
      float ret = super.getDestroyProgress(state, player, world, pos);
      if (!player.hasCorrectToolForDrops(state)) {
         TileEntityBlock te = getTe(world, pos);
         if (te != null && te.teBlock.getHarvestTool() == TeBlock.HarvestTool.None) {
            ret *= 3.3333333F;
         }
      }

      return ret;
   }

   public boolean canHarvestBlock(BlockGetter world, BlockPos pos, Player player) {
      boolean ret = super.canHarvestBlock(world.getBlockState(pos), world, pos, player);
      if (ret) {
         return ret;
      } else {
         TileEntityBlock te = getTe(world, pos);
         if (te == null) {
            return false;
         } else {
            switch (te.teBlock.getHarvestTool()) {
               case None:
                  return true;
               case Wrench:
                  ItemStack stack = player.getMainHandItem();
                  if (!stack.isEmpty()) {
                     String tool = TeBlock.HarvestTool.Pickaxe.toolClass;
                     return stack.is(ItemTags.PICKAXES);
                  }
               default:
                  return false;
            }
         }
      }
   }

   public String getHarvestTool(BlockState state) {
      return state.getBlock() != this ? null : ((MetaTeBlock)state.getValue(this.typeProperty)).teBlock.getHarvestTool().toolClass;
   }

   public int getHarvestLevel(BlockState state) {
      return state.getBlock() != this ? 0 : ((MetaTeBlock)state.getValue(this.typeProperty)).teBlock.getHarvestTool().level;
   }

   public void getDrops(NonNullList<ItemStack> list, BlockGetter world, BlockPos pos, BlockState state, int fortune) {
      list.addAll(this.getDrops(world, pos, state, fortune));
   }

   @Override
   public List<ItemStack> getDrops(BlockGetter world, BlockPos pos, BlockState state, int fortune) {
      TileEntityBlock te = getTe(world, pos);
      if (te == null) {
         Level realWorld = Util.getWorld(world);
         if (realWorld != null && realWorld.isClientSide() || realWorld == null && !IC2.platform.isSimulating()) {
            return new ArrayList<>();
         }

         int idx = nextRemovedTeIndex;

         int checkIdx;
         do {
            checkIdx = (idx + 4 - 1) % 4;
            WeakReference<TileEntityBlock> ref = removedTes[checkIdx];
            TileEntityBlock cTe;
            if (ref != null && (cTe = ref.get()) != null && (realWorld == null || cTe.getLevel() == realWorld) && cTe.getBlockPos().equals(pos)) {
               te = cTe;
               removedTes[checkIdx] = null;
               break;
            }

            idx = checkIdx;
         } while (checkIdx != nextRemovedTeIndex);

         if (te == null) {
            return new ArrayList<>();
         }
      }

      List<ItemStack> ret = new ArrayList<>();
      boolean wasWrench = ConfigUtil.getBool(MainConfig.get(), "balance/ignoreWrenchRequirement");
      if (!wasWrench) {
         Player player = harvesters.get();
         if (player != null) {
            ItemStack stack = player.getMainHandItem();
            if (!stack.isEmpty()) {
               String tool = TeBlock.HarvestTool.Wrench.toolClass;
               wasWrench |= stack.getItem() instanceof ITeBlockSpecialItem || stack.getItem() instanceof ItemToolWrench;
            }
         }
      }

      ret.addAll(te.getSelfDrops(fortune, wasWrench));
      ret.addAll(te.getAuxDrops(fortune));
      return ret;
   }

   @Override
   public float func_176195_g(BlockState state, Level world, BlockPos pos) {
      TileEntityBlock te = getTe(world, pos);
      return te == null ? 5.0F : te.getHardness();
   }

   public float getExplosionResistance(Level world, BlockPos pos, Entity exploder, Explosion explosion) {
      TileEntityBlock te = getTe(world, pos);
      return te == null ? 10.0F : te.getExplosionResistance(exploder, explosion);
   }

   public boolean canEntityDestroy(BlockState state, BlockGetter world, BlockPos pos, Entity entity) {
      TileEntityBlock te = getTe(world, pos);
      return te == null ? true : te.canEntityDestroy(entity);
   }

   @Override
   public Direction getFacing(Level world, BlockPos pos) {
      TileEntityBlock te = getTe(world, pos);
      return te == null ? Direction.DOWN : te.getFacing();
   }

   @Override
   public boolean canSetFacing(Level world, BlockPos pos, Direction newDirection, Player player) {
      TileEntityBlock te = getTe(world, pos);
      return te == null ? false : te.canSetFacingWrench(newDirection, player);
   }

   @Override
   public boolean setFacing(Level world, BlockPos pos, Direction newDirection, Player player) {
      TileEntityBlock te = getTe(world, pos);
      return te == null ? false : te.setFacingWrench(newDirection, player);
   }

   @Override
   public boolean wrenchCanRemove(Level world, BlockPos pos, Player player) {
      TileEntityBlock te = getTe(world, pos);
      return te == null ? false : te.wrenchCanRemove(player);
   }

   @Override
   public List<ItemStack> getWrenchDrops(Level world, BlockPos pos, BlockState state, BlockEntity te, Player player, int fortune) {
      return !(te instanceof TileEntityBlock) ? Collections.emptyList() : ((TileEntityBlock)te).getWrenchDrops(player, fortune);
   }

   public boolean isCrop(BlockGetter world, BlockPos pos) {
      TileEntityBlock te = getTe(world, pos);
      return te != null && te.isCrop();
   }

   public SoundType getSoundType(BlockState state, Level world, BlockPos pos, Entity entity) {
      TileEntityBlock te = getTe(world, pos);
      return te == null ? super.getSoundType(state, world, pos, entity) : te.getBlockSound(entity);
   }

   private static TileEntityBlock getTe(BlockGetter world, BlockPos pos) {
      BlockEntity te = world.getBlockEntity(pos);
      return te instanceof TileEntityBlock ? (TileEntityBlock)te : null;
   }

   public BlockState getPlant(BlockGetter world, BlockPos pos) {
      return world.getBlockState(pos);
   }

   public boolean rotateBlock(Level world, BlockPos pos, Direction axis) {
      TileEntityBlock te = getTe(world, pos);
      if (te != null) {
         Direction target = te.getFacing().getClockWise(axis.getAxis());
         if (te.getSupportedFacings().contains(target) && te.getFacing() != target) {
            te.setFacing(target);
            return true;
         }
      }

      return false;
   }

   public Direction[] getValidRotations(Level world, BlockPos pos) {
      TileEntityBlock te = getTe(world, pos);
      if (te == null) {
         return null;
      } else {
         Set<Direction> facings = te.getSupportedFacings();
         return !facings.isEmpty() ? facings.toArray(new Direction[facings.size()]) : null;
      }
   }

   public boolean isIC2() {
      return this.item.identifier == TeBlock.invalid.getIdentifier();
   }

   public ItemBlockTileEntity getItem() {
      return this.item;
   }

   public final Property<MetaTeBlock> getTypeProperty() {
      Property<MetaTeBlock> ret = this.typeProperty != null ? this.typeProperty : currentTypeProperty.get();

      assert ret != null : "The type property can't be obtained.";

      return ret;
   }

   public final MaterialProperty getMaterialProperty() {
      MaterialProperty ret = this.materialProperty != null ? this.materialProperty : currentMaterialProperty.get();

      assert ret != null : "The matieral property can't be obtained.";

      return ret;
   }

   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return NativeBlockEntities.create(pos, state);
   }

   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
      return (world, pos, blockState, entity) -> {
         if (entity instanceof TileEntityBlock machine) {
            machine.func_73660_a();
         }
      };
   }

   @Override
   protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
      TileEntityBlock te = getTe(world, pos);
      return te == null ? Shapes.block() : Shapes.create(te.getOutlineBoundingBox());
   }

   @Override
   protected VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
      TileEntityBlock te = getTe(world, pos);
      if (te == null) {
         return Shapes.block();
      } else {
         VoxelShape shape = Shapes.empty();

         for (AABB box : te.getAabbs(true)) {
            shape = Shapes.or(shape, Shapes.create(box));
         }

         return shape;
      }
   }

   protected VoxelShape getVisualShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
      return this.getShape(state, world, pos, context);
   }

   public void setPlacedBy(Level world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
      this.func_180633_a(world, pos, state, placer, stack);
   }

   protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
      Vec3 p = hit.getLocation().subtract(Vec3.atLowerCornerOf(pos));
      return (InteractionResult)(this.func_180639_a(world, pos, state, player, hand, hit.getDirection(), (float)p.x, (float)p.y, (float)p.z)
         ? InteractionResult.SUCCESS
         : InteractionResult.PASS);
   }

   protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
      return this.useItemOn(ItemStack.EMPTY, state, world, pos, player, InteractionHand.MAIN_HAND, hit);
   }

   protected void attack(BlockState state, Level world, BlockPos pos, Player player) {
      this.func_180649_a(world, pos, player);
   }

   protected int getSignal(BlockState state, BlockGetter world, BlockPos pos, Direction side) {
      return this.func_180656_a(state, world, pos, side);
   }

   protected boolean isSignalSource(BlockState state) {
      return true;
   }

   protected boolean hasAnalogOutputSignal(BlockState state) {
      return true;
   }

   protected int getAnalogOutputSignal(BlockState state, Level world, BlockPos pos, Direction side) {
      return this.func_180641_l(state, world, pos);
   }

   @Override
   public boolean canHarvestBlock(BlockState state, BlockGetter world, BlockPos pos, Player player) {
      return this.canHarvestBlock(world, pos, player);
   }

   @Override
   protected List<ItemStack> getDrops(BlockState state, Builder params) {
      Entity entity = (Entity)params.getOptionalParameter(LootContextParams.THIS_ENTITY);
      harvesters.set(entity instanceof Player player ? player : null);
      ItemInstance tool = (ItemInstance)params.getOptionalParameter(LootContextParams.TOOL);
      int fortune = tool == null
         ? 0
         : EnchantmentHelper.getItemEnchantmentLevel(
            params.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.FORTUNE), tool
         );

      List var6;
      try {
         var6 = this.getDrops(params.getLevel(), BlockPos.containing((Position)params.getParameter(LootContextParams.ORIGIN)), state, fortune);
      } finally {
         harvesters.remove();
      }

      return var6;
   }

   public boolean onDestroyedByPlayer(BlockState state, Level world, BlockPos pos, Player player, ItemStack stack, boolean harvest, FluidState fluid) {
      return this.removedByPlayer(state, world, pos, player, harvest);
   }

   public void onBlockExploded(BlockState state, ServerLevel world, BlockPos pos, Explosion explosion) {
      this.onBlockExploded(world, pos, explosion);
   }

   public float getExplosionResistance(BlockState state, BlockGetter world, BlockPos pos, Explosion explosion) {
      TileEntityBlock te = getTe(world, pos);
      return te == null ? 10.0F : te.getExplosionResistance(explosion.getDirectSourceEntity(), explosion);
   }
}
