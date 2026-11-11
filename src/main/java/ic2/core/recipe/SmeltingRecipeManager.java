package ic2.core.recipe;

import com.google.common.collect.Iterables;
import ic2.api.recipe.IMachineRecipeManager;
import ic2.api.recipe.IRecipeInput;
import ic2.api.recipe.MachineRecipe;
import ic2.api.recipe.MachineRecipeResult;
import ic2.api.recipe.Recipes;
import ic2.core.util.ItemStacks;
import ic2.core.util.StackUtil;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

public class SmeltingRecipeManager implements IMachineRecipeManager<ItemStack, ItemStack, ItemStack> {
   private static int nextId;
   private static final List<SmeltingRecipe> configured = new ArrayList<>();

   public boolean addRecipe(ItemStack input, ItemStack output, CompoundTag metadata, boolean replace) {
      if (find(input) != null && !replace) {
         return false;
      } else {
         float experience = metadata != null && metadata.contains("experience") ? metadata.getFloatOr("experience", 0.0F) : 0.0F;
         if (experience < 0.0F) {
            throw new IllegalArgumentException("Negative xp for " + StackUtil.toStringSafe(input) + " -> " + StackUtil.toStringSafe(output));
         } else if (!input.isEmpty() && !output.isEmpty()) {
            ItemStack key = ItemStacks.create(input.getItem(), 1, ItemStacks.rawMeta(input));
            SmeltingRecipe recipe = SmeltingRules.create(new RecipeInputItemStack(key, 1).getIngredient(), output, experience);
            Identifier id = Identifier.fromNamespaceAndPath("ic2", "configured_smelting/" + nextId++);
            configured.addFirst(recipe);
            NativeRecipePack.add(id, recipe);
            return true;
         } else {
            throw new IllegalArgumentException("Empty smelting input or output");
         }
      }
   }

   public MachineRecipeResult<ItemStack, ItemStack, ItemStack> apply(ItemStack input, boolean acceptTest) {
      SmeltingRecipe recipe = find(input);
      return recipe == null ? null : SmeltingRules.apply(recipe, input);
   }

   private static SmeltingRecipe find(ItemStack input) {
      if (input.isEmpty()) {
         return null;
      } else {
         MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
         if (server != null) {
            return server.getRecipeManager()
               .getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(input), server.overworld())
               .<SmeltingRecipe>map(RecipeHolder::value)
               .orElse(null);
         } else {
            for (SmeltingRecipe recipe : configured) {
               if (recipe.input().test(input)) {
                  return recipe;
               }
            }

            for (Recipe<?> recipex : NativeRecipes.all()) {
               if (recipex instanceof SmeltingRecipe smelting && smelting.input().test(input)) {
                  return smelting;
               }
            }

            return null;
         }
      }
   }

   public static int priority(Recipe<?> recipe) {
      return 1000 + configured.size() - configured.indexOf(recipe);
   }

   @Override
   public Iterable<? extends MachineRecipe<ItemStack, ItemStack>> getRecipes() {
      throw new UnsupportedOperationException();
   }

   @Override
   public boolean isIterable() {
      return false;
   }

   public static enum SmeltingBridge implements IMachineRecipeManager<IRecipeInput, Collection<ItemStack>, ItemStack> {
      INSTANCE;

      public boolean addRecipe(IRecipeInput input, Collection<ItemStack> output, CompoundTag metadata, boolean replace) {
         ItemStack realOutput = (ItemStack)Iterables.getOnlyElement(output);
         boolean ret = false;

         for (ItemStack stack : input.getInputs()) {
            ret |= Recipes.furnace.addRecipe(stack, realOutput, metadata, replace);
         }

         return ret;
      }

      public MachineRecipeResult<IRecipeInput, Collection<ItemStack>, ItemStack> apply(ItemStack input, boolean acceptTest) {
         MachineRecipeResult<ItemStack, ItemStack, ItemStack> normal = Recipes.furnace.apply(input, acceptTest);
         if (normal == null) {
            return null;
         } else {
            MachineRecipe<ItemStack, ItemStack> result = normal.getRecipe();
            IRecipeInput resultIn = Recipes.inputFactory.forStack(result.getInput());
            Collection<ItemStack> resultOut = Collections.singletonList(result.getOutput());
            CompoundTag resultNBT = result.getMetaData();
            return new MachineRecipe<>(resultIn, resultOut, resultNBT).getResult(normal.getAdjustedInput());
         }
      }

      @Override
      public Iterable<? extends MachineRecipe<IRecipeInput, Collection<ItemStack>>> getRecipes() {
         throw new UnsupportedOperationException();
      }

      @Override
      public boolean isIterable() {
         return false;
      }
   }
}
