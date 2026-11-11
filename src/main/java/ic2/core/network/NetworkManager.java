package ic2.core.network;

import ic2.api.network.ClientModifiable;
import ic2.api.network.INetworkClientTileEntityEventListener;
import ic2.api.network.INetworkDataProvider;
import ic2.api.network.INetworkItemEventListener;
import ic2.api.network.INetworkManager;
import ic2.core.ContainerBase;
import ic2.core.ExplosionIC2;
import ic2.core.IC2;
import ic2.core.IHasGui;
import ic2.core.WorldData;
import ic2.core.block.ITeBlock;
import ic2.core.block.TileEntityBlock;
import ic2.core.item.IHandHeldInventory;
import ic2.core.item.IHandHeldSubInventory;
import ic2.core.item.tool.HandHeldInventory;
import ic2.core.util.LogCategory;
import ic2.core.util.ReflectionUtil;
import ic2.core.util.StackUtil;
import ic2.core.util.Util;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.zip.DeflaterOutputStream;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

public class NetworkManager implements INetworkManager {
   private static final int maxPacketDataLength = 32764;
   public static final String channelName = "ic2";

   protected boolean isClient() {
      return false;
   }

   public void onTickEnd(WorldData worldData) {
      try {
         TeUpdate.send(worldData, this);
      } catch (IOException var3) {
         throw new RuntimeException(var3);
      }
   }

   public final void sendPlayerItemData(Player player, int slot, Object... data) {
      GrowingBuffer buffer = new GrowingBuffer(256);

      try {
         SubPacketType.PlayerItemData.writeTo(buffer);
         buffer.writeByte(slot);
         DataEncoder.encode(buffer, ((ItemStack)player.getInventory().getNonEquipmentItems().get(slot)).getItem(), false);
         buffer.writeVarInt(data.length);

         for (Object o : data) {
            DataEncoder.encode(buffer, o);
         }
      } catch (IOException var9) {
         throw new RuntimeException(var9);
      }

      buffer.flip();
      if (!this.isClient()) {
         this.sendPacket(buffer, true, (ServerPlayer)player);
      } else {
         this.sendPacket(buffer);
      }
   }

   @Override
   public final void updateTileEntityField(BlockEntity te, String field) {
      if (!this.isClient()) {
         getTeUpdateData(te).addGlobalField(field);
      } else if (this.getClientModifiableField(te.getClass(), field) == null) {
         IC2.log.warn(LogCategory.Network, "Field update for %s failed.", te);
      } else {
         GrowingBuffer buffer = new GrowingBuffer(64);

         try {
            SubPacketType.TileEntityData.writeTo(buffer);
            DataEncoder.encode(buffer, te, false);
            writeFieldData(te, field, buffer);
         } catch (IOException var5) {
            throw new RuntimeException(var5);
         }

         buffer.flip();
         this.sendPacket(buffer);
      }
   }

   private Field getClientModifiableField(Class<?> cls, String fieldName) {
      Field field = ReflectionUtil.getFieldRecursive(cls, fieldName);
      if (field == null) {
         IC2.log.warn(LogCategory.Network, "Can't find field %s in %s.", fieldName, cls.getName());
         return null;
      } else if (field.getAnnotation(ClientModifiable.class) == null) {
         IC2.log.warn(LogCategory.Network, "The field %s in %s is not modifiable.", fieldName, cls.getName());
         return null;
      } else {
         return field;
      }
   }

   private static TeUpdateDataServer getTeUpdateData(BlockEntity te) {
      assert IC2.platform.isSimulating();

      if (te == null) {
         throw new NullPointerException();
      } else {
         WorldData worldData = WorldData.get(te.getLevel());
         TeUpdateDataServer ret = worldData.tesToUpdate.get(te);
         if (ret == null) {
            ret = new TeUpdateDataServer();
            worldData.tesToUpdate.put(te, ret);
         }

         return ret;
      }
   }

   public final void updateTileEntityFieldTo(BlockEntity te, String field, ServerPlayer player) {
      assert !this.isClient();

      getTeUpdateData(te).addPlayerField(field, player);
   }

