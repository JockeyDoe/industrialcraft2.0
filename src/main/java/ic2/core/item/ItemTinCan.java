package ic2.core.item;

import ic2.core.IC2;
import ic2.core.item.type.CraftingItemType;
import ic2.core.ref.ItemName;
import ic2.core.util.StackUtil;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class ItemTinCan extends ItemIC2 {
   public ItemTinCan() {
      super(ItemName.filled_tin_can);
   }

   public InteractionResult use(Level world, Player player, InteractionHand hand) {
      ItemStack stack = StackUtil.get(player, hand);
      return (InteractionResult)(!world.isClientSide() && player.getFoodData().needsFood() ? this.onEaten(player, stack) : InteractionResult.PASS);
   }

   public InteractionResult onEaten(Player player, ItemStack stack) {
      int amount = Math.min(StackUtil.getSize(stack), 20 - player.getFoodData().getFoodLevel());
      if (amount <= 0) {
         return InteractionResult.PASS;
      } else {
         ItemStack emptyStack = StackUtil.copyWithSize(ItemName.crafting.getItemStack(CraftingItemType.tin_can), amount);
         if (StackUtil.storeInventoryItem(emptyStack, player, true)) {
            player.getFoodData().eat(amount, amount);
            stack = StackUtil.decSize(stack, amount);
            StackUtil.storeInventoryItem(emptyStack, player, false);
            IC2.platform.playSoundSp("Tools/eat.ogg", 1.0F, 1.0F);
            return InteractionResult.SUCCESS.heldItemTransformedTo(stack);
         } else {
            return InteractionResult.PASS;
         }
      }
   }
}
