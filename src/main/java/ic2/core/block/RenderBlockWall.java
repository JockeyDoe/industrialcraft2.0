package ic2.core.block;

import ic2.core.block.comp.Obscuration;
import ic2.core.block.state.Ic2BlockState;
import ic2.core.item.tool.ItemObscurator;
import ic2.core.model.AbstractModel;
import ic2.core.model.BasicBakedBlockModel;
import ic2.core.model.ISpecialParticleModel;
import ic2.core.model.Ic2Geometry;
import ic2.core.model.MergedBlockModel;
import ic2.core.model.ModelUtil;
import ic2.core.model.VdUtil;
import ic2.core.ref.BlockName;
import ic2.core.util.Ic2Color;
import java.nio.Buffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

public class RenderBlockWall extends AbstractModel implements ISpecialParticleModel {
   @Override
   public List<BakedQuad> func_188616_a(BlockState rawState, Direction side, long rand) {
      if (!(rawState instanceof Ic2BlockState.Ic2BlockStateInstance state)) {
         return ModelUtil.getBlockModel(BlockName.wall.getBlockState(Ic2Color.white)).func_188616_a(rawState, side, rand);
      } else if (!state.hasValue(TileEntityWall.renderStateProperty)) {
         return ModelUtil.getBlockModel(BlockName.wall.getBlockState(Ic2Color.white)).func_188616_a(state, side, rand);
      } else {
         TileEntityWall.WallRenderState prop = state.getValue(TileEntityWall.renderStateProperty);
         float[][] uvs = new float[6][];
         int[][] colorMultipliers = new int[6][];
         TextureAtlasSprite[][] sprites = new TextureAtlasSprite[6][];
         int total = 0;

         for (int i = 0; i < 6; i++) {
            Obscuration.ObscurationData data = prop.obscurations[i];
            if (data != null) {
               ItemObscurator.ObscuredRenderInfo renderInfo = ItemObscurator.getRenderInfo(data.state, data.side);
               if (renderInfo != null && renderInfo.uvs.length == 4 * data.colorMultipliers.length) {
                  uvs[i] = renderInfo.uvs;
                  colorMultipliers[i] = data.colorMultipliers;
                  sprites[i] = renderInfo.sprites;
                  total += data.colorMultipliers.length;
               }
            }
         }

         Ic2Geometry baseModel = ModelUtil.getBlockModel(BlockName.wall.getBlockState(prop.color));
         if (total == 0) {
            return baseModel.func_188616_a(state, side, rand);
         } else {
            MergedBlockModel mergedModel = generateModel(baseModel, state, colorMultipliers);
            mergedModel.setSprite(uvs, colorMultipliers, sprites);
            return mergedModel.func_188616_a(state, side, rand);
         }
      }
   }

   private static MergedBlockModel generateModel(Ic2Geometry baseModel, BlockState state, int[][] colorMultipliers) {
      float offset = 0.001F;
      List<BakedQuad>[] mergedQuads = new List[6];
      int[] retextureStart = new int[6];
      IntBuffer buffer = VdUtil.getQuadBuffer();

      for (Direction side : Direction.values()) {
         int[] sideColorMultipliers = colorMultipliers[side.ordinal()];
         List<BakedQuad> baseFaceQuads = baseModel.func_188616_a(state, side, 0L);
         if (sideColorMultipliers == null) {
            mergedQuads[side.ordinal()] = baseFaceQuads;
         } else {
            List<BakedQuad> mergedFaceQuads = new ArrayList<>(baseFaceQuads.size() + sideColorMultipliers.length);
            mergedFaceQuads.addAll(baseFaceQuads);

            for (int sideColorMultiplier : sideColorMultipliers) {
               generateQuad(side, 0.001F, buffer);
               mergedFaceQuads.add(BasicBakedBlockModel.createQuad(Arrays.copyOf(buffer.array(), buffer.position()), side, null));
               ((Buffer)buffer).rewind();
            }

            mergedQuads[side.ordinal()] = mergedFaceQuads;
         }

         retextureStart[side.ordinal()] = baseFaceQuads.size();
      }

      return new MergedBlockModel(baseModel, mergedQuads, retextureStart);
   }