   public final void sendComponentUpdate(TileEntityBlock te, String componentName, ServerPlayer player, GrowingBuffer data) {
      assert !this.isClient();

      if (player.level() != te.getLevel()) {
         throw new IllegalArgumentException("mismatched world (te " + te.getLevel() + ", player " + player.level() + ")");
      } else {
         GrowingBuffer buffer = new GrowingBuffer(64);

         try {
            SubPacketType.TileEntityBlockComponent.writeTo(buffer);
            DataEncoder.encode(buffer, te, false);
            buffer.writeString(componentName);
            buffer.writeVarInt(data.available());
            data.writeTo(buffer);
         } catch (IOException var7) {
            throw new RuntimeException(var7);
         }

         buffer.flip();
         this.sendPacket(buffer, true, player);
      }
   }

   @Override
   public final void initiateTileEntityEvent(BlockEntity te, int event, boolean limitRange) {
      assert !this.isClient();

      if (!te.getLevel().players().isEmpty()) {
         GrowingBuffer buffer = new GrowingBuffer(32);

         try {
            SubPacketType.TileEntityEvent.writeTo(buffer);
            DataEncoder.encode(buffer, te, false);
            buffer.writeInt(event);
         } catch (IOException var9) {
            throw new RuntimeException(var9);
         }

         buffer.flip();

         for (ServerPlayer target : (ArrayList)getPlayersInRange(te.getLevel(), te.getBlockPos(), new ArrayList())) {
            if (limitRange) {
               int dX = (int)(te.getBlockPos().getX() + 0.5 - target.getX());
               int dZ = (int)(te.getBlockPos().getZ() + 0.5 - target.getZ());
               if (dX * dX + dZ * dZ > 400) {
                  continue;
               }
            }

            this.sendPacket(buffer, false, target);
         }
      }
   }

   @Override
   public final void initiateItemEvent(Player player, ItemStack stack, int event, boolean limitRange) {
      if (StackUtil.isEmpty(stack)) {
         throw new NullPointerException("invalid stack: " + StackUtil.toStringSafe(stack));
      } else {
         assert !this.isClient();

         GrowingBuffer buffer = new GrowingBuffer(256);

         try {
            SubPacketType.ItemEvent.writeTo(buffer);
            DataEncoder.encode(buffer, player.getGameProfile(), false);
            DataEncoder.encode(buffer, stack, false);
            buffer.writeInt(event);
         } catch (Exception var10) {
            throw new RuntimeException(var10);
         }

         buffer.flip();

         for (ServerPlayer target : (ArrayList)getPlayersInRange(player.level(), player.blockPosition(), new ArrayList())) {
            if (limitRange) {
               int dX = (int)(player.getX() - target.getX());
               int dZ = (int)(player.getZ() - target.getZ());
               if (dX * dX + dZ * dZ > 400) {
                  continue;
               }
            }

            this.sendPacket(buffer, false, target);
         }
      }
   }

   @Override
   public void initiateClientItemEvent(ItemStack stack, int event) {
      assert false;
   }

   @Override
   public void initiateClientTileEntityEvent(BlockEntity te, int event) {
      assert false;
   }

   public void initiateRpc(int id, Class<? extends IRpcProvider<?>> provider, Object[] args) {
      assert false;
   }

   public void requestGUI(IHasGui inventory) {
      assert false;
   }

   public final void initiateGuiDisplay(ServerPlayer player, IHasGui inventory, int windowId) {
      this.initiateGuiDisplay(player, inventory, windowId, null);
   }

