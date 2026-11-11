package ic2.core.model;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.cuboid.ItemTransforms;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material.Baked;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

public record NativeGeometry(BlockStateModel model, ItemTransforms transforms, boolean blockLight) implements Ic2Geometry {
   public NativeGeometry(BlockStateModel model) {
      this(model, ItemTransforms.NO_TRANSFORMS, true);
   }

   public NativeGeometry(BlockStateModel model, ItemTransforms transforms) {
      this(model, transforms, true);
   }

   @Override
   public List<BakedQuad> func_188616_a(BlockState state, Direction face, long seed) {
      ArrayList<BlockStateModelPart> parts = new ArrayList<>();
      this.model.collectParts(RandomSource.create(seed), parts);
      ArrayList<BakedQuad> quads = new ArrayList<>();

      for (BlockStateModelPart part : parts) {
         quads.addAll(part.getQuads(face));
      }

      return quads;
   }

   @Override
   public void collectParts(RandomSource random, List<BlockStateModelPart> parts) {
      this.model.collectParts(random, parts);
   }

   @Override
   public void collectParts(BlockAndTintGetter world, BlockPos pos, BlockState state, RandomSource random, List<BlockStateModelPart> parts) {
      this.model.collectParts(world, pos, state, random, parts);
   }

   @Override
   public Baked particleMaterial() {
      return this.model.particleMaterial();
   }

   @Override
   public Baked particleMaterial(BlockAndTintGetter world, BlockPos pos, BlockState state) {
      return this.model.particleMaterial(world, pos, state);
   }

   @Override
   public int materialFlags() {
      return this.model.materialFlags();
   }

   @Override
   public boolean func_177555_b() {
      return true;
   }

   @Override
   public boolean func_177556_c() {
      return this.blockLight;
   }

   @Override
   public boolean func_188618_c() {
      return false;
   }

   @Override
   public TextureAtlasSprite func_177554_e() {
      return this.model.particleMaterial().sprite();
   }

   @Override
   public ItemTransforms func_177552_f() {
      return this.transforms;
   }

   @Override
   public Ic2ItemOverrides func_188617_f() {
      return Ic2ItemOverrides.EMPTY;
   }
}
