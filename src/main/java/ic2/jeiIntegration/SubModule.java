package ic2.jeiIntegration;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import ic2.api.crops.CropCard;
import ic2.api.crops.Crops;
import ic2.api.recipe.Recipes;
import ic2.core.block.machine.gui.GuiIndustrialWorkbench;
import ic2.core.gui.IClickHandler;
import ic2.core.gui.MouseButton;
import ic2.core.gui.RecipeButton;
import ic2.core.item.ItemCropSeed;
import ic2.core.item.block.ItemCable;
import ic2.core.item.type.CraftingItemType;
import ic2.core.ref.BlockName;
import ic2.core.ref.ItemName;
import ic2.core.ref.TeBlock;
import ic2.jeiIntegration.recipe.crafting.AdvRecipeHandler;
import ic2.jeiIntegration.recipe.crafting.AdvShapelessRecipeHandler;
import ic2.jeiIntegration.recipe.crafting.GradualRecipeHandler;
import ic2.jeiIntegration.recipe.crafting.JetpackRecipeHandler;
import ic2.jeiIntegration.recipe.crafting.JetpackRecipeWrapper;
import ic2.jeiIntegration.recipe.machine.CannerCanningHandler;
import ic2.jeiIntegration.recipe.machine.CannerCategory;
import ic2.jeiIntegration.recipe.machine.CannerEnrichmentHandler;
import ic2.jeiIntegration.recipe.machine.DynamicCategory;
import ic2.jeiIntegration.recipe.machine.ElectrolyzerCategory;
import ic2.jeiIntegration.recipe.machine.ElectrolyzerRecipeHandler;
import ic2.jeiIntegration.recipe.machine.IORecipeCategory;
import ic2.jeiIntegration.recipe.machine.IORecipeHandler;
import ic2.jeiIntegration.recipe.machine.IRecipeWrapperGenerator;
import ic2.jeiIntegration.recipe.machine.MetalFormerCategory;
import ic2.jeiIntegration.recipe.machine.RecyclerCategory;
import ic2.jeiIntegration.recipe.misc.ScrapboxRecipeCategory;
import ic2.jeiIntegration.recipe.misc.ScrapboxRecipeHandler;
import ic2.jeiIntegration.recipe.misc.ScrapboxRecipeWrapper;
import ic2.jeiIntegration.transferhandlers.TransferHandlerBatchCrafter;
import ic2.jeiIntegration.transferhandlers.TransferHandlerIndustrialWorkbench;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.registration.IModIngredientRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import mezz.jei.api.runtime.IIngredientManager;
import mezz.jei.api.runtime.IJeiRuntime;
import mezz.jei.api.runtime.IRecipesGui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;

@JeiPlugin
public class SubModule extends RecipePlugin {
   private IIngredientManager ingredientRegistry;

