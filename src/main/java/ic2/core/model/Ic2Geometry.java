package ic2.core.model;

import ic2.core.block.BlockTileEntity;
import ic2.core.block.state.Ic2BlockState;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.item.CuboidItemModelWrapper;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState.FoilType;
import net.minecraft.client.renderer.item.ItemStackRenderState.LayerRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.cuboid.ItemTransforms;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.ItemQuads;
import net.minecraft.client.resources.model.sprite.Material.Baked;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.commons.lang3.tuple.Pair;
import org.joml.Matrix4f;

public interface Ic2Geometry extends BlockStateModel, ItemModel {
   List<BakedQuad> func_188616_a(BlockState var1, Direction var2, long var3);

   boolean func_177555_b();

   boolean func_177556_c();

   boolean func_188618_c();

   TextureAtlasSprite func_177554_e();

   ItemTransforms func_177552_f();

   Ic2ItemOverrides func_188617_f();

   default Pair<? extends Ic2Geometry, Matrix4f> handlePerspective(ItemDisplayContext context) {
      Matrix4f identity = new Matrix4f();
      return Pair.of(this, identity);
   }

   default Baked particleMaterial() {
      return new Baked(this.func_177554_e(), false);
   }

   default Baked particleMaterial(BlockAndTintGetter world, BlockPos pos, BlockState state) {
      state = renderState(world, pos, state);
      return this instanceof ISpecialParticleModel special && state instanceof Ic2BlockState.Ic2BlockStateInstance extended
         ? new Baked(special.getParticleTexture(extended), false)
         : this.particleMaterial();
   }

   private static BlockState renderState(BlockAndTintGetter world, BlockPos pos, BlockState state) {
      if (state.getBlock() instanceof BlockTileEntity block) {
         state = block.func_176221_a(state, world, pos);
         state = block.getExtendedState(state, world, pos);
      }

      return state;
   }

   default int materialFlags() {
      return 0;
   }

   default BlockStateModelPart part(final BlockState state, final long seed) {
      return new BlockStateModelPart() {
         {
            Objects.requireNonNull(Ic2Geometry.this);
         }

         public List<BakedQuad> getQuads(Direction face) {
            return Ic2Geometry.this.func_188616_a(state, face, seed);
         }

         public boolean useAmbientOcclusion() {
            return Ic2Geometry.this.func_177555_b();
         }

         public Baked particleMaterial() {
            return Ic2Geometry.this instanceof ISpecialParticleModel special && state instanceof Ic2BlockState.Ic2BlockStateInstance extended
               ? new Baked(special.getParticleTexture(extended), false)
               : Ic2Geometry.this.particleMaterial();
         }

         public int materialFlags() {
            return Ic2Geometry.this.materialFlags();
         }
      };
   }

   default void collectParts(RandomSource random, List<BlockStateModelPart> parts) {
      parts.add(this.part(null, random.nextLong()));
   }

   default void collectParts(BlockAndTintGetter world, BlockPos pos, BlockState state, RandomSource random, List<BlockStateModelPart> parts) {
      state = renderState(world, pos, state);
      parts.add(this.part(state, random.nextLong()));
   }

   default void update(
      ItemStackRenderState output, ItemStack stack, ItemModelResolver resolver, ItemDisplayContext context, ClientLevel level, ItemOwner owner, int seed
   ) {
      Ic2ItemOverrides overrides = this.func_188617_f();
      Ic2Geometry selected = overrides == null ? this : overrides.handleItemState(this, stack, level, owner == null ? null : owner.asLivingEntity());
      output.appendModelIdentityElement(selected);
      List<BakedQuad> quads = new ArrayList<>(selected.func_188616_a(null, null, seed));

      for (Direction face : Direction.values()) {
         quads.addAll(selected.func_188616_a(null, face, seed));
      }

      LayerRenderState layer = output.newLayer();
      layer.setQuads(ItemQuads.split(quads));
      int maxTint = quads.stream().mapToInt(quad -> quad.materialInfo().tintIndex()).max().orElse(-1);

      for (int tint = 0; tint <= maxTint; tint++) {
         layer.tintLayers().add(Ic2ItemTint.color(stack, tint));
      }

      layer.setExtents(() -> CuboidItemModelWrapper.computeExtents(quads));
      layer.setParticleMaterial(selected.particleMaterial());
      layer.setUsesBlockLight(selected.func_177556_c());
      ItemTransforms transforms = selected.func_177552_f();
      if (transforms != null) {
         layer.setItemTransform(transforms.getTransform(context));
      }

      if (stack.hasFoil()) {
         layer.setFoilType(FoilType.STANDARD);
      }
   }
}