   public final void initiateGuiDisplay(ServerPlayer player, IHasGui inventory, int windowId, Integer ID) {
      assert !this.isClient();

      try {
         GrowingBuffer buffer = new GrowingBuffer(32);
         SubPacketType.GuiDisplay.writeTo(buffer);
         MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
         boolean isAdmin = server.getPlayerList().isOp(new NameAndId(player.getGameProfile()));
         buffer.writeBoolean(isAdmin);
         if (inventory instanceof BlockEntity te) {
            buffer.writeByte(0);
            DataEncoder.encode(buffer, te, false);
         } else if (player.getMainHandItem() != null && player.getMainHandItem().getItem() instanceof IHandHeldInventory) {
            buffer.writeByte(1);
            buffer.writeInt(player.getInventory().getSelectedSlot());
            this.handleSubData(buffer, player.getMainHandItem(), ID);
         } else if (player.getOffhandItem() != null && player.getOffhandItem().getItem() instanceof IHandHeldInventory) {
            buffer.writeByte(1);
            buffer.writeInt(-1);
            this.handleSubData(buffer, player.getOffhandItem(), ID);
         } else {
            IC2.platform
               .displayError(
                  "An unknown GUI type was attempted to be displayed.\nThis could happen due to corrupted data from a player or a bug.\n\n(Technical information: "
                     + inventory
                     + ")"
               );
         }

         buffer.writeInt(windowId);
         buffer.flip();
         this.sendPacket(buffer, true, player);
      } catch (IOException var9) {
         throw new RuntimeException(var9);
      }
   }

   private final void handleSubData(GrowingBuffer buffer, ItemStack stack, Integer ID) {
      boolean subInv = ID != null && stack.getItem() instanceof IHandHeldSubInventory;
      buffer.writeBoolean(subInv);
      if (subInv) {
         buffer.writeShort(ID);
      }
   }

   public final void sendInitialData(BlockEntity te, ServerPlayer player) {
      assert !this.isClient();

      if (te instanceof INetworkDataProvider) {
         TeUpdateDataServer updateData = getTeUpdateData(te);

         for (String field : ((INetworkDataProvider)te).getNetworkedFields()) {
            updateData.addPlayerField(field, player);
         }
      }
   }

   @Override
   public final void sendInitialData(BlockEntity te) {
      assert !this.isClient();

      if (te instanceof INetworkDataProvider) {
         TeUpdateDataServer updateData = getTeUpdateData(te);
         List<String> fields = ((INetworkDataProvider)te).getNetworkedFields();

         for (String field : fields) {
            updateData.addGlobalField(field);
         }

         if (TeUpdate.debug) {
            IC2.log.info(LogCategory.Network, "Sending initial TE data for %s (%s).", Util.formatPosition(te), fields);
         }
      }
   }

   public final void sendChat(ServerPlayer player, String message) {
      assert !this.isClient();

      GrowingBuffer buffer = new GrowingBuffer(message.length() * 2);
      buffer.writeString(message);
      buffer.flip();
      this.sendLargePacket(player, 1, buffer);
   }

   public final void sendConsole(ServerPlayer player, String message) {
      assert !this.isClient();

      GrowingBuffer buffer = new GrowingBuffer(message.length() * 2);
      buffer.writeString(message);
      buffer.flip();
      this.sendLargePacket(player, 2, buffer);
   }

   public final void sendContainerFields(ContainerBase<?> container, String... fieldNames) {
      for (String fieldName : fieldNames) {
         this.sendContainerField(container, fieldName);
      }
   }

   public final void sendContainerField(ContainerBase<?> container, String fieldName) {
      if (this.isClient() && this.getClientModifiableField(container.getClass(), fieldName) == null) {
         IC2.log.warn(LogCategory.Network, "Field update for %s failed.", container);
      } else {
         GrowingBuffer buffer = new GrowingBuffer(256);

         try {
            SubPacketType.ContainerData.writeTo(buffer);
            buffer.writeInt(container.containerId);
            writeFieldData(container, fieldName, buffer);
         } catch (IOException var6) {
            throw new RuntimeException(var6);
         }

         buffer.flip();
         if (!this.isClient()) {
            for (ServerPlayer listener : container.getListeners()) {
               if (listener instanceof ServerPlayer) {
                  this.sendPacket(buffer, false, listener);
               }
            }
         } else {
            this.sendPacket(buffer);
         }
      }
   }

