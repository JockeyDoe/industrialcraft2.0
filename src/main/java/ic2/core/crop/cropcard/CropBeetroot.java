package ic2.core.crop.cropcard;

import ic2.api.crops.CropProperties;
import ic2.core.crop.CropVanilla;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;

public class CropBeetroot extends CropVanilla {
   public CropBeetroot() {
      super((CropBlock)Blocks.BEETROOTS);
   }

   @Override
   public String getId() {
      return "beetroots";
   }

   @Override
   public CropProperties getProperties() {
      return new CropProperties(1, 0, 4, 0, 1, 2);
   }

   @Override
   public String[] getAttributes() {
      return new String[]{"Red", "Food", "Beetroot"};
   }

   @Override
   public ItemStack getProduct() {
      return new ItemStack(Items.BEETROOT, 1);
   }

   @Override
   public ItemStack getSeeds() {
      return new ItemStack(Items.BEETROOT_SEEDS);
   }
}
