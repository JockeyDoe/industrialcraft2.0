package ic2.core.block.machine.tileentity;

import ic2.api.upgrade.IUpgradableBlock;
import ic2.api.upgrade.UpgradableProperty;
import ic2.core.IHasGui;
import ic2.core.init.Localization;
import ic2.core.item.type.MiscResourceType;
import ic2.core.profile.NotClassic;
import ic2.core.recipe.AdvRecipe;
import ic2.core.recipe.LegacyCraftingRecipe;
import ic2.core.recipe.NativeCrafting;
import ic2.core.ref.ItemName;
import ic2.core.ref.TeBlock;
import ic2.core.util.StackUtil;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@NotClassic
public class TileEntityAssemblyBench extends TileEntityBatchCrafter implements IHasGui, IUpgradableBlock {
   public static final List<Recipe> RECIPES = new ArrayList<>();

   @Override
   protected Recipe findRecipe() {
      for (Recipe recipe : RECIPES) {
         if (NativeCrafting.matches(recipe, this.crafting, this.getLevel())) {
            return recipe;
         }
      }

      return null;
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public void addInformation(ItemStack stack, List<String> tooltip, TooltipFlag advanced) {
      tooltip.add("You probably want the " + Localization.translate(this.getBlockType().getDescriptionId() + "." + TeBlock.replicator.getName()));
   }

   @Override
   public Set<UpgradableProperty> getUpgradableProperties() {
      return EnumSet.of(UpgradableProperty.Transformer, UpgradableProperty.ItemConsuming, UpgradableProperty.ItemProducing);
   }

   public static class UuRecipe extends AdvRecipe {
      protected final ItemStack output;
      protected final boolean[][] shape;

      public static TileEntityAssemblyBench.UuRecipe create(ItemStack output, Object... args) {
         Queue<String> inputArrangement = new ArrayDeque<>();

         for (Object arg : args) {
            if (arg instanceof String str) {
               if (str.isEmpty() || str.length() > 3) {
                  AdvRecipe.displayError("none or too many crafting columns", "Input: " + str + "\nSize: " + str.length(), output, false);
               }

               inputArrangement.add(str);
            }
         }

         boolean[][] shape = new boolean[3][3];

         for (int y = 0; y < 3; y++) {
            String layer = inputArrangement.poll();

            for (int x = 0; x < 3; x++) {
               shape[y][x] = layer.charAt(x) != ' ';
            }
         }

         return new TileEntityAssemblyBench.UuRecipe(output, shape);
      }

      public UuRecipe(ItemStack output, boolean[][] shape) {
         super(output, recipeArguments(shape));
         if (StackUtil.isEmpty(output)) {
            AdvRecipe.displayError("Empty result", "UU recipe with shape " + Arrays.deepToString(shape), output, false);
         }

         int inputWidth = shape[0].length;

         for (boolean[] col : shape) {
            if (col.length != inputWidth) {
               AdvRecipe.displayError("Inconsistent recipe shape", "UU recipe with shape " + Arrays.deepToString(shape), output, false);
            }
         }

         this.output = output;
         this.shape = shape;
      }

      private static Object[] recipeArguments(boolean[][] shape) {
         List<Object> args = new ArrayList<>();
         if (shape.length != 3) {
            throw new IllegalArgumentException("UU recipe requires three rows");
         } else {
            for (boolean[] row : shape) {
               if (row.length != 3) {
                  throw new IllegalArgumentException("UU recipe requires three columns");
               }

               StringBuilder pattern = new StringBuilder();

               for (boolean occupied : row) {
                  pattern.append((char)(occupied ? 'U' : ' '));
               }

               args.add(pattern.toString());
            }

            args.add('U');
            args.add(ItemName.misc_resource.getItemStack(MiscResourceType.matter));
            return args.toArray();
         }
      }

      @Override
      public boolean matches(CraftingContainer inv, Level world) {
         ItemStack uu = ItemName.misc_resource.getItemStack(MiscResourceType.matter);
         int y = 0;
         int height = inv.getHeight();

         for (int width = inv.getWidth(); y < height; y++) {
            boolean[] layer = this.shape[y];

            for (int x = 0; x < width; x++) {
               ItemStack stack = inv.getItem(x + y * width);
               if (layer[x]) {
                  if (!StackUtil.checkItemEquality(stack, uu)) {
                     return false;
                  }
               } else if (!StackUtil.isEmpty(stack)) {
                  return false;
               }
            }
         }

         return true;
      }

      @Override
      public ItemStack func_77571_b() {
         return this.output;
      }

      @Override
      public ItemStack func_77572_b(CraftingContainer inv) {
         return this.func_77571_b().copy();
      }

      @Override
      public boolean func_194133_a(int width, int height) {
         throw new UnsupportedOperationException();
      }

      @Override
      public LegacyCraftingRecipe setRegistryName(Identifier name) {
         throw new UnsupportedOperationException();
      }

      @Override
      public Identifier getRegistryName() {
         throw new UnsupportedOperationException();
      }

      @Override
      public Class<Recipe> getRegistryType() {
         throw new UnsupportedOperationException();
      }
   }
}
