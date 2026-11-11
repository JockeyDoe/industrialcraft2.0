package ic2.api.tile;

import ic2.api.info.Info;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModLoadingContext;

@OnlyIn(Dist.CLIENT)
public class RotorRegistry {
   private static RotorRegistry.IRotorRegistry INSTANCE;

   public static <T extends BlockEntity & IRotorProvider> void registerRotorProvider(Class<T> clazz) {
      if (INSTANCE != null) {
         INSTANCE.registerRotorProvider(clazz);
      }
   }

   public static void setInstance(RotorRegistry.IRotorRegistry i) {
      ModContainer mc = ModLoadingContext.get().getActiveContainer();
      if (mc != null && Info.MOD_ID.equals(mc.getModId())) {
         INSTANCE = i;
      } else {
         throw new IllegalAccessError("Only IC2 can set the instance");
      }
   }

   public static <T extends BlockEntity & IRotorProvider> void registerRotorProvider(BlockEntityType<T> type) {
      if (INSTANCE != null) {
         INSTANCE.registerRotorProvider(type);
      }
   }

   public interface IRotorRegistry {
      <T extends BlockEntity & IRotorProvider> void registerRotorProvider(Class<T> var1);

      <T extends BlockEntity & IRotorProvider> void registerRotorProvider(BlockEntityType<T> var1);
   }
}
