package ic2.core.network;

import ic2.core.IC2;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.HandlerThread;

@EventBusSubscriber(modid = "ic2")
public final class Ic2PayloadRegistration {
   @SubscribeEvent
   public static void register(RegisterPayloadHandlersEvent event) {
      event.registrar("26.3-1").executesOn(HandlerThread.NETWORK).playBidirectional(Ic2Payload.TYPE, Ic2Payload.CODEC,
         (packet, context) -> context.enqueueWork(() -> IC2.network.get(true).receive(packet.bytes(), context.player())),
         (packet, context) -> {
         if (packet.bytes().length > 0 && packet.bytes()[0] == SubPacketType.Rpc.getId()) {
            RpcHandler.processRpcResponse(GrowingBuffer.wrap(packet.bytes()));
         } else {
            context.enqueueWork(() -> IC2.network.get(false).receive(packet.bytes(), context.player()));
         }
      });
   }
}
