package ic2.core.item;

import ic2.core.block.state.EnumProperty;
import ic2.core.block.state.IIdProvider;
import ic2.core.ref.IMultiItem;
import ic2.core.ref.ItemName;
import ic2.core.util.ItemStacks;
import ic2.core.util.StackUtil;
import java.util.EnumSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class ItemMulti<T extends Enum<T> & IIdProvider> extends ItemIC2 implements IMultiItem<T> {
   protected final EnumProperty<T> typeProperty;
   private final Map<T, ItemMulti.IItemRightClickHandler> rightClickHandlers = new IdentityHashMap<>();
   private final Map<T, ItemMulti.IItemUseHandler> useHandlers = new IdentityHashMap<>();
   private final Map<T, ItemMulti.IItemUpdateHandler> updateHandlers = new IdentityHashMap<>();
   private final Map<T, Rarity> rarityFilter = new IdentityHashMap<>();

   public static <T extends Enum<T> & IIdProvider> ItemMulti<T> create(ItemName name, Class<T> typeClass) {
      EnumProperty<T> typeProperty = new EnumProperty<>("type", typeClass);
      if (typeProperty.getAllowedValues().size() > 32767) {
         throw new IllegalArgumentException("Too many values to fit in a short for " + typeClass);
      } else {
         return new ItemMulti<>(name, typeProperty);
      }
   }

   private ItemMulti(ItemName name, EnumProperty<T> typeProperty) {
      super(name);
      this.typeProperty = typeProperty;
      ItemDefaults.variants(this, true);
   }

   protected ItemMulti(ItemName name, Class<T> typeClass) {
      this(name, new EnumProperty<>("type", typeClass));
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public void registerModels(ItemName name) {
      for (T type : this.typeProperty.getAllowedValues()) {
         this.registerModel(type.getId(), name, type.getModelName());
      }
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public final int getItemColor(ItemStack stack) {
      T type = this.getType(stack);
      return type == null ? super.getItemColor(stack) : type.getColor();
   }

   @Override
   public final String func_77667_c(ItemStack stack) {
      T type = this.getType(stack);
      return type == null ? super.func_77667_c(stack) : super.func_77667_c(stack) + "." + type.getName();
   }

   public ItemStack getItemStack(T type) {
      if (!this.typeProperty.getAllowedValues().contains(type)) {
         throw new IllegalArgumentException("invalid property value " + type + " for property " + this.typeProperty);
      } else {
         return this.getItemStackUnchecked(type);
      }
   }

   private ItemStack getItemStackUnchecked(T type) {
      return ItemStacks.create(this, 1, type.getId());
   }

   @Override
   public ItemStack getItemStack(String variant) {
      T type = this.typeProperty.getValue(variant).orElse(null);
      if (type == null) {
         throw new IllegalArgumentException("invalid variant " + variant + " for " + this);
      } else {
         return this.getItemStackUnchecked(type);
      }
   }

   @Override
   public String getVariant(ItemStack stack) {
      if (stack == null) {
         throw new NullPointerException("null stack");
      } else if (stack.getItem() != this) {
         throw new IllegalArgumentException("The stack " + stack + " doesn't match " + this);
      } else {
         T type = this.getType(stack);
         if (type == null) {
            throw new IllegalArgumentException("The stack " + stack + " doesn't reference any valid subtype");
         } else {
            return type.getName();
         }
      }
   }

   @Override
   public final void func_150895_a(CreativeModeTab tab, NonNullList<ItemStack> subItems) {
      if (this.func_194125_a(tab)) {
         for (T type : this.typeProperty.getShownValues()) {
            subItems.add(this.getItemStackUnchecked(type));
         }
      }
   }

   @Override
   public Set<T> getAllTypes() {
      return EnumSet.allOf(this.typeProperty.getValueClass());
   }

   public final T getType(ItemStack stack) {
      return this.typeProperty.getValue(ItemStacks.rawMeta(stack));
   }

   public InteractionResult use(Level world, Player player, InteractionHand hand) {
      ItemStack stack = StackUtil.get(player, hand);
      T type = this.getType(stack);
      if (type == null) {
         return InteractionResult.PASS;
      } else {
         ItemMulti.IItemRightClickHandler handler = this.rightClickHandlers.get(type);
         return (InteractionResult)(handler == null ? InteractionResult.PASS : handler.onRightClick(stack, player, hand));
      }
   }

   @Override
   public InteractionResult func_180614_a(Player player, Level world, BlockPos pos, InteractionHand hand, Direction side, float hitX, float hitY, float hitZ) {
      ItemStack stack = StackUtil.get(player, hand);
      T type = this.getType(stack);
      if (type == null) {
         return InteractionResult.PASS;
      } else {
         ItemMulti.IItemUseHandler handler = this.useHandlers.get(type);
         return (InteractionResult)(handler == null ? InteractionResult.PASS : handler.onUse(stack, player, pos, hand, side));
      }
   }

   public void func_77663_a(ItemStack stack, Level world, Entity entity, int slotIndex, boolean isCurrentItem) {
      T type = this.getType(stack);
      if (type != null) {
         ItemMulti.IItemUpdateHandler handler = this.updateHandlers.get(type);
         if (handler != null) {
            handler.onUpdate(stack, world, entity, slotIndex, isCurrentItem);
         }
      }
   }

   @Override
   public Rarity func_77613_e(ItemStack stack) {
      Rarity rarity = this.rarityFilter.get(this.getType(stack));
      return rarity != null ? rarity : super.func_77613_e(stack);
   }

   public void setRightClickHandler(T type, ItemMulti.IItemRightClickHandler handler) {
      if (type == null) {
         for (T cType : this.typeProperty.getAllowedValues()) {
            this.setRightClickHandler(cType, handler);
         }
      } else {
         this.rightClickHandlers.put(type, handler);
      }
   }

   public void setUseHandler(T type, ItemMulti.IItemUseHandler handler) {
      if (type == null) {
         for (T cType : this.typeProperty.getAllowedValues()) {
            this.setUseHandler(cType, handler);
         }
      } else {
         this.useHandlers.put(type, handler);
      }
   }

   public void setUpdateHandler(T type, ItemMulti.IItemUpdateHandler handler) {
      if (type == null) {
         for (T cType : this.typeProperty.getAllowedValues()) {
            this.setUpdateHandler(cType, handler);
         }
      } else {
         this.updateHandlers.put(type, handler);
      }
   }

   public void setRarity(T type, Rarity rarity) {
      if (type == null) {
         this.setRarity(rarity);
      } else {
         this.rarityFilter.put(type, rarity);
      }
   }

   public interface IItemRightClickHandler {
      InteractionResult onRightClick(ItemStack var1, Player var2, InteractionHand var3);
   }

   public interface IItemUpdateHandler {
      void onUpdate(ItemStack var1, Level var2, Entity var3, int var4, boolean var5);
   }

   public interface IItemUseHandler {
      InteractionResult onUse(ItemStack var1, Player var2, BlockPos var3, InteractionHand var4, Direction var5);
   }
}
