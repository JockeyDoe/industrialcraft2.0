package ic2.core.item.tool;

import ic2.api.item.ICustomDamageItem;
import ic2.core.block.state.EnumProperty;
import ic2.core.block.state.IIdProvider;
import ic2.core.item.ItemDefaults;
import ic2.core.item.ItemIC2;
import ic2.core.item.ItemMulti;
import ic2.core.item.ItemToolIC2;
import ic2.core.ref.IMultiItem;
import ic2.core.ref.ItemName;
import ic2.core.util.ItemData;
import ic2.core.util.ItemStacks;
import ic2.core.util.StackUtil;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class ItemToolMulti<T extends Enum<T> & IIdProvider> extends ItemToolIC2 implements IMultiItem<T>, ICustomDamageItem {
   protected final EnumProperty<T> typeProperty;
   private final Map<T, ItemMulti.IItemRightClickHandler> rightClickHandlers = new IdentityHashMap<>();
   private final Map<T, ItemMulti.IItemUseHandler> useHandlers = new IdentityHashMap<>();
   private final Map<T, ItemMulti.IItemUpdateHandler> updateHandlers = new IdentityHashMap<>();
   private final Map<T, Rarity> rarityFilter = new IdentityHashMap<>();

   public static <T extends Enum<T> & IIdProvider> ItemToolMulti<T> create(
      ItemName name, Class<T> typeClass, float damage, float speed, HarvestLevel harvestLevel, Set<? extends IToolClass> toolClasses, Set<Block> mineableBlocks
   ) {
      EnumProperty<T> typeProperty = new EnumProperty<>("type", typeClass);
      if (typeProperty.getAllowedValues().size() > 32767) {
         throw new IllegalArgumentException("Too many values to fit in a short for " + typeClass);
      } else {
         return new ItemToolMulti<>(name, typeProperty, damage, speed, harvestLevel, toolClasses, mineableBlocks);
      }
   }

   private ItemToolMulti(
      ItemName name,
      EnumProperty<T> typeProperty,
      float damage,
      float speed,
      HarvestLevel harvestLevel,
      Set<? extends IToolClass> toolClasses,
      Set<Block> mineableBlocks
   ) {
      super(name, damage, speed, harvestLevel, toolClasses, mineableBlocks);
      this.typeProperty = typeProperty;
      ItemDefaults.variants(this, true);
   }

   protected ItemToolMulti(ItemName name, Class<T> typeClass, HarvestLevel harvestLevel, Set<? extends IToolClass> toolClasses) {
      this(name, typeClass, harvestLevel, toolClasses, new HashSet<>());
   }

   protected ItemToolMulti(ItemName name, Class<T> typeClass, HarvestLevel harvestLevel, Set<? extends IToolClass> toolClasses, Set<Block> mineableBlocks) {
      this(name, typeClass, 0.0F, 0.0F, harvestLevel, toolClasses, mineableBlocks);
   }

   protected ItemToolMulti(
      ItemName name, Class<T> typeClass, float damage, float speed, HarvestLevel harvestLevel, Set<? extends IToolClass> toolClasses, Set<Block> mineableBlocks
   ) {
      this(name, new EnumProperty<>("type", typeClass), damage, speed, harvestLevel, toolClasses, mineableBlocks);
   }

   @Override
   public final String func_77667_c(ItemStack stack) {
      T type = this.getType(stack);
      return type == null ? super.func_77667_c(stack) : super.func_77667_c(stack) + "." + type.getName();
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
   public Rarity func_77613_e(ItemStack stack) {
      Rarity rarity = this.rarityFilter.get(this.getType(stack));
      return rarity != null ? rarity : super.func_77613_e(stack);
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

   public boolean showDurabilityBar(ItemStack stack) {
      return true;
   }

   public double getDurabilityForDisplay(ItemStack stack) {
      return (double)this.getCustomDamage(stack) / this.getMaxCustomDamage(stack);
   }

   public boolean func_77645_m() {
      return true;
   }

   public boolean isDamaged(ItemStack stack) {
      return this.getCustomDamage(stack) > 0;
   }

   public int getDamage(ItemStack stack) {
      return this.getCustomDamage(stack);
   }

   public int getMaxDamage(ItemStack stack) {
      return this.getMaxCustomDamage(stack);
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public void registerModels(ItemName name) {
      for (T type : this.typeProperty.getAllowedValues()) {
         ItemIC2.registerModel(this, type.getId(), name, type.getModelName());
      }
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public final int getItemColor(ItemStack stack, int tintIndex) {
      T type = this.getType(stack);
      return type == null ? super.getItemColor(stack, tintIndex) : type.getColor();
   }

   public ItemStack getItemStack(T type) {
      if (!this.typeProperty.getAllowedValues().contains(type)) {
         throw new IllegalArgumentException("Invalid property value " + type + " for property " + this.typeProperty);
      } else {
         return this.getItemStackUnchecked(type);
      }
   }

   @Override
   public ItemStack getItemStack(String variant) {
      T type = this.typeProperty.getValue(variant).orElse(null);
      if (type == null) {
         throw new IllegalArgumentException("Invalid variant " + variant + " for " + this);
      } else {
         return this.getItemStackUnchecked(type);
      }
   }

   @Override
   public String getVariant(ItemStack stack) {
      if (stack == null) {
         throw new NullPointerException("The stack cannot be null");
      } else if (stack.getItem() != this) {
         throw new IllegalArgumentException("The stack " + stack + " does not match " + this);
      } else {
         T type = this.getType(stack);
         if (type == null) {
            throw new IllegalArgumentException("The stack " + stack + " does not reference any valid subtype");
         } else {
            return type.getName();
         }
      }
   }

   @Override
   public Set<T> getAllTypes() {
      return EnumSet.allOf(this.typeProperty.getValueClass());
   }

   @Override
   public int getCustomDamage(ItemStack stack) {
      if (!ItemData.has(stack)) {
         return 0;
      } else {
         CompoundTag data = ItemData.read(stack);

         assert data != null;

         return data.contains("durability") ? data.getIntOr("durability", 0) : 0;
      }
   }

   @Override
   public int getMaxCustomDamage(ItemStack stack) {
      if (!ItemData.has(stack)) {
         return 0;
      } else {
         CompoundTag data = ItemData.read(stack);

         assert data != null;

         return data.contains("maxDurability") ? data.getIntOr("maxDurability", 0) : 0;
      }
   }

   @Override
   public void setCustomDamage(ItemStack stack, int damage) {
      CompoundTag data = StackUtil.getOrCreateNbtData(stack);
      data.putInt("durability", damage);
      ItemData.commit(stack, data);
   }

   @Override
   public boolean applyCustomDamage(ItemStack stack, int damage, LivingEntity source) {
      this.setCustomDamage(stack, this.getCustomDamage(stack) + damage);
      return true;
   }

   public final T getType(ItemStack stack) {
      return this.typeProperty.getValue(ItemStacks.rawMeta(stack));
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

   private ItemStack getItemStackUnchecked(T type) {
      return ItemStacks.create(this, 1, type.getId());
   }
}
