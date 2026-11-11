package ic2.core.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import ic2.api.recipe.IRecipeInput;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.material.Fluid;

public final class InputCodecs {
   public static final Codec<IRecipeInput> CODEC = Codec.recursive(
      "ic2_recipe_input", self -> Codec.STRING.dispatch("kind", InputCodecs::kind, kind -> codec(kind, self))
   );

   private InputCodecs() {
   }

   private static String kind(IRecipeInput input) {
      if (input instanceof RecipeInputItemStack) {
         return "stack";
      } else if (input instanceof RecipeInputItemStackExact) {
         return "exact";
      } else if (input instanceof RecipeInputOreDict) {
         return "ore";
      } else if (input instanceof RecipeInputMultiple) {
         return "any";
      } else if (input instanceof RecipeInputFluidContainer) {
         return "fluid";
      } else if (input instanceof RecipeInputIngredient) {
         return "ingredient";
      } else if (input instanceof ArmorDyeingRecipe.RecipeInputClass) {
         return "class";
      } else {
         throw new IllegalArgumentException("No codec registered for recipe input " + input.getClass().getName());
      }
   }

   private static MapCodec<? extends IRecipeInput> codec(String kind, Codec<IRecipeInput> self) {
      return switch (kind) {
         case "stack" -> RecipeStackMatcher.Definition.CODEC
            .fieldOf("definition")
            .xmap(value -> new RecipeInputItemStack(value.stack(), value.amount()), value -> new RecipeStackMatcher.Definition(value.input, value.amount));
         case "exact" -> RecipeStackMatcher.Definition.CODEC
            .fieldOf("definition")
            .xmap(value -> new RecipeInputItemStackExact(value.stack(), value.amount()), value -> new RecipeStackMatcher.Definition(value.input, value.amount));
         case "ore" -> RecordCodecBuilder.mapCodec(
            i -> i.group(
                  Codec.STRING.fieldOf("name").forGetter(v -> v.input),
                  Codec.intRange(1, Integer.MAX_VALUE).fieldOf("amount").forGetter(v -> v.amount),
                  Codec.INT.optionalFieldOf("meta").forGetter(v -> Optional.ofNullable(v.meta))
               )
               .apply(i, (name, amount, meta) -> new RecipeInputOreDict(name, amount, (Integer)meta.orElse(null)))
         );
         case "any" -> self.listOf().fieldOf("options").xmap(RecipeInputMultiple::new, v -> List.of(v.inputs));
         case "fluid" -> RecordCodecBuilder.mapCodec(
            i -> i.group(
                  BuiltInRegistries.FLUID.byNameCodec().optionalFieldOf("fluid").forGetter(v -> Optional.ofNullable(v.fluid)),
                  Codec.intRange(1, Integer.MAX_VALUE).fieldOf("amount").forGetter(v -> v.amount)
               )
               .apply(i, (fluid, amount) -> new RecipeInputFluidContainer((Fluid)fluid.orElse(null), amount))
         );
         case "ingredient" -> Ingredient.CODEC.fieldOf("ingredient").xmap(RecipeInputIngredient::new, RecipeInputIngredient::getIngredient);
         case "class" -> RecordCodecBuilder.mapCodec(
            i -> i.group(
                  Codec.STRING.comapFlatMap(InputCodecs::itemClass, Class::getName).fieldOf("class").forGetter(v -> v.type),
                  Codec.intRange(1, Integer.MAX_VALUE).fieldOf("amount").forGetter(v -> v.amount)
               )
               .apply(i, ArmorDyeingRecipe.RecipeInputClass::new)
         );
         default -> throw new IllegalArgumentException("Unknown IC2 input kind: " + kind);
      };
   }

   private static DataResult<Class<?>> itemClass(String name) {
      try {
         Class<?> type = Class.forName(name, false, InputCodecs.class.getClassLoader());
         return Item.class.isAssignableFrom(type) ? DataResult.success(type) : DataResult.error(() -> "Not an item class: " + name);
      } catch (ClassNotFoundException var2) {
         return DataResult.error(() -> "Unknown item class: " + name);
      }
   }
}
