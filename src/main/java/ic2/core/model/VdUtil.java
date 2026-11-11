package ic2.core.model;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.renderpearl.api.vertex.VertexFormat;
import ic2.core.util.Util;
import java.nio.Buffer;
import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.Direction;

public class VdUtil {
   public static final int quadVertexCount = 4;
   public static final VertexFormat vertexFormat = DefaultVertexFormat.BLOCK;
   public static final int dataStride = 7;
   private static final int[] faceShades = getFaceShades();

   public static IntBuffer getQuadBuffer() {
      return IntBuffer.allocate(28);
   }

   public static void addCuboid(
      float xS,
      float yS,
      float zS,
      float xE,
      float yE,
      float zE,
      Set<Direction> faces,
      TextureAtlasSprite sprite,
      List<BakedQuad>[] faceQuads,
      List<BakedQuad> generalQuads
   ) {
      float spriteU = sprite.getU0();
      float spriteV = sprite.getV0();
      addCuboid(xS, yS, zS, xE, yE, zE, spriteU, spriteV, sprite.getU1() - spriteU, sprite.getV1() - spriteV, faces, sprite, faceQuads, generalQuads);
   }

   public static void addCuboid(
      float xS,
      float yS,
      float zS,
      float xE,
      float yE,
      float zE,
      int color,
      Set<Direction> faces,
      TextureAtlasSprite sprite,
      List<BakedQuad>[] faceQuads,
      List<BakedQuad> generalQuads
   ) {
      float spriteU = sprite.getU0();
      float spriteV = sprite.getV0();
      addCuboid(xS, yS, zS, xE, yE, zE, color, spriteU, spriteV, sprite.getU1() - spriteU, sprite.getV1() - spriteV, faces, sprite, faceQuads, generalQuads);
   }

   public static void addFlippedCuboid(
      float xS,
      float yS,
      float zS,
      float xE,
      float yE,
      float zE,
      Set<Direction> faces,
      TextureAtlasSprite sprite,
      List<BakedQuad>[] faceQuads,
      List<BakedQuad> generalQuads
   ) {
      float spriteU = sprite.getU1();
      float spriteV = sprite.getV1();
      addCuboid(xS, yS, zS, xE, yE, zE, spriteU, spriteV, sprite.getU0() - spriteU, sprite.getV0() - spriteV, faces, sprite, faceQuads, generalQuads);
   }

   public static void addFlippedCuboid(
      float xS,
      float yS,
      float zS,
      float xE,
      float yE,
      float zE,
      int colour,
      Set<Direction> faces,
      TextureAtlasSprite sprite,
      List<BakedQuad>[] faceQuads,
      List<BakedQuad> generalQuads
   ) {
      float spriteU = sprite.getU1();
      float spriteV = sprite.getV1();
      addCuboid(xS, yS, zS, xE, yE, zE, colour, spriteU, spriteV, sprite.getU0() - spriteU, sprite.getV0() - spriteV, faces, sprite, faceQuads, generalQuads);
   }

   public static void addFlippedCuboidWithYOffset(
      float xS,
      float yS,
      float zS,
      float xE,
      float yE,
      float zE,
      int colour,
      Set<Direction> faces,
      TextureAtlasSprite sprite,
      List<BakedQuad>[] faceQuads,
      List<BakedQuad> generalQuads,
      float offset
   ) {
      float spriteU = sprite.getU1();
      float spriteV = sprite.getV1();
      addCuboidWithYOffset(
         xS, yS, zS, xE, yE, zE, colour, spriteU, spriteV, sprite.getU0() - spriteU, sprite.getV0() - spriteV, faces, sprite, faceQuads, generalQuads, offset
      );
   }

