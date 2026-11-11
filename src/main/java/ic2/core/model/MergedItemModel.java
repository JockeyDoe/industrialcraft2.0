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

public class MergedItemModel implements Ic2Geometry {
   private static final int xDataIndex = 0;
   private static final int yDataIndex = 1;
   private static final int colorDataIndex = 3;
   private static final int uDataIndex = 4;
   private static final int vDataIndex = 5;
   private final Ic2Geometry parent;
   private final List<BakedQuad> mergedQuads;
   private final int retextureStart;
   private final int textureStride;
   private float[] currentUvs;
   private int[] currentColorMultipliers;
   private TextureAtlasSprite currentSprite;

   public MergedItemModel(Ic2Geometry parent, List<BakedQuad> mergedQuads, int retextureStart, int textureStride) {
      this.parent = parent;
      this.mergedQuads = mergedQuads;
      this.retextureStart = retextureStart;
      this.textureStride = textureStride;
   }

   public MergedItemModel copy() {
      List<BakedQuad> newMergedQuads = new ArrayList<>(this.mergedQuads);

      for (int i = this.retextureStart; i < this.mergedQuads.size(); i++) {
         BakedQuad oldQuad = this.mergedQuads.get(i);
         newMergedQuads.set(i, oldQuad);
      }

      return new MergedItemModel(this.parent, newMergedQuads, this.retextureStart, this.textureStride);
   }

   public void setSprite(TextureAtlasSprite sprite, int colorMultiplier, float uSShift, float vSShift, float uEShift, float vEShift) {
      boolean matchingUvs = this.currentUvs != null
         && this.currentUvs.length == 4
         && this.currentUvs[0] == sprite.getU0()
         && this.currentUvs[1] == sprite.getV0()
         && this.currentUvs[2] == sprite.getU1()
         && this.currentUvs[3] == sprite.getV1();
      boolean matchingColorMul = this.currentColorMultipliers != null && this.currentColorMultipliers[0] == colorMultiplier;
      if (!matchingUvs || !matchingColorMul || sprite != this.currentSprite) {
         this.currentSprite = sprite;
         if (!matchingUvs) {
            this.currentUvs = new float[]{sprite.getU0(), sprite.getV0(), sprite.getU1(), sprite.getV1()};
         }

         if (!matchingColorMul) {
            this.currentColorMultipliers = new int[]{colorMultiplier};
         }

         this.setSpriteUnchecked(uSShift, vSShift, uEShift, vEShift);
      }
   }

   public void setSprite(float[] uvs, int[] colorMultipliers, float uSShift, float vSShift, float uEShift, float vEShift) {
      boolean changedSprite = this.currentSprite != null;
      this.currentSprite = null;
      boolean matchingUvs = Arrays.equals(uvs, this.currentUvs);
      boolean matchingColorMul = Arrays.equals(colorMultipliers, this.currentColorMultipliers);
      if (!matchingUvs || !matchingColorMul || changedSprite) {
         if (!matchingUvs) {
            this.currentUvs = uvs;
         }

         if (!matchingColorMul) {
            this.currentColorMultipliers = colorMultipliers;
         }

         this.setSpriteUnchecked(uSShift, vSShift, uEShift, vEShift);
      }
   }

   private void setSpriteUnchecked(float uSShift, float vSShift, float uEShift, float vEShift) {
      if (this.mergedQuads.size() - this.retextureStart > this.textureStride * this.currentColorMultipliers.length) {
         throw new IllegalStateException(
            String.format(
               "mismatched size/stride/multipliers: retex-quads=%d, stride=%d, muls=%d",
               this.mergedQuads.size() - this.retextureStart,
               this.textureStride,
               this.currentColorMultipliers.length
            )
         );
      } else if (this.currentUvs.length != this.currentColorMultipliers.length * 4) {
         throw new IllegalStateException(
            String.format("mismatched uvs/multipliers: uvs=%d, muls=%d", this.currentUvs.length, this.currentColorMultipliers.length)
         );
      } else {
         int texture = 0;

         for (int baseIdx = this.retextureStart; baseIdx < this.mergedQuads.size(); baseIdx += this.textureStride) {
            float uS = this.currentUvs[texture * 4];
            float vS = this.currentUvs[texture * 4 + 1];
            float uE = this.currentUvs[texture * 4 + 2];
            float vE = this.currentUvs[texture * 4 + 3];
            float du = uE - uS;
            float dv = vE - vS;
            du /= uEShift - uSShift;
            uS -= du * uSShift;
            dv /= vEShift - vSShift;
            vS -= dv * (1.0F - vEShift);
            int colorMultiplier = mapColor(this.currentColorMultipliers[texture]);

            for (int i = 0; i < this.textureStride; i++) {
               int[] vertexData = QuadData.read(this.mergedQuads.get(baseIdx + i));

               for (int j = 0; j < 4; j++) {
                  int offset = j * 7;
                  vertexData[offset + 3] = colorMultiplier;
                  float x = Float.intBitsToFloat(vertexData[offset + 0]);
                  float y = Float.intBitsToFloat(vertexData[offset + 1]);
                  vertexData[offset + 4] = Float.floatToRawIntBits(uS + x * du);
                  vertexData[offset + 5] = Float.floatToRawIntBits(vS + y * dv);
               }

               this.mergedQuads.set(baseIdx + i, QuadData.replace(this.mergedQuads.get(baseIdx + i), vertexData, this.currentSprite));
            }

            texture++;
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
      return side != null ? this.parent.func_188616_a(state, side, rand) : this.mergedQuads;
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
