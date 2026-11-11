package ic2.core.item.block;

import ic2.core.block.Ic2Leaves;
import ic2.core.init.Localization;
import ic2.core.item.ItemDefaults;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;

public class ItemIc2Leaves extends ItemBlockIC2 {
   public ItemIc2Leaves(Block block) {
      super((LeavesBlock)block);
      ItemDefaults.variants(this, false);
   }

   public String func_77658_a() {
      return "ic2.leaves";
   }

   @Override
   public String func_77667_c(ItemStack stack) {
      return this.func_77658_a() + "." + Ic2Leaves.LeavesType.rubber.getName();
   }

   @Override
   public Component getName(ItemStack stack) {
      return Component.translatable(this.func_77667_c(stack));
   }

   @Override
   public String func_77653_i(ItemStack stack) {
      return Localization.translate(this.func_77667_c(stack));
   }
}