   private static void addCuboid(
      float xS,
      float yS,
      float zS,
      float xE,
      float yE,
      float zE,
      float spriteU,
      float spriteV,
      float spriteWidth,
      float spriteHeight,
      Set<Direction> faces,
      TextureAtlasSprite sprite,
      List<BakedQuad>[] faceQuads,
      List<BakedQuad> generalQuads
   ) {
      IntBuffer quadBuffer = getQuadBuffer();

      for (Direction facing : faces) {
         boolean isFace;
         switch (facing) {
            case DOWN:
               if (xS == xE || zS == zE) {
                  continue;
               }

               generateBlockVertex(xS, yS, zS, spriteU + spriteWidth * xS, spriteV + spriteHeight * zS, facing, quadBuffer);
               generateBlockVertex(xE, yS, zS, spriteU + spriteWidth * xE, spriteV + spriteHeight * zS, facing, quadBuffer);
               generateBlockVertex(xE, yS, zE, spriteU + spriteWidth * xE, spriteV + spriteHeight * zE, facing, quadBuffer);
               generateBlockVertex(xS, yS, zE, spriteU + spriteWidth * xS, spriteV + spriteHeight * zE, facing, quadBuffer);
               isFace = yS == 0.0F;
               break;
            case UP:
               if (xS == xE || zS == zE) {
                  continue;
               }

               generateBlockVertex(xS, yE, zS, spriteU + spriteWidth * xS, spriteV + spriteHeight * zS, facing, quadBuffer);
               generateBlockVertex(xS, yE, zE, spriteU + spriteWidth * xS, spriteV + spriteHeight * zE, facing, quadBuffer);
               generateBlockVertex(xE, yE, zE, spriteU + spriteWidth * xE, spriteV + spriteHeight * zE, facing, quadBuffer);
               generateBlockVertex(xE, yE, zS, spriteU + spriteWidth * xE, spriteV + spriteHeight * zS, facing, quadBuffer);
               isFace = yE == 1.0F;
               break;
            case NORTH:
               if (xS == xE || yS == yE) {
                  continue;
               }

               generateBlockVertex(xS, yS, zS, spriteU + spriteWidth * xS, spriteV + spriteHeight * yS, facing, quadBuffer);
               generateBlockVertex(xS, yE, zS, spriteU + spriteWidth * xS, spriteV + spriteHeight * yE, facing, quadBuffer);
               generateBlockVertex(xE, yE, zS, spriteU + spriteWidth * xE, spriteV + spriteHeight * yE, facing, quadBuffer);
               generateBlockVertex(xE, yS, zS, spriteU + spriteWidth * xE, spriteV + spriteHeight * yS, facing, quadBuffer);
               isFace = zS == 0.0F;
               break;
            case SOUTH:
               if (xS == xE || yS == yE) {
                  continue;
               }

               generateBlockVertex(xS, yS, zE, spriteU + spriteWidth * xS, spriteV + spriteHeight * yS, facing, quadBuffer);
               generateBlockVertex(xE, yS, zE, spriteU + spriteWidth * xE, spriteV + spriteHeight * yS, facing, quadBuffer);
               generateBlockVertex(xE, yE, zE, spriteU + spriteWidth * xE, spriteV + spriteHeight * yE, facing, quadBuffer);
               generateBlockVertex(xS, yE, zE, spriteU + spriteWidth * xS, spriteV + spriteHeight * yE, facing, quadBuffer);
               isFace = zE == 1.0F;
               break;
            case WEST:
               if (yS == yE || zS == zE) {
                  continue;
               }

               generateBlockVertex(xS, yS, zS, spriteU + spriteWidth * zS, spriteV + spriteHeight * yS, facing, quadBuffer);
               generateBlockVertex(xS, yS, zE, spriteU + spriteWidth * zE, spriteV + spriteHeight * yS, facing, quadBuffer);
               generateBlockVertex(xS, yE, zE, spriteU + spriteWidth * zE, spriteV + spriteHeight * yE, facing, quadBuffer);
               generateBlockVertex(xS, yE, zS, spriteU + spriteWidth * zS, spriteV + spriteHeight * yE, facing, quadBuffer);
               isFace = xS == 0.0F;
               break;
            case EAST:
               if (yS == yE || zS == zE) {
                  continue;
               }

               generateBlockVertex(xE, yS, zS, spriteU + spriteWidth * zS, spriteV + spriteHeight * yS, facing, quadBuffer);
               generateBlockVertex(xE, yE, zS, spriteU + spriteWidth * zS, spriteV + spriteHeight * yE, facing, quadBuffer);
               generateBlockVertex(xE, yE, zE, spriteU + spriteWidth * zE, spriteV + spriteHeight * yE, facing, quadBuffer);
               generateBlockVertex(xE, yS, zE, spriteU + spriteWidth * zE, spriteV + spriteHeight * yS, facing, quadBuffer);
               isFace = xE == 1.0F;
               break;
            default:
               throw new IllegalArgumentException("Unexpected facing: " + facing);
         }

         if (quadBuffer.position() > 0) {
            BakedQuad quad = BasicBakedBlockModel.createQuad(Arrays.copyOf(quadBuffer.array(), quadBuffer.position()), facing, sprite);
            if (isFace) {
               faceQuads[facing.ordinal()].add(quad);
            } else {
               generalQuads.add(quad);
            }

            ((Buffer)quadBuffer).rewind();
         }
      }
   }

