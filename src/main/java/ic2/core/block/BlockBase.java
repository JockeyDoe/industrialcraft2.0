package ic2.core.block;

import com.google.common.collect.ImmutableList;
import ic2.core.IC2;
import ic2.core.block.state.BlockStates;
import ic2.core.init.BlocksItems;
import ic2.core.init.Localization;
import ic2.core.item.block.ItemBlockIC2;
import ic2.core.item.tool.HarvestRules;
import ic2.core.model.ModelKey;
import ic2.core.model.ModelRegistry;
import ic2.core.model.ModelUtil;
import ic2.core.ref.BlockMaterial;
import ic2.core.ref.BlockName;
import ic2.core.ref.IBlockModelProvider;
import ic2.core.util.ItemStacks;
import ic2.core.util.Util;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Position;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.event.EventHooks;

public abstract class BlockBase extends Block implements IBlockModelProvider {
   private String legacyName;
   private CreativeModeTab creativeTab;
   private float legacyHardness = Float.NaN;
   private float legacyResistance = Float.NaN;
   private SoundType legacySound;
   private boolean legacyRandomTicks;
   private final Map<BlockState, HarvestRules.Requirement> harvestRules = new HashMap<>();
   private HarvestRules.Requirement defaultHarvestRule;
   private final BlockMaterial materialCategory;

   public void setHarvestLevel(String tool, int level) {
      this.defaultHarvestRule = new HarvestRules.Requirement(tool, level);
   }

   public void setHarvestLevel(String tool, int level, BlockState state) {
      this.harvestRules.put(state, new HarvestRules.Requirement(tool, level));
   }

   public HarvestRules.Requirement harvestRequirement(BlockState state) {
      return this.harvestRules.getOrDefault(state, this.defaultHarvestRule);
   }

   public boolean canHarvestBlock(BlockState state, BlockGetter world, BlockPos pos, Player player) {
      HarvestRules.Requirement requirement = this.harvestRequirement(state);
      return requirement == null ? super.canHarvestBlock(state, world, pos, player) : HarvestRules.allows(player.getMainHandItem(), requirement);
   }

   public BlockBase func_149663_c(String name) {
      this.legacyName = name;
      return this;
   }

   public BlockBase func_149647_a(CreativeModeTab tab) {
      this.creativeTab = tab;
      return this;
   }

   public BlockBase func_149711_c(float hardness) {
      this.legacyHardness = hardness;
      return this;
   }

   public BlockBase func_149752_b(float resistance) {
      this.legacyResistance = resistance * 3.0F;
      return this;
   }

   public BlockBase func_149672_a(SoundType sound) {
      this.legacySound = sound;
      return this;
   }

   public BlockBase func_149675_a(boolean ticks) {
      this.legacyRandomTicks = ticks;
      return this;
   }

   protected float getDestroyProgress(BlockState state, Player player, BlockGetter world, BlockPos pos) {
      if (Float.isNaN(this.legacyHardness)) {
         return super.getDestroyProgress(state, player, world, pos);
      } else if (this.legacyHardness == -1.0F) {
         return 0.0F;
      } else {
         int modifier = EventHooks.doPlayerHarvestCheck(player, state, world, pos) ? 30 : 100;
         return player.getDestroySpeed(state, pos) / this.legacyHardness / modifier;
      }
   }

   public float getExplosionResistance() {
      return Float.isNaN(this.legacyResistance) ? super.getExplosionResistance() : this.legacyResistance;
   }

   protected SoundType getSoundType(BlockState state) {
      return this.legacySound == null ? super.getSoundType(state) : this.legacySound;
   }

   protected boolean isRandomlyTicking(BlockState state) {
      return this.legacyRandomTicks || super.isRandomlyTicking(state);
   }

   protected StateDefinition<Block, BlockState> func_180661_e() {
      return BlockStates.definition(this, new Property[0]);
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(this.func_180661_e().getProperties().toArray(new Property[0]));
   }

