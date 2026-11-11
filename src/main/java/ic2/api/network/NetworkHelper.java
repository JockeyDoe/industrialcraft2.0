package ic2.api.network;

import ic2.api.info.Info;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.fml.LogicalSide;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.util.thread.EffectiveSide;

public final class NetworkHelper {
   private static INetworkManager serverInstance;
   private static INetworkManager clientInstance;

   public static void updateTileEntityField(BlockEntity te, String field) {
      getNetworkManager(EffectiveSide.get()).updateTileEntityField(te, field);
   }

   public static void initiateTileEntityEvent(BlockEntity te, int event, boolean limitRange) {
      getNetworkManager(EffectiveSide.get()).initiateTileEntityEvent(te, event, limitRange);
   }

   public static void initiateItemEvent(Player player, ItemStack stack, int event, boolean limitRange) {
      getNetworkManager(EffectiveSide.get()).initiateItemEvent(player, stack, event, limitRange);
   }

   public static void sendInitialData(BlockEntity te) {
      getNetworkManager(EffectiveSide.get()).sendInitialData(te);
   }

   public static void initiateClientTileEntityEvent(BlockEntity te, int event) {
      getNetworkManager(EffectiveSide.get()).initiateClientTileEntityEvent(te, event);
   }

   public static void initiateClientItemEvent(ItemStack stack, int event) {
      getNetworkManager(EffectiveSide.get()).initiateClientItemEvent(stack, event);
   }

   public static INetworkManager getNetworkManager(LogicalSide side) {
      return side.isClient() ? clientInstance : serverInstance;
   }

   public static void setInstance(INetworkManager server, INetworkManager client) {
      ModContainer mc = ModLoadingContext.get().getActiveContainer();
      if (mc != null && Info.MOD_ID.equals(mc.getModId())) {
         serverInstance = server;
         clientInstance = client;
      } else {
         throw new IllegalAccessError();
      }
   }
}
