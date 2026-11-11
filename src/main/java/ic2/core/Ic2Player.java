package ic2.core;

import com.google.common.base.Charsets;
import com.mojang.authlib.GameProfile;
import ic2.core.util.LevelIds;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

public class Ic2Player {
   public static Player get(Level world) {
      return world instanceof ServerLevel ? FakePlayerFactory.get((ServerLevel)world, getGameProfile(LevelIds.id(world))) : null;
   }

   private static GameProfile getGameProfile(int dim) {
      String name = "[IC2 " + dim + "]";
      UUID uuid = UUID.nameUUIDFromBytes(name.getBytes(Charsets.UTF_8));
      return new GameProfile(uuid, name);
   }
}
