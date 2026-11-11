package ic2.jeiIntegration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public abstract class RecipePlugin implements IModPlugin {
   protected RecipePlugin.Registry definitions;

   public Identifier getPluginUid() {
      return Identifier.fromNamespaceAndPath("ic2", "integration");
   }

   public abstract void register(RecipePlugin.Registry var1);

   public void registerCategories(IRecipeCategoryRegistration registration) {
      this.definitions = new RecipePlugin.Registry(registration.getJeiHelpers());
      this.definitions.addRecipeCategories(new RecipePresentation.IRecipeCategory[]{new CraftingPresentation(registration.getJeiHelpers().getGuiHelper())});
      this.register(this.definitions);
      registration.addRecipeCategories(this.definitions.categories.values().toArray(NativeRecipeCategory[]::new));
   }

   public void registerRecipes(IRecipeRegistration registration) {
      for (Entry<String, List<RecipePresentation.IRecipeWrapper>> entry : this.definitions.recipes.entrySet()) {
         NativeRecipeCategory category = this.definitions.categories.get(entry.getKey());
         if (category == null) {
            throw new IllegalStateException("IC2 recipe category absent: " + entry.getKey());
         }

         registration.addRecipes(category.getRecipeType(), entry.getValue());
      }
   }

   public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
      this.definitions.catalysts.forEach((uid, stacks) -> {
         NativeRecipeCategory category = this.definitions.categories.get(uid);
         Object var10000;
         if (category != null) {
            var10000 = category.getRecipeType();
         } else {
            switch (uid) {
               case "minecraft.crafting":
                  var10000 = RecipeTypes.CRAFTING;
                  break;
               case "minecraft.smelting":
                  var10000 = RecipeTypes.SMELTING;
                  break;
               case "minecraft.fuel":
                  var10000 = RecipeTypes.SMELTING_FUEL;
                  break;
               default:
                  throw new IllegalArgumentException(uid);
            }
         }

         IRecipeType<? extends Object> type = (IRecipeType<? extends Object>)var10000;
         stacks.forEach(stack -> registration.addRecipeCatalyst(stack, new IRecipeType[]{type}));
      });
   }

   public static final class Registry {
      private final IJeiHelpers helpers;
      final Map<String, NativeRecipeCategory> categories = new LinkedHashMap<>();
      final Map<String, List<RecipePresentation.IRecipeWrapper>> recipes = new LinkedHashMap<>();
      final Map<String, List<ItemStack>> catalysts = new LinkedHashMap<>();
      final List<RecipePresentation.IRecipeHandler<?>> handlers = new ArrayList<>();

      Registry(IJeiHelpers helpers) {
         this.helpers = helpers;
      }

      public IJeiHelpers getJeiHelpers() {
         return this.helpers;
      }

      public void addRecipeHandlers(RecipePresentation.IRecipeHandler<?>[] values) {
         this.handlers.addAll(Arrays.asList(values));
      }

      public void addRecipeCategories(RecipePresentation.IRecipeCategory<?>[] values) {
         for (RecipePresentation.IRecipeCategory<?> value : values) {
            this.categories.put(value.getUid(), new NativeRecipeCategory((RecipePresentation.IRecipeCategory<RecipePresentation.IRecipeWrapper>)value));
         }
      }

      public void addRecipeCategoryCraftingItem(ItemStack stack, String[] ids) {
         for (String uid : ids) {
            this.catalysts.computeIfAbsent(uid, k -> new ArrayList<>()).add(stack);
         }
      }

      public void addRecipes(List<?> values) {
         for (Object value : values) {
            RecipePresentation.IRecipeHandler handler = this.handlers
               .stream()
               .filter(h -> h.getRecipeClass().isInstance(value))
               .findFirst()
               .orElseThrow(() -> new IllegalArgumentException("No IC2 JEI presenter for " + value));
            if (handler.isRecipeValid(value)) {
               this.recipes.computeIfAbsent(handler.getRecipeCategoryUid(value), k -> new ArrayList<>()).add(handler.getRecipeWrapper(value));
            }
         }
      }
   }
}
