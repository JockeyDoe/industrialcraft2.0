package ic2.core.util;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

public final class LevelIds {
   private static final Map<Integer, Identifier> keys = new HashMap<>();

   private LevelIds() {
   }

   public static synchronized int id(Level world) {
      Identifier key = world.dimension().identifier();
      int id = world.dimension() == Level.OVERWORLD
         ? 0
         : (world.dimension() == Level.NETHER ? -1 : (world.dimension() == Level.END ? 1 : key.toString().hashCode()));
      Identifier prior = keys.putIfAbsent(id, key);
      if (prior != null && !prior.equals(key)) {
         throw new IllegalStateException("IC2 dimension identity collision: " + prior + " and " + key);
      } else {
         return id;
      }
   }

   public static Level get(int id) {
      MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
      if (server != null) {
         for (ServerLevel world : server.getAllLevels()) {
            if (id(world) == id) {
               return world;
            }
         }
      }

      return null;
   }
}