   @Override
   public void register(RecipePlugin.Registry registry) {
      IGuiHelper guiHelper = registry.getJeiHelpers().getGuiHelper();
      registry.addRecipeHandlers(new RecipePresentation.IRecipeHandler[]{new AdvRecipeHandler()});
      registry.addRecipeHandlers(new RecipePresentation.IRecipeHandler[]{new AdvShapelessRecipeHandler()});
      registry.addRecipeHandlers(new RecipePresentation.IRecipeHandler[]{new GradualRecipeHandler()});
      registry.addRecipeHandlers(new RecipePresentation.IRecipeHandler[]{new JetpackRecipeHandler()});
      registry.addRecipes(JetpackRecipeWrapper.generateJetpackRecipes());
      registry.addRecipeCategories(new RecipePresentation.IRecipeCategory[]{new ScrapboxRecipeCategory(guiHelper)});
      registry.addRecipeCategoryCraftingItem(ItemName.crafting.getItemStack(CraftingItemType.scrap_box), new String[]{"ic2.scrapbox"});
      registry.addRecipeHandlers(new RecipePresentation.IRecipeHandler[]{new ScrapboxRecipeHandler()});
      registry.addRecipes(ScrapboxRecipeWrapper.createRecipes());
      registry.addRecipeHandlers(
         new RecipePresentation.IRecipeHandler[]{
            new IORecipeHandler(), new ElectrolyzerRecipeHandler(), new CannerEnrichmentHandler(), new CannerCanningHandler()
         }
      );
      this.addMachineRecipes(registry, new DynamicCategory<>(TeBlock.macerator, Recipes.macerator, guiHelper), IRecipeWrapperGenerator.basicMachine);
      this.addMachineRecipes(registry, new DynamicCategory<>(TeBlock.extractor, Recipes.extractor, guiHelper), IRecipeWrapperGenerator.basicMachine);
      this.addMachineRecipes(registry, new DynamicCategory<>(TeBlock.compressor, Recipes.compressor, guiHelper), IRecipeWrapperGenerator.basicMachine);
      this.addMachineRecipes(registry, new DynamicCategory<>(TeBlock.centrifuge, Recipes.centrifuge, guiHelper), IRecipeWrapperGenerator.basicMachine);
      this.addMachineRecipes(registry, new DynamicCategory<>(TeBlock.blast_furnace, Recipes.blastfurnace, guiHelper), IRecipeWrapperGenerator.basicMachine);
      this.addMachineRecipes(registry, new DynamicCategory<>(TeBlock.ore_washing_plant, Recipes.oreWashing, guiHelper), IRecipeWrapperGenerator.basicMachine);
      this.addMachineRecipes(registry, new DynamicCategory<>(TeBlock.block_cutter, Recipes.blockcutter, guiHelper), IRecipeWrapperGenerator.blockCutter);
      this.addMachineRecipes(registry, new MetalFormerCategory(Recipes.metalformerExtruding, 0, guiHelper), IRecipeWrapperGenerator.basicMachine);
      this.addMachineRecipes(registry, new MetalFormerCategory(Recipes.metalformerRolling, 1, guiHelper), IRecipeWrapperGenerator.basicMachine);
      this.addMachineRecipes(registry, new MetalFormerCategory(Recipes.metalformerCutting, 2, guiHelper), IRecipeWrapperGenerator.basicMachine);
      this.addMachineRecipes(registry, new ElectrolyzerCategory(guiHelper), IRecipeWrapperGenerator.electrolyzer);
      this.addMachineRecipes(registry, CannerCategory.enriching(guiHelper), IRecipeWrapperGenerator.cannerEnrichment);
      this.addMachineRecipes(registry, CannerCategory.bottling(guiHelper), IRecipeWrapperGenerator.cannerBottling);
      this.addMachineRecipes(registry, new DynamicCategory<>(TeBlock.solid_canner, Recipes.cannerBottle, guiHelper), IRecipeWrapperGenerator.cannerBottling);
      this.addMachineRecipes(registry, new RecyclerCategory(guiHelper), IRecipeWrapperGenerator.recycler);
      registry.addRecipeCategoryCraftingItem(BlockName.te.getItemStack(TeBlock.iron_furnace), new String[]{"minecraft.smelting"});
      registry.addRecipeCategoryCraftingItem(BlockName.te.getItemStack(TeBlock.electric_furnace), new String[]{"minecraft.smelting"});
      registry.addRecipeCategoryCraftingItem(BlockName.te.getItemStack(TeBlock.induction_furnace), new String[]{"minecraft.smelting"});
      registry.addRecipeCategoryCraftingItem(BlockName.te.getItemStack(TeBlock.iron_furnace), new String[]{"minecraft.fuel"});
      registry.addRecipeCategoryCraftingItem(BlockName.te.getItemStack(TeBlock.generator), new String[]{"minecraft.fuel"});
      registry.addRecipeCategoryCraftingItem(BlockName.te.getItemStack(TeBlock.solid_heat_generator), new String[]{"minecraft.fuel"});
      registry.addRecipeCategoryCraftingItem(BlockName.te.getItemStack(TeBlock.industrial_workbench), new String[]{"minecraft.crafting"});
      registry.addRecipeCategoryCraftingItem(BlockName.te.getItemStack(TeBlock.batch_crafter), new String[]{"minecraft.crafting"});
      if (FMLEnvironment.getDist() == Dist.CLIENT) {
         (new Runnable() {
            {
               Objects.requireNonNull(SubModule.this);
            }

            @Override
            public void run() {
               GuiIndustrialWorkbench.jeiScreenRecipesGuiCheck = new Predicate<Screen>() {
                  {
                     Objects.requireNonNull(<VAR_NAMELESS_ENCLOSURE>);
                  }

                  public boolean apply(Screen input) {
                     return input == Minecraft.getInstance().gui.screen() && SubModule.recipesScreenActive(input);
                  }
               };
            }
         }).run();
      }
   }

