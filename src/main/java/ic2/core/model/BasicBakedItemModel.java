package ic2.core.model;

import java.util.Collections;
import java.util.List;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.cuboid.ItemTransforms;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

public class BasicBakedItemModel extends AbstractBakedModel {
   private final List<BakedQuad> quads;
   private final TextureAtlasSprite particleTexture;

   public BasicBakedItemModel(List<BakedQuad> quads, TextureAtlasSprite particleTexture) {
      this.quads = quads;
      this.particleTexture = particleTexture;
   }

   @Override
   public List<BakedQuad> func_188616_a(BlockState state, Direction side, long rand) {
      return side != null ? Collections.emptyList() : this.quads;
   }

   @Override
   public TextureAtlasSprite func_177554_e() {
      return this.particleTexture;
   }

   @Override
   public ItemTransforms func_177552_f() {
      return ItemTransforms.NO_TRANSFORMS;
   }

   public static BakedQuad createQuad(int[] vertexData, Direction side) {
      return QuadData.create(vertexData, side, ModelRegistry.missingSprite());
   }
}
