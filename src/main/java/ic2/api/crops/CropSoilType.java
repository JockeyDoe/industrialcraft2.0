package ic2.api.crops;

import javax.annotation.Nonnull;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public enum CropSoilType {
   FARMLAND(Blocks.FARMLAND),
   MYCELIUM(Blocks.MYCELIUM),
   SAND(Blocks.SAND),
   SOULSAND(Blocks.SOUL_SAND);

   private final Block block;

   private CropSoilType(@Nonnull Block block) {
      this.block = block;
   }

   public Block getBlock() {
      return this.block;
   }

   public static boolean contais(Block block) {
      for (CropSoilType aux : values()) {
         if (aux.getBlock() == block) {
            return true;
         }
      }

      return false;
   }
}