   private <T> void addMachineRecipes(RecipePlugin.Registry registry, IORecipeCategory<T> category, IRecipeWrapperGenerator<T> wrappergen) {
      registry.addRecipeCategories(new RecipePresentation.IRecipeCategory[]{category});
      registry.addRecipes(wrappergen.getRecipeList(category));
      registry.addRecipeCategoryCraftingItem(category.getBlockStack(), new String[]{category.getUid()});
   }

   public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
      this.ingredientRegistry = jeiRuntime.getIngredientManager();
      if (this.ingredientRegistry != null) {
         List<ItemStack> items = new ArrayList<>();

         for (CropCard crop : Crops.instance.getCrops()) {
            items.add(ItemCropSeed.generateItemStackFromValues(crop, 1, 1, 1, 4));
         }

         this.ingredientRegistry.addIngredientsAtRuntime(VanillaTypes.ITEM_STACK, items);
      }

      if (FMLEnvironment.getDist().isClient()) {
         final IRecipesGui recipesGUI = jeiRuntime.getRecipesGui();
         (new Runnable() {
               {
                  Objects.requireNonNull(SubModule.this);
               }

               @Override
               public void run() {
                  RecipeButton.jeiRecipeListOpener = new Function<String[], IClickHandler>() {
                     {
                        Objects.requireNonNull(<VAR_NAMELESS_ENCLOSURE>);
                     }

                     public IClickHandler apply(final String[] input) {
                        assert input != null;

                        return new IClickHandler() {
                           {
                              Objects.requireNonNull(<VAR_NAMELESS_ENCLOSURE>);
                           }

                           @Override
                           public void onClick(MouseButton button) {
                              if (input.length > 0) {
                                 recipesGUI.showTypes(
                                    Arrays.stream(input)
                                       .map(
                                          id -> (IRecipeType)(SubModule.this.definitions.categories.containsKey(id)
                                             ? SubModule.this.definitions.categories.get(id).getRecipeType()
                                             : (
                                                id.equals("minecraft.crafting")
                                                   ? RecipeTypes.CRAFTING
                                                   : (id.equals("minecraft.smelting") ? RecipeTypes.SMELTING : RecipeTypes.SMELTING_FUEL)
                                             ))
                                       )
                                       .map(type -> (IRecipeType)type)
                                       .toList()
                                 );
                              }
                           }
                        };
                     }
                  };
               }
            })
            .run();
      }
   }

   public void registerItemSubtypes(ISubtypeRegistration registry) {
      registry.registerSubtypeInterpreter(ItemName.cable.getInstance(), (stack, context) -> ItemName.cable.<ItemCable>getInstance().getVariant(stack));
   }

   private static boolean recipesScreenActive(Screen screen) {
      return screen != null && screen.getClass().getName().startsWith("mezz.jei.");
   }

   public void registerRecipeTransferHandlers(IRecipeTransferRegistration registry) {
      registry.addRecipeTransferHandler(new TransferHandlerIndustrialWorkbench(registry.getTransferHelper()), RecipeTypes.CRAFTING);
      registry.addRecipeTransferHandler(new TransferHandlerBatchCrafter(), RecipeTypes.CRAFTING);
   }

   public void registerIngredients(IModIngredientRegistration registry) {
   }
}
