package ic2.core.item.tool;

import ic2.core.model.Ic2Geometry;
import ic2.core.model.Ic2ItemOverrides;
import ic2.core.model.MaskOverlayModel;
import ic2.core.model.ModelUtil;
import ic2.core.util.StackUtil;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class RenderObscurator extends MaskOverlayModel {
   private static final Identifier baseModelLoc = Identifier.fromNamespaceAndPath("ic2", "item/tool/electric/obscurator_raw");
   private static final Identifier maskTextureLoc = Identifier.fromNamespaceAndPath("ic2", "textures/items/tool/electric/obscurator_mask.png");
   private final Ic2ItemOverrides overrideHandler = new Ic2ItemOverrides(Collections.emptyList()) {
      {
         Objects.requireNonNull(RenderObscurator.this);
      }

      @Override
      public Ic2Geometry handleItemState(Ic2Geometry originalModel, ItemStack stack, Level world, LivingEntity entity) {
         if (stack == null) {
            return ModelUtil.getMissingModel();
         } else {
            CompoundTag nbt = StackUtil.getOrCreateNbtData(stack);
            BlockState state = ItemObscurator.getState(nbt);
            Direction side = ItemObscurator.getSide(nbt);
            int[] colorMultipliers = ItemObscurator.getColorMultipliers(nbt);
            ItemObscurator.ObscuredRenderInfo renderInfo;
            return state != null
                  && side != null
                  && (renderInfo = ItemObscurator.getRenderInfo(state, side)) != null
                  && colorMultipliers != null
                  && colorMultipliers.length * 4 == renderInfo.uvs.length
               ? RenderObscurator.this.get(renderInfo.uvs, colorMultipliers)
               : RenderObscurator.this.get();
         }
      }
   };

   public RenderObscurator() {
      super(baseModelLoc, maskTextureLoc, true, 0.001F);
   }

   @Override
   public Ic2ItemOverrides func_188617_f() {
      return this.overrideHandler;
   }
}
