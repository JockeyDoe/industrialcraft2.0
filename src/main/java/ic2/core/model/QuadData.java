package ic2.core.model;

import com.mojang.blaze3d.platform.Transparency;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.BakedQuad.MaterialInfo;
import net.minecraft.client.resources.model.sprite.Material.Baked;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.client.model.quad.BakedColors;
import net.neoforged.neoforge.client.model.quad.BakedNormals;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public final class QuadData {
   private QuadData() {
   }

   public static int[] read(BakedQuad quad) {
      int[] data = new int[28];

      for (int i = 0; i < 4; i++) {
         int o = i * 7;
         Vector3fc p = quad.position(i);
         data[o] = Float.floatToRawIntBits(p.x());
         data[o + 1] = Float.floatToRawIntBits(p.y());
         data[o + 2] = Float.floatToRawIntBits(p.z());
         int color = quad.bakedColors().color(i);
         data[o + 3] = color & -16711936 | (color & 0xFF) << 16 | color >>> 16 & 0xFF;
         data[o + 4] = Float.floatToRawIntBits(UVPair.unpackU(quad.packedUV(i)));
         data[o + 5] = Float.floatToRawIntBits(UVPair.unpackV(quad.packedUV(i)));
         data[o + 6] = quad.bakedNormals().normal(i);
      }

      return data;
   }

   public static BakedQuad create(int[] data, Direction face, TextureAtlasSprite sprite) {
      MaterialInfo material = MaterialInfo.of(new Baked(sprite, false), Transparency.TRANSPARENT, -1, null, 0, true);
      return create(data, face, material);
   }

   public static BakedQuad replace(BakedQuad quad, int[] data, TextureAtlasSprite sprite) {
      MaterialInfo info = quad.materialInfo();
      MaterialInfo material = sprite == null
         ? info
         : new MaterialInfo(
            sprite,
            info.layer(),
            info.itemRenderType(),
            info.itemGlintRenderType(),
            info.itemGlintSpecialRenderType(),
            info.tintIndex(),
            info.shadeDirectionOverride(),
            info.lightEmission(),
            info.ambientOcclusion()
         );
      return create(data, quad.direction(), material);
   }

   private static BakedQuad create(int[] data, Direction face, MaterialInfo material) {
      if (data.length != 28) {
         throw new IllegalArgumentException("Expected four IC2 vertices");
      } else {
         Vector3f[] p = new Vector3f[4];
         long[] uv = new long[4];
         int[] c = new int[4];
         int[] n = new int[4];

         for (int i = 0; i < 4; i++) {
            int o = i * 7;
            p[i] = new Vector3f(Float.intBitsToFloat(data[o]), Float.intBitsToFloat(data[o + 1]), Float.intBitsToFloat(data[o + 2]));
            uv[i] = UVPair.pack(Float.intBitsToFloat(data[o + 4]), Float.intBitsToFloat(data[o + 5]));
            int color = data[o + 3];
            c[i] = color & -16711936 | (color & 0xFF) << 16 | color >>> 16 & 0xFF;
            n[i] = data[o + 6];
         }

         return new BakedQuad(
            p[0], p[1], p[2], p[3], uv[0], uv[1], uv[2], uv[3], face, material, BakedNormals.of(n[0], n[1], n[2], n[3]), BakedColors.of(c[0], c[1], c[2], c[3])
         );
      }
   }
}