   public final void sendContainerEvent(ContainerBase<?> container, String event) {
      GrowingBuffer buffer = new GrowingBuffer(64);
      SubPacketType.ContainerEvent.writeTo(buffer);
      buffer.writeInt(container.containerId);
      buffer.writeString(event);
      buffer.flip();
      if (!this.isClient()) {
         for (ServerPlayer listener : container.getListeners()) {
            if (listener instanceof ServerPlayer) {
               this.sendPacket(buffer, false, listener);
            }
         }
      } else {
         this.sendPacket(buffer);
      }
   }

   public final void sendHandHeldInvField(ContainerBase<?> container, String fieldName) {
      if (!(container.base instanceof HandHeldInventory)) {
         IC2.log.warn(LogCategory.Network, "Invalid container (%s) sent for field update.", container);
      } else if (this.isClient() && this.getClientModifiableField(container.base.getClass(), fieldName) == null) {
         IC2.log.warn(LogCategory.Network, "Field update for %s failed.", container);
      } else {
         GrowingBuffer buffer = new GrowingBuffer(256);

         try {
            SubPacketType.HandHeldInvData.writeTo(buffer);
            buffer.writeInt(container.containerId);
            writeFieldData(container.base, fieldName, buffer);
         } catch (IOException var6) {
            throw new RuntimeException(var6);
         }

         buffer.flip();
         if (!this.isClient()) {
            for (ServerPlayer listener : container.getListeners()) {
               if (listener instanceof ServerPlayer) {
                  this.sendPacket(buffer, false, listener);
               }
            }
         } else {
            this.sendPacket(buffer);
         }
      }
   }

   public final void initiateTeblockLandEffect(Level world, double x, double y, double z, int count, ITeBlock teBlock) {
      this.initiateTeblockLandEffect(world, null, x, y, z, count, teBlock);
   }

   public final void initiateTeblockLandEffect(Level world, BlockPos pos, double x, double y, double z, int count, ITeBlock teBlock) {
      assert !this.isClient();

      GrowingBuffer buffer = new GrowingBuffer(64);

      try {
         SubPacketType.TileEntityBlockLandEffect.writeTo(buffer);
         DataEncoder.encode(buffer, world, false);
         if (pos != null) {
            buffer.writeBoolean(true);
            DataEncoder.encode(buffer, pos, false);
         } else {
            buffer.writeBoolean(false);
         }

         buffer.writeDouble(x);
         buffer.writeDouble(y);
         buffer.writeDouble(z);
         buffer.writeInt(count);
         buffer.writeString(teBlock.getName());
      } catch (IOException var16) {
         throw new RuntimeException(var16);
      }

      buffer.flip();

      for (Player player : world.players()) {
         if (player instanceof ServerPlayer) {
            double distance = player.distanceToSqr(x, y, z);
            if (distance <= 1024.0) {
               this.sendPacket(buffer, false, (ServerPlayer)player);
            }
         }
      }
   }

   public final void initiateTeblockRunEffect(Level world, Entity entity, ITeBlock teBlock) {
      this.initiateTeblockRunEffect(world, null, entity, teBlock);
   }

   public final void initiateTeblockRunEffect(Level world, BlockPos pos, Entity entity, ITeBlock teBlock) {
      assert !this.isClient();

      GrowingBuffer buffer = new GrowingBuffer(64);

      try {
         SubPacketType.TileEntityBlockRunEffect.writeTo(buffer);
         DataEncoder.encode(buffer, world, false);
         if (pos != null) {
            buffer.writeBoolean(true);
            DataEncoder.encode(buffer, pos, false);
         } else {
            buffer.writeBoolean(false);
         }

         buffer.writeDouble(entity.getX() + (IC2.random.nextFloat() - 0.5) * entity.getBbWidth());
         buffer.writeDouble(entity.getBoundingBox().minY + 0.1);
         buffer.writeDouble(entity.getZ() + (IC2.random.nextFloat() - 0.5) * entity.getBbWidth());
         buffer.writeDouble(-entity.getDeltaMovement().x * 4.0);
         buffer.writeDouble(-entity.getDeltaMovement().z * 4.0);
         buffer.writeString(teBlock.getName());
      } catch (IOException var10) {
         throw new RuntimeException(var10);
      }

      buffer.flip();

      for (Player player : world.players()) {
         if (player instanceof ServerPlayer) {
            double distance = player.distanceToSqr(entity.getX(), entity.getY(), entity.getZ());
            if (distance <= 1024.0) {
               this.sendPacket(buffer, false, (ServerPlayer)player);
            }
         }
      }
   }

