package ic2.core.recipe;

import ic2.api.recipe.IRecipeInput;
import ic2.core.init.NativeRegistration;
import java.util.Objects;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay.Composite;
import net.minecraft.world.item.crafting.display.SlotDisplay.ItemStackSlotDisplay;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;
import net.neoforged.neoforge.registries.NeoForgeRegistries.Keys;

public final class IngredientRecipeInput implements ICustomIngredient {
   public static final IngredientType<IngredientRecipeInput> TYPE = NativeRegistration.add(
      Keys.INGREDIENT_TYPES,
      Identifier.fromNamespaceAndPath("ic2", "input"),
      new IngredientType(InputCodecs.CODEC.fieldOf("input").xmap(IngredientRecipeInput::new, v -> v.part))
   );
   private final IRecipeInput part;

   public IngredientRecipeInput(IRecipeInput part) {
      this.part = Objects.requireNonNull(part);
   }

   public boolean test(ItemStack stack) {
      return !stack.isEmpty() && this.part.matches(stack);
   }

   public Stream<Holder<Item>> items() {
      return this.part.getInputs().stream().filter(s -> !s.isEmpty()).<Holder<Item>>map(ItemStack::typeHolder).distinct();
   }

   public boolean isSimple() {
      return false;
   }

   public SlotDisplay display() {
      return new Composite(
         this.part
            .getInputs()
            .stream()
            .filter(stack -> !stack.isEmpty())
            .map(stack -> new ItemStackSlotDisplay(ItemStackTemplate.fromNonEmptyStack(stack)))
            .toList()
      );
   }

   public IngredientType<?> getType() {
      return TYPE;
   }
}
