package ic2.core.block;

import ic2.core.IC2;
import ic2.core.init.NativeRegistration;
import ic2.core.ref.BlockName;
import ic2.core.ref.MetaTeBlock;
import ic2.core.ref.TeBlock;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public final class NativeBlockEntities {
   private static final ThreadLocal<NativeBlockEntities.Construction> construction = new ThreadLocal<>();
   private static final Map<Class<? extends TileEntityBlock>, BlockEntityType<TileEntityBlock>> types = new LinkedHashMap<>();
   private static BlockEntityType<TileEntityBlock> dummyType;

   private NativeBlockEntities() {
   }

   public static BlockEntityType<?> constructionType() {
      return construction.get() == null ? dummyType() : construction.get().type;
   }

   public static BlockPos constructionPos() {
      return construction.get() == null ? BlockPos.ZERO : construction.get().pos;
   }

   public static BlockState constructionState() {
      return construction.get() == null ? BlockName.te.getInstance().defaultBlockState() : construction.get().state;
   }

   private static BlockEntityType<TileEntityBlock> dummyType() {
      if (dummyType == null) {
         dummyType = new BlockEntityType((pos, state) -> null, new HashSet<>(TeBlockRegistry.getAllBlocks()));
      }

      return dummyType;
   }

   public static BlockEntityType<TileEntityBlock> type(Class<? extends TileEntityBlock> cls) {
      BlockEntityType<TileEntityBlock> result = types.get(cls);
      if (result == null) {
         throw new IllegalStateException("Unregistered IC2 machine type: " + cls.getName());
      } else {
         return result;
      }
   }

   public static void register(String id, Class<? extends TileEntityBlock> cls) {
      if (!types.containsKey(cls)) {
         BlockEntityType<TileEntityBlock> type = new BlockEntityType(
            (pos, state) -> instantiate(cls, pos, state), new HashSet<>(TeBlockRegistry.getAllBlocks())
         );
         types.put(cls, NativeRegistration.add(Registries.BLOCK_ENTITY_TYPE, IC2.getIdentifier(id), type));
      }
   }

   public static <T extends TileEntityBlock> T instantiate(Class<T> cls, BlockPos pos, BlockState state) {
      NativeBlockEntities.Construction previous = construction.get();
      construction.set(new NativeBlockEntities.Construction(type(cls), pos.immutable(), state));

      TileEntityBlock var4;
      try {
         var4 = TileEntityBlock.instantiate(cls);
      } finally {
         if (previous == null) {
            construction.remove();
         } else {
            construction.set(previous);
         }
      }

      return (T)var4;
   }

   public static <T extends TileEntityBlock> BlockEntityType<T> rendererType(Class<T> cls) {
      return (BlockEntityType<T>)type(cls);
   }

   public static TileEntityBlock create(BlockPos pos, BlockState state) {
      if (state.getBlock() instanceof BlockTileEntity block) {
         ITeBlock te = ((MetaTeBlock)state.getValue(block.typeProperty)).teBlock;
         Class<? extends TileEntityBlock> cls = te.getTeClass();
         if (cls != null && cls.isAnnotationPresent(TeBlock.Delegated.class)) {
            cls = cls.getAnnotation(TeBlock.Delegated.class).current();
         }

         return cls == null ? null : instantiate(cls, pos, state);
      } else {
         return null;
      }
   }

   public static TileEntityBlock at(TileEntityBlock machine, BlockPos pos) {
      if (machine.getBlockPos().equals(pos)) {
         return machine;
      } else {
         TileEntityBlock copy = instantiate((Class<TileEntityBlock>)machine.getClass(), pos, machine.getBlockState());
         copy.loadAdditional(machine.saveAdditional(new CompoundTag()));
         return copy;
      }
   }

   private record Construction(BlockEntityType<?> type, BlockPos pos, BlockState state) {
   }
}
