package ic2.core.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.Identifier;

public record Ic2Payload(byte[] bytes) implements CustomPacketPayload {
   public static final int MAX_BYTES = 32764;
   public static final Type<Ic2Payload> TYPE = new Type(Identifier.fromNamespaceAndPath("ic2", "data"));
   public static final StreamCodec<RegistryFriendlyByteBuf, Ic2Payload> CODEC = new StreamCodec<RegistryFriendlyByteBuf, Ic2Payload>() {
      public Ic2Payload decode(RegistryFriendlyByteBuf buffer) {
         return new Ic2Payload(buffer.readByteArray(32764));
      }

      public void encode(RegistryFriendlyByteBuf buffer, Ic2Payload packet) {
         buffer.writeByteArray(packet.bytes);
      }
   };

   public Ic2Payload(byte[] bytes) {
      if (bytes.length > 32764) {
         throw new IllegalArgumentException("IC2 packet too large: " + bytes.length);
      } else {
         bytes = (byte[])bytes.clone();
         this.bytes = bytes;
      }
   }

   public byte[] bytes() {
      return (byte[])this.bytes.clone();
   }

   public Type<Ic2Payload> type() {
      return TYPE;
   }

   public static Ic2Payload of(GrowingBuffer buffer, boolean advance) {
      ByteBuf raw = buffer.toByteBuf(advance);
      byte[] bytes = new byte[raw.readableBytes()];
      raw.getBytes(raw.readerIndex(), bytes);
      return new Ic2Payload(bytes);
   }
}
