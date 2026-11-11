package ic2.core.item;

import ic2.core.block.TileEntityBarrel;
import ic2.core.block.state.IIdProvider;
import ic2.core.ref.ItemName;
import ic2.core.util.ItemStacks;
import ic2.core.util.StackUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.util.Prediction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.Level;

public class ItemMug extends ItemMulti<ItemMug.MugType> {
   public ItemMug() {
      super(ItemName.mug, ItemMug.MugType.class);
      ItemDefaults.stackSize(this, 1);
   }

   @Override
   public InteractionResult func_180614_a(Player player, Level world, BlockPos pos, InteractionHand hand, Direction side, float hitX, float hitY, float hitZ) {
      ItemStack stack = StackUtil.get(player, hand);
      ItemMug.MugType type = this.getType(stack);
      if (type == ItemMug.MugType.empty) {
         if (world.isClientSide()) {
            return InteractionResult.FAIL;
         }

         if (!(world.getBlockEntity(pos) instanceof TileEntityBarrel barrel)) {
            return InteractionResult.PASS;
         }

         if (!barrel.getActive() || barrel.getFacing() != side) {
            return InteractionResult.PASS;
         }

         int value = barrel.calculateMetaValue();
         if (barrel.drainLiquid(1)) {
            ItemStack is = ItemStacks.create(ItemName.booze_mug.getInstance(), 1, value);
            stack = StackUtil.decSize(stack);
            if (!StackUtil.isEmpty(stack)) {
               if (!player.getInventory().add(is)) {
                  player.drop(is, false, Prediction.SERVER_ONLY);
               }
            } else {
               StackUtil.set(player, hand, is);
            }

            return InteractionResult.SUCCESS;
         }
      }

      return InteractionResult.PASS;
   }

   public ItemStack func_77654_b(ItemStack stack, Level worldIn, LivingEntity entityLiving) {
      if (!(entityLiving instanceof Player player)) {
         return stack;
      } else {
         ItemMug.MugType type = this.getType(stack);
         if (type != null && type != ItemMug.MugType.empty) {
            int maxAmplifier;
            int extraDuration;
            switch (type) {
               case cold_coffee:
                  maxAmplifier = 1;
                  extraDuration = 600;
                  break;
               case dark_coffee:
                  maxAmplifier = 5;
                  extraDuration = 1200;
                  break;
               case coffee:
                  maxAmplifier = 6;
                  extraDuration = 1200;
                  break;
               default:
                  throw new IllegalStateException("unexpected type: " + type);
            }

            int highest = 0;
            int x = this.amplifyEffect(player, MobEffects.SPEED, maxAmplifier, extraDuration);
            if (x > highest) {
               highest = x;
            }

            x = this.amplifyEffect(player, MobEffects.HASTE, maxAmplifier, extraDuration);
            if (x > highest) {
               highest = x;
            }

            if (type == ItemMug.MugType.coffee) {
               highest -= 2;
            }

            if (highest >= 3) {
               player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, (highest - 2) * 200, 0));
               if (highest >= 4) {
                  player.addEffect(new MobEffectInstance(MobEffects.INSTANT_DAMAGE, 1, highest - 3));
               }
            }

            return this.getItemStack(ItemMug.MugType.empty);
         } else {
            return stack;
         }
      }
   }

   private int amplifyEffect(Player player, Holder<MobEffect> potion, int maxAmplifier, int extraDuration) {
      MobEffectInstance eff = player.getEffect(potion);
      if (eff != null) {
         int newAmp = eff.getAmplifier();
         int newDur = eff.getDuration();
         if (newAmp < maxAmplifier) {
            newAmp++;
         }

         newDur += extraDuration;

         assert potion == eff.getEffect();

         player.addEffect(new MobEffectInstance(potion, newDur, newAmp));
         return newAmp;
      } else {
         player.addEffect(new MobEffectInstance(potion, 300, 0));
         return 1;
      }
   }

   public int func_77626_a(ItemStack stack) {
      ItemMug.MugType type = this.getType(stack);
      return type != null && type != ItemMug.MugType.empty ? 32 : 0;
   }

   public ItemUseAnimation func_77661_b(ItemStack stack) {
      ItemMug.MugType type = this.getType(stack);
      return type != null && type != ItemMug.MugType.empty ? ItemUseAnimation.DRINK : ItemUseAnimation.NONE;
   }

   @Override
   public InteractionResult use(Level world, Player player, InteractionHand hand) {
      ItemMug.MugType type = this.getType(StackUtil.get(player, hand));
      if (type != null && type != ItemMug.MugType.empty) {
         player.startUsingItem(hand);
      }

      return super.use(world, player, hand);
   }

   public static enum MugType implements IIdProvider {
      empty,
      cold_coffee,
      dark_coffee,
      coffee;

      @Override
      public String getName() {
         return this.name();
      }

      @Override
      public int getId() {
         return this.ordinal();
      }
   }
}
