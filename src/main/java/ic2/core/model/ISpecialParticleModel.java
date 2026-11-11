package ic2.core.model;

import ic2.core.block.state.Ic2BlockState;
import ic2.core.util.Ic2TerrainParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.level.block.state.BlockState;

public interface ISpecialParticleModel extends Ic2Geometry {
   default boolean needsEnhancing(BlockState state) {
      return ModelUtil.getMissingModel().func_177554_e().contents().name().equals(this.func_177554_e().contents().name());
   }

   default void enhanceParticle(Particle particle, Ic2BlockState.Ic2BlockStateInstance state) {
      if (particle instanceof Ic2TerrainParticle terrain) {
         terrain.sprite(this.getParticleTexture(state));
      }
   }

   default TextureAtlasSprite getParticleTexture(Ic2BlockState.Ic2BlockStateInstance state) {
      return this.func_177554_e();
   }
}
