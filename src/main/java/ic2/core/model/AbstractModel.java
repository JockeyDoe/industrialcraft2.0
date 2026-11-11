package ic2.core.model;

import com.mojang.renderpearl.api.vertex.VertexFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.cuboid.ItemTransforms;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;

public abstract class AbstractModel implements IReloadableModel, Ic2Geometry {
   @Override
   public Collection<Identifier> getDependencies() {
      return Collections.emptyList();
   }

   @Override
   public Collection<Identifier> getTextures() {
      return Collections.emptyList();
   }

   @Override
   public Ic2Geometry bake(Matrix4f state, VertexFormat format, Function<Identifier, TextureAtlasSprite> bakedTextureGetter) {
      return this;
   }

   @Override
   public Matrix4f getDefaultState() {
      return new Matrix4f();
   }

   @Override
   public List<BakedQuad> func_188616_a(BlockState state, Direction side, long rand) {
      throw new UnsupportedOperationException();
   }

   @Override
   public boolean func_177555_b() {
      return true;
   }

   @Override
   public boolean func_177556_c() {
      return false;
   }

   @Override
   public boolean func_188618_c() {
      return false;
   }

   @Override
   public TextureAtlasSprite func_177554_e() {
      return ModelRegistry.missingSprite();
   }

   @Override
   public ItemTransforms func_177552_f() {
      return ItemTransforms.NO_TRANSFORMS;
   }

   @Override
   public Ic2ItemOverrides func_188617_f() {
      return Ic2ItemOverrides.EMPTY;
   }
}
