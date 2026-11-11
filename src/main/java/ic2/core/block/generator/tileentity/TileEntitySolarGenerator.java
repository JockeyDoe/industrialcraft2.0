package ic2.core.block.generator.tileentity;

import ic2.api.crops.BiomeCategory;
import ic2.core.IC2;
import ic2.core.init.MainConfig;
import ic2.core.network.GuiSynced;
import ic2.core.util.BiomeUtil;
import ic2.core.util.ConfigUtil;
import ic2.core.util.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;

public class TileEntitySolarGenerator extends TileEntityBaseGenerator {
   @GuiSynced
   public float skyLight;
   private int ticker = IC2.random.nextInt(128);
   private static final int tickRate = 128;
   private static final double energyMultiplier = ConfigUtil.getDouble(MainConfig.get(), "balance/energy/generator/solar");

   public TileEntitySolarGenerator() {
      super(1.0, 1, 2);
   }

   @Override
   protected void onLoaded() {
      super.onLoaded();
      this.updateSunVisibility();
   }

   @Override
   public boolean gainEnergy() {
      if (++this.ticker % 128 == 0) {
         this.updateSunVisibility();
      }

      if (this.skyLight > 0.0F) {
         this.energy.addEnergy(energyMultiplier * this.skyLight);
         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean gainFuel() {
      return false;
   }

   public void updateSunVisibility() {
      this.skyLight = getSkyLight(this.getLevel(), this.getBlockPos().above());
   }

   public static float getSkyLight(Level world, BlockPos pos) {
      if (!world.dimensionType().hasSkyLight()) {
         return 0.0F;
      } else {
         float sunBrightness = Util.limit(
            (float)Math.cos((Float)world.environmentAttributes().getValue(EnvironmentAttributes.SUN_ANGLE, pos) * (float) (Math.PI / 180.0)) * 2.0F + 0.2F,
            0.0F,
            1.0F
         );
         if (!BiomeCategory.hasType(BiomeUtil.getBiome(world, pos), BiomeCategory.SANDY)) {
            sunBrightness *= 1.0F - world.getRainLevel(1.0F) * 5.0F / 16.0F;
            sunBrightness *= 1.0F - world.getThunderLevel(1.0F) * 5.0F / 16.0F;
            sunBrightness = Util.limit(sunBrightness, 0.0F, 1.0F);
         }

         return world.getBrightness(LightLayer.SKY, pos) / 15.0F * sunBrightness;
      }
   }

   @Override
   public boolean needsFuel() {
      return false;
   }

   @Override
   public boolean getGuiState(String name) {
      return "sunlight".equals(name) ? this.skyLight > 0.0F : super.getGuiState(name);
   }

   @Override
   protected boolean delayActiveUpdate() {
      return true;
   }
}
