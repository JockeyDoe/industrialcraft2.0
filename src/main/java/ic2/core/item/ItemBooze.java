package ic2.core.item;

import ic2.core.block.state.IIdProvider;
import ic2.core.model.ModelKey;
import ic2.core.model.ModelRegistry;
import ic2.core.ref.ItemName;
import ic2.core.util.ItemStacks;
import ic2.core.util.StackUtil;
import java.util.Objects;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
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
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class ItemBooze extends ItemIC2 {
   public String[] solidRatio = new String[]{"Watery ", "Clear ", "Lite ", "", "Strong ", "Thick ", "Stodge ", "X"};
   public String[] hopsRatio = new String[]{"Soup ", "Alcfree ", "White ", "", "Dark ", "Full ", "Black ", "X"};
   public String[] timeRatioNames = new String[]{"Brew", "Youngster", "Beer", "Ale", "Dragonblood", "Black Stuff"};
   public int[] baseDuration = new int[]{300, 600, 900, 1200, 1600, 2000, 2400};
   public float[] baseIntensity = new float[]{0.4F, 0.75F, 1.0F, 1.5F, 2.0F};
   public static float rumStackability = 2.0F;
   public static int rumDuration = 600;

   public ItemBooze() {
      super(ItemName.booze_mug);
      ItemDefaults.stackSize(this, 1);
      this.func_77637_a(null);
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public void registerModels(final ItemName name) {
      ModelRegistry.setCustomMeshDefinition(this, new ModelRegistry.ItemMeshDefinition() {
         {
            Objects.requireNonNull(ItemBooze.this);
         }

         @Override
         public ModelKey func_178113_a(ItemStack stack) {
            int meta = ItemStacks.rawMeta(stack);
            int type = ItemBooze.getTypeOfValue(meta);
            ItemBooze.BoozeMugType mugType;
            if (type == 1) {
               int timeRatio = Math.min(ItemBooze.getTimeRatioOfBeerValue(meta), ItemBooze.this.timeRatioNames.length - 1);
               mugType = ItemBooze.BoozeMugType.values[timeRatio];
            } else {
               if (type != 2) {
                  return null;
               }

               mugType = ItemBooze.BoozeMugType.rum;
            }

            return ItemIC2.getModelLocation(name, mugType.getName());
         }
      });

      for (ItemBooze.BoozeMugType type : ItemBooze.BoozeMugType.values) {
         ModelRegistry.registerItemVariants(this, new ModelKey[]{getModelLocation(name, type.getName())});
      }
   }

   @Override
   public Component getName(ItemStack stack) {
      return Component.literal(this.func_77653_i(stack));
   }

   @Override
   public String func_77653_i(ItemStack itemstack) {
      int meta = ItemStacks.rawMeta(itemstack);
      int type = getTypeOfValue(meta);
      if (type == 1) {
         int timeRatio = Math.min(getTimeRatioOfBeerValue(meta), this.timeRatioNames.length - 1);
         return timeRatio == this.timeRatioNames.length - 1
            ? this.timeRatioNames[timeRatio]
            : this.solidRatio[getSolidRatioOfBeerValue(meta)] + this.hopsRatio[getHopsRatioOfBeerValue(meta)] + this.timeRatioNames[timeRatio];
      } else {
         return type == 2 ? "Rum" : "Zero";
      }
   }

   public ItemStack func_77654_b(ItemStack stack, Level world, LivingEntity living) {
      int meta = ItemStacks.rawMeta(stack);
      int type = getTypeOfValue(meta);
      if (type == 0) {
         return ItemName.mug.getItemStack(ItemMug.MugType.empty);
      } else {
         if (type == 1) {
            if (getTimeRatioOfBeerValue(meta) == 5) {
               return this.drinkBlackStuff(living);
            }

            int solidRatio = getSolidRatioOfBeerValue(meta);
            int alc = getHopsRatioOfBeerValue(meta);
            int duration = this.baseDuration[solidRatio];
            float intensity = this.baseIntensity[getTimeRatioOfBeerValue(meta)];
            if (living instanceof Player) {
               ((Player)living).getFoodData().eat(6 - alc, solidRatio * 0.15F);
            }

            int max = (int)(intensity * (alc * 0.5F));
            MobEffectInstance slow = living.getEffect(MobEffects.MINING_FATIGUE);
            int level = -1;
            if (slow != null) {
               level = slow.getAmplifier();
            }

            this.amplifyEffect(living, MobEffects.MINING_FATIGUE, max, intensity, duration);
            if (level > -1) {
               this.amplifyEffect(living, MobEffects.STRENGTH, max, intensity, duration);
               if (level > 0) {
                  this.amplifyEffect(living, MobEffects.SLOWNESS, max / 2, intensity, duration);
                  if (level > 1) {
                     this.amplifyEffect(living, MobEffects.RESISTANCE, max - 1, intensity, duration);
                     if (level > 2) {
                        this.amplifyEffect(living, MobEffects.NAUSEA, 0, intensity, duration);
                        if (level > 3) {
                           living.addEffect(new MobEffectInstance(MobEffects.INSTANT_DAMAGE, 1, living.level().getRandom().nextInt(3)));
                        }
                     }
                  }
               }
            }
         }

         if (type == 2) {
            if (getProgressOfRumValue(meta) < 100) {
               this.drinkBlackStuff(living);
            } else {
               this.amplifyEffect(living, MobEffects.FIRE_RESISTANCE, 0, rumStackability, rumDuration);
               MobEffectInstance def = living.getEffect(MobEffects.RESISTANCE);
               int levelx = -1;
               if (def != null) {
                  levelx = def.getAmplifier();
               }

               this.amplifyEffect(living, MobEffects.RESISTANCE, 2, rumStackability, rumDuration);
               if (levelx >= 0) {
                  this.amplifyEffect(living, MobEffects.BLINDNESS, 0, rumStackability, rumDuration);
               }

               if (levelx >= 1) {
                  this.amplifyEffect(living, MobEffects.NAUSEA, 0, rumStackability, rumDuration);
               }
            }
         }

         return ItemName.mug.getItemStack(ItemMug.MugType.empty);
      }
   }

   public void amplifyEffect(LivingEntity living, Holder<MobEffect> potion, int max, float intensity, int duration) {
      MobEffectInstance eff = living.getEffect(potion);
      if (eff == null) {
         living.addEffect(new MobEffectInstance(potion, duration, 0));
      } else {
         int currentDuration = eff.getDuration();
         int maxnewdur = (int)(duration * (1.0F + intensity * 2.0F) - currentDuration) / 2;
         if (maxnewdur < 0) {
            maxnewdur = 0;
         }

         if (maxnewdur < duration) {
            duration = maxnewdur;
         }

         currentDuration += duration;
         int newamp = eff.getAmplifier();
         if (newamp < max) {
            newamp++;
         }

         living.addEffect(new MobEffectInstance(potion, currentDuration, newamp));
      }
   }

   public ItemStack drinkBlackStuff(LivingEntity living) {
      switch (living.level().getRandom().nextInt(6)) {
         case 1:
            living.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 1200, 0));
            break;
         case 2:
            living.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 2400, 0));
            break;
         case 3:
            living.addEffect(new MobEffectInstance(MobEffects.POISON, 2400, 0));
            break;
         case 4:
            living.addEffect(new MobEffectInstance(MobEffects.POISON, 200, 2));
            break;
         case 5:
            living.addEffect(new MobEffectInstance(MobEffects.INSTANT_DAMAGE, 1, living.level().getRandom().nextInt(4)));
      }

      return ItemName.mug.getItemStack(ItemMug.MugType.empty);
   }

   public int func_77626_a(ItemStack itemstack) {
      return 32;
   }

   public ItemUseAnimation func_77661_b(ItemStack itemstack) {
      return ItemUseAnimation.DRINK;
   }

   public InteractionResult use(Level world, Player player, InteractionHand hand) {
      player.startUsingItem(hand);
      return InteractionResult.SUCCESS.heldItemTransformedTo(StackUtil.get(player, hand));
   }

   public static int getTypeOfValue(int value) {
      return unpackValue(value, 0, 2);
   }

   public static int getAmountOfValue(int value) {
      return getTypeOfValue(value) == 0 ? 0 : unpackValue(value, 2, 5) + 1;
   }

   public static int getSolidRatioOfBeerValue(int value) {
      return unpackValue(value, 7, 3);
   }

   public static int getHopsRatioOfBeerValue(int value) {
      return unpackValue(value, 10, 3);
   }

   public static int getTimeRatioOfBeerValue(int value) {
      return unpackValue(value, 13, 3);
   }

   public static int getProgressOfRumValue(int value) {
      return unpackValue(value, 7, 7);
   }

   private static int unpackValue(int value, int bitshift, int take) {
      value >>>= bitshift;
      int mask = (1 << take) - 1;
      return value & mask;
   }

   private static enum BoozeMugType implements IIdProvider {
      beer_brew,
      beer_youngster,
      beer_beer,
      beer_ale,
      beer_dragon_blood,
      beer_black_stuff,
      rum;

      public static final ItemBooze.BoozeMugType[] values = values();

      @Override
      public String getName() {
         return this.name();
      }

      @Override
      public int getId() {
         throw new UnsupportedOperationException();
      }
   }
}
