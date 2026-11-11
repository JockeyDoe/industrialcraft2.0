package ic2.core.network;

import com.mojang.authlib.GameProfile;
import ic2.api.network.INetworkItemEventListener;
import ic2.api.network.INetworkTileEntityEventListener;
import ic2.core.ExplosionIC2;
import ic2.core.IC2;
import ic2.core.IHasGui;
import ic2.core.NativeMenus;
import ic2.core.audio.AudioPosition;
import ic2.core.audio.PositionSpec;
import ic2.core.block.ITeBlock;
import ic2.core.block.TeBlockRegistry;
import ic2.core.block.TileEntityBlock;
import ic2.core.block.comp.Components;
import ic2.core.block.comp.TileEntityComponent;
import ic2.core.item.IHandHeldInventory;
import ic2.core.item.IHandHeldSubInventory;
import ic2.core.util.LevelIds;
import ic2.core.util.ParticleUtil;
import ic2.core.util.StackUtil;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Objects;
import java.util.zip.InflaterOutputStream;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

@OnlyIn(Dist.CLIENT)
public class NetworkManagerClient extends NetworkManager {
   private GrowingBuffer largePacketBuffer;

   @Override
   protected boolean isClient() {
      return true;
   }

   @Override
   public void initiateClientItemEvent(ItemStack stack, int event) {
      try {
         GrowingBuffer buffer = new GrowingBuffer(256);
         SubPacketType.ItemEvent.writeTo(buffer);
         DataEncoder.encode(buffer, stack, false);
         buffer.writeInt(event);
         buffer.flip();
         this.sendPacket(buffer);
      } catch (IOException var4) {
         throw new RuntimeException(var4);
      }
   }

   @Override
   public void initiateKeyUpdate(int keyState) {
      GrowingBuffer buffer = new GrowingBuffer(5);
      SubPacketType.KeyUpdate.writeTo(buffer);
      buffer.writeInt(keyState);
      buffer.flip();
      this.sendPacket(buffer);
   }

   @Override
   public void initiateClientTileEntityEvent(BlockEntity te, int event) {
      try {
         GrowingBuffer buffer = new GrowingBuffer(32);
         SubPacketType.TileEntityEvent.writeTo(buffer);
         DataEncoder.encode(buffer, te, false);
         buffer.writeInt(event);
         buffer.flip();
         this.sendPacket(buffer);
      } catch (IOException var4) {
         throw new RuntimeException(var4);
      }
   }

   @Override
   public void initiateRpc(int id, Class<? extends IRpcProvider<?>> provider, Object[] args) {
      try {
         GrowingBuffer buffer = new GrowingBuffer(256);
         SubPacketType.Rpc.writeTo(buffer);
         buffer.writeInt(id);
         buffer.writeString(provider.getName());
         DataEncoder.encode(buffer, args);
         buffer.flip();
         this.sendPacket(buffer);
      } catch (IOException var5) {
         throw new RuntimeException(var5);
      }
   }

   @Override
   public void requestGUI(IHasGui inventory) {
      try {
         GrowingBuffer buffer = new GrowingBuffer(32);
         SubPacketType.RequestGUI.writeTo(buffer);
         if (inventory instanceof BlockEntity te) {
            buffer.writeBoolean(false);
            DataEncoder.encode(buffer, te, false);
         } else {
            Player player = Minecraft.getInstance().player;
            if (!StackUtil.isEmpty(player.containerMenu.getCarried()) && player.containerMenu.getCarried().getItem() instanceof IHandHeldInventory
               || !StackUtil.isEmpty(player.getOffhandItem()) && player.getOffhandItem().getItem() instanceof IHandHeldInventory) {
               buffer.writeBoolean(true);
            } else {
               IC2.platform
                  .displayError(
                     "An unknown GUI type was attempted to be displayed.\nThis could happen due to corrupted data from a player or a bug.\n\n(Technical information: "
                        + inventory
                        + ")"
                  );
            }
         }

         buffer.flip();
         this.sendPacket(buffer);
      } catch (IOException var4) {
         throw new RuntimeException(var4);
      }
   }

   @Override
   protected void sendToServer(Ic2Payload payload) {
      ClientPacketDistributor.sendToServer(payload, new CustomPacketPayload[0]);
   }

   @Override
   public void receive(byte[] bytes, Player player) {
      try {
         this.onPacketData(GrowingBuffer.wrap(bytes), player);
      } catch (IOException var4) {
         throw new IllegalArgumentException("Invalid IC2 client packet", var4);
      }
   }

