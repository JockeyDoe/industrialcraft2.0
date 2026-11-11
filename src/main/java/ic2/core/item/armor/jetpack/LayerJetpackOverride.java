package ic2.core.item.armor.jetpack;

import com.mojang.blaze3d.vertex.PoseStack;
import ic2.core.model.NativeRendererRegistration;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.resources.model.EquipmentClientInfo.LayerType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;

public final class LayerJetpackOverride extends RenderLayer<HumanoidRenderState, HumanoidModel<HumanoidRenderState>> {
   private final HumanoidModel<HumanoidRenderState> armor;
   private final EquipmentLayerRenderer equipmentRenderer;

   public LayerJetpackOverride(RenderLayerParent<HumanoidRenderState, HumanoidModel<HumanoidRenderState>> parent, Context context) {
      super(parent);
      this.armor = (HumanoidModel<HumanoidRenderState>)ArmorModelSet.bake(ModelLayers.PLAYER_ARMOR, context.getModelSet(), HumanoidModel::new)
         .get(EquipmentSlot.CHEST);
      this.equipmentRenderer = context.getEquipmentRenderer();
   }

   public void submit(PoseStack pose, SubmitNodeCollector collector, int light, HumanoidRenderState state, float yaw, float pitch) {
      if (Boolean.TRUE.equals(state.getRenderData(NativeRendererRegistration.ATTACHED_JETPACK))) {
         ItemStack jetpack = JetpackHandler.jetpack.copy();
         Equippable equippable = (Equippable)jetpack.get(DataComponents.EQUIPPABLE);
         if (equippable != null && !equippable.assetId().isEmpty()) {
            this.equipmentRenderer
               .renderLayers(
                  LayerType.HUMANOID, (ResourceKey)equippable.assetId().orElseThrow(), this.armor, state, jetpack, pose, collector, light, state.outlineColor
               );
         }
      }
   }
}
