package ic2.core;

import ic2.core.init.NativeRegistration;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.consume_effects.ClearAllStatusEffectsConsumeEffect;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent.Remove;

@EventBusSubscriber(modid = "ic2")
public class IC2Potion extends MobEffect {
   public static IC2Potion radiation;
   private final List<ItemStack> curativeItems;

   public static void init() {
   }

   public IC2Potion(String name, boolean badEffect, int liquidColor, ItemStack... curativeItems) {
      super(badEffect ? MobEffectCategory.HARMFUL : MobEffectCategory.BENEFICIAL, liquidColor);
      this.curativeItems = List.of(curativeItems);
      NativeRegistration.add(Registries.MOB_EFFECT, IC2.getIdentifier(name), this);
   }

   public Holder<MobEffect> holder() {
      return BuiltInRegistries.MOB_EFFECT.wrapAsHolder(this);
   }

   public String getDescriptionId() {
      return "ic2.potion.radiation";
   }

   public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
      if (this == radiation) {
         entity.hurtServer(level, IC2DamageSource.radiation.in(level), amplifier / 100 + 0.5F);
      }

      return true;
   }

   public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
      if (this != radiation) {
         return false;
      } else {
         int rate = 25 >> amplifier;
         return rate <= 0 || duration % rate == 0;
      }
   }

   public void func_76394_a(LivingEntity entity, int amplifier) {
      if (entity.level() instanceof ServerLevel level) {
         this.applyEffectTick(level, entity, amplifier);
      }
   }

   public boolean func_76397_a(int duration, int amplifier) {
      return this.shouldApplyEffectTickThisTick(duration, amplifier);
   }

   public void applyTo(LivingEntity entity, int duration, int amplifier) {
      entity.addEffect(new MobEffectInstance(this.holder(), duration, amplifier));
   }

   @SubscribeEvent
   public static void removeEffect(Remove event) {
      if (event.getEffect().value() instanceof IC2Potion effect && event.getEntity().isUsingItem()) {
         ItemStack stack = event.getEntity().getUseItem();
         Consumable consumable = (Consumable)stack.get(DataComponents.CONSUMABLE);
         if (consumable != null
            && consumable.onConsumeEffects().stream().anyMatch(e -> e instanceof ClearAllStatusEffectsConsumeEffect)
            && effect.curativeItems.stream().noneMatch(cure -> ItemStack.isSameItem(cure, stack))) {
            event.setCanceled(true);
         }
      }
   }
}
