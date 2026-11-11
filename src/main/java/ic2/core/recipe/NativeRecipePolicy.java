package ic2.core.recipe;

import ic2.api.recipe.IRecipeInput;
import ic2.core.util.StackUtil;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.item.crafting.SingleItemRecipe;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.item.crafting.AbstractCookingRecipe.CookingBookInfo;
import net.minecraft.world.item.crafting.Recipe.CommonInfo;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.crafting.RecipePriorityManager;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.TagsUpdatedEvent.ServerDataLoad;
import net.neoforged.neoforge.resource.NeoForgeReloadListeners;

@EventBusSubscriber(modid = "ic2")
public final class NativeRecipePolicy {
   private static List<IRecipeInput> purge = List.of();
   private static boolean unifySmelting;

   private NativeRecipePolicy() {
   }

   public static void configure(List<IRecipeInput> inputs, boolean unify) {
      purge = List.copyOf(inputs);
      unifySmelting = unify;
   }

   public static ItemStack output(Recipe<?> recipe) {
      if (recipe instanceof LegacyCraftingRecipe legacy) {
         return legacy.func_77571_b();
      } else if (recipe instanceof ShapedRecipe shaped) {
         return shaped.assemble(CraftingInput.EMPTY);
      } else if (recipe instanceof ShapelessRecipe shapeless) {
         return shapeless.assemble(CraftingInput.EMPTY);
      } else if (recipe instanceof SingleItemRecipe single) {
         return single.assemble(new SingleRecipeInput(ItemStack.EMPTY));
      } else {
         ContextMap context = ContextMap.builder().buildAndValidate(SlotDisplayContext.CONTEXT);
         return recipe.display()
            .stream()
            .map(display -> display.result().resolveForFirstStack(context))
            .filter(stack -> !stack.isEmpty())
            .findFirst()
            .orElse(ItemStack.EMPTY);
      }
   }

   public static List<RecipeHolder<?>> apply(Collection<RecipeHolder<?>> source) {
      List<RecipeHolder<?>> result = new ArrayList<>();

      for (RecipeHolder<?> holder : source) {
         Recipe<?> recipe = holder.value();
         ItemStack output = output(recipe);
         if (holder.id().identifier().getNamespace().equals("ic2") || output.isEmpty() || !purge.stream().anyMatch(input -> input.matches(output))) {
            if (unifySmelting && recipe instanceof SmeltingRecipe smelting && !output.isEmpty()) {
               ItemStack unified = unify(output);
               if (!ItemStack.isSameItemSameComponents(unified, output)) {
                  Recipe<?> var8 = new SmeltingRecipe(
                     new CommonInfo(smelting.showNotification()),
                     new CookingBookInfo(smelting.category(), smelting.group()),
                     smelting.input(),
                     ItemStackTemplate.fromNonEmptyStack(unified),
                     smelting.experience(),
                     smelting.cookingTime()
                  );
                  holder = new RecipeHolder(holder.id(), var8);
               }
            }

            result.add(holder);
         }
      }

      return List.copyOf(result);
   }

   private static ItemStack unify(ItemStack output) {
      for (int oreId : RecipeTags.getOreIDs(output)) {
         for (ItemStack ore : RecipeTags.getOres(RecipeTags.getOreName(oreId))) {
            if (!ore.isEmpty() && BuiltInRegistries.ITEM.getKey(ore.getItem()).getNamespace().equals("ic2")) {
               return StackUtil.copyWithSize(ore, output.getCount());
            }
         }
      }

      return output;
   }

   @SubscribeEvent
   public static void reload(AddServerReloadListenersEvent event) {
      Identifier id = Identifier.fromNamespaceAndPath("ic2", "recipe_policy");
      RecipeManager manager = event.getServerResources().getRecipeManager();
      RecipePriorityManager priorities = (RecipePriorityManager)event.getRegistry().get(NeoForgeReloadListeners.RECIPE_PRIORITIES);
      event.addListener(id, (ResourceManagerReloadListener)resources -> {
         Collection<RecipeHolder<?>> adjusted = manager.getRecipes();
         Object2IntOpenHashMap<ResourceKey<Recipe<?>>> ordered = new Object2IntOpenHashMap();
         if (priorities != null) {
            ordered.putAll(priorities.getRecipePriorities());
         }

         for (RecipeHolder<?> holder : adjusted) {
            if (holder.value() instanceof ShapedRecipe && !ordered.containsKey(holder.id())) {
               ordered.put(holder.id(), 200);
            }
         }

         manager.setPriorityMap(Object2IntMaps.unmodifiable(ordered));
      });
      event.addDependency(NeoForgeReloadListeners.RECIPE_PRIORITIES, id);
   }

   @SubscribeEvent(priority = EventPriority.LOWEST)
   public static void tagsBound(ServerDataLoad event) {
      RecipeManager manager = event.getServerResources().getRecipeManager();
      List<RecipeHolder<?>> adjusted = apply(manager.getRecipes());
      replace(manager, "recipes", RecipeMap.createClient(adjusted));
      replace(manager, "learnableRecipes", adjusted.stream().filter(holder -> !holder.value().isSpecial()).toList());
   }

   private static void replace(RecipeManager manager, String name, Object value) {
      try {
         Field field = RecipeManager.class.getDeclaredField(name);
         field.setAccessible(true);
         field.set(manager, value);
      } catch (ReflectiveOperationException var4) {
         throw new IllegalStateException("Cannot apply IC2 recipe policy to " + name, var4);
      }
   }
}
