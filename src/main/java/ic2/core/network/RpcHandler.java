package ic2.core.network;

import ic2.core.IC2;
import ic2.core.util.LogCategory;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut;
import net.neoforged.neoforge.common.NeoForge;

public class RpcHandler {
   private static ConcurrentMap<String, IRpcProvider<?>> providers = new ConcurrentHashMap<>();
   private static ConcurrentMap<Integer, Rpc<?>> pending = new ConcurrentHashMap<>();

   public static boolean registerProvider(IRpcProvider<?> provider) {
      return providers.putIfAbsent(provider.getClass().getName(), provider) == null;
   }

   public static <V> Rpc<V> run(Class<? extends IRpcProvider<V>> provider, Object... args) {
      int id = IC2.random.nextInt();
      Rpc<V> rpc = new Rpc<>();
      Rpc<V> prev = (Rpc<V>)pending.putIfAbsent(id, rpc);
      if (prev != null) {
         return run(provider, args);
      } else {
         IC2.network.get(false).initiateRpc(id, provider, args);
         return rpc;
      }
   }

   protected static void processRpcRequest(GrowingBuffer is, ServerPlayer player) throws IOException {
      int id = is.readInt();
      String providerClassName = is.readString();
      Object[] args = (Object[])DataEncoder.decode(is);
      IRpcProvider<?> provider = providers.get(providerClassName);
      if (provider == null) {
         IC2.log.warn(LogCategory.Network, "Invalid RPC request from %s.", player.getName());
      } else {
         Object result = provider.executeRpc(args);
         GrowingBuffer buffer = new GrowingBuffer(256);
         SubPacketType.Rpc.writeTo(buffer);
         buffer.writeInt(id);
         DataEncoder.encode(buffer, result, true);
         buffer.flip();
         IC2.network.get(true).sendPacket(buffer, true, player);
      }
   }

   public RpcHandler() {
      NeoForge.EVENT_BUS.register(this);
   }

   @SubscribeEvent
   public void onDisconnect(LoggingOut event) {
      for (Rpc<?> rpc : pending.values()) {
         rpc.cancel(true);
      }

      pending.clear();
   }

   static void processRpcResponse(GrowingBuffer buffer) {
      try {
         buffer.readByte();
         int id = buffer.readInt();
         Object result = DataEncoder.decode(buffer);
         Rpc<?> rpc = pending.remove(id);
         if (rpc == null) {
            IC2.log.warn(LogCategory.Network, "RPC %d wasn't found while trying to process its response.", id);
         } else {
            rpc.finish(result);
         }
      } catch (IOException var4) {
         throw new RuntimeException(var4);
      }
   }
}
