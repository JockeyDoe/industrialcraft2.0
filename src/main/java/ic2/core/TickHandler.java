package ic2.core;

import ic2.core.init.MainConfig;
import ic2.core.item.tool.ItemNanoSaber;
import ic2.core.util.ConfigUtil;
import ic2.core.util.LogCategory;
import ic2.core.util.Util;
import ic2.core.util.WorldQueries;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.EnderChestBlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent.Post;
import net.neoforged.neoforge.event.tick.LevelTickEvent.Pre;

public class TickHandler {
   private static final boolean debugupdate = System.getProperty("ic2.debugupdate") != null;
   private static final Map<IWorldTickCallback, Throwable> debugTraces = debugupdate ? new WeakHashMap<>() : null;
   private static Throwable lastDebugTrace;

   public TickHandler() {
      NeoForge.EVENT_BUS.register(this);
   }

   @SubscribeEvent
   public void onWorldTickStart(Pre event) {
      this.onWorldTick(event);
   }

   @SubscribeEvent
   public void onWorldTickEnd(Post event) {
      this.onWorldTick(event);
   }

   public void onWorldTick(LevelTickEvent event) {
      Level world = event.getLevel();
      WorldData worldData = WorldData.get(world, false);
      if (worldData != null) {
         if (event instanceof Pre) {
            IC2.platform.profilerStartSection("updates");
            processUpdates(world, worldData);
            if (!world.isClientSide()) {
               IC2.platform.profilerEndStartSection("retrogen");
               Ic2WorldDecorator.onTick(world, worldData);
               IC2.platform.profilerEndStartSection("Wind");
               worldData.windSim.updateWind();
               if (ConfigUtil.getBool(MainConfig.get(), "balance/disableEnderChest")) {
                  IC2.platform.profilerEndStartSection("EnderChestCheck");

                  for (BlockEntity te : WorldQueries.loadedBlockEntities((ServerLevel)world)) {
                     if (te instanceof EnderChestBlockEntity && !te.isRemoved() && !world.isEmptyBlock(te.getBlockPos())) {
                        world.removeBlock(te.getBlockPos(), false);
                        IC2.log.info(LogCategory.General, "Removed vanilla ender chest at %s.", Util.formatPosition(te));
                     }
                  }
               }
            }

            IC2.platform.profilerEndSection();
         } else {
            IC2.platform.profilerStartSection("Networking");
            IC2.network.get(!world.isClientSide()).onTickEnd(worldData);
            IC2.platform.profilerEndSection();
         }
      }
   }

   @SubscribeEvent
   public void onServerTick(net.neoforged.neoforge.event.tick.ServerTickEvent.Pre event) {
      ItemNanoSaber.ticker++;
   }

   @SubscribeEvent
   public void onClientTick(net.neoforged.neoforge.client.event.ClientTickEvent.Pre event) {
      IC2.platform.profilerStartSection("Keyboard");
      IC2.keyboard.sendKeyUpdate();
      IC2.platform.profilerEndStartSection("AudioManager");
      IC2.audioManager.onTick();
      IC2.platform.profilerEndStartSection("updates");
      Level world = IC2.platform.getPlayerWorld();
      if (world != null) {
         processUpdates(world, WorldData.get(world));
      }

      IC2.platform.profilerEndSection();
   }

   public void requestSingleWorldTick(Level world, IWorldTickCallback callback) {
      WorldData.get(world).singleUpdates.add(callback);
      if (debugupdate) {
         debugTraces.put(callback, new Throwable());
      }
   }

   public void requestContinuousWorldTick(Level world, IWorldTickCallback update) {
      WorldData worldData = WorldData.get(world);
      if (!worldData.continuousUpdatesInUse) {
         worldData.continuousUpdates.add(update);
      } else {
         worldData.continuousUpdatesToRemove.remove(update);
         worldData.continuousUpdatesToAdd.add(update);
      }

      if (debugupdate) {
         debugTraces.put(update, new Throwable());
      }
   }

   public void removeContinuousWorldTick(Level world, IWorldTickCallback update) {
      WorldData worldData = WorldData.get(world);
      if (!worldData.continuousUpdatesInUse) {
         worldData.continuousUpdates.remove(update);
      } else {
         worldData.continuousUpdatesToAdd.remove(update);
         worldData.continuousUpdatesToRemove.add(update);
      }
   }

   public static Throwable getLastDebugTrace() {
      return lastDebugTrace;
   }

   private static void processUpdates(Level world, WorldData worldData) {
      IC2.platform.profilerStartSection("single-update");

      IWorldTickCallback callback;
      while ((callback = worldData.singleUpdates.poll()) != null) {
         if (debugupdate) {
            lastDebugTrace = debugTraces.remove(callback);
         }

         callback.onTick(world);
      }

      IC2.platform.profilerEndStartSection("cont-update");
      worldData.continuousUpdatesInUse = true;

      for (IWorldTickCallback update : worldData.continuousUpdates) {
         if (debugupdate) {
            lastDebugTrace = debugTraces.remove(update);
         }

         update.onTick(world);
      }

      worldData.continuousUpdatesInUse = false;
      if (debugupdate) {
         lastDebugTrace = null;
      }

      worldData.continuousUpdates.addAll(worldData.continuousUpdatesToAdd);
      worldData.continuousUpdatesToAdd.clear();
      worldData.continuousUpdates.removeAll(worldData.continuousUpdatesToRemove);
      worldData.continuousUpdatesToRemove.clear();
      IC2.platform.profilerEndSection();
   }
}
