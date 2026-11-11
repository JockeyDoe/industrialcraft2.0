package ic2.core;

import ic2.core.block.personal.TradingMarket;
import ic2.core.energy.grid.EnergyNetLocal;
import ic2.core.network.TeUpdateDataServer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;

public class WorldData {
   private static ConcurrentMap<Level, WorldData> idxClient = new ConcurrentHashMap<>();
   private static ConcurrentMap<Level, WorldData> idxServer = new ConcurrentHashMap<>();
   final Queue<IWorldTickCallback> singleUpdates = new ConcurrentLinkedQueue<>();
   final Set<IWorldTickCallback> continuousUpdates = new HashSet<>();
   boolean continuousUpdatesInUse = false;
   final List<IWorldTickCallback> continuousUpdatesToAdd = new ArrayList<>();
   final List<IWorldTickCallback> continuousUpdatesToRemove = new ArrayList<>();
   public final EnergyNetLocal energyNet;
   public final Map<BlockEntity, TeUpdateDataServer> tesToUpdate = new IdentityHashMap<>();
   public final TradingMarket tradeMarket;
   public final WindSim windSim;
   public final Map<LevelChunk, CompoundTag> worldGenData = new IdentityHashMap<>();
   public final Set<LevelChunk> chunksToDecorate = Collections.newSetFromMap(new IdentityHashMap<>());
   public final Set<LevelChunk> initialChunksToDecorate = Collections.newSetFromMap(new IdentityHashMap<>());
   public final Set<LevelChunk> pendingUnloadChunks = Collections.newSetFromMap(new IdentityHashMap<>());

   private WorldData(Level world) {
      if (!world.isClientSide()) {
         this.energyNet = EnergyNetLocal.create(world);
         this.tradeMarket = new TradingMarket(world);
         this.windSim = new WindSim(world);
      } else {
         this.energyNet = null;
         this.tradeMarket = null;
         this.windSim = null;
      }
   }

   public static WorldData get(Level world) {
      return get(world, true);
   }

   public static WorldData get(Level world, boolean load) {
      if (world == null) {
         throw new IllegalArgumentException("world is null");
      } else {
         ConcurrentMap<Level, WorldData> index = getIndex(!world.isClientSide());
         WorldData ret = index.get(world);
         if (ret == null && load) {
            ret = new WorldData(world);
            WorldData prev = index.putIfAbsent(world, ret);
            if (prev != null) {
               ret = prev;
            }

            return ret;
         } else {
            return ret;
         }
      }
   }

   public static void onWorldUnload(Level world) {
      getIndex(!world.isClientSide()).remove(world);
   }

   private static ConcurrentMap<Level, WorldData> getIndex(boolean simulating) {
      return simulating ? idxServer : idxClient;
   }
}
