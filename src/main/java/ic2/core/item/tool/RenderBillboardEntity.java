package ic2.core.item.tool;

import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.resources.Identifier;

public class RenderBillboardEntity extends NoopRenderer<EntityParticle> {
   private final Identifier texture = Identifier.fromNamespaceAndPath("ic2", "textures/models/beam.png");

   public RenderBillboardEntity(Context manager) {
      super(manager);
   }

   public void doRender(EntityParticle entity, double x, double y, double z, float yaw, float partialTickTime) {
   }

   protected Identifier getEntityTexture(EntityParticle entity) {
      return this.texture;
   }
}