   private static void addCuboid(
      float xS,
      float yS,
      float zS,
      float xE,
      float yE,
      float zE,
      int color,
      float spriteU,
      float spriteV,
      float spriteWidth,
      float spriteHeight,
      Set<Direction> faces,
      TextureAtlasSprite sprite,
      List<BakedQuad>[] faceQuads,
      List<BakedQuad> generalQuads
   ) {
      IntBuffer quadBuffer = getQuadBuffer();

      for (Direction facing : faces) {
         boolean isFace;
         switch (facing) {
            case DOWN:
               if (xS == xE || zS == zE) {
                  continue;
               }

               generateBlockVertex(xS, yS, zS, color, spriteU + spriteWidth * xS, spriteV + spriteHeight * zS, facing, quadBuffer);
               generateBlockVertex(xE, yS, zS, color, spriteU + spriteWidth * xE, spriteV + spriteHeight * zS, facing, quadBuffer);
               generateBlockVertex(xE, yS, zE, color, spriteU + spriteWidth * xE, spriteV + spriteHeight * zE, facing, quadBuffer);
               generateBlockVertex(xS, yS, zE, color, spriteU + spriteWidth * xS, spriteV + spriteHeight * zE, facing, quadBuffer);
               isFace = yS == 0.0F;
               break;
            case UP:
               if (xS == xE || zS == zE) {
                  continue;
               }

               generateBlockVertex(xS, yE, zS, color, spriteU + spriteWidth * xS, spriteV + spriteHeight * zS, facing, quadBuffer);
               generateBlockVertex(xS, yE, zE, color, spriteU + spriteWidth * xS, spriteV + spriteHeight * zE, facing, quadBuffer);
               generateBlockVertex(xE, yE, zE, color, spriteU + spriteWidth * xE, spriteV + spriteHeight * zE, facing, quadBuffer);
               generateBlockVertex(xE, yE, zS, color, spriteU + spriteWidth * xE, spriteV + spriteHeight * zS, facing, quadBuffer);
               isFace = yE == 1.0F;
               break;
            case NORTH:
               if (xS == xE || yS == yE) {
                  continue;
               }

               generateBlockVertex(xS, yS, zS, color, spriteU + spriteWidth * xS, spriteV + spriteHeight * yS, facing, quadBuffer);
               generateBlockVertex(xS, yE, zS, color, spriteU + spriteWidth * xS, spriteV + spriteHeight * yE, facing, quadBuffer);
               generateBlockVertex(xE, yE, zS, color, spriteU + spriteWidth * xE, spriteV + spriteHeight * yE, facing, quadBuffer);
               generateBlockVertex(xE, yS, zS, color, spriteU + spriteWidth * xE, spriteV + spriteHeight * yS, facing, quadBuffer);
               isFace = zS == 0.0F;
               break;
            case SOUTH:
               if (xS == xE || yS == yE) {
                  continue;
               }

               generateBlockVertex(xS, yS, zE, color, spriteU + spriteWidth * xS, spriteV + spriteHeight * yS, facing, quadBuffer);
               generateBlockVertex(xE, yS, zE, color, spriteU + spriteWidth * xE, spriteV + spriteHeight * yS, facing, quadBuffer);
               generateBlockVertex(xE, yE, zE, color, spriteU + spriteWidth * xE, spriteV + spriteHeight * yE, facing, quadBuffer);
               generateBlockVertex(xS, yE, zE, color, spriteU + spriteWidth * xS, spriteV + spriteHeight * yE, facing, quadBuffer);
               isFace = zE == 1.0F;
               break;
            case WEST:
               if (yS == yE || zS == zE) {
                  continue;
               }

               generateBlockVertex(xS, yS, zS, color, spriteU + spriteWidth * zS, spriteV + spriteHeight * yS, facing, quadBuffer);
               generateBlockVertex(xS, yS, zE, color, spriteU + spriteWidth * zE, spriteV + spriteHeight * yS, facing, quadBuffer);
               generateBlockVertex(xS, yE, zE, color, spriteU + spriteWidth * zE, spriteV + spriteHeight * yE, facing, quadBuffer);
               generateBlockVertex(xS, yE, zS, color, spriteU + spriteWidth * zS, spriteV + spriteHeight * yE, facing, quadBuffer);
               isFace = xS == 0.0F;
               break;
            case EAST:
               if (yS == yE || zS == zE) {
                  continue;
               }

               generateBlockVertex(xE, yS, zS, color, spriteU + spriteWidth * zS, spriteV + spriteHeight * yS, facing, quadBuffer);
               generateBlockVertex(xE, yE, zS, color, spriteU + spriteWidth * zS, spriteV + spriteHeight * yE, facing, quadBuffer);
               generateBlockVertex(xE, yE, zE, color, spriteU + spriteWidth * zE, spriteV + spriteHeight * yE, facing, quadBuffer);
               generateBlockVertex(xE, yS, zE, color, spriteU + spriteWidth * zE, spriteV + spriteHeight * yS, facing, quadBuffer);
               isFace = xE == 1.0F;
               break;
            default:
               throw new IllegalArgumentException("Unexpected facing: " + facing);
         }

         if (quadBuffer.position() > 0) {
            BakedQuad quad = BasicBakedBlockModel.createQuad(Arrays.copyOf(quadBuffer.array(), quadBuffer.position()), facing, sprite);
            if (isFace) {
               faceQuads[facing.ordinal()].add(quad);
            } else {
               generalQuads.add(quad);
            }

            ((Buffer)quadBuffer).rewind();
         }
      }
   }

