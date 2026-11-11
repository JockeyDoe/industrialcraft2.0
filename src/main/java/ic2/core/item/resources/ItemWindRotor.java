package ic2.core.item.resources;

import ic2.api.item.IKineticRotor;
import ic2.core.block.kineticgenerator.gui.GuiWaterKineticGenerator;
import ic2.core.block.kineticgenerator.gui.GuiWindKineticGenerator;
import ic2.core.init.Localization;
import ic2.core.item.ItemDefaults;
import ic2.core.item.ItemGradualInt;
import ic2.core.profile.NotClassic;
import ic2.core.ref.ItemName;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@NotClassic
public class ItemWindRotor extends ItemGradualInt implements IKineticRotor {
   private final int maxWindStrength;
   private final int minWindStrength;
   private final int radius;
   private final float efficiency;
   private final Identifier renderTexture;
   private final boolean water;

   public ItemWindRotor(ItemName name, int Radius, int durability, float efficiency, int minWindStrength, int maxWindStrength, Identifier RenderTexture) {
      super(name, durability);
      ItemDefaults.stackSize(this, 1);
      this.radius = Radius;
      this.efficiency = efficiency;
      this.renderTexture = RenderTexture;
      this.minWindStrength = minWindStrength;
      this.maxWindStrength = maxWindStrength;
      this.water = name != ItemName.rotor_wood;
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public void func_77624_a(ItemStack stack, Level world, List<String> tooltip, TooltipFlag advanced) {
      tooltip.add(Localization.translate("ic2.itemrotor.wind.info", this.minWindStrength, this.maxWindStrength));
      IKineticRotor.GearboxType type = null;
      if (Minecraft.getInstance().gui.screen() instanceof GuiWaterKineticGenerator) {
         type = IKineticRotor.GearboxType.WATER;
      } else if (Minecraft.getInstance().gui.screen() instanceof GuiWindKineticGenerator) {
         type = IKineticRotor.GearboxType.WIND;
      }

      if (type != null) {
         tooltip.add(Localization.translate("ic2.itemrotor.fitsin." + this.isAcceptedType(stack, type)));
      }
   }

   @Override
   public int getDiameter(ItemStack stack) {
      return this.radius;
   }

   @Override
   public Identifier getRotorRenderTexture(ItemStack stack) {
      return this.renderTexture;
   }

   @Override
   public float getEfficiency(ItemStack stack) {
      return this.efficiency;
   }

   @Override
   public int getMinWindStrength(ItemStack stack) {
      return this.minWindStrength;
   }

   @Override
   public int getMaxWindStrength(ItemStack stack) {
      return this.maxWindStrength;
   }

   @Override
   public boolean isAcceptedType(ItemStack stack, IKineticRotor.GearboxType type) {
      return type == IKineticRotor.GearboxType.WIND || this.water;
   }
}
