package ic2.api.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public interface IItemAPI {
   BlockState getBlockState(String var1, String var2);

   ItemStack getItemStack(String var1, String var2);

   Block getBlock(String var1);

   Item getItem(String var1);
}