   final void sendLargePacket(ServerPlayer player, int id, GrowingBuffer data) {
      GrowingBuffer buffer = new GrowingBuffer(16384);
      buffer.writeShort(0);

      try {
         DeflaterOutputStream deflate = new DeflaterOutputStream(buffer);
         data.writeTo(deflate);
         deflate.close();
      } catch (IOException var8) {
         throw new RuntimeException(var8);
      }

      buffer.flip();
      boolean firstPacket = true;

      boolean lastPacket;
      do {
         lastPacket = buffer.available() <= 32764;
         if (!firstPacket) {
            buffer.skipBytes(-2);
         }

         SubPacketType.LargePacket.writeTo(buffer);
         int state = 0;
         if (firstPacket) {
            state |= 1;
         }

         if (lastPacket) {
            state |= 2;
         }

         state |= id << 2;
         buffer.write(state);
         buffer.skipBytes(-2);
         if (lastPacket) {
            this.sendPacket(buffer, true, player);

            assert !buffer.hasAvailable();
         } else {
            this.sendPacket(buffer.copy(32764), true, player);
         }

         firstPacket = false;
      } while (!lastPacket);
   }

   public void receive(byte[] bytes, Player player) {
      try {
         this.onPacketData(GrowingBuffer.wrap(bytes), player);
      } catch (IOException var4) {
         throw new IllegalArgumentException("Invalid IC2 packet", var4);
      }
   }

   private void onPacketData(GrowingBuffer is, final Player player) throws IOException {
      if (is.hasAvailable()) {
         SubPacketType packetType = SubPacketType.read(is, true);
         if (packetType != null) {
            switch (packetType) {
               case ItemEvent: {
                  final ItemStack stack = DataEncoder.decode(is, ItemStack.class);
                  final int event = is.readInt();
                  if (stack.getItem() instanceof INetworkItemEventListener) {
                     IC2.platform.requestTick(true, new Runnable() {
                        {
                           Objects.requireNonNull(NetworkManager.this);
                        }

                        @Override
                        public void run() {
                           ((INetworkItemEventListener)stack.getItem()).onNetworkEvent(stack, player, event);
                        }
                     });
                  }
                  break;
               }
               case KeyUpdate:
                  final int keyState = is.readInt();
                  IC2.platform.requestTick(true, new Runnable() {
                     {
                        Objects.requireNonNull(NetworkManager.this);
                     }

                     @Override
                     public void run() {
                        IC2.keyboard.processKeyUpdate(player, keyState);
                     }
                  });
                  break;
               case TileEntityEvent: {
                  final Object teDeferred = DataEncoder.decodeDeferred(is, BlockEntity.class);
                  final int event = is.readInt();
                  IC2.platform.requestTick(true, new Runnable() {
                     {
                        Objects.requireNonNull(NetworkManager.this);
                     }

                     @Override
                     public void run() {
                        BlockEntity te = DataEncoder.getValue(teDeferred);
                        if (te instanceof INetworkClientTileEntityEventListener) {
                           ((INetworkClientTileEntityEventListener)te).onNetworkEvent(player, event);
                        }
                     }
                  });
                  break;
               }
               case RequestGUI: {
                  final boolean hand = is.readBoolean();
                  final Object teDeferred = hand ? null : DataEncoder.decodeDeferred(is, BlockEntity.class);
                  IC2.platform
                     .requestTick(
                        true,
                        new Runnable() {
                           {
                              Objects.requireNonNull(NetworkManager.this);
                           }

                           private IHasGui tryFindGUI(ItemStack stack) {
                              return !StackUtil.isEmpty(stack) && stack.getItem() instanceof IHandHeldInventory
                                 ? ((IHandHeldInventory)stack.getItem()).getInventory(player, stack)
                                 : null;
                           }

                           @Override
                           public void run() {
                              if (hand) {
                                 for (ItemStack stack : List.of(player.getMainHandItem(), player.getOffhandItem())) {
                                    IHasGui gui = this.tryFindGUI(stack);
                                    if (gui != null) {
                                       IC2.platform.launchGui(player, gui);
                                       break;
                                    }
                                 }
                              } else {
                                 BlockEntity te = DataEncoder.getValue(teDeferred);
                                 if (te instanceof IHasGui) {
                                    IC2.platform.launchGui(player, (IHasGui)te);
                                 }
                              }
                           }
                        }
                     );
                  break;
               }
               case Rpc:
                  RpcHandler.processRpcRequest(is, (ServerPlayer)player);
                  break;
               default:
                  this.onCommonPacketData(packetType, true, is, player);
            }
         }
      }
   }