   public int func_176201_c(BlockState state) {
      return this.getStateDefinition().getPossibleStates().indexOf(state);
   }

   public BlockState func_176203_a(int meta) {
      ImmutableList<BlockState> states = this.getStateDefinition().getPossibleStates();
      return meta >= 0 && meta < states.size() ? (BlockState)states.get(meta) : this.defaultBlockState();
   }

   public BlockState func_180642_a(Level level, BlockPos pos, Direction face, float x, float y, float z, int meta, LivingEntity placer) {
      return this.func_176203_a(meta);
   }

   public void func_149666_a(CreativeModeTab tab, NonNullList<ItemStack> items) {
      if (tab == this.creativeTab) {
         items.add(new ItemStack(this));
      }
   }

   public void func_180650_b(Level level, BlockPos pos, BlockState state, Random random) {
   }

   protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
      this.func_180650_b(level, pos, state, new Random(random.nextLong()));
   }

   public AABB func_185496_a(BlockState state, BlockGetter world, BlockPos pos) {
      return new AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0);
   }

   public AABB func_180646_a(BlockState state, BlockGetter world, BlockPos pos) {
      return this.func_185496_a(state, world, pos);
   }

   protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
      AABB box = this.func_185496_a(state, world, pos);
      return box == null ? Shapes.empty() : Shapes.create(box);
   }

   protected VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
      AABB box = this.func_180646_a(state, world, pos);
      return box == null ? Shapes.empty() : Shapes.create(box);
   }

   public ItemStack func_185473_a(Level world, BlockPos pos, BlockState state) {
      return ItemStacks.create(Item.byBlock(this), 1, this.func_176201_c(state));
   }

   protected ItemStack getCloneItemStack(LevelReader world, BlockPos pos, BlockState state, boolean includeData) {
      return world instanceof Level level ? this.func_185473_a(level, pos, state) : super.getCloneItemStack(world, pos, state, includeData);
   }

   public boolean func_176196_c(Level world, BlockPos pos) {
      return world.isInWorldBounds(pos);
   }

   protected boolean canSurvive(BlockState state, LevelReader world, BlockPos pos) {
      return world instanceof Level level ? this.func_176196_c(level, pos) : super.canSurvive(state, world, pos);
   }

   public boolean func_176198_a(Level world, BlockPos pos, Direction side) {
      return this.func_176196_c(world, pos);
   }

   public float func_176195_g(BlockState state, Level world, BlockPos pos) {
      return Float.isNaN(this.legacyHardness) ? state.getDestroySpeed(world, pos) : this.legacyHardness;
   }

   public boolean func_176225_a(BlockState state, BlockGetter world, BlockPos pos, Direction side) {
      return !world.getBlockState(pos.relative(side)).isSolidRender();
   }

   protected static void func_185492_a(BlockPos pos, AABB mask, List<AABB> result, AABB box) {
      if (box != null) {
         AABB moved = box.move(pos);
         if (mask.intersects(moved)) {
            result.add(moved);
         }
      }
   }

   public boolean func_149686_d(BlockState state) {
      return state.isCollisionShapeFullBlock(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
   }

   public List<ItemStack> getDrops(BlockGetter world, BlockPos pos, BlockState state, int fortune) {
      Item item = Item.byBlock(this);
      return item == Items.AIR ? List.of() : List.of(ItemStacks.create(item, 1, this.func_176201_c(state)));
   }

   protected List<ItemStack> getDrops(BlockState state, net.minecraft.world.level.storage.loot.LootParams.Builder builder) {
      BlockPos pos = BlockPos.containing((Position)builder.getParameter(LootContextParams.ORIGIN));
      ItemInstance tool = (ItemInstance)builder.getOptionalParameter(LootContextParams.TOOL);
      int fortune = tool == null
         ? 0
         : EnchantmentHelper.getItemEnchantmentLevel(
            builder.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.FORTUNE), tool
         );
      return this.getDrops(builder.getLevel(), pos, state, fortune);
   }

   public void func_176226_b(Level world, BlockPos pos, BlockState state, int fortune) {
      if (world instanceof ServerLevel) {
         for (ItemStack drop : this.getDrops(world, pos, state, fortune)) {
            Block.popResource(world, pos, drop);
         }
      }
   }

   public BlockMaterial getMaterialCategory(BlockState state) {
      return this.materialCategory;
   }

   protected BlockBase(BlockName name, BlockMaterial material) {
      this(name, material, ItemBlockIC2.supplier);
   }

   protected BlockBase(BlockName name, BlockMaterial material, Class<? extends BlockItem> itemClass) {
      this(name, material, createItemBlockSupplier(itemClass));
   }

   protected BlockBase(BlockName name, BlockMaterial material, Function<Block, Item> itemSupplier) {
      super(
         material.properties()
            .dynamicShape()
            .setId(ResourceKey.create(Registries.BLOCK, IC2.getIdentifier(name == null ? "internal" : name.name())))
            .overrideDescription("ic2." + (name == null ? "internal" : name.name()))
      );
      this.materialCategory = material;
      this.func_149647_a(IC2.tabIC2);
      if (name != null) {
         this.register(name.name(), IC2.getIdentifier(name.name()), itemSupplier);
         name.setInstance(this);
      }
   }

   protected void register(String name, Identifier identifier, Function<Block, Item> itemSupplier) {
      this.func_149663_c(name);
      BlocksItems.registerBlock(this, identifier);
      if (itemSupplier != null) {
         BlocksItems.registerItem(itemSupplier.apply(this), identifier);
      }
   }

   protected static Function<Block, Item> createItemBlockSupplier(final Class<? extends BlockItem> cls) {
      if (cls == null) {
         throw new NullPointerException("null item class");
      } else {
         return new Function<Block, Item>() {
            public Item apply(Block input) {
               try {
                  return (Item)cls.getConstructor(Block.class).newInstance(input);
               } catch (Exception var3) {
                  throw new RuntimeException(var3);
               }
            }
         };
      }
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public void registerModels(BlockName name) {
      registerDefaultItemModel(this);
   }

   @OnlyIn(Dist.CLIENT)
   public static void registerDefaultItemModel(Block block) {
      registerItemModels(block, Arrays.asList(block.defaultBlockState()));
   }

   @OnlyIn(Dist.CLIENT)
   public static void registerItemModels(Block block, Iterable<BlockState> states) {
      registerItemModels(block, states, null);
   }

   @OnlyIn(Dist.CLIENT)
   public static void registerItemModels(Block block, Iterable<BlockState> states, ModelRegistry.IStateMapper mapper) {
      Item item = Item.byBlock(block);
      if (item != null && item != Items.AIR) {
         Identifier loc = Util.getName(item);
         if (loc != null) {
            Map<BlockState, ModelKey> locations = mapper != null ? mapper.func_178130_a(block) : null;

            for (BlockState state : states) {
               int meta = BlockStates.metadata(state);
               ModelKey location = locations != null ? locations.get(state) : ModelUtil.getModelLocation(loc, state);
               if (location == null) {
                  throw new RuntimeException("can't map state " + state);
               }

               ModelRegistry.setCustomModelResourceLocation(item, meta, location);
            }
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static void registerDefaultVanillaItemModel(Block block, String path) {
      Item item = Item.byBlock(block);
      if (item != null && item != Items.AIR) {
         Identifier loc = Util.getName(item);
         if (loc != null) {
            if (path != null && !path.isEmpty()) {
               path = path + "/" + loc.toString();
            } else {
               path = loc.toString();
            }

            ModelRegistry.setCustomModelResourceLocation(item, 0, new ModelKey(path, null));
         }
      }
   }

   public String func_149732_F() {
      return Localization.translate(this.getDescriptionId());
   }

   public boolean canBeReplacedByLeaves(BlockState state, BlockGetter world, BlockPos pos) {
      return false;
   }

   public Rarity getRarity(ItemStack stack) {
      return Rarity.COMMON;
   }
}
