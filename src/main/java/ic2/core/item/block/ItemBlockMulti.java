package ic2.core.item.block;

import ic2.core.block.BlockMultiID;
import ic2.core.item.ItemDefaults;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

public class ItemBlockMulti extends ItemBlockIC2 {
   public ItemBlockMulti(Block block) {
      super(block);
      ItemDefaults.variants(this, true);
   }

   public int func_77647_b(int damage) {
      return damage;
   }

   @Override
   public String func_77667_c(ItemStack stack) {
      return super.func_77667_c(stack) + "." + ((BlockMultiID)this.getBlock()).getVariant(stack);
   }
}
