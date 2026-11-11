package ic2.core.block;

import ic2.core.block.state.BlockStates;
import ic2.core.block.state.EnumProperty;
import ic2.core.block.state.IIdProvider;
import ic2.core.block.type.IBlockSound;
import ic2.core.block.type.IExtBlockType;
import ic2.core.item.block.ItemBlockMulti;
import ic2.core.ref.BlockMaterial;
import ic2.core.ref.BlockName;
import ic2.core.ref.IMultiBlock;
import ic2.core.util.ItemStacks;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class BlockMultiID<T extends Enum<T> & IIdProvider> extends BlockBase implements IMultiBlock<T> {
   private static final ThreadLocal<EnumProperty<? extends Enum<?>>> currentTypeProperty = new UnstartingThreadLocal<>();
   protected final EnumProperty<T> typeProperty = this.getTypeProperty();

   public static <T extends Enum<T> & IIdProvider> BlockMultiID<T> create(BlockName name, BlockMaterial material, Class<T> typeClass) {
      EnumProperty<T> typeProperty = createTypeProperty(typeClass);
      currentTypeProperty.set(typeProperty);
      BlockMultiID<T> ret = new BlockMultiID<>(name, material);
      currentTypeProperty.remove();
      return ret;
   }

   private static <T extends Enum<T> & IIdProvider> EnumProperty<T> createTypeProperty(Class<T> typeClass) {
      EnumProperty<T> ret = new EnumProperty<>("type", typeClass);
      if (ret.getAllowedValues().size() > 16) {
         throw new IllegalArgumentException("Too many values to fit in 16 meta values for " + typeClass);
      } else {
         return ret;
      }
   }

   protected static <T extends Enum<T> & IIdProvider, U extends BlockMultiID<T>> U create(Class<U> blockClass, Class<T> typeClass, Object... ctorArgs) {
      EnumProperty<T> typeProperty = createTypeProperty(typeClass);
      Constructor<U> ctor = null;

      label89:
      for (Constructor<?> cCtor : blockClass.getDeclaredConstructors()) {
         Class<?>[] parameterTypes = cCtor.getParameterTypes();
         if (parameterTypes.length == ctorArgs.length) {
            for (int i = 0; i < parameterTypes.length; i++) {
               Class<?> type = parameterTypes[i];
               Object arg = ctorArgs[i];
               if (arg == null && type.isPrimitive() || arg != null && !parameterTypes[i].isInstance(arg)) {
                  continue label89;
               }
            }

            if (ctor != null) {
               throw new IllegalArgumentException("ambiguous constructor");
            }

            ctor = (Constructor<U>)cCtor;
         }
      }

      if (ctor == null) {
         throw new IllegalArgumentException("no matching constructor");
      } else {
         currentTypeProperty.set(typeProperty);

         U ret;
         try {
            ctor.setAccessible(true);
            ret = (U)ctor.newInstance(ctorArgs);
         } catch (Exception var16) {
            throw new RuntimeException(var16);
         } finally {
            currentTypeProperty.remove();
         }

         return ret;
      }
   }

   protected BlockMultiID(BlockName name, BlockMaterial material) {
      this(name, material, ItemBlockMulti.class);
   }

   protected BlockMultiID(BlockName name, BlockMaterial material, Class<? extends BlockItem> itemClass) {
      super(name, material, itemClass);
      this.registerDefaultState(BlockStates.with((BlockState)this.stateDefinition.any(), this.typeProperty, this.typeProperty.getDefault()));
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public void registerModels(BlockName name) {
      registerItemModels(this, this.getTypeStates());
   }

   protected final List<BlockState> getTypeStates() {
      List<BlockState> ret = new ArrayList<>(this.typeProperty.getAllowedValues().size());

      for (T type : this.typeProperty.getAllowedValues()) {
         ret.add(BlockStates.with(this.defaultBlockState(), this.typeProperty, type));
      }

      return ret;
   }

   @Override
   protected StateDefinition func_180661_e() {
      return BlockStates.definition(this, new Property[]{this.getTypeProperty()});
   }

   @Override
   public BlockState func_176203_a(int meta) {
      EnumProperty<T> typeProperty = this.getTypeProperty();
      return BlockStates.with(this.defaultBlockState(), typeProperty, typeProperty.getValueOrDefault(meta));
   }

   @Override
   public int func_176201_c(BlockState state) {
      return ((IIdProvider)((Enum)BlockStates.value(state, this.getTypeProperty()))).getId();
   }

   public T getType(BlockGetter world, BlockPos pos) {
      return this.getType(world.getBlockState(pos));
   }

   public final T getType(BlockState state) {
      return state.getBlock() != this ? null : BlockStates.value(state, this.typeProperty);
   }

   public BlockState getState(T type) {
      if (type == null) {
         throw new IllegalArgumentException("invalid type: " + type);
      } else {
         return BlockStates.with(this.defaultBlockState(), this.typeProperty, type);
      }
   }

   @Override
   public BlockState getState(String variant) {
      if (variant == null) {
         return this.defaultBlockState();
      } else {
         for (T type : this.typeProperty.getAllowedValues()) {
            if (type.name().equals(variant)) {
               return this.getState(type);
            }
         }

         throw new IllegalArgumentException("Invalid type " + variant + " for " + this);
      }
   }

   public ItemStack getItemStack(T type) {
      return this.getItemStack(this.getState(type));
   }

   @Override
   public ItemStack getItemStack(String variant) {
      if (variant == null) {
         throw new IllegalArgumentException("invalid type: " + variant);
      } else {
         T type = this.typeProperty.getValueByName(variant);
         if (type == null) {
            throw new IllegalArgumentException("invalid variant " + variant + " for " + this);
         } else {
            return this.getItemStack(type);
         }
      }
   }

   @Override
   public String getVariant(ItemStack stack) {
      if (stack == null) {
         throw new NullPointerException("null stack");
      } else {
         Item item = Item.byBlock(this);
         if (stack.getItem() != item) {
            throw new IllegalArgumentException("The stack " + stack + " doesn't match " + item + " (" + this + ")");
         } else {
            BlockState state = this.func_176203_a(ItemStacks.rawMeta(stack));
            T type = this.getType(state);
            return type.getName();
         }
      }
   }

   public ItemStack getItemStack(BlockState state) {
      if (state.getBlock() != this) {
         return null;
      } else {
         Item item = Item.byBlock(this);
         if (item != null && item != Items.AIR) {
            int meta = this.func_176201_c(state);
            return ItemStacks.create(item, 1, meta);
         } else {
            throw new RuntimeException("no matching item for " + this);
         }
      }
   }

   @Override
   public List<ItemStack> getDrops(BlockGetter world, BlockPos pos, BlockState state, int fortune) {
      ItemStack stack = this.getItemStack(state);
      if (stack == null) {
         return new ArrayList<>();
      } else {
         List<ItemStack> ret = new ArrayList<>();
         ret.add(stack);
         return ret;
      }
   }

   @Override
   public void func_149666_a(CreativeModeTab tabs, NonNullList<ItemStack> itemList) {
      for (T type : this.typeProperty.getShownValues()) {
         itemList.add(this.getItemStack(type));
      }
   }

   @Override
   public Set<T> getAllTypes() {
      return EnumSet.allOf(this.typeProperty.getValueClass());
   }

   @Override
   public ItemStack func_185473_a(Level world, BlockPos pos, BlockState state) {
      return this.getItemStack(state);
   }

   public final EnumProperty<T> getTypeProperty() {
      EnumProperty<T> ret;
      if (this.typeProperty != null) {
         ret = this.typeProperty;
      } else {
         ret = (EnumProperty<T>)currentTypeProperty.get();
         if (ret == null) {
            throw new IllegalStateException("The type property can't be obtained.");
         }
      }

      return ret;
   }

   @Override
   public float func_176195_g(BlockState state, Level world, BlockPos pos) {
      if (IExtBlockType.class.isAssignableFrom(this.typeProperty.getValueClass())) {
         T type = this.getType(state);
         if (type != null) {
            return ((IExtBlockType)type).getHardness();
         }
      }

      return super.func_176195_g(state, world, pos);
   }

   public float getExplosionResistance(Level world, BlockPos pos, Entity exploder, Explosion explosion) {
      if (IExtBlockType.class.isAssignableFrom(this.typeProperty.getValueClass())) {
         T type = this.getType(world, pos);
         if (type != null) {
            return ((IExtBlockType)type).getExplosionResistance();
         }
      }

      return super.getExplosionResistance(world.getBlockState(pos), world, pos, explosion);
   }

   public SoundType getSoundType(BlockState state, Level world, BlockPos pos, Entity entity) {
      if (IBlockSound.class.isAssignableFrom(this.typeProperty.getValueClass())) {
         T type = this.getType(state);
         if (type != null) {
            return ((IBlockSound)type).getSound();
         }
      }

      return super.getSoundType(state, world, pos, entity);
   }
}
