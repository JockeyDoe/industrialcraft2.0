package ic2.core.item.tool;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public class RenderCrossed extends EntityRenderer<EntityMiningLaser, RenderCrossed.State> {
   private final Identifier texture;

   public RenderCrossed(Context context, Identifier texture) {
      super(context);
      this.texture = texture;
   }

   public RenderCrossed.State createRenderState() {
      return new RenderCrossed.State();
   }

   public void extractRenderState(EntityMiningLaser entity, RenderCrossed.State state, float partial) {
      super.extractRenderState(entity, state, partial);
      state.yaw = Mth.lerp(partial, entity.yRotO, entity.getYRot());
      state.pitch = Mth.lerp(partial, entity.xRotO, entity.getXRot());
      state.visible = entity.yRotO != 0.0F || entity.xRotO != 0.0F;
   }

   public void submit(RenderCrossed.State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
      if (state.visible) {
         pose.pushPose();

         try {
            pose.rotateDegrees(Axis.YP, state.yaw - 90.0F);
            pose.rotateDegrees(Axis.ZP, state.pitch);
            pose.rotateDegrees(Axis.XP, 45.0F);
            pose.scale(0.05625F, 0.05625F, 0.05625F);
            pose.translate(-4.0F, 0.0F, 0.0F);
            int light = state.lightCoords;
            collector.submitCustomGeometry(pose, RenderTypes.entityCutout(this.texture), (transform, vertices) -> {
               float[][] back = new float[][]{{-7.0F, -2.0F, -2.0F}, {-7.0F, -2.0F, 2.0F}, {-7.0F, 2.0F, 2.0F}, {-7.0F, 2.0F, -2.0F}};

               for (int side = 0; side < 2; side++) {
                  for (int i = 0; i < 4; i++) {
                     int index = side == 0 ? i : 3 - i;
                     float[] p = back[index];
                     float u = index != 1 && index != 2 ? 0.0F : 0.15625F;
                     float v = index >= 2 ? 0.3125F : 0.15625F;
                     vertex(vertices, transform, p[0], p[1], p[2], u, v, side == 0 ? 1.0F : -1.0F, 0.0F, 0.0F, light);
                  }
               }

               for (int side = 0; side < 4; side++) {
                  for (int i = 0; i < 4; i++) {
                     float x = i != 0 && i != 3 ? 8.0F : -8.0F;
                     float y = i < 2 ? -2.0F : 2.0F;
                     float angle = (float)(side * Math.PI / 2.0);
                     float cy = (float)Math.cos(angle);
                     float sy = (float)Math.sin(angle);
                     vertex(vertices, transform, x, y * cy, y * sy, i != 1 && i != 2 ? 0.0F : 0.5F, i >= 2 ? 0.15625F : 0.0F, 0.0F, -sy, cy, light);
                  }
               }
            });
         } finally {
            pose.popPose();
         }

         super.submit(state, pose, collector, camera);
      }
   }

   private static void vertex(VertexConsumer vertices, Pose pose, float x, float y, float z, float u, float v, float nx, float ny, float nz, int light) {
      vertices.addVertex(pose, x, y, z).setColor(-1).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, nx, ny, nz);
   }

   public static final class State extends EntityRenderState {
      float yaw;
      float pitch;
      boolean visible;
   }
}
