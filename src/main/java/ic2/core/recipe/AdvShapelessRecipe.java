package ic2.core.recipe;

import ic2.api.item.ElectricItem;
import ic2.api.recipe.ICraftingRecipeManager;
import ic2.api.recipe.IRecipeInput;
import ic2.core.init.MainConfig;
import ic2.core.init.Rezepte;
import ic2.core.util.StackUtil;
import ic2.core.util.Util;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay.ItemSlotDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay.ItemStackSlotDisplay;
import net.minecraft.world.level.Level;

public class AdvShapelessRecipe implements LegacyCraftingRecipe {
   public ItemStack output;
   public IRecipeInput[] input;
   public boolean hidden;
   public boolean consuming;
   private Identifier name;

   public RecipeSerializer<? extends CraftingRecipe> getSerializer() {
      return RecipeCodecs.ADVSHAPELESSRECIPE;
   }

   public static void addAndRegister(ItemStack result, Object... args) {
      try {
         Rezepte.registerRecipe(new AdvShapelessRecipe(result, args));
      } catch (RuntimeException var3) {
         if (!MainConfig.ignoreInvalidRecipes) {
            throw var3;
         }
      }
   }

   public AdvShapelessRecipe(ItemStack result, Object... args) {
      if (result == null) {
         AdvRecipe.displayError("null result", null, null, true);
      } else {
         result = result.copy();
      }

      this.input = new IRecipeInput[args.length - Util.countInArray(args, Boolean.class, ICraftingRecipeManager.AttributeContainer.class)];
      int inputIndex = 0;

      for (Object o : args) {
         if (o instanceof Boolean) {
            this.hidden = (Boolean)o;
         } else if (o instanceof ICraftingRecipeManager.AttributeContainer) {
            this.hidden = ((ICraftingRecipeManager.AttributeContainer)o).hidden;
            this.consuming = ((ICraftingRecipeManager.AttributeContainer)o).consuming;
         } else {
            try {
               this.input[inputIndex++] = AdvRecipe.getRecipeObject(o);
            } catch (Exception var9) {
               var9.printStackTrace();
               AdvRecipe.displayError("unknown type", "O: " + o + "\nT: " + o.getClass().getName(), result, true);
            }
         }
      }

      if (inputIndex != this.input.length) {
         AdvRecipe.displayError("length calculation error", "I: " + inputIndex + "\nL: " + this.input.length, result, true);
      }

      this.output = result;
   }

   @Override
   public boolean matches(CraftingContainer inventorycrafting, Level world) {
      return this.func_77572_b(inventorycrafting) != StackUtil.emptyStack;
   }

   @Override
   public ItemStack func_77572_b(CraftingContainer inventorycrafting) {
      List<Predicate<ItemStack>> predicates = Arrays.stream(this.input).map(value -> value::matches).toList();
      return CraftingAlgorithms.shapeless(inventorycrafting, predicates, this.output, ElectricItem.manager);
   }

   @Override
   public ItemStack func_77571_b() {
      return this.output;
   }

   public boolean canShow() {
      return AdvRecipe.canShow(this.input, this.output, this.hidden);
   }

   @Override
   public NonNullList<ItemStack> func_179532_b(CraftingContainer inv) {
      return this.consuming ? NonNullList.withSize(inv.getContainerSize(), StackUtil.emptyStack) : LegacyCraftingRecipe.super.func_179532_b(inv);
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
      return x * y >= this.input.length;
   }

   public NonNullList<Ingredient> func_192400_c() {
      NonNullList<Ingredient> list = NonNullList.create();
      if (!this.hidden) {
         for (IRecipeInput input : this.input) {
            list.add(input.getIngredient());
         }
      }

      return list;
   }

   public boolean func_192399_d() {
      return this.hidden;
   }

   @Override
   public boolean isSpecial() {
      return this.hidden;
   }

   @Override
   public PlacementInfo placementInfo() {
      return PlacementInfo.create(Arrays.stream(this.input).map(IRecipeInput::getIngredient).toList());
   }

   public List<RecipeDisplay> display() {
      return this.hidden
         ? List.of()
         : List.of(
            new ShapelessCraftingRecipeDisplay(
               Arrays.stream(this.input).map(value -> value.getIngredient().display()).toList(),
               new ItemStackSlotDisplay(ItemStackTemplate.fromNonEmptyStack(this.output)),
               new ItemSlotDisplay(Items.CRAFTING_TABLE)
            )
         );
   }
}