   private static void generateQuad(Direction side, float offset, IntBuffer out) {
      float neg = -offset;
      float pos = 1.0F + offset;
      switch (side) {
         case DOWN:
            VdUtil.generateBlockVertex(neg, neg, neg, 0.0F, 0.0F, side, out);
            VdUtil.generateBlockVertex(pos, neg, neg, 1.0F, 0.0F, side, out);
            VdUtil.generateBlockVertex(pos, neg, pos, 1.0F, 1.0F, side, out);
            VdUtil.generateBlockVertex(neg, neg, pos, 0.0F, 1.0F, side, out);
            break;
         case UP:
            VdUtil.generateBlockVertex(neg, pos, neg, 0.0F, 0.0F, side, out);
            VdUtil.generateBlockVertex(neg, pos, pos, 0.0F, 1.0F, side, out);
            VdUtil.generateBlockVertex(pos, pos, pos, 1.0F, 1.0F, side, out);
            VdUtil.generateBlockVertex(pos, pos, neg, 1.0F, 0.0F, side, out);
            break;
         case NORTH:
            VdUtil.generateBlockVertex(neg, neg, neg, 0.0F, 0.0F, side, out);
            VdUtil.generateBlockVertex(neg, pos, neg, 0.0F, 1.0F, side, out);
            VdUtil.generateBlockVertex(pos, pos, neg, 1.0F, 1.0F, side, out);
            VdUtil.generateBlockVertex(pos, neg, neg, 1.0F, 0.0F, side, out);
            break;
         case SOUTH:
            VdUtil.generateBlockVertex(neg, neg, pos, 0.0F, 0.0F, side, out);
            VdUtil.generateBlockVertex(pos, neg, pos, 1.0F, 0.0F, side, out);
            VdUtil.generateBlockVertex(pos, pos, pos, 1.0F, 1.0F, side, out);
            VdUtil.generateBlockVertex(neg, pos, pos, 0.0F, 1.0F, side, out);
            break;
         case WEST:
            VdUtil.generateBlockVertex(neg, neg, neg, 0.0F, 0.0F, side, out);
            VdUtil.generateBlockVertex(neg, neg, pos, 1.0F, 0.0F, side, out);
            VdUtil.generateBlockVertex(neg, pos, pos, 1.0F, 1.0F, side, out);
            VdUtil.generateBlockVertex(neg, pos, neg, 0.0F, 1.0F, side, out);
            break;
         case EAST:
            VdUtil.generateBlockVertex(pos, neg, neg, 0.0F, 0.0F, side, out);
            VdUtil.generateBlockVertex(pos, pos, neg, 0.0F, 1.0F, side, out);
            VdUtil.generateBlockVertex(pos, pos, pos, 1.0F, 1.0F, side, out);
            VdUtil.generateBlockVertex(pos, neg, pos, 1.0F, 0.0F, side, out);
            break;
         default:
            throw new IllegalArgumentException();
      }
   }

   @Override
   public void onReload() {
   }

   @Override
   public boolean needsEnhancing(BlockState state) {
      return true;
   }

   @Override
   public TextureAtlasSprite getParticleTexture(Ic2BlockState.Ic2BlockStateInstance state) {
      if (!state.hasValue(TileEntityWall.renderStateProperty)) {
         return ModelUtil.getMissingModel().func_177554_e();
      } else {
         TileEntityWall.WallRenderState prop = state.getValue(TileEntityWall.renderStateProperty);
         Obscuration.ObscurationData data = prop.obscurations[Direction.UP.ordinal()];
         return data == null
            ? ModelUtil.getBlockModel(BlockName.wall.getBlockState(prop.color)).func_177554_e()
            : ModelUtil.getBlockModel(data.state).func_177554_e();
      }
   }
}
