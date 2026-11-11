package ic2.core.block.personal;

import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.Unit;

public class ModelPersonalChest extends Model<Unit> {
   public ModelPersonalChest() {
      super(createLayer().bakeRoot(), RenderTypes::entityCutout);
   }

   public static LayerDefinition createLayer() {
      MeshDefinition mesh = new MeshDefinition();
      mesh.getRoot().addOrReplaceChild("door", CubeListBuilder.create().texOffs(30, 0).mirror().addBox(2.0F, 1.0F, 2.0F, 12.0F, 14.0F, 1.0F), PartPose.ZERO);
      return LayerDefinition.create(mesh, 64, 64);
   }
}
