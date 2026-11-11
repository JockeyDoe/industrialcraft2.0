package ic2.core.item;

import ic2.core.model.FluidRendering;
import ic2.core.model.Ic2Geometry;
import ic2.core.model.Ic2ItemOverrides;
import ic2.core.model.MaskOverlayModel;
import ic2.core.model.ModelUtil;
import ic2.core.util.FluidTransfer;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;

public class FluidCellModel extends MaskOverlayModel {
   private static final Identifier baseModelLoc = Identifier.fromNamespaceAndPath("ic2", "item/cell/fluid_cell_case");
   private static final Identifier maskTextureLoc = Identifier.fromNamespaceAndPath("ic2", "textures/items/cell/fluid_cell_window.png");
   private final Ic2ItemOverrides overrideHandler = new Ic2ItemOverrides(Collections.emptyList()) {
      {
         Objects.requireNonNull(FluidCellModel.this);
      }

      @Override
      public Ic2Geometry handleItemState(Ic2Geometry originalModel, ItemStack stack, Level world, LivingEntity entity) {
         if (stack == null) {
            return ModelUtil.getMissingModel();
         } else {
            FluidStack fs = FluidTransfer.getFluidContained(stack);
            return fs != null && !fs.isEmpty() ? FluidCellModel.this.get(FluidRendering.still(fs), FluidRendering.color(fs)) : FluidCellModel.this.get();
         }
      }
   };

   public FluidCellModel() {
      super(baseModelLoc, maskTextureLoc, false, -0.1F);
   }

   @Override
   public Ic2ItemOverrides func_188617_f() {
      return this.overrideHandler;
   }
}
