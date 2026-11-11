package ic2.core.recipe;

import java.util.List;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.context.ContextMap;
import net.minecraft.util.context.ContextMap.Builder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

public final class NativeRecipes {
   private NativeRecipes() {
   }

   public static List<Recipe<?>> all() {
      MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
      return server != null
         ? server.getRecipeManager().getRecipes().stream().map(holder -> holder.value()).toList()
         : List.copyOf(NativeRecipePack.configuredRecipes().values());
   }

   public static List<ItemStack> examples(Ingredient ingredient) {
      Builder builder = ContextMap.builder();
      MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
      if (server != null) {
         builder.set(SlotDisplayContext.REGISTRIES, server.registryAccess());
      }

      return ingredient.display().resolveForStacks(builder.buildAndValidate(SlotDisplayContext.CONTEXT));
   }
}
