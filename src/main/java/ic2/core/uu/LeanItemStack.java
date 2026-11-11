package ic2.core.uu;

import ic2.core.item.ItemDefaults;
import ic2.core.util.ItemData;
import ic2.core.util.ItemStacks;
import ic2.core.util.StackUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

class LeanItemStack {
   private final Item item;
   private final int meta;
   private final CompoundTag nbt;
   private final int size;
   private int hashCode;

   public LeanItemStack(ItemStack stack) {
      this(stack.getItem(), StackUtil.getRawMeta(stack), ItemData.read(stack), StackUtil.getSize(stack));
   }

   public LeanItemStack(ItemStack stack, int size) {
      this(stack.getItem(), StackUtil.getRawMeta(stack), ItemData.read(stack), size);
   }

   public LeanItemStack(Item item, int meta, CompoundTag nbt, int size) {
      if (item == null) {
         throw new NullPointerException("null item");
      } else {
         this.item = item;
         this.meta = meta;
         this.nbt = nbt;
         this.size = size;
      }
   }

   public Item getItem() {
      return this.item;
   }

   public int getMeta() {
      return this.meta;
   }

   public CompoundTag getNbt() {
      return this.nbt;
   }

   public int getSize() {
      return this.size;
   }

   @Override
   public String toString() {
      return String.format("%dx%s@%d", this.size, BuiltInRegistries.ITEM.getKey(this.item), this.meta);
   }

   public boolean hasSameItem(LeanItemStack o) {
      return this.item == o.item && (this.meta == o.meta || !ItemDefaults.hasVariants(this.item)) && StackUtil.checkNbtEquality(this.nbt, o.nbt);
   }

   public LeanItemStack copy() {
      return this.copyWithSize(this.size);
   }

   public LeanItemStack copyWithSize(int newSize) {
      LeanItemStack ret = new LeanItemStack(this.item, this.meta, this.nbt, newSize);
      ret.hashCode = this.hashCode;
      return ret;
   }

   public ItemStack toMcStack() {
      if (this.size <= 0) {
         return StackUtil.emptyStack;
      } else {
         ItemStack ret = ItemStacks.create(this.item, this.size, this.meta);
         ItemData.store(ret, this.nbt);
         return ret;
      }
   }

   @Override
   public boolean equals(Object obj) {
      return !(obj instanceof LeanItemStack o)
         ? false
         : this.item == o.item && this.meta == o.meta && (this.nbt == null && o.nbt == null || this.nbt != null && o.nbt != null && this.nbt.equals(o.nbt));
   }

   @Override
   public int hashCode() {
      if (this.hashCode == 0) {
         this.hashCode = this.calculateHashCode();
      }

      return this.hashCode;
   }

   private int calculateHashCode() {
      int ret = System.identityHashCode(this.item);
      ret = ret * 31 + this.meta;
      if (this.nbt != null) {
         ret = ret * 61 + this.nbt.hashCode();
      }

      if (ret == 0) {
         ret = -1;
      }

      return ret;
   }
}
