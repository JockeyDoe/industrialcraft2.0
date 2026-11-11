package ic2.core.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.cuboid.ItemTransforms;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.commons.lang3.tuple.Pair;
import org.joml.Matrix4f;

public class MergedBlockModel implements Ic2Geometry {
   private static final byte[][] uvMap = new byte[][]{{1, 0, 0, 0, 0, 1}, {1, 0, 0, 0, 1, 0}, {0, 0, 1, 0, 1, 0}};
   private static final int quadVertexCount = 4;
   private static final int xDataIndex = 0;
   private static final int yDataIndex = 1;
   private static final int zDataIndex = 2;
   private static final int colorDataIndex = 3;
   private static final int uDataIndex = 4;
   private static final int vDataIndex = 5;
   private final Ic2Geometry parent;
   private final List<BakedQuad>[] mergedFaceQuads;
   private final int[] retextureStart;
   private static final int textureStride = 1;
   private final float[][] currentUvs = new float[6][];
   private final int[][] currentColorMultipliers = new int[6][];

   public MergedBlockModel(Ic2Geometry parent, List<BakedQuad>[] mergedFaceQuads, int[] retextureStart) {
      this.parent = parent;
      this.mergedFaceQuads = mergedFaceQuads;
      this.retextureStart = retextureStart;
   }

   public MergedBlockModel copy() {
      List<BakedQuad>[] newMergedQuads = new List[this.mergedFaceQuads.length];

      for (int side = 0; side < this.mergedFaceQuads.length; side++) {
         List<BakedQuad> mergedFaceQuads = this.mergedFaceQuads[side];
         List<BakedQuad> newMergedFaceQuads = new ArrayList<>(mergedFaceQuads);
         newMergedQuads[side] = newMergedFaceQuads;

         for (int i = this.retextureStart[side]; i < mergedFaceQuads.size(); i++) {
            BakedQuad oldQuad = mergedFaceQuads.get(i);
            newMergedFaceQuads.set(i, oldQuad);
         }
      }

      return new MergedBlockModel(this.parent, newMergedQuads, this.retextureStart);
   }

   public void setSprite(float[][] uvs, int[][] colorMultipliers, TextureAtlasSprite[][] sprites) {
      for (int i = 0; i < 6; i++) {
         boolean matchingUvs = Arrays.equals(uvs[i], this.currentUvs[i]);
         boolean matchingColorMul = Arrays.equals(colorMultipliers[i], this.currentColorMultipliers[i]);
         if (!matchingUvs || !matchingColorMul) {
            if (!matchingUvs) {
               this.currentUvs[i] = uvs[i];
            }

            if (!matchingColorMul) {
               this.currentColorMultipliers[i] = colorMultipliers[i];
            }

            if (this.currentColorMultipliers[i] != null) {
               this.setSpriteUnchecked(
                  this.mergedFaceQuads[i], this.retextureStart[i], this.currentUvs[i], uvMap[i / 2], this.currentColorMultipliers[i], sprites[i]
               );
            }
         }
      }
   }

   private void setSpriteUnchecked(List<BakedQuad> quads, int retextureStart, float[] uvs, byte[] uvMap, int[] colorMultipliers, TextureAtlasSprite[] sprites) {
      if (quads.size() - retextureStart > colorMultipliers.length) {
         throw new IllegalStateException(
            String.format("mismatched size/stride/multipliers: retex-quads=%d, stride=%d, muls=%d", quads.size() - retextureStart, 1, colorMultipliers.length)
         );
      } else if (uvs.length != colorMultipliers.length * 4) {
         throw new IllegalStateException(String.format("mismatched uvs/multipliers: uvs=%d, muls=%d", uvs.length, colorMultipliers.length));
      } else {
         for (int texture = 0; texture < colorMultipliers.length; texture++) {
            float uS = uvs[texture * 4];
            float vS = uvs[texture * 4 + 1];
            float uE = uvs[texture * 4 + 2];
            float vE = uvs[texture * 4 + 3];
            float du = uE - uS;
            float dv = vE - vS;
            int colorMultiplier = mapColor(colorMultipliers[texture]);
            TextureAtlasSprite sprite = sprites[texture];

            for (int i = 0; i < 1; i++) {
               BakedQuad quad = quads.get(retextureStart + texture + i);
               int[] vertexData = QuadData.read(quad);

               for (int j = 0; j < 4; j++) {
                  int offset = j * 7;
                  vertexData[offset + 3] = colorMultiplier;
                  float x = Float.intBitsToFloat(vertexData[offset + 0]);
                  float y = Float.intBitsToFloat(vertexData[offset + 1]);
                  float z = Float.intBitsToFloat(vertexData[offset + 2]);
                  vertexData[offset + 4] = Float.floatToRawIntBits(uS + du * (x * uvMap[0] + y * uvMap[1] + z * uvMap[2]));
                  vertexData[offset + 5] = Float.floatToRawIntBits(vS + dv * (x * uvMap[3] + y * uvMap[4] + z * uvMap[5]));
               }

               quads.set(retextureStart + texture + i, QuadData.replace(quad, vertexData, sprite));
            }
         }
      }
   }

   private static int mapColor(int color) {
      int a = color >>> 24;
      return a > 0
         ? color & -16711936 | (color & 0xFF) << 16 | (color & 0xFF0000) >> 16
         : 0xFF000000 | color & 0xFF00 | (color & 0xFF) << 16 | (color & 0xFF0000) >> 16;
   }

   @Override
   public List<BakedQuad> func_188616_a(BlockState state, Direction side, long rand) {
      return side == null ? this.parent.func_188616_a(state, side, rand) : this.mergedFaceQuads[side.ordinal()];
   }

   @Override
   public boolean func_177555_b() {
      return this.parent.func_177555_b();
   }

   @Override
   public boolean func_177556_c() {
      return this.parent.func_177556_c();
   }

   @Override
   public boolean func_188618_c() {
      return this.parent.func_188618_c();
   }

   @Override
   public TextureAtlasSprite func_177554_e() {
      return this.parent.func_177554_e();
   }

   @Deprecated
   @Override
   public ItemTransforms func_177552_f() {
      return this.parent.func_177552_f();
   }

   @Override
   public Pair<? extends Ic2Geometry, Matrix4f> handlePerspective(ItemDisplayContext cameraTransformType) {
      return Pair.of(this, (Matrix4f)this.parent.handlePerspective(cameraTransformType).getRight());
   }

   @Override
   public Ic2ItemOverrides func_188617_f() {
      return this.parent.func_188617_f();
   }
}
