package ic2.core.model;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.cuboid.ItemTransforms;

public abstract class AbstractBakedModel implements Ic2Geometry {
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
      return null;
   }

   @Override
   public ItemTransforms func_177552_f() {
      return null;
   }

   @Override
   public Ic2ItemOverrides func_188617_f() {
      return null;
   }
}
