package ic2.core.gui;

import ic2.core.GuiIC2;
import ic2.core.block.comp.FluidTankView;
import ic2.core.init.Localization;
import ic2.core.model.FluidRendering;
import java.util.List;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;

public class FluidSlot extends GuiElement<FluidSlot> {
   public static final int posU = 8;
   public static final int posV = 160;
   public static final int normalWidth = 18;
   public static final int normalHeight = 18;
   public static final int fluidOffsetX = 1;
   public static final int fluidOffsetY = 1;
   public static final int fluidNetWidth = 16;
   public static final int fluidNetHeight = 16;
   private final FluidTankView tank;

   public static FluidSlot createFluidSlot(GuiIC2<?> gui, int x, int y, FluidTankView tank) {
      return new FluidSlot(gui, x, y, 18, 18, tank);
   }

   protected FluidSlot(GuiIC2<?> gui, int x, int y, int width, int height, FluidTankView tank) {
      super(gui, x, y, width, height);
      if (tank == null) {
         throw new NullPointerException("Null FluidTank instance.");
      } else {
         this.tank = tank;
      }
   }

   @Override
   public void drawBackground(int mouseX, int mouseY) {
      bindCommonTexture();
      FluidStack fs = this.tank.getFluid();
      this.gui.drawTexturedRect(this.x, this.y, this.width, this.height, 8.0, 160.0);
      if (fs != null && fs.getAmount() > 0) {
         int fluidX = this.x + 1;
         int fluidY = this.y + 1;
         int fluidWidth = 16;
         int fluidHeight = 16;
         Fluid fluid = fs.getFluid();
         TextureAtlasSprite sprite = fluid != null ? FluidRendering.still(fs) : null;
         int color = fluid != null ? FluidRendering.color(fs) : -1;
         bindBlockTexture();
         this.gui.drawSprite(fluidX, fluidY, fluidWidth, fluidHeight, sprite, color, 1.0, false, false);
      }
   }

   @Override
   protected List<String> getToolTip() {
      List<String> ret = super.getToolTip();
      FluidStack fs = this.tank.getFluid();
      if (fs != null && fs.getAmount() > 0) {
         Fluid fluid = fs.getFluid();
         if (fluid != null) {
            ret.add(fluid.getFluidType().getDescription(fs).getString());
            ret.add("Amount: " + fs.getAmount() + " " + Localization.translate("ic2.generic.text.mb"));
            String state = fs.getFluid().getFluidType().getDensity() < 0 ? "Gas" : "Liquid";
            ret.add("Type: " + state);
         } else {
            ret.add("Invalid FluidStack instance.");
         }
      } else {
         ret.add("No Fluid");
         ret.add("Amount: 0 " + Localization.translate("ic2.generic.text.mb"));
         ret.add("Type: Not Available");
      }

      return ret;
   }
}
