package ic2.core.util;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.TerrainParticle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.level.block.state.BlockState;

public final class Ic2TerrainParticle extends TerrainParticle {
   public Ic2TerrainParticle(ClientLevel level, double x, double y, double z, double dx, double dy, double dz, BlockState state) {
      super(level, x, y, z, dx, dy, dz, state);
   }

   public void sprite(TextureAtlasSprite sprite) {
      this.setSprite(sprite);
   }
}
