package ic2.core.model;

import java.util.Collections;
import java.util.List;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

public class BasicBakedBlockModel extends AbstractBakedModel {
   private final List<BakedQuad>[] faceQuads;
   private final List<BakedQuad> generalQuads;
   private final TextureAtlasSprite particleTexture;

   public BasicBakedBlockModel(List<BakedQuad>[] faceQuads, List<BakedQuad> generalQuads, TextureAtlasSprite particleTexture) {
      this.faceQuads = faceQuads;
      this.generalQuads = generalQuads;
      this.particleTexture = particleTexture;
   }

   @Override
   public List<BakedQuad> func_188616_a(BlockState state, Direction side, long rand) {
      if (side == null) {
         return this.generalQuads;
      } else {
         return this.faceQuads == null ? Collections.emptyList() : this.faceQuads[side.ordinal()];
      }
   }

   @Override
   public TextureAtlasSprite func_177554_e() {
      return this.particleTexture;
   }

   public static BakedQuad createQuad(int[] vertexData, Direction side, TextureAtlasSprite sprite) {
      return QuadData.create(vertexData, side, sprite);
   }
}
