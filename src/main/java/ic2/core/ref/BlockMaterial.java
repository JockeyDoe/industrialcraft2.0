package ic2.core.ref;

import ic2.core.block.BlockBase;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

public class BlockMaterial {
   public static final BlockMaterial AIR = new BlockMaterial("air", MapColor.NONE, false, false, true, false);
   public static final BlockMaterial WOOD = new BlockMaterial("wood", MapColor.WOOD, true, false, false, true);
   public static final BlockMaterial ROCK = new BlockMaterial("rock", MapColor.STONE, true, false, false, false);
   public static final BlockMaterial IRON = new BlockMaterial("iron", MapColor.METAL, true, false, false, false);
   public static final BlockMaterial ANVIL = new BlockMaterial("anvil", MapColor.METAL, true, false, false, false);
   public static final BlockMaterial GROUND = new BlockMaterial("ground", MapColor.DIRT, true, false, false, false);
   public static final BlockMaterial WATER = new BlockMaterial("water", MapColor.WATER, false, true, true, false);
   public static final BlockMaterial LAVA = new BlockMaterial("lava", MapColor.FIRE, false, true, true, false);
   public static final BlockMaterial LEAVES = new BlockMaterial("leaves", MapColor.PLANT, true, false, false, true);
   public static final BlockMaterial PLANTS = new BlockMaterial("plants", MapColor.PLANT, false, false, true, true);
   public static final BlockMaterial VINE = new BlockMaterial("vine", MapColor.PLANT, false, false, true, true);
   public static final BlockMaterial CLOTH = new BlockMaterial("cloth", MapColor.WOOL, true, false, false, true);
   public static final BlockMaterial FIRE = new BlockMaterial("fire", MapColor.FIRE, false, false, true, false);
   public static final BlockMaterial GLASS = new BlockMaterial("glass", MapColor.NONE, true, false, false, false);
   public static final BlockMaterial CIRCUITS = new BlockMaterial("circuits", MapColor.NONE, false, false, true, false);
   public static final BlockMaterial TNT = new BlockMaterial("tnt", MapColor.FIRE, true, false, false, true);
   public static final BlockMaterial CORAL = new BlockMaterial("coral", MapColor.PLANT, false, false, false, false);
   public static final BlockMaterial CLAY = new BlockMaterial("clay", MapColor.CLAY, true, false, false, false);
   public static final BlockMaterial GOURD = new BlockMaterial("gourd", MapColor.PLANT, true, false, false, false);
   public final String name;
   private final MapColor color;
   private final boolean solid;
   private final boolean liquid;
   private final boolean replaceable;
   private final boolean flammable;
   protected boolean requiresTool;
   protected boolean immovable;

   protected BlockMaterial(String name, MapColor color, boolean solid, boolean liquid, boolean replaceable, boolean flammable) {
      this.name = name;
      this.color = color;
      this.solid = solid;
      this.liquid = liquid;
      this.replaceable = replaceable;
      this.flammable = flammable;
   }

   public static BlockMaterial liquid(String name, MapColor color) {
      return new BlockMaterial(name, color, false, true, true, false);
   }

   public Properties properties() {
      Properties properties = Properties.of().mapColor(this.color);
      if (!this.solid) {
         properties.noCollision();
      }

      if (this.replaceable) {
         properties.replaceable();
      }

      if (this.flammable) {
         properties.ignitedByLava();
      }

      if (this.requiresTool) {
         properties.requiresCorrectToolForDrops();
      }

      if (this.immovable) {
         properties.pushReaction(PushReaction.IMMOVEABLE);
      }

      return properties;
   }

   public boolean isSolid() {
      return this.solid;
   }

   public boolean blocksMotion() {
      return this.solid;
   }

   public boolean isLiquid() {
      return this.liquid;
   }

   public boolean isReplaceable() {
      return this.replaceable;
   }

   public boolean isFlammable() {
      return this.flammable;
   }

   public boolean isToolNotRequired() {
      return !this.requiresTool;
   }

   public MapColor getMapColor() {
      return this.color;
   }

   public PushReaction getPushReaction() {
      return this.immovable ? PushReaction.IMMOVEABLE : PushReaction.PUSH_PULL;
   }

   public static BlockMaterial of(BlockState state) {
      if (state.isAir()) {
         return AIR;
      } else if (state.getBlock() instanceof BlockBase block) {
         return block.getMaterialCategory(state);
      } else if (state.getFluidState().is(FluidTags.WATER)) {
         return WATER;
      } else if (state.getFluidState().is(FluidTags.LAVA)) {
         return LAVA;
      } else if (state.is(BlockTags.LOGS) || state.is(BlockTags.PLANKS) || state.is(BlockTags.WOODEN_FENCES)) {
         return WOOD;
      } else if (state.is(BlockTags.LEAVES)) {
         return LEAVES;
      } else if (state.is(BlockTags.WOOL)) {
         return CLOTH;
      } else if (state.is(BlockTags.ANVIL)) {
         return ANVIL;
      } else if (state.is(Blocks.TNT)) {
         return TNT;
      } else if (state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE)) {
         return FIRE;
      } else if (state.is(Blocks.VINE)) {
         return VINE;
      } else if (state.is(Blocks.CLAY)) {
         return CLAY;
      } else if (state.is(Blocks.PUMPKIN) || state.is(Blocks.CARVED_PUMPKIN) || state.is(Blocks.MELON)) {
         return GOURD;
      } else if (state.is(Blocks.IRON_BLOCK) || state.is(Blocks.GOLD_BLOCK) || state.is((Block)Blocks.COPPER_BLOCK.weathering().unaffected())) {
         return IRON;
      } else if (state.is(BlockTags.DIRT) || state.is(BlockTags.SAND)) {
         return GROUND;
      } else if (!state.is(BlockTags.FLOWERS) && !state.canBeReplaced()) {
         return state.isSolid() ? ROCK : CIRCUITS;
      } else {
         return PLANTS;
      }
   }
}
