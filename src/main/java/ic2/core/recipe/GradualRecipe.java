package ic2.core.recipe;

import ic2.api.item.ICustomDamageItem;
import ic2.core.init.MainConfig;
import ic2.core.init.Rezepte;
import ic2.core.util.StackUtil;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public class GradualRecipe implements LegacyCraftingRecipe {
   public ICustomDamageItem item;
   public ItemStack chargeMaterial;
   public int amount;
   public boolean hidden;
   private Identifier name;

   public RecipeSerializer<? extends CraftingRecipe> getSerializer() {
      return RecipeCodecs.GRADUALRECIPE;
   }

   public static void addAndRegister(ItemStack itemToFill, int amount, Object... args) {
      try {
         if (itemToFill == null) {
            AdvRecipe.displayError("Null item to fill", null, null, true);
         } else {
            if (!(itemToFill.getItem() instanceof ICustomDamageItem)) {
               AdvRecipe.displayError("Filling item must extends ItemGradualInt", null, itemToFill, true);
            }

            ICustomDamageItem fillingItem = (ICustomDamageItem)itemToFill.getItem();
            Boolean hidden = false;
            ItemStack filler = null;

            for (Object o : args) {
               if (o instanceof Boolean) {
                  hidden = (Boolean)o;
               } else {
                  try {
                     filler = AdvRecipe.getRecipeObject(o).getInputs().get(0);
                     break;
                  } catch (IndexOutOfBoundsException var11) {
                     AdvRecipe.displayError("Invalid filler item: " + o, null, itemToFill, true);
                  } catch (Exception var12) {
                     var12.printStackTrace();
                     AdvRecipe.displayError("unknown type", "O: " + o + "\nT: " + o.getClass().getName(), itemToFill, true);
                  }
               }
            }

            Rezepte.registerRecipe(new GradualRecipe(fillingItem, filler, amount, hidden));
         }
      } catch (RuntimeException var13) {
         if (!MainConfig.ignoreInvalidRecipes) {
            throw var13;
         }
      }
   }

   public GradualRecipe(ICustomDamageItem item, ItemStack chargeMaterial, int amount) {
      this(item, chargeMaterial, amount, false);
   }

   public GradualRecipe(ICustomDamageItem item, ItemStack chargeMaterial, int amount, boolean hidden) {
      this.item = item;
      this.chargeMaterial = chargeMaterial;
      this.amount = amount;
      this.hidden = hidden;
   }

   @Override
   public boolean matches(CraftingContainer ic, Level world) {
      return this.func_77572_b(ic) != StackUtil.emptyStack;
   }

   @Override
   public ItemStack func_77572_b(CraftingContainer ic) {
      return CraftingAlgorithms.gradual(ic, this.item, offered -> StackUtil.checkItemEquality(offered, this.chargeMaterial), this.amount);
   }

   @Override
   public ItemStack func_77571_b() {
      return new ItemStack((Item)this.item);
   }

   @Override
   public NonNullList<ItemStack> func_179532_b(CraftingContainer inv) {
      return LegacyCraftingRecipe.super.func_179532_b(inv);
   }

   public boolean canShow() {
      return AdvRecipe.canShow(new Object[]{this.chargeMaterial}, this.func_77571_b(), this.hidden);
   }

   @Override
   public LegacyCraftingRecipe setRegistryName(Identifier name) {
      this.name = name;
      return this;
   }

   @Override
   public Identifier getRegistryName() {
      return this.name;
   }

   public Class<Recipe> getRegistryType() {
      return Recipe.class;
   }

   public boolean func_194133_a(int x, int y) {
      return x * y >= 2;
   }
}
