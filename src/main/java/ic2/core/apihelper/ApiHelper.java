package ic2.core.apihelper;

import ic2.api.info.Info;
import ic2.api.item.IC2Items;
import ic2.api.network.NetworkHelper;
import ic2.api.tile.IRotorProvider;
import ic2.api.tile.RotorRegistry;
import ic2.core.IC2;
import ic2.core.IC2DamageSource;
import ic2.core.model.NativeRendererRegistration;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class ApiHelper {
   public static void preload() {
      Info.DMG_ELECTRIC = IC2DamageSource.electricity;
      Info.DMG_NUKE_EXPLOSION = IC2DamageSource.nuke;
      Info.DMG_RADIATION = IC2DamageSource.radiation;
      IC2Items.setInstance(new ItemAPI());
      NetworkHelper.setInstance(IC2.network.get(true), IC2.network.get(false));
      if (IC2.platform.isRendering()) {
         RotorRegistry.setInstance(new RotorRegistry.IRotorRegistry() {
            @Override
            public <T extends BlockEntity & IRotorProvider> void registerRotorProvider(Class<T> clazz) {
               NativeRendererRegistration.rotor(clazz);
            }

            @Override
            public <T extends BlockEntity & IRotorProvider> void registerRotorProvider(BlockEntityType<T> type) {
               NativeRendererRegistration.rotor(type);
            }
         });
      }
   }
}