   private static void addCuboidWithYOffset(
      float xS,
      float yS,
      float zS,
      float xE,
      float yE,
      float zE,
      int color,
      float spriteU,
      float spriteV,
      float spriteWidth,
      float spriteHeight,
      Set<Direction> faces,
      TextureAtlasSprite sprite,
      List<BakedQuad>[] faceQuads,
      List<BakedQuad> generalQuads,
      float offset
   ) {
      IntBuffer quadBuffer = getQuadBuffer();

      for (Direction facing : faces) {
         boolean isFace;
         switch (facing) {
            case DOWN:
               if (xS == xE || zS == zE) {
                  continue;
               }

               generateBlockVertex(xS, yS + offset, zS, color, spriteU + spriteWidth * xS, spriteV + spriteHeight * zS, facing, quadBuffer);
               generateBlockVertex(xE, yS + offset, zS, color, spriteU + spriteWidth * xE, spriteV + spriteHeight * zS, facing, quadBuffer);
               generateBlockVertex(xE, yS + offset, zE, color, spriteU + spriteWidth * xE, spriteV + spriteHeight * zE, facing, quadBuffer);
               generateBlockVertex(xS, yS + offset, zE, color, spriteU + spriteWidth * xS, spriteV + spriteHeight * zE, facing, quadBuffer);
               isFace = yS == 0.0F;
               break;
            case UP:
               if (xS == xE || zS == zE) {
                  continue;
               }

               generateBlockVertex(xS, yE + offset, zS, color, spriteU + spriteWidth * xS, spriteV + spriteHeight * zS, facing, quadBuffer);
               generateBlockVertex(xS, yE + offset, zE, color, spriteU + spriteWidth * xS, spriteV + spriteHeight * zE, facing, quadBuffer);
               generateBlockVertex(xE, yE + offset, zE, color, spriteU + spriteWidth * xE, spriteV + spriteHeight * zE, facing, quadBuffer);
               generateBlockVertex(xE, yE + offset, zS, color, spriteU + spriteWidth * xE, spriteV + spriteHeight * zS, facing, quadBuffer);
               isFace = yE == 1.0F;
               break;
            case NORTH:
               if (xS == xE || yS == yE) {
                  continue;
               }

               generateBlockVertex(xS, yS + offset, zS, color, spriteU + spriteWidth * xS, spriteV + spriteHeight * yS, facing, quadBuffer);
               generateBlockVertex(xS, yE + offset, zS, color, spriteU + spriteWidth * xS, spriteV + spriteHeight * yE, facing, quadBuffer);
               generateBlockVertex(xE, yE + offset, zS, color, spriteU + spriteWidth * xE, spriteV + spriteHeight * yE, facing, quadBuffer);
               generateBlockVertex(xE, yS + offset, zS, color, spriteU + spriteWidth * xE, spriteV + spriteHeight * yS, facing, quadBuffer);
               isFace = zS == 0.0F;
               break;
            case SOUTH:
               if (xS == xE || yS == yE) {
                  continue;
               }

               generateBlockVertex(xS, yS + offset, zE, color, spriteU + spriteWidth * xS, spriteV + spriteHeight * yS, facing, quadBuffer);
               generateBlockVertex(xE, yS + offset, zE, color, spriteU + spriteWidth * xE, spriteV + spriteHeight * yS, facing, quadBuffer);
               generateBlockVertex(xE, yE + offset, zE, color, spriteU + spriteWidth * xE, spriteV + spriteHeight * yE, facing, quadBuffer);
               generateBlockVertex(xS, yE + offset, zE, color, spriteU + spriteWidth * xS, spriteV + spriteHeight * yE, facing, quadBuffer);
               isFace = zE == 1.0F;
               break;
            case WEST:
               if (yS == yE || zS == zE) {
                  continue;
               }

               generateBlockVertex(xS, yS + offset, zS, color, spriteU + spriteWidth * zS, spriteV + spriteHeight * yS, facing, quadBuffer);
               generateBlockVertex(xS, yS + offset, zE, color, spriteU + spriteWidth * zE, spriteV + spriteHeight * yS, facing, quadBuffer);
               generateBlockVertex(xS, yE + offset, zE, color, spriteU + spriteWidth * zE, spriteV + spriteHeight * yE, facing, quadBuffer);
               generateBlockVertex(xS, yE + offset, zS, color, spriteU + spriteWidth * zS, spriteV + spriteHeight * yE, facing, quadBuffer);
               isFace = xS == 0.0F;
               break;
            case EAST:
               if (yS == yE || zS == zE) {
                  continue;
               }

               generateBlockVertex(xE, yS + offset, zS, color, spriteU + spriteWidth * zS, spriteV + spriteHeight * yS, facing, quadBuffer);
               generateBlockVertex(xE, yE + offset, zS, color, spriteU + spriteWidth * zS, spriteV + spriteHeight * yE, facing, quadBuffer);
               generateBlockVertex(xE, yE + offset, zE, color, spriteU + spriteWidth * zE, spriteV + spriteHeight * yE, facing, quadBuffer);
               generateBlockVertex(xE, yS + offset, zE, color, spriteU + spriteWidth * zE, spriteV + spriteHeight * yS, facing, quadBuffer);
               isFace = xE == 1.0F;
               break;
            default:
               throw new IllegalArgumentException("Unexpected facing: " + facing);
         }

         if (quadBuffer.position() > 0) {
            BakedQuad quad = BasicBakedBlockModel.createQuad(Arrays.copyOf(quadBuffer.array(), quadBuffer.position()), facing, sprite);
            if (isFace) {
               faceQuads[facing.ordinal()].add(quad);
            } else {
               generalQuads.add(quad);
            }

            ((Buffer)quadBuffer).rewind();
         }
      }
   }

