package ic2.core.util;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public final class VanillaVariants {
   public static final Block[] SAPLINGS = new Block[]{
      Blocks.OAK_SAPLING, Blocks.SPRUCE_SAPLING, Blocks.BIRCH_SAPLING, Blocks.JUNGLE_SAPLING, Blocks.ACACIA_SAPLING, Blocks.DARK_OAK_SAPLING
   };
   private static final Block[] LOGS = new Block[]{Blocks.OAK_LOG, Blocks.SPRUCE_LOG, Blocks.BIRCH_LOG, Blocks.JUNGLE_LOG};
   private static final Block[] LEAVES = new Block[]{Blocks.OAK_LEAVES, Blocks.SPRUCE_LEAVES, Blocks.BIRCH_LEAVES, Blocks.JUNGLE_LEAVES};
   private static final Block[] PLANKS = new Block[]{
      Blocks.OAK_PLANKS, Blocks.SPRUCE_PLANKS, Blocks.BIRCH_PLANKS, Blocks.JUNGLE_PLANKS, Blocks.ACACIA_PLANKS, Blocks.DARK_OAK_PLANKS
   };
   private static final Block[] STONES = new Block[]{
      Blocks.STONE, Blocks.GRANITE, Blocks.POLISHED_GRANITE, Blocks.DIORITE, Blocks.POLISHED_DIORITE, Blocks.ANDESITE, Blocks.POLISHED_ANDESITE
   };
   private static final Block[] FLOWERS = new Block[]{
      Blocks.POPPY,
      Blocks.BLUE_ORCHID,
      Blocks.ALLIUM,
      Blocks.AZURE_BLUET,
      Blocks.RED_TULIP,
      Blocks.ORANGE_TULIP,
      Blocks.WHITE_TULIP,
      Blocks.PINK_TULIP,
      Blocks.OXEYE_DAISY
   };

   private VanillaVariants() {
   }

   private static <T> T at(T[] values, int meta) {
      if (meta >= 0 && meta < values.length) {
         return values[meta];
      } else {
         throw new IllegalArgumentException("Invalid vanilla metadata " + meta);
      }
   }

   public static Block sapling(int meta) {
      return at(SAPLINGS, meta);
   }

   public static Block log(int meta) {
      return at(LOGS, meta & 3);
   }

   public static Block log2(int meta) {
      return at(new Block[]{Blocks.ACACIA_LOG, Blocks.DARK_OAK_LOG}, meta & 3);
   }

   public static Block leaves(int meta) {
      return at(LEAVES, meta & 3);
   }

   public static Block leaves2(int meta) {
      return at(new Block[]{Blocks.ACACIA_LEAVES, Blocks.DARK_OAK_LEAVES}, meta & 3);
   }

   public static Block planks(int meta) {
      return at(PLANKS, meta);
   }

   public static Block stone(int meta) {
      return at(STONES, meta);
   }

   public static Block dirt(int meta) {
      return at(new Block[]{Blocks.DIRT, Blocks.COARSE_DIRT, Blocks.PODZOL}, meta);
   }

   public static Block flower(int meta) {
      return at(FLOWERS, meta);
   }

   public static Block wool(int meta) {
      return (Block)Blocks.WOOL.pick(DyeColor.byId(meta));
   }

   public static Block stainedGlass(int meta) {
      return (Block)Blocks.STAINED_GLASS.pick(DyeColor.byId(meta));
   }

   public static Item dye(int meta) {
      return (Item)Items.DYE.pick(DyeColor.byId(15 - meta));
   }

   public static Item fish(int meta) {
      return at(new Item[]{Items.COD, Items.SALMON, Items.TROPICAL_FISH, Items.PUFFERFISH}, meta);
   }

   public static Item cookedFish(int meta) {
      return at(new Item[]{Items.COOKED_COD, Items.COOKED_SALMON}, meta);
   }

   public static Item skull(int meta) {
      return at(
         new Item[]{Items.SKELETON_SKULL, Items.WITHER_SKELETON_SKULL, Items.ZOMBIE_HEAD, Items.PLAYER_HEAD, Items.CREEPER_HEAD, Items.DRAGON_HEAD}, meta
      );
   }

   public static Item legacyItem(String id, int meta) {
      return switch (id) {
         case "minecraft:trapdoor" -> Items.OAK_TRAPDOOR;
         case "minecraft:fence" -> Items.OAK_FENCE;
         case "minecraft:wooden_door" -> Items.OAK_DOOR;
         case "minecraft:wooden_pressure_plate" -> Items.OAK_PRESSURE_PLATE;
         case "minecraft:reeds" -> Items.SUGAR_CANE;
         case "minecraft:waterlily" -> Items.LILY_PAD;
         case "minecraft:yellow_flower" -> Items.DANDELION;
         case "minecraft:brick_block" -> Items.BRICKS;
         case "minecraft:nether_brick" -> Items.NETHER_BRICKS;
         case "minecraft:netherbrick" -> Items.NETHER_BRICK;
         case "minecraft:hardened_clay" -> Items.TERRACOTTA;
         case "minecraft:grass" -> Items.GRASS_BLOCK;
         case "minecraft:melon" -> Items.MELON_SLICE;
         case "minecraft:snow" -> Items.SNOW_BLOCK;
         case "minecraft:snow_layer" -> Items.SNOW;
         case "minecraft:record_13" -> Items.MUSIC_DISC_13;
         case "minecraft:record_cat" -> Items.MUSIC_DISC_CAT;
         case "minecraft:coal" -> (Item)at(new Item[]{Items.COAL, Items.CHARCOAL}, meta);
         case "minecraft:golden_apple" -> meta == 1 ? Items.ENCHANTED_GOLDEN_APPLE : Items.GOLDEN_APPLE;
         case "minecraft:carpet" -> (Item)Items.CARPET.pick(DyeColor.byId(meta));
         case "minecraft:stained_hardened_clay" -> (Item)Items.DYED_TERRACOTTA.pick(DyeColor.byId(meta));
         case "minecraft:sand" -> (Item)at(new Item[]{Items.SAND, Items.RED_SAND}, meta);
         case "minecraft:sponge" -> (Item)at(new Item[]{Items.SPONGE, Items.WET_SPONGE}, meta);
         case "minecraft:stonebrick" -> (Item)at(
            new Item[]{Items.STONE_BRICKS, Items.MOSSY_STONE_BRICKS, Items.CRACKED_STONE_BRICKS, Items.CHISELED_STONE_BRICKS}, meta
         );
         case "minecraft:prismarine" -> (Item)at(new Item[]{Items.PRISMARINE, Items.PRISMARINE_BRICKS, Items.DARK_PRISMARINE}, meta);
         case "minecraft:sandstone" -> (Item)at(new Item[]{Items.SANDSTONE, Items.CHISELED_SANDSTONE, Items.SMOOTH_SANDSTONE}, meta);
         case "minecraft:red_sandstone" -> (Item)at(new Item[]{Items.RED_SANDSTONE, Items.CHISELED_RED_SANDSTONE, Items.SMOOTH_RED_SANDSTONE}, meta);
         case "minecraft:quartz_block" -> (Item)at(new Item[]{Items.QUARTZ_BLOCK, Items.CHISELED_QUARTZ_BLOCK, Items.QUARTZ_PILLAR}, meta);
         case "minecraft:stone_slab" -> (Item)at(
            new Item[]{
               Items.SMOOTH_STONE_SLAB,
               Items.SANDSTONE_SLAB,
               Items.PETRIFIED_OAK_SLAB,
               Items.COBBLESTONE_SLAB,
               Items.BRICK_SLAB,
               Items.STONE_BRICK_SLAB,
               Items.NETHER_BRICK_SLAB,
               Items.QUARTZ_SLAB
            },
            meta
         );
         case "minecraft:double_plant" -> (Item)at(
            new Item[]{Items.SUNFLOWER, Items.LILAC, Items.TALL_GRASS, Items.LARGE_FERN, Items.ROSE_BUSH, Items.PEONY}, meta
         );
         case "minecraft:tallgrass" -> (Item)at(new Item[]{Items.DEAD_BUSH, Items.SHORT_GRASS, Items.FERN}, meta);
         case "minecraft:sapling" -> sapling(meta).asItem();
         case "minecraft:log" -> log(meta).asItem();
         case "minecraft:log2" -> log2(meta).asItem();
         case "minecraft:leaves" -> leaves(meta).asItem();
         case "minecraft:leaves2" -> leaves2(meta).asItem();
         case "minecraft:planks" -> planks(meta).asItem();
         case "minecraft:stone" -> stone(meta).asItem();
         case "minecraft:dirt" -> dirt(meta).asItem();
         case "minecraft:red_flower" -> flower(meta).asItem();
         case "minecraft:wool" -> wool(meta).asItem();
         case "minecraft:stained_glass" -> stainedGlass(meta).asItem();
         case "minecraft:dye" -> {
            switch (meta) {
               case 0:
                  yield Items.INK_SAC;
               case 3:
                  yield Items.COCOA_BEANS;
               case 4:
                  yield Items.LAPIS_LAZULI;
               case 15:
                  yield Items.BONE_MEAL;
               default:
                  yield dye(meta);
            }
         }
         case "minecraft:fish" -> fish(meta);
         case "minecraft:cooked_fish" -> cookedFish(meta);
         case "minecraft:skull" -> skull(meta);
         default -> null;
      };
   }

   public static List<Item> legacyItems(String id) {
      int count = switch (id) {
         case "minecraft:sapling", "minecraft:planks", "minecraft:double_plant", "minecraft:skull" -> 6;
         case "minecraft:log", "minecraft:leaves", "minecraft:stonebrick", "minecraft:fish" -> 4;
         case "minecraft:log2", "minecraft:leaves2", "minecraft:sand", "minecraft:sponge", "minecraft:coal", "minecraft:golden_apple", "minecraft:cooked_fish" -> 2;
         case "minecraft:dirt", "minecraft:prismarine", "minecraft:sandstone", "minecraft:red_sandstone", "minecraft:quartz_block", "minecraft:tallgrass" -> 3;
         case "minecraft:stone" -> 7;
         case "minecraft:red_flower" -> 9;
         case "minecraft:stone_slab" -> 8;
         case "minecraft:wool", "minecraft:carpet", "minecraft:stained_glass", "minecraft:stained_hardened_clay", "minecraft:dye" -> 16;
         default -> 0;
      };
      ArrayList<Item> result = new ArrayList<>();

      for (int meta = 0; meta < count; meta++) {
         result.add(legacyItem(id, meta));
      }

      return List.copyOf(result);
   }
}
