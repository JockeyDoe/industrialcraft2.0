package ic2.core.model;

import com.mojang.renderpearl.api.vertex.VertexFormat;
import java.util.Collection;
import java.util.function.Function;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;

public interface ModelSource {
   Collection<Identifier> getDependencies();

   Collection<Identifier> getTextures();

   Ic2Geometry bake(Matrix4f var1, VertexFormat var2, Function<Identifier, TextureAtlasSprite> var3);

   Matrix4f getDefaultState();
}