   protected void onCommonPacketData(SubPacketType packetType, boolean simulating, GrowingBuffer is, final Player player) throws IOException {
      switch (packetType) {
         case PlayerItemData:
            final int slot = is.readByte();
            final Item item = DataEncoder.decode(is, Item.class);
            int dataCount = is.readVarInt();
            final Object[] subData = new Object[dataCount];

            for (int i = 0; i < dataCount; i++) {
               subData[i] = DataEncoder.decode(is);
            }

            if (slot >= 0 && slot < 9) {
               IC2.platform.requestTick(simulating, new Runnable() {
                  {
                     Objects.requireNonNull(NetworkManager.this);
                  }

                  @Override
                  public void run() {
                     for (int i = 0; i < subData.length; i++) {
                        subData[i] = DataEncoder.getValue(subData[i]);
                     }

                     ItemStack stack = (ItemStack)player.getInventory().getNonEquipmentItems().get(slot);
                     if (!StackUtil.isEmpty(stack) && stack.getItem() == item && item instanceof IPlayerItemDataListener) {
                        ((IPlayerItemDataListener)item).onPlayerItemNetworkData(player, slot, subData);
                     }
                  }
               });
            }
            break;
         case ContainerData: {
            final int windowId = is.readInt();
            final String fieldName = is.readString();
            final Object value = DataEncoder.decode(is);
            IC2.platform
               .requestTick(
                  simulating,
                  new Runnable() {
                     {
                        Objects.requireNonNull(NetworkManager.this);
                     }

                     @Override
                     public void run() {
                        if (player.containerMenu instanceof ContainerBase
                           && player.containerMenu.containerId == windowId
                           && (
                              NetworkManager.this.isClient()
                                 || NetworkManager.this.getClientModifiableField(player.containerMenu.getClass(), fieldName) != null
                           )) {
                           ReflectionUtil.setValueRecursive(player.containerMenu, fieldName, DataEncoder.getValue(value));
                        }
                     }
                  }
               );
            break;
         }
         case ContainerEvent: {
            final int windowId = is.readInt();
            final String event = is.readString();
            IC2.platform.requestTick(simulating, new Runnable() {
               {
                  Objects.requireNonNull(NetworkManager.this);
               }

               @Override
               public void run() {
                  if (player.containerMenu instanceof ContainerBase && player.containerMenu.containerId == windowId) {
                     ((ContainerBase)player.containerMenu).onContainerEvent(event);
                  }
               }
            });
            break;
         }
         case HandHeldInvData: {
            final int windowId = is.readInt();
            final String fieldName = is.readString();
            final Object value = DataEncoder.decode(is);
            IC2.platform
               .requestTick(
                  simulating,
                  new Runnable() {
                     {
                        Objects.requireNonNull(NetworkManager.this);
                     }

                     @Override
                     public void run() {
                        if (player.containerMenu instanceof ContainerBase && player.containerMenu.containerId == windowId) {
                           ContainerBase<?> container = (ContainerBase<?>)player.containerMenu;
                           if (container.base instanceof HandHeldInventory
                              && (NetworkManager.this.isClient() || NetworkManager.this.getClientModifiableField(container.base.getClass(), fieldName) != null)
                              )
                            {
                              ReflectionUtil.setValueRecursive(container.base, fieldName, DataEncoder.getValue(value));
                           }
                        }
                     }
                  }
               );
            break;
         }
         case TileEntityData: {
            final Object teDeferred = DataEncoder.decodeDeferred(is, BlockEntity.class);
            final String fieldName = is.readString();
            final Object value = DataEncoder.decode(is);
            IC2.platform.requestTick(simulating, new Runnable() {
               {
                  Objects.requireNonNull(NetworkManager.this);
               }

               @Override
               public void run() {
                  BlockEntity te = DataEncoder.getValue(teDeferred);
                  if (te != null && (NetworkManager.this.isClient() || NetworkManager.this.getClientModifiableField(te.getClass(), fieldName) != null)) {
                     ReflectionUtil.setValueRecursive(te, fieldName, DataEncoder.getValue(value));
                  }
               }
            });
            break;
         }
         default:
            IC2.log.warn(LogCategory.Network, "Unhandled packet type: %s", packetType.name());
      }
   }

