package ic2.core;

import ic2.core.block.machine.tileentity.TileEntityChunkloader;
import it.unimi.dsi.fastutil.longs.LongIterator;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.world.chunk.RegisterTicketControllersEvent;
import net.neoforged.neoforge.common.world.chunk.TicketController;
import net.neoforged.neoforge.common.world.chunk.TicketHelper;
import net.neoforged.neoforge.common.world.chunk.TicketSet;
import net.neoforged.neoforge.event.level.LevelEvent.Unload;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

@EventBusSubscriber(modid = "ic2")
public final class ChunkLoaderLogic {
   private static ChunkLoaderLogic instance;
   private static final Map<ServerLevel, Map<BlockPos, ChunkLoaderLogic.Ticket>> tickets = new IdentityHashMap<>();
   private static final TicketController controller = new TicketController(IC2.getIdentifier("chunkloaders"), ChunkLoaderLogic::restore);

   ChunkLoaderLogic() {
      if (instance != null) {
         throw new IllegalStateException();
      } else {
         instance = this;
      }
   }

   @SubscribeEvent
   public static void register(RegisterTicketControllersEvent event) {
      event.register(controller);
   }

   private static void restore(ServerLevel world, TicketHelper helper) {
      Map<BlockPos, ChunkLoaderLogic.Ticket> owners = tickets.computeIfAbsent(world, k -> new HashMap<>());

      for (Entry<BlockPos, TicketSet> entry : helper.getBlockTickets().entrySet()) {
         BlockPos owner = entry.getKey();
         if (!(world.getBlockEntity(owner) instanceof TileEntityChunkloader)) {
            helper.removeAllTickets(owner);
         } else {
            ChunkLoaderLogic.Ticket ticket = new ChunkLoaderLogic.Ticket(world, owner);
            LongIterator var7 = entry.getValue().normal().iterator();

            while (var7.hasNext()) {
               long packed = (Long)var7.next();
               ticket.chunks.add(ChunkPos.unpack(packed));
            }

            var7 = entry.getValue().naturalSpawning().iterator();

            while (var7.hasNext()) {
               long packed = (Long)var7.next();
               helper.removeTicket(owner, packed, true);
            }

            owners.put(owner, ticket);
         }
      }
   }

   @SubscribeEvent
   public static void unloadWorld(Unload event) {
      if (event.getLevel() instanceof ServerLevel level) {
         tickets.remove(level);
      }
   }

   @SubscribeEvent
   public static void serverStopped(ServerStoppedEvent event) {
      tickets.clear();
   }

   public ChunkLoaderLogic.Ticket getTicket(Level world, BlockPos pos, boolean create) {
      if (!(world instanceof ServerLevel server)) {
         return null;
      } else {
         ChunkLoaderLogic.Ticket ticket = tickets.computeIfAbsent(server, k -> new HashMap<>()).get(pos);
         return ticket == null && create ? this.createTicket(server, pos) : ticket;
      }
   }

   public ChunkLoaderLogic.Ticket createTicket(Level world, BlockPos pos) {
      if (world instanceof ServerLevel server) {
         if (this.getTicket(server, pos, false) != null) {
            throw new IllegalStateException("Duplicate chunkloader ticket");
         } else {
            ChunkLoaderLogic.Ticket ticket = new ChunkLoaderLogic.Ticket(server, pos);
            tickets.get(server).put(ticket.owner, ticket);
            this.addChunkToTicket(ticket, getChunkCoords(pos));
            return ticket;
         }
      } else {
         return null;
      }
   }

   public void addChunkToTicket(ChunkLoaderLogic.Ticket ticket, ChunkPos chunk) {
      if (!ticket.chunks.contains(chunk) && controller.forceChunk(ticket.world, ticket.owner, chunk.x(), chunk.z(), true, false)) {
         ticket.chunks.add(chunk);
      }
   }

   public void removeChunkFromTicket(ChunkLoaderLogic.Ticket ticket, ChunkPos chunk) {
      if (!chunk.equals(getChunkCoords(ticket.owner))
         && ticket.chunks.contains(chunk)
         && controller.forceChunk(ticket.world, ticket.owner, chunk.x(), chunk.z(), false, false)) {
         ticket.chunks.remove(chunk);
      }
   }

   public void removeTicket(Level world, BlockPos pos) {
      ChunkLoaderLogic.Ticket ticket = this.getTicket(world, pos, false);
      if (ticket != null) {
         this.removeTicket(ticket);
      }
   }

   public void removeTicket(ChunkLoaderLogic.Ticket ticket) {
      if (ticket != null) {
         for (ChunkPos chunk : List.copyOf(ticket.chunks)) {
            controller.forceChunk(ticket.world, ticket.owner, chunk.x(), chunk.z(), false, false);
         }

         ticket.chunks.clear();
         Map<BlockPos, ChunkLoaderLogic.Ticket> owners = tickets.get(ticket.world);
         if (owners != null) {
            owners.remove(ticket.owner);
            if (owners.isEmpty()) {
               tickets.remove(ticket.world);
            }
         }
      }
   }

   public int getMaxChunksPerTicket() {
      return 25;
   }

   public static ChunkLoaderLogic getInstance() {
      return instance;
   }

   public static long serialize(ChunkPos chunk) {
      return chunk.x() & 4294967295L | (chunk.z() & 4294967295L) << 32;
   }

   public static ChunkPos deserialize(long value) {
      return ChunkPos.unpack(value);
   }

   public static ChunkPos getChunkCoords(BlockPos pos) {
      return ChunkPos.containing(pos);
   }

   public static final class Ticket {
      final ServerLevel world;
      final BlockPos owner;
      final Set<ChunkPos> chunks = new LinkedHashSet<>();

      Ticket(ServerLevel world, BlockPos owner) {
         this.world = world;
         this.owner = owner.immutable();
      }

      public Set<ChunkPos> getChunkList() {
         return Collections.unmodifiableSet(this.chunks);
      }
   }
}
