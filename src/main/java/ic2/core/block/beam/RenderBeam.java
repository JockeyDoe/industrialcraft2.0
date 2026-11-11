package ic2.core.block.beam;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

public class RenderBeam extends EntityRenderer<EntityParticle, EntityRenderState> {
   private static final Identifier texture = Identifier.fromNamespaceAndPath("ic2", "textures/models/beam.png");

   public RenderBeam(Context context) {
      super(context);
   }

   public EntityRenderState createRenderState() {
      return new EntityRenderState();
   }

   public void submit(EntityRenderState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
      pose.pushPose();

      try {
         pose.rotate(camera.orientation);
         int light = state.lightCoords;
         collector.submitCustomGeometry(
            pose,
            RenderTypes.entityTranslucent(texture),
            (transform, vertices) -> {
               for (int i = 0; i < 4; i++) {
                  float x = i != 0 && i != 3 ? 0.1F : -0.1F;
                  float y = i < 2 ? -0.1F : 0.1F;
                  vertices.addVertex(transform, x, y, 0.0F)
                     .setColor(-1)
                     .setUv(i != 0 && i != 3 ? 1.0F : 0.0F, i < 2 ? 1.0F : 0.0F)
                     .setOverlay(OverlayTexture.NO_OVERLAY)
                     .setLight(light)
                     .setNormal(transform, 0.0F, 0.0F, 1.0F);
               }
            }
         );
      } finally {
         pose.popPose();
      }

      super.submit(state, pose, collector, camera);
   }
}