   public void initiateKeyUpdate(int keyState) {
   }

   public void sendLoginData() {
   }

   public final void initiateExplosionEffect(Level world, Vec3 pos, ExplosionIC2.Type type) {
      assert !this.isClient();

      try {
         GrowingBuffer buffer = new GrowingBuffer(32);
         SubPacketType.ExplosionEffect.writeTo(buffer);
         DataEncoder.encode(buffer, world, false);
         DataEncoder.encode(buffer, pos, false);
         DataEncoder.encode(buffer, type, false);
         buffer.flip();

         for (Object obj : world.players()) {
            if (obj instanceof ServerPlayer player && player.distanceToSqr(pos.x, pos.y, pos.z) < 128.0) {
               this.sendPacket(buffer, false, player);
            }
         }
      } catch (IOException var8) {
         throw new RuntimeException(var8);
      }
   }

   protected final void sendPacket(GrowingBuffer buffer) {
      if (!this.isClient()) {
         PacketDistributor.sendToAllPlayers(Ic2Payload.of(buffer, true), new CustomPacketPayload[0]);
      } else {
         this.sendToServer(Ic2Payload.of(buffer, true));
      }
   }

   protected final void sendPacket(GrowingBuffer buffer, boolean advancePos, ServerPlayer player) {
      assert !this.isClient();

      PacketDistributor.sendToPlayer(player, Ic2Payload.of(buffer, advancePos), new CustomPacketPayload[0]);
   }

   static <T extends Collection<ServerPlayer>> T getPlayersInRange(Level world, BlockPos pos, T result) {
      if (!(world instanceof ServerLevel)) {
         return result;
      } else {
         result.addAll(((ServerLevel)world).getChunkSource().chunkMap.getPlayers(new ChunkPos(pos.getX() >> 4, pos.getZ() >> 4), false));
         return result;
      }
   }

   static void writeFieldData(Object object, String fieldName, GrowingBuffer out) throws IOException {
      int pos = fieldName.indexOf(61);
      if (pos != -1) {
         out.writeString(fieldName.substring(0, pos));
         DataEncoder.encode(out, fieldName.substring(pos + 1));
      } else {
         out.writeString(fieldName);

         try {
            DataEncoder.encode(out, ReflectionUtil.getValueRecursive(object, fieldName));
         } catch (NoSuchFieldException var5) {
            throw new RuntimeException("Can't find field " + fieldName + " in " + object.getClass().getName(), var5);
         }
      }
   }

   protected void sendToServer(Ic2Payload payload) {
      throw new IllegalStateException("Client sender used on server");
   }
}
