package ic2.core.block;

import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.Unit;

public class KineticGeneratorRotor extends Model<Unit> {
   public KineticGeneratorRotor(int radius) {
      super(createLayer(radius).bakeRoot(), RenderTypes::entityCutout);
   }

   public static LayerDefinition createLayer(int radius) {
      MeshDefinition mesh = new MeshDefinition();
      float[][] rotations = new float[][]{{0.0F, -0.5F, 0.0F}, {3.1F, 0.5F, 0.0F}, {4.7F, 0.0F, 0.5F}, {1.5F, 0.0F, -0.5F}};

      for (int i = 0; i < rotations.length; i++) {
         float[] r = rotations[i];
         mesh.getRoot()
            .addOrReplaceChild(
               "blade" + i,
               CubeListBuilder.create().texOffs(0, 0).addBox(0.0F, 0.0F, -4.0F, 1.0F, radius * 8, 8.0F),
               PartPose.offsetAndRotation(-8.0F, 0.0F, 0.0F, r[0], r[1], r[2])
            );
      }

      return LayerDefinition.create(mesh, 32, 256);
   }
}
