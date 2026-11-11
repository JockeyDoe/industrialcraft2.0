package ic2.core.block;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.TntMinecartRenderer;
import net.minecraft.client.renderer.entity.TntRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.state.TntRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;

public class RenderExplosiveBlock extends EntityRenderer<EntityIC2Explosive, TntRenderState> {
   private final BlockModelResolver models;

   public RenderExplosiveBlock(Context context) {
      super(context);
      this.models = context.getBlockModelResolver();
      this.shadowRadius = 0.5F;
   }

   public TntRenderState createRenderState() {
      return new TntRenderState();
   }

   public void extractRenderState(EntityIC2Explosive entity, TntRenderState state, float partial) {
      super.extractRenderState(entity, state, partial);
      state.fuseRemainingInTicks = entity.fuse - partial + 1.0F;
      this.models.update(state.blockState, entity.renderBlockState, TntRenderer.BLOCK_DISPLAY_CONTEXT);
   }

   public void submit(TntRenderState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
      pose.pushPose();

      try {
         pose.translate(0.0, 0.5, 0.0);
         float fuse = state.fuseRemainingInTicks;
         if (fuse < 10.0F) {
            float scale = 1.0F + TntRenderer.getSwellAmount(fuse);
            pose.scale(scale, scale, scale);
         }

         pose.translate(-0.5, -0.5, -0.5);
         if (!state.blockState.isEmpty()) {
            TntMinecartRenderer.submitWhiteSolidBlock(state.blockState, pose, collector, state.lightCoords, TntRenderer.isLit(fuse), state.outlineColor);
         }
      } finally {
         pose.popPose();
      }

      super.submit(state, pose, collector, camera);
   }
}
