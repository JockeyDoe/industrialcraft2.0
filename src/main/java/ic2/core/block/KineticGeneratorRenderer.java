package ic2.core.block;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import ic2.api.tile.IRotorProvider;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Unit;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

public class KineticGeneratorRenderer<T extends BlockEntity> implements BlockEntityRenderer<T, KineticGeneratorRenderer.State> {
   private final Map<Integer, KineticGeneratorRotor> models = new HashMap<>();

   public KineticGeneratorRenderer.State createRenderState() {
      return new KineticGeneratorRenderer.State();
   }

   public void extractRenderState(T te, KineticGeneratorRenderer.State state, float partialTicks, Vec3 camera, CrumblingOverlay breaking) {
      super.extractRenderState(te, state, partialTicks, camera, breaking);
      state.diameter = 0;
      if (te instanceof IRotorProvider rotor && te.getLevel() != null) {
         state.diameter = rotor.getRotorDiameter();
         state.angle = rotor.getAngle();
         state.texture = rotor.getRotorRenderTexture();
         state.facing = rotor.getFacing();
         state.lightCoords = LightCoordsUtil.getLightCoords(te.getLevel(), te.getBlockPos().relative(state.facing));
      }
   }

   public void submit(KineticGeneratorRenderer.State state, PoseStack poses, SubmitNodeCollector collector, CameraRenderState camera) {
      if (state.diameter > 0 && state.texture != null) {
         KineticGeneratorRotor model = this.models.computeIfAbsent(state.diameter, KineticGeneratorRotor::new);
         poses.pushPose();
         poses.translate(0.5, 0.5, 0.5);
         switch (state.facing) {
            case NORTH:
               poses.rotateDegrees(Axis.YP, -90.0F);
               break;
            case EAST:
               poses.rotateDegrees(Axis.YP, -180.0F);
               break;
            case SOUTH:
               poses.rotateDegrees(Axis.YP, -270.0F);
               break;
            case UP:
               poses.rotateDegrees(Axis.ZP, -90.0F);
         }

         poses.rotateDegrees(Axis.XP, state.angle);
         poses.translate(-0.2, 0.0, 0.0);
         collector.submitModel(model, Unit.INSTANCE, poses, state.texture, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
         poses.popPose();
      }
   }

   public static class State extends BlockEntityRenderState {
      int diameter;
      float angle;
      Identifier texture;
      Direction facing;
   }
}
