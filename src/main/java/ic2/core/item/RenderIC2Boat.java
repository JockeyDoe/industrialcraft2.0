package ic2.core.item;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.Model.Simple;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.boat.BoatModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.AbstractBoatRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.state.BoatRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Unit;

public class RenderIC2Boat extends AbstractBoatRenderer {
   private final EntityModel<BoatRenderState> boat;
   private final Simple waterPatch;

   public RenderIC2Boat(Context context, String texture) {
      super(context, Identifier.fromNamespaceAndPath("ic2", texture));
      this.boat = new BoatModel(context.bakeLayer(ModelLayers.OAK_BOAT));
      this.waterPatch = new Simple(context.bakeLayer(ModelLayers.BOAT_WATER_PATCH), t -> RenderTypes.waterMask());
   }

   protected EntityModel<BoatRenderState> model() {
      return this.boat;
   }

   protected void submitTypeAdditions(BoatRenderState state, PoseStack poses, SubmitNodeCollector collector, int light) {
      if (!state.isUnderWater) {
         collector.submitModel(this.waterPatch, Unit.INSTANCE, poses, this.texture, light, OverlayTexture.NO_OVERLAY, state.outlineColor);
      }
   }
}
