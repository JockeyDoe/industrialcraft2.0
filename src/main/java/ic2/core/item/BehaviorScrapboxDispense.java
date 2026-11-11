package ic2.core.item;

import ic2.api.recipe.Recipes;
import ic2.core.item.type.CraftingItemType;
import ic2.core.ref.ItemName;
import ic2.core.util.StackUtil;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.DispenserBlock;

public class BehaviorScrapboxDispense extends DefaultDispenseItemBehavior {
   protected ItemStack execute(BlockSource blockSource, ItemStack stack) {
      if (StackUtil.checkItemEquality(stack, ItemName.crafting.getItemStack(CraftingItemType.scrap_box))) {
         Direction facing = (Direction)blockSource.state().getValue(DispenserBlock.FACING);
         Position position = DispenserBlock.getDispensePosition(blockSource);
         spawnItem(blockSource.level(), Recipes.scrapboxDrops.getDrop(stack, true), 6, facing, position);
      }

      return stack;
   }
}
