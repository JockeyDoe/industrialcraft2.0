package ic2.core.energy.grid;

import ic2.api.energy.EnergyNet;
import ic2.api.energy.event.EnergyTileLoadEvent;
import ic2.api.energy.event.EnergyTileUnloadEvent;
import ic2.api.energy.tile.IEnergyTile;
import ic2.api.info.ILocatable;
import ic2.core.IC2;
import ic2.core.util.LogCategory;
import ic2.core.util.Util;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.LevelTickEvent.Post;
import net.neoforged.neoforge.event.tick.LevelTickEvent.Pre;

public class EventHandler {
   private static boolean initialized;

   public static void init() {
      if (initialized) {
         throw new IllegalStateException("already initialized");
      } else {
         initialized = true;
         NeoForge.EVENT_BUS.register(new EventHandler());
      }
   }

   private EventHandler() {
   }

   @SubscribeEvent
   public void onEnergyTileLoad(EnergyTileLoadEvent event) {
      if (event.getLevel().isClientSide()) {
         IC2.log
            .warn(
               LogCategory.EnergyNet,
               "EnergyTileLoadEvent: posted for %s client-side, aborting",
               Util.toString(event.tile, event.getLevel(), EnergyNet.instance.getPos(event.tile))
            );
      } else if (event.tile instanceof BlockEntity) {
         EnergyNet.instance.addTile((BlockEntity & IEnergyTile)event.tile);
      } else {
         if (!(event.tile instanceof ILocatable)) {
            throw new IllegalArgumentException("invalid tile type: " + event.tile);
         }

         EnergyNet.instance.addTile((ILocatable & IEnergyTile)event.tile);
      }
   }

   @SubscribeEvent
   public void onEnergyTileUnload(EnergyTileUnloadEvent event) {
      if (event.getLevel().isClientSide()) {
         IC2.log
            .warn(
               LogCategory.EnergyNet,
               "EnergyTileUnloadEvent: posted for %s client-side, aborting",
               Util.toString(event.tile, event.getLevel(), EnergyNet.instance.getPos(event.tile))
            );
      } else {
         EnergyNet.instance.removeTile(event.tile);
      }
   }

   @SubscribeEvent
   public void onWorldTickStart(Pre event) {
      if (!event.getLevel().isClientSide()) {
         EnergyNetGlobal.getLocal(event.getLevel()).onTickStart();
      }
   }

   @SubscribeEvent
   public void onWorldTickEnd(Post event) {
      if (!event.getLevel().isClientSide()) {
         EnergyNetGlobal.getLocal(event.getLevel()).onTickEnd();
      }
   }
}