   private void onPacketData(GrowingBuffer is, Player player) throws IOException {
      if (is.hasAvailable()) {
         SubPacketType packetType = SubPacketType.read(is, false);
         if (packetType != null) {
            switch (packetType) {
               case LargePacket:
                  int state = is.readUnsignedByte();
                  if ((state & 2) != 0) {
                     GrowingBuffer input;
                     if ((state & 1) != 0) {
                        input = is;
                     } else {
                        input = this.largePacketBuffer;
                        if (input == null) {
                           throw new IOException("unexpected large packet continuation");
                        }

                        is.writeTo(input);
                        input.flip();
                        this.largePacketBuffer = null;
                     }

                     GrowingBuffer decompBuffer = new GrowingBuffer(input.available() * 2);
                     InflaterOutputStream inflate = new InflaterOutputStream(decompBuffer);
                     input.writeTo(inflate);
                     inflate.close();
                     decompBuffer.flip();
                     switch (state >> 2) {
                        case 0:
                           TeUpdate.receive(decompBuffer);
                           return;
                        case 1:
                           processChatPacket(decompBuffer);
                           return;
                        case 2:
                           processConsolePacket(decompBuffer);
                     }
                  } else {
                     if ((state & 1) != 0) {
                        assert this.largePacketBuffer == null;

                        this.largePacketBuffer = new GrowingBuffer(32752);
                     }

                     if (this.largePacketBuffer == null) {
                        throw new IOException("unexpected large packet continuation");
                     }

                     is.writeTo(this.largePacketBuffer);
                  }
                  break;
               case TileEntityEvent: {
                  final Object teDeferred = DataEncoder.decodeDeferred(is, BlockEntity.class);
                  final int event = is.readInt();
                  IC2.platform.requestTick(false, new Runnable() {
                     {
                        Objects.requireNonNull(NetworkManagerClient.this);
                     }

                     @Override
                     public void run() {
                        BlockEntity te = DataEncoder.getValue(teDeferred);
                        if (te instanceof INetworkTileEntityEventListener) {
                           ((INetworkTileEntityEventListener)te).onNetworkEvent(event);
                        }
                     }
                  });
                  break;
               }
               case ItemEvent: {
                  final GameProfile profile = DataEncoder.decode(is, GameProfile.class);
                  final ItemStack stack = DataEncoder.decode(is, ItemStack.class);
                  final int event = is.readInt();
                  IC2.platform
                     .requestTick(
                        false,
                        new Runnable() {
                           {
                              Objects.requireNonNull(NetworkManagerClient.this);
                           }

                           @Override
                           public void run() {
                              Level world = Minecraft.getInstance().level;

                              for (Object obj : world.players()) {
                                 Player player = (Player)obj;
                                 if (profile.id() != null && profile.id().equals(player.getGameProfile().id())
                                    || profile.id() == null && profile.name().equals(player.getGameProfile().name())) {
                                    if (stack.getItem() instanceof INetworkItemEventListener) {
                                       ((INetworkItemEventListener)stack.getItem()).onNetworkEvent(stack, player, event);
                                    }
                                    break;
                                 }
                              }
                           }
                        }
                     );
                  break;
               }
               case GuiDisplay:
                  final boolean isAdmin = is.readBoolean();
                  switch (is.readByte()) {
                     case 0: {
                        final Object teDeferredx = DataEncoder.decodeDeferred(is, BlockEntity.class);
                        final int windowId = is.readInt();
                        IC2.platform.requestTick(false, new Runnable() {
                           {
                              Objects.requireNonNull(NetworkManagerClient.this);
                           }

                           @Override
                           public void run() {
                              Player player = IC2.platform.getPlayerInstance();
                              BlockEntity te = DataEncoder.getValue(teDeferredx);
                              if (te instanceof IHasGui) {
                                 NativeMenus.launchClient(windowId, player, (IHasGui)te, isAdmin);
                              } else if (player instanceof LocalPlayer) {
                                 ((LocalPlayer)player).connection.send(new ServerboundContainerClosePacket(windowId));
                              }
                           }
                        });
                        return;
                     }
                     case 1: {
                        final int currentItemPosition = is.readInt();
                        final boolean subGUI = is.readBoolean();
                        final short ID = subGUI ? is.readShort() : 0;
                        final int windowId = is.readInt();
                        IC2.platform
                           .requestTick(
                              false,
                              new Runnable() {
                                 {
                                    Objects.requireNonNull(NetworkManagerClient.this);
                                 }

                                 @Override
                                 public void run() {
                                    Player player = IC2.platform.getPlayerInstance();
                                    ItemStack currentItem;
                                    if (currentItemPosition < 0) {
                                       int actualItemPosition = ~currentItemPosition;
                                       if (actualItemPosition > 0) {
                                          return;
                                       }

                                       currentItem = player.getOffhandItem();
                                    } else {
                                       if (currentItemPosition != player.getInventory().getSelectedSlot()) {
                                          return;
                                       }

                                       currentItem = player.getMainHandItem();
                                    }

                                    if (currentItem != null && currentItem.getItem() instanceof IHandHeldInventory) {
                                       if (subGUI && currentItem.getItem() instanceof IHandHeldSubInventory) {
                                          NativeMenus.launchClient(
                                             windowId, player, ((IHandHeldSubInventory)currentItem.getItem()).getSubInventory(player, currentItem, ID), isAdmin
                                          );
                                       } else {
                                          NativeMenus.launchClient(
                                             windowId, player, ((IHandHeldInventory)currentItem.getItem()).getInventory(player, currentItem), isAdmin
                                          );
                                       }
                                    } else if (player instanceof LocalPlayer) {
                                       ((LocalPlayer)player).connection.send(new ServerboundContainerClosePacket(windowId));
                                    }
                                 }
                              }
                           );
                        return;
                     }
                     default:
                        return;
                  }
               case ExplosionEffect: {
                  final Object worldDeferred = DataEncoder.decodeDeferred(is, Level.class);
                  final Vec3 pos = DataEncoder.decode(is, Vec3.class);
                  final ExplosionIC2.Type type = DataEncoder.decodeEnum(is, ExplosionIC2.Type.class);
                  IC2.platform
                     .requestTick(
                        false,
                        new Runnable() {
                           {
                              Objects.requireNonNull(NetworkManagerClient.this);
                           }

                           @Override
                           public void run() {
                              Level world = DataEncoder.getValue(worldDeferred);
                              if (world != null) {
                                 switch (type) {
                                    case Normal:
                                       world.playLocalSound(
                                          pos.x,
                                          pos.y,
                                          pos.z,
                                          (SoundEvent)SoundEvents.GENERIC_EXPLODE.value(),
                                          SoundSource.BLOCKS,
                                          4.0F,
                                          (1.0F + (world.getRandom().nextFloat() - world.getRandom().nextFloat()) * 0.2F) * 0.7F,
                                          false
                                       );
                                       world.addParticle(ParticleTypes.EXPLOSION_EMITTER, pos.x, pos.y, pos.z, 0.0, 0.0, 0.0);
                                       break;
                                    case Electrical:
                                       IC2.audioManager
                                          .playOnce(
                                             new AudioPosition(world, (float)pos.x, (float)pos.y, (float)pos.z),
                                             PositionSpec.Center,
                                             "Machines/MachineOverload.ogg",
                                             true,
                                             IC2.audioManager.getDefaultVolume()
                                          );
                                       world.addParticle(ParticleTypes.EXPLOSION_EMITTER, pos.x, pos.y, pos.z, 0.0, 0.0, 0.0);
                                       break;
                                    case Heat:
                                       world.playLocalSound(
                                          pos.x,
                                          pos.y,
                                          pos.z,
                                          SoundEvents.LAVA_EXTINGUISH,
                                          SoundSource.BLOCKS,
                                          4.0F,
                                          (1.0F + (world.getRandom().nextFloat() - world.getRandom().nextFloat()) * 0.2F) * 0.7F,
                                          false
                                       );
                                       world.addParticle(ParticleTypes.EXPLOSION_EMITTER, pos.x, pos.y, pos.z, 0.0, 0.0, 0.0);
                                       break;
                                    case Nuclear:
                                       IC2.audioManager
                                          .playOnce(
                                             new AudioPosition(world, (float)pos.x, (float)pos.y, (float)pos.z),
                                             PositionSpec.Center,
                                             "Tools/NukeExplosion.ogg",
                                             true,
                                             IC2.audioManager.getDefaultVolume()
                                          );
                                       world.addParticle(ParticleTypes.EXPLOSION_EMITTER, pos.x, pos.y, pos.z, 0.0, 0.0, 0.0);
                                 }
                              }
                           }
                        }
                     );
                  break;
               }
               case Rpc:
                  throw new RuntimeException("Received unexpected RPC packet");
               case TileEntityBlockComponent: {
                  final int dimensionId = is.readInt();
                  final BlockPos pos = DataEncoder.decode(is, BlockPos.class);
                  String componentName = is.readString();
                  final Class<? extends TileEntityComponent> componentCls = Components.getClass(componentName);
                  if (componentCls == null) {
                     throw new IOException("invalid component: " + componentName);
                  }

                  int dataLen = is.readVarInt();
                  if (dataLen > 65536) {
                     throw new IOException("data length limit exceeded");
                  }

                  final byte[] data = new byte[dataLen];
                  is.readFully(data);
                  IC2.platform.requestTick(false, new Runnable() {
                     {
                        Objects.requireNonNull(NetworkManagerClient.this);
                     }

                     @Override
                     public void run() {
                        Level world = Minecraft.getInstance().level;
                        if (LevelIds.id(world) == dimensionId) {
                           BlockEntity teRaw = world.getBlockEntity(pos);
                           if (teRaw instanceof TileEntityBlock) {
                              TileEntityComponent component = ((TileEntityBlock)teRaw).getComponent(componentCls);
                              if (component != null) {
                                 DataInputStream dataIs = new DataInputStream(new ByteArrayInputStream(data));

                                 try {
                                    component.onNetworkUpdate(dataIs);
                                 } catch (IOException var6) {
                                    throw new RuntimeException(var6);
                                 }
                              }
                           }
                        }
                     }
                  });
                  break;
               }
               case TileEntityBlockLandEffect: {
                  final Object worldDeferred = DataEncoder.decodeDeferred(is, Level.class);
                  final BlockPos pos;
                  if (is.readBoolean()) {
                     pos = (BlockPos)DataEncoder.decode(is, DataEncoder.EncodedType.BlockPos);
                  } else {
                     pos = null;
                  }

                  final double x = is.readDouble();
                  final double y = is.readDouble();
                  final double z = is.readDouble();
                  final int count = is.readInt();
                  final ITeBlock teBlock = TeBlockRegistry.get(is.readString());
                  IC2.platform.requestTick(false, new Runnable() {
                     {
                        Objects.requireNonNull(NetworkManagerClient.this);
                     }

                     @Override
                     public void run() {
                        Level world = DataEncoder.getValue(worldDeferred);
                        if (world != null) {
                           ParticleUtil.spawnBlockLandParticles(world, pos, x, y, z, count, teBlock);
                        }
                     }
                  });
                  break;
               }
               case TileEntityBlockRunEffect: {
                  final Object worldDeferred = DataEncoder.decodeDeferred(is, Level.class);
                  final BlockPos pos;
                  if (is.readBoolean()) {
                     pos = (BlockPos)DataEncoder.decode(is, DataEncoder.EncodedType.BlockPos);
                  } else {
                     pos = null;
                  }

                  final double x = is.readDouble();
                  final double y = is.readDouble();
                  final double z = is.readDouble();
                  final double xSpeed = is.readDouble();
                  final double zSpeed = is.readDouble();
                  final ITeBlock teBlock = TeBlockRegistry.get(is.readString());
                  IC2.platform.requestTick(false, new Runnable() {
                     {
                        Objects.requireNonNull(NetworkManagerClient.this);
                     }

                     @Override
                     public void run() {
                        Level world = DataEncoder.getValue(worldDeferred);
                        if (world != null) {
                           ParticleUtil.spawnBlockRunParticles(world, pos, x, y, z, xSpeed, zSpeed, teBlock);
                        }
                     }
                  });
                  break;
               }
               default:
                  this.onCommonPacketData(packetType, false, is, player);
            }
         }
      }
   }

   private static void processChatPacket(GrowingBuffer buffer) {
      final String messages = buffer.readString();
      IC2.platform.requestTick(false, new Runnable() {
         @Override
         public void run() {
            for (String line : messages.split("[\\r\\n]+")) {
               IC2.platform.messagePlayer(null, line);
            }
         }
      });
   }

   private static void processConsolePacket(GrowingBuffer buffer) {
      String messages = buffer.readString();
      PrintStream console = new PrintStream(new FileOutputStream(FileDescriptor.out));

      for (String line : messages.split("[\\r\\n]+")) {
         console.println(line);
      }

      console.flush();
   }
}