   public static void generateVertex(float x, float y, float z, int color, float u, float v, Direction facing, IntBuffer out) {
      generateVertex(x, y, z, color, u, v, facing.getStepX(), facing.getStepY(), facing.getStepZ(), out);
   }

   public static void generateVertex(float x, float y, float z, int color, float u, float v, float nx, float ny, float nz, IntBuffer out) {
      out.put(Float.floatToRawIntBits(x));
      out.put(Float.floatToRawIntBits(y));
      out.put(Float.floatToRawIntBits(z));
      out.put(color);
      out.put(Float.floatToRawIntBits(u));
      out.put(Float.floatToRawIntBits(v));
      out.put(packNormals(nx, ny, nz));
   }

   public static void generateBlockVertex(float x, float y, float z, int color, float u, float v, Direction facing, IntBuffer out) {
      generateVertex(x, y, z, color, u, v, facing.getStepX(), facing.getStepY(), facing.getStepZ(), out);
   }

   public static void generateBlockVertex(float x, float y, float z, float u, float v, Direction facing, IntBuffer out) {
      generateVertex(x, y, z, faceShades[facing.ordinal()], u, v, facing.getStepX(), facing.getStepY(), facing.getStepZ(), out);
   }

   private static int packNormals(float nx, float ny, float nz) {
      return mapFloatToByte(nx) | mapFloatToByte(ny) << 8 | mapFloatToByte(nz) << 16;
   }

   private static int mapFloatToByte(float f) {
      assert f >= -1.0F && f <= 1.0F;

      return Math.round(f * 127.0F) & 0xFF;
   }

   private static int[] getFaceShades() {
      int[] ret = new int[Direction.values().length];
      double[] faceBrightness = new double[]{0.5, 1.0, 0.8, 0.8, 0.6, 0.6};

      for (Direction facing : Direction.values()) {
         int brightness = Util.limit((int)(faceBrightness[facing.ordinal()] * 255.0), 0, 255);
         ret[facing.ordinal()] = 0xFF000000 | brightness << 16 | brightness << 8 | brightness;
      }

      return ret;
   }
}
