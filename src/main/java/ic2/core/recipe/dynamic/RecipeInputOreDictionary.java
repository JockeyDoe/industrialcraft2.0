package ic2.core.recipe.dynamic;

import ic2.core.recipe.RecipeTags;
import ic2.core.util.ItemStacks;
import java.util.List;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class RecipeInputOreDictionary extends RecipeInputIngredient<String> {
   public final int amount;
   public final Integer meta;
   private List<ItemStack> equivalents;

   public static RecipeInputOreDictionary of(String ingredient) {
      return of(ingredient, 1);
   }

   public static RecipeInputOreDictionary of(String ingredient, int amount) {
      return of(ingredient, amount, null);
   }

   public static RecipeInputOreDictionary of(String ingredient, int amount, Integer meta) {
      return new RecipeInputOreDictionary(ingredient, amount, meta);
   }

   protected RecipeInputOreDictionary(String ingredient) {
      this(ingredient, 1);
   }

   protected RecipeInputOreDictionary(String ingredient, int amount) {
      this(ingredient, amount, null);
   }

   protected RecipeInputOreDictionary(String ingredient, int amount, Integer meta) {
      super(ingredient);
      this.amount = amount;
      this.meta = meta;
   }

   @Override
   public Object getUnspecific() {
      return null;
   }

   @Override
   public RecipeInputIngredient<String> copy() {
      throw new UnsupportedOperationException("Not supported");
   }

   @Override
   public boolean isEmpty() {
      return this.amount <= 0;
   }

   @Override
   public int getCount() {
      return this.amount;
   }

   @Override
   public void shrink(int amount) {
      throw new UnsupportedOperationException("Not supported");
   }

   @Override
   public boolean matches(Object other) {
      if (!(other instanceof ItemStack)) {
         return false;
      } else {
         List<ItemStack> inputs = this.getEquivalents();
         boolean useOreStackMeta = this.meta == null;
         Item subjectItem = ((ItemStack)other).getItem();
         int subjectMeta = ItemStacks.rawMeta((ItemStack)other);

         for (ItemStack oreStack : inputs) {
            Item oreItem = oreStack.getItem();
            if (oreItem != null) {
               int metaRequired = useOreStackMeta ? ItemStacks.rawMeta(oreStack) : this.meta;
               if (subjectItem == oreItem && (subjectMeta == metaRequired || metaRequired == 32767)) {
                  return true;
               }
            }
         }

         return false;
      }
   }

   @Override
   public boolean matchesStrict(Object other) {
      return !(other instanceof String) ? false : this.ingredient.equals(other);
   }

   @Override
   public String toStringSafe() {
      return this.ingredient;
   }

   private List<ItemStack> getEquivalents() {
      if (this.equivalents != null) {
         return this.equivalents;
      } else {
         List<ItemStack> ret = RecipeTags.getOres(this.ingredient);
         if (ret != RecipeTags.EMPTY_LIST) {
            this.equivalents = ret;
         }

         return ret;
      }
   }
}
