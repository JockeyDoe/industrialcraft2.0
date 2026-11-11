package ic2.core.item;

import ic2.core.IC2;
import ic2.core.IC2Potion;
import ic2.core.ref.ItemName;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class ItemTerraWart extends ItemFoodIc2 {
   public ItemTerraWart() {
      super(ItemName.terra_wart, 0, 1.0F, false);
   }

   @Override
   public ItemStack func_77654_b(ItemStack stack, Level world, LivingEntity player) {
      IC2.platform.removePotion(player, MobEffects.NAUSEA);
      IC2.platform.removePotion(player, MobEffects.MINING_FATIGUE);
      IC2.platform.removePotion(player, MobEffects.HUNGER);
      IC2.platform.removePotion(player, MobEffects.SLOWNESS);
      IC2.platform.removePotion(player, MobEffects.WEAKNESS);
      IC2.platform.removePotion(player, MobEffects.BLINDNESS);
      IC2.platform.removePotion(player, MobEffects.POISON);
      IC2.platform.removePotion(player, MobEffects.WITHER);
      MobEffectInstance effect = player.getEffect(IC2Potion.radiation.holder());
      if (effect != null) {
         if (effect.getDuration() <= 600) {
            IC2.platform.removePotion(player, IC2Potion.radiation.holder());
         } else {
            IC2.platform.removePotion(player, IC2Potion.radiation.holder());
            IC2Potion.radiation.applyTo(player, effect.getDuration() - 600, effect.getAmplifier());
         }
      }

      return super.func_77654_b(stack, world, player);
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public Rarity func_77613_e(ItemStack stack) {
      return Rarity.RARE;
   }
}
