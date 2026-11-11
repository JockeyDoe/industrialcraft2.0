package ic2.core.block.personal;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Unit;
import net.minecraft.world.phys.Vec3;

public class TileEntityPersonalChestRenderer implements BlockEntityRenderer<TileEntityPersonalChest, TileEntityPersonalChestRenderer.State> {
   private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("ic2", "textures/models/newsafe.png");
   private final ModelPersonalChest model = new ModelPersonalChest();

   public TileEntityPersonalChestRenderer.State createRenderState() {
      return new TileEntityPersonalChestRenderer.State();
   }

   public void extractRenderState(
      TileEntityPersonalChest te, TileEntityPersonalChestRenderer.State state, float partialTicks, Vec3 camera, CrumblingOverlay breaking
   ) {
      super.extractRenderState(te, state, partialTicks, camera, breaking);
      state.facing = te.getFacing();
      state.lidAngle = te.getLidAngle(partialTicks);
   }

   public void submit(TileEntityPersonalChestRenderer.State state, PoseStack poses, SubmitNodeCollector collector, CameraRenderState camera) {
      poses.pushPose();
      poses.translate(0.5, 0.5, 0.5);

      float yaw = switch (state.facing) {
         case SOUTH -> 180.0F;
         case WEST -> 90.0F;
         case EAST -> -90.0F;
         default -> 0.0F;
      };
      poses.rotateDegrees(Axis.YP, yaw);
      poses.translate(-0.5, -0.5, -0.5);
      poses.translate(0.84375, 0.0, 0.15625);
      poses.rotateDegrees(Axis.YP, -90.0F * state.lidAngle * state.lidAngle * state.lidAngle);
      poses.translate(-0.84375, 0.0, -0.15625);
      collector.submitModel(this.model, Unit.INSTANCE, poses, TEXTURE, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
      poses.popPose();
   }

   public static class State extends BlockEntityRenderState {
      Direction facing;
      float lidAngle;
   }
}
