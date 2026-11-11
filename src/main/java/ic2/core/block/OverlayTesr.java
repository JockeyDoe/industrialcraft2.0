package ic2.core.block;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.Vec3;

public class OverlayTesr implements BlockEntityRenderer<TileEntityBlock, OverlayTesr.State> {
   private static final BlockDisplayContext CONTEXT = BlockDisplayContext.create();

   public OverlayTesr.State createRenderState() {
      return new OverlayTesr.State();
   }

   public void extractRenderState(TileEntityBlock te, OverlayTesr.State state, float partialTicks, Vec3 camera, CrumblingOverlay breaking) {
      super.extractRenderState(te, state, partialTicks, camera, breaking);
      Minecraft.getInstance().getBlockModelResolver().update(state.model, te.getBlockState(), CONTEXT);
   }

   public void submit(OverlayTesr.State state, PoseStack poses, SubmitNodeCollector collector, CameraRenderState camera) {
      poses.pushPose();
      poses.translate(0.5, 0.5, 0.5);
      poses.scale(1.001F, 1.001F, 1.001F);
      poses.translate(-0.5, -0.5, -0.5);
      state.model.submitMultiLayer(poses, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
      poses.popPose();
   }

   public static class State extends BlockEntityRenderState {
      final BlockModelRenderState model = new BlockModelRenderState();
   }
}
