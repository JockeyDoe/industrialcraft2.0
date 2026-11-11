package ic2.core.item;

import ic2.core.init.Localization;
import ic2.core.ref.ItemName;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties.Builder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class ItemFoodIc2 extends ItemIC2 {
   protected ItemFoodIc2(ItemName name, int amount, float saturation, boolean isWolfFood) {
      super(name, foodProperties(name, amount, saturation));
   }

   private static Properties foodProperties(ItemName name, int amount, float saturation) {
      Builder food = new Builder().nutrition(amount).saturationModifier(saturation);
      if (name == ItemName.terra_wart) {
         food.alwaysEdible();
      }

      Properties properties = new Properties().food(food.build());
      if (name == ItemName.terra_wart) {
         properties.rarity(Rarity.RARE);
      }

      return properties;
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public void registerModels(ItemName name) {
      ItemIC2.registerModel(this, 0, name, null);
   }

   @Override
   public String func_77658_a() {
      return this.getDescriptionId();
   }

   public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
      return this.func_77654_b(stack, level, entity);
   }

   public ItemStack func_77654_b(ItemStack stack, Level level, LivingEntity entity) {
      return super.finishUsingItem(stack, level, entity);
   }

   @Override
   public String func_77667_c(ItemStack stack) {
      return this.func_77658_a();
   }

   @Override
   public String func_77657_g(ItemStack stack) {
      return this.func_77667_c(stack);
   }

   @Override
   public String func_77653_i(ItemStack stack) {
      return Localization.translate(this.func_77667_c(stack));
   }
}
