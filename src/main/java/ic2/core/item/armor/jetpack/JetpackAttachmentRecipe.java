package ic2.core.item.armor.jetpack;

import ic2.api.item.ElectricItem;
import ic2.api.recipe.IRecipeInput;
import ic2.api.recipe.Recipes;
import ic2.core.init.MainConfig;
import ic2.core.item.type.CraftingItemType;
import ic2.core.recipe.LegacyCraftingRecipe;
import ic2.core.recipe.RecipeCodecs;
import ic2.core.ref.ItemName;
import ic2.core.util.ConfigUtil;
import ic2.core.util.ItemStacks;
import ic2.core.util.StackUtil;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public class JetpackAttachmentRecipe implements LegacyCraftingRecipe {
   private final IRecipeInput attachmentPlate = Recipes.inputFactory.forStack(ItemName.crafting.getItemStack(CraftingItemType.jetpack_attachment_plate));
   public static final Set<Item> blacklistedItems = Collections.newSetFromMap(new IdentityHashMap<>());
   private Identifier name;

   public RecipeSerializer<? extends CraftingRecipe> getSerializer() {
      return RecipeCodecs.JETPACK;
   }

   public static void init() {
      for (ItemStack stack : ConfigUtil.asStackList(MainConfig.get(), "recipes/jetpackAttachmentBlacklist")) {
         blacklistedItems.add(stack.getItem());
      }
   }

   @Override
   public boolean matches(CraftingContainer inv, Level worldIn) {
      return this.func_77572_b(inv) != StackUtil.emptyStack;
   }

   @Override
   public ItemStack func_77572_b(CraftingContainer inv) {
      ItemStack jetpack = null;
      ItemStack armor = null;
      boolean attachmentPlate = false;

      for (int i = 0; i < inv.getContainerSize(); i++) {
         ItemStack currentStack = inv.getItem(i);
         if (!StackUtil.isEmpty(currentStack)) {
            Item item = currentStack.getItem();
            if (item == ItemName.jetpack_electric.getInstance()) {
               if (jetpack != null) {
                  return StackUtil.emptyStack;
               }

               jetpack = currentStack;
            } else if (ItemStacks.equipmentSlot(currentStack) == EquipmentSlot.CHEST && !blacklistedItems.contains(item)) {
               if (armor != null) {
                  return StackUtil.emptyStack;
               }

               armor = currentStack;
            } else {
               if (!this.attachmentPlate.matches(currentStack)) {
                  return StackUtil.emptyStack;
               }

               if (attachmentPlate) {
                  return StackUtil.emptyStack;
               }

               attachmentPlate = true;
            }
         }
      }

      if (jetpack != null && armor != null && attachmentPlate && !JetpackHandler.hasJetpackAttached(armor)) {
         ItemStack ret = armor.copy();
         JetpackHandler.setJetpackAttached(ret, true);
         ElectricItem.manager.charge(ret, ElectricItem.manager.getCharge(jetpack), Integer.MAX_VALUE, true, false);
         return ret;
      } else {
         return StackUtil.emptyStack;
      }
   }

   @Override
   public ItemStack func_77571_b() {
      return StackUtil.emptyStack;
   }

   @Override
   public NonNullList<ItemStack> func_179532_b(CraftingContainer inv) {
      return LegacyCraftingRecipe.super.func_179532_b(inv);
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
      return x * y >= 3;
   }

   static {
      blacklistedItems.add(ItemName.jetpack.getInstance());
      blacklistedItems.add(ItemName.jetpack_electric.getInstance());
      blacklistedItems.add(ItemName.quantum_chestplate.getInstance());
   }
}
