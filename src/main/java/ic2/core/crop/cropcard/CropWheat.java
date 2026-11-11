package ic2.core.crop.cropcard;

import ic2.api.crops.CropProperties;
import ic2.api.crops.ICropTile;
import ic2.core.crop.CropVanilla;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;

public class CropWheat extends CropVanilla {
   public CropWheat() {
      super((CropBlock)Blocks.WHEAT);
   }

   @Override
   public String getId() {
      return "wheat";
   }

   @Override
   public CropProperties getProperties() {
      return new CropProperties(1, 0, 4, 0, 0, 2);
   }

   @Override
   public String[] getAttributes() {
      return new String[]{"Yellow", "Food", "Wheat"};
   }

   @Override
   public ItemStack getProduct() {
      return new ItemStack(Items.WHEAT, 1);
   }

   @Override
   public ItemStack getSeeds() {
      return new ItemStack(Items.WHEAT_SEEDS);
   }

   @Override
   public int getSizeAfterHarvest(ICropTile crop) {
      return 2;
   }
}
