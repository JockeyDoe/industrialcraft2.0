package ic2.core.block;

import ic2.core.IC2;
import ic2.core.init.BlocksItems;
import ic2.core.item.block.ItemIc2Leaves;
import ic2.core.model.ModelKey;
import ic2.core.model.ModelRegistry;
import ic2.core.ref.BlockName;
import ic2.core.ref.IBlockModelProvider;
import ic2.core.util.ItemStacks;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.sounds.AmbientLeavesBlockSoundPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class Ic2Leaves extends LeavesBlock implements IBlockModelProvider {
   public static final EnumProperty<Ic2Leaves.LeavesType> typeProperty = EnumProperty.create("type", Ic2Leaves.LeavesType.class);
   private static final int checkDecayFlag = 8;
   private static final int decayableFlag = 4;

   public Ic2Leaves() {
      super(
         AmbientLeavesBlockSoundPlayer.noAmbientSound(),
         Properties.ofFullCopy(Blocks.OAK_LEAVES).setId(ResourceKey.create(Registries.BLOCK, IC2.getIdentifier("leaves"))).overrideDescription("ic2.leaves")
      );
      Identifier name = IC2.getIdentifier(BlockName.leaves.name());
      BlocksItems.registerBlock(this, name);
      BlocksItems.registerItem(new ItemIc2Leaves(this), name);
      BlockName.leaves.setInstance(this);
      this.registerDefaultState(dryState((BlockState)((BlockState)this.defaultBlockState().setValue(DISTANCE, 7)).setValue(PERSISTENT, false)));
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public void registerModels(BlockName name) {
      ModelRegistry.IStateMapper mapper = block -> {
         Map<BlockState, ModelKey> result = new HashMap<>();
         Iterator i$ = block.getStateDefinition().getPossibleStates().iterator();

         while (i$.hasNext()) {
            BlockState state = (BlockState)i$.next();
            result.put(state, new ModelKey("minecraft:block/birch_leaves", ""));
         }

         return result;
      };
      ModelRegistry.setCustomStateMapper(this, mapper);
      List<BlockState> states = new ArrayList<>(typeProperty.getPossibleValues().size());

      for (Ic2Leaves.LeavesType type : Ic2Leaves.LeavesType.values) {
         states.add(getDropState(this.defaultBlockState()));
      }

      BlockBase.registerItemModels(this, states, mapper);
   }

   private static BlockState getDropState(BlockState state) {
      return dryState((BlockState)((BlockState)state.setValue(DISTANCE, 1)).setValue(PERSISTENT, true));
   }

   public static BlockState dryState(BlockState state) {
      return (BlockState)state.setValue(WATERLOGGED, false);
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      super.createBlockStateDefinition(builder);
   }

   public BlockState func_176203_a(int meta) {
      boolean checkDecay = (meta & 8) != 0;
      boolean decayable = (meta & 4) != 0;
      meta &= 3;
      BlockState ret = (BlockState)((BlockState)this.defaultBlockState().setValue(DISTANCE, checkDecay ? 7 : 1)).setValue(PERSISTENT, !decayable);
      if (meta < Ic2Leaves.LeavesType.values.length) {
      }

      return ret;
   }

   public int func_176201_c(BlockState state) {
      int ret = 0;
      if ((Integer)state.getValue(DISTANCE) == 7) {
         ret |= 8;
      }

      if (!(Boolean)state.getValue(PERSISTENT)) {
         ret |= 4;
      }

      return ret | Ic2Leaves.LeavesType.rubber.ordinal();
   }

   public boolean func_149662_c(BlockState state) {
      return false;
   }

   @OnlyIn(Dist.CLIENT)
   public ChunkSectionLayer func_180664_k() {
      return ChunkSectionLayer.CUTOUT;
   }

   public boolean func_176225_a(BlockState state, BlockGetter world, BlockPos pos, Direction side) {
      BlockPos nPos = pos.relative(side);
      return (!this.func_149662_c(state) || world.getBlockState(nPos) != state) && !world.getBlockState(nPos).isFaceSturdy(world, nPos, side.getOpposite());
   }

   public Item func_180660_a(BlockState state, Random rand, int fortune) {
      return Ic2Leaves.LeavesType.rubber.getSapling().getItem();
   }

   public int func_180651_a(BlockState state) {
      return ItemStacks.rawMeta(Ic2Leaves.LeavesType.rubber.getSapling());
   }

   protected int func_176232_d(BlockState state) {
      return Ic2Leaves.LeavesType.rubber.saplingDropChance;
   }

   public List<ItemStack> onSheared(ItemStack item, BlockGetter world, BlockPos pos, int fortune) {
      BlockState state = getDropState(world.getBlockState(pos));
      return Arrays.asList(ItemStacks.create(this, 1, this.func_176201_c(state)));
   }

   public void func_149666_a(CreativeModeTab tab, NonNullList<ItemStack> list) {
      BlockState state = getDropState(this.defaultBlockState());

      for (Ic2Leaves.LeavesType type : Ic2Leaves.LeavesType.values) {
         list.add(ItemStacks.create(this, 1, this.func_176201_c(state)));
      }
   }

   public boolean canBeReplacedByLeaves(BlockState state, BlockGetter world, BlockPos pos) {
      return true;
   }

   public boolean isShearable(ItemStack item, BlockGetter world, BlockPos pos) {
      return true;
   }

   public boolean isLeaves(BlockState state, BlockGetter world, BlockPos pos) {
      return true;
   }

   public int getFireSpreadSpeed(BlockGetter world, BlockPos pos, Direction face) {
      return 30;
   }

   public int getFlammability(BlockGetter world, BlockPos pos, Direction face) {
      return 20;
   }

   public static enum LeavesType implements StringRepresentable {
      rubber(35);

      public final int saplingDropChance;
      private static final Ic2Leaves.LeavesType[] values = values();

      private LeavesType(int saplingDropChance) {
         this.saplingDropChance = saplingDropChance;
      }

      public String getSerializedName() {
         return this.name();
      }

      public String getName() {
         return this.name();
      }

      public ItemStack getSapling() {
         return new ItemStack(BlockName.sapling.getInstance());
      }
   }
}
