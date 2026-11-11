package ic2.core.crop.cropcard;

import ic2.api.crops.CropProperties;
import ic2.api.crops.ICropTile;
import ic2.core.IC2;
import ic2.core.crop.CropVanillaStem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

public class CropPumpkin extends CropVanillaStem {
   public CropPumpkin() {
      super(4);
   }

   @Override
   public String getId() {
      return "pumpkin";
   }

   @Override
   public CropProperties getProperties() {
      return new CropProperties(1, 0, 1, 0, 3, 1);
   }

   @Override
   public String[] getAttributes() {
      return new String[]{"Orange", "Decoration", "Stem"};
   }

   @Override
   protected ItemStack getProduct() {
      return new ItemStack(Blocks.PUMPKIN);
   }

   @Override
   protected ItemStack getSeeds() {
      return new ItemStack(Items.PUMPKIN_SEEDS, IC2.random.nextInt(3) + 1);
   }

   @Override
   public int getGrowthDuration(ICropTile crop) {
      return crop.getCurrentSize() == 3 ? 600 : 200;
   }
}
