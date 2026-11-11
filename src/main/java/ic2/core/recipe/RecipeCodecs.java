package ic2.core.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import ic2.api.item.ICustomDamageItem;
import ic2.api.recipe.ICraftingRecipeManager;
import ic2.core.init.NativeRegistration;
import ic2.core.item.armor.jetpack.JetpackAttachmentRecipe;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;

public final class RecipeCodecs {
   public static final RecipeSerializer<AdvRecipe> ADVRECIPE = register("shaped", shaped(AdvRecipe::new));
   public static final RecipeSerializer<ColourCarryingRecipe> COLOUR = register("colour_carrying", shaped(ColourCarryingRecipe::new));
   public static final RecipeSerializer<AdvShapelessRecipe> ADVSHAPELESSRECIPE = register(
      "shapeless",
      RecordCodecBuilder.mapCodec(
         i -> i.group(
               RecipeStackCodec.CODEC.fieldOf("result").forGetter(v -> v.output),
               InputCodecs.CODEC.listOf().fieldOf("ingredients").forGetter(v -> List.of(v.input)),
               Codec.BOOL.optionalFieldOf("hidden", false).forGetter(v -> v.hidden),
               Codec.BOOL.optionalFieldOf("consuming", false).forGetter(v -> v.consuming)
            )
            .apply(i, (output, inputs, hidden, consuming) -> {
               List<Object> args = new ArrayList<>(inputs);
               args.add(attributes(hidden, consuming));
               return new AdvShapelessRecipe(output, args.toArray());
            })
      )
   );
   public static final RecipeSerializer<GradualRecipe> GRADUALRECIPE = register(
      "gradual",
      RecordCodecBuilder.mapCodec(
         i -> i.group(
               BuiltInRegistries.ITEM
                  .byNameCodec()
                  .validate(
                     item -> item instanceof ICustomDamageItem
                        ? DataResult.success(item)
                        : DataResult.error(() -> "IC2 gradual result must support custom damage")
                  )
                  .fieldOf("item")
                  .forGetter(v -> (Item)v.item),
               RecipeStackCodec.CODEC.fieldOf("filler").forGetter(v -> v.chargeMaterial),
               Codec.INT.fieldOf("amount").forGetter(v -> v.amount),
               Codec.BOOL.optionalFieldOf("hidden", false).forGetter(v -> v.hidden)
            )
            .apply(i, (item, filler, amount, hidden) -> new GradualRecipe((ICustomDamageItem)item, filler, amount, hidden))
      )
   );
   public static final RecipeSerializer<ArmorDyeingRecipe> DYE = register(
      "armor_dyeing", InputCodecs.CODEC.fieldOf("armor").xmap(ArmorDyeingRecipe::new, v -> v.armour)
   );
   public static final RecipeSerializer<JetpackAttachmentRecipe> JETPACK = register("jetpack_attachment", MapCodec.unit(JetpackAttachmentRecipe::new));

   private RecipeCodecs() {
   }

   private static ICraftingRecipeManager.AttributeContainer attributes(boolean hidden, boolean consuming) {
      return new ICraftingRecipeManager.AttributeContainer(hidden, consuming);
   }

   private static <T extends AdvRecipe> MapCodec<T> shaped(BiFunction<ItemStack, Object[], T> factory) {
      return RecordCodecBuilder.mapCodec(
         i -> i.group(
               RecipeStackCodec.CODEC.fieldOf("result").forGetter(v -> v.output),
               Codec.STRING.listOf().fieldOf("pattern").forGetter(v -> v.pattern),
               Codec.unboundedMap(Codec.STRING, InputCodecs.CODEC).fieldOf("key").forGetter(v -> v.keys),
               Codec.BOOL.optionalFieldOf("hidden", false).forGetter(v -> v.hidden),
               Codec.BOOL.optionalFieldOf("consuming", false).forGetter(v -> v.consuming)
            )
            .apply(i, (output, pattern, keys, hidden, consuming) -> {
               List<Object> args = new ArrayList<>(pattern);
               keys.forEach((key, input) -> {
                  if (key.length() == 1 && key.charAt(0) != ' ') {
                     args.add(key.charAt(0));
                     args.add(input);
                  } else {
                     throw new IllegalArgumentException("Invalid IC2 recipe key: " + key);
                  }
               });
               args.add(attributes(hidden, consuming));
               return factory.apply(output, args.toArray());
            })
      );
   }

   private static <T extends CraftingRecipe> RecipeSerializer<T> register(String path, MapCodec<T> codec) {
      return NativeRegistration.add(
         Registries.RECIPE_SERIALIZER,
         Identifier.fromNamespaceAndPath("ic2", path),
         new RecipeSerializer(codec, ByteBufCodecs.fromCodecWithRegistries(codec.codec()))
      );
   }

   public static void init() {
      Objects.requireNonNull(IngredientRecipeInput.TYPE);
   }
}
