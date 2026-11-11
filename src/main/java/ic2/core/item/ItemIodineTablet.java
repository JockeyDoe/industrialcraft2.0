package ic2.core.item;

import ic2.core.IC2;
import ic2.core.IC2Potion;
import ic2.core.profile.NotClassic;
import ic2.core.ref.ItemName;
import ic2.core.util.StackUtil;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

@NotClassic
public class ItemIodineTablet extends ItemIC2 {
   public ItemIodineTablet() {
      super(ItemName.iodine_tablet);
   }

   public InteractionResult use(Level world, Player player, InteractionHand hand) {
      ItemStack stack = StackUtil.get(player, hand);
      return (InteractionResult)(!world.isClientSide() ? this.onEaten(player, stack) : InteractionResult.PASS);
   }

   public InteractionResult onEaten(Player player, ItemStack stack) {
      MobEffectInstance radiation = player.getEffect(IC2Potion.radiation.holder());
      if (radiation == null) {
         return InteractionResult.PASS;
      } else {
         int duration = radiation.getDuration() / 20;
         int amount = Math.min(StackUtil.getSize(stack), duration);
         if (amount <= 0) {
            return InteractionResult.PASS;
         } else {
            player.removeEffect(IC2Potion.radiation.holder());
            if (amount < duration) {
               player.addEffect(new MobEffectInstance(IC2Potion.radiation.holder(), (duration - amount) * 20));
            }

            stack = StackUtil.decSize(stack, amount);
            IC2.platform.playSoundSp("Tools/eat.ogg", 1.0F, 1.0F);
            return InteractionResult.SUCCESS.heldItemTransformedTo(stack);
         }
      }
   }
}
