package ic2.core.util;

import java.util.List;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ColorCollection;

public final class BlockColors {
   private static final List<ColorCollection<Block>> FAMILIES = List.of(
      Blocks.WOOL,
      Blocks.WOOL_STAIRS,
      Blocks.WOOL_SLAB,
      Blocks.CARPET,
      Blocks.STAINED_GLASS,
      Blocks.STAINED_GLASS_PANE,
      Blocks.DYED_TERRACOTTA,
      Blocks.GLAZED_TERRACOTTA,
      Blocks.CONCRETE,
      Blocks.CONCRETE_STAIRS,
      Blocks.CONCRETE_SLAB,
      Blocks.CONCRETE_POWDER,
      Blocks.DYED_SHULKER_BOX,
      Blocks.BED,
      Blocks.DYED_CANDLE
   );

   private BlockColors() {
   }

   public static Block recolor(Block block, DyeColor color) {
      for (ColorCollection<Block> family : FAMILIES) {
         if (family.asList().contains(block)) {
            return (Block)family.pick(color);
         }
      }

      return null;
   }
}
