package ic2.core.model;

import ic2.core.Ic2Fluid;
import ic2.core.block.BlockIC2Fluid;
import ic2.core.ref.FluidName;
import ic2.core.util.Util;
import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.block.FluidModel.Unbaked;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.environment.FogEnvironment;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterFluidModelsEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.fluid.FluidTintSource;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import org.joml.Vector4f;

@EventBusSubscriber(modid = "ic2", value = Dist.CLIENT)
public final class FluidRendering {
   private static final Map<Ic2Fluid, FluidName> fluids = new IdentityHashMap<>();

   public static void add(Ic2Fluid fluid, FluidName name) {
      fluids.put(fluid, name);
   }

   public static TextureAtlasSprite still(FluidStack stack) {
      return Minecraft.getInstance().getModelManager().getFluidStateModelSet().get(stack.getFluid().defaultFluidState()).stillMaterial().sprite();
   }

   public static int color(FluidStack stack) {
      FluidState state = stack.getFluid().defaultFluidState();
      FluidModel model = Minecraft.getInstance().getModelManager().getFluidStateModelSet().get(state);
      return color(model, state, -1);
   }

   public static int color(FluidModel model, FluidState state, int fallback) {
      FluidTintSource source = model.fluidTintSource();
      return source == null ? fallback : source.color(state);
   }

   @SubscribeEvent
   public static void models(RegisterFluidModelsEvent event) {
      for (FluidName name : FluidName.values()) {
         if (name.hasInstance() && name.getInstance() instanceof Ic2Fluid fluid) {
            int tint = 0xFF000000 | ((BlockIC2Fluid)fluid.getBlock()).getColor();
            event.register(
               new Unbaked(new Material(name.getTextureLocation(false)), new Material(name.getTextureLocation(true)), null, state -> tint),
               fluid,
               fluid.getFlowing()
            );
         }
      }
   }

   @SubscribeEvent
   public static void register(RegisterClientExtensionsEvent event) {
      for (FluidName name : FluidName.values()) {
         if (name.hasInstance() && name.getInstance() instanceof Ic2Fluid fluid) {
            event.registerFluidType(new IClientFluidTypeExtensions() {
               public void modifyFogColor(Camera camera, float partial, ClientLevel world, int distance, float darken, Vector4f color) {
                  int tint = ((BlockIC2Fluid)fluid.getBlock()).getColor();
                  color.set((tint >>> 16 & 0xFF) / 255.0F, (tint >>> 8 & 0xFF) / 255.0F, (tint & 0xFF) / 255.0F, color.w);
               }

               public void modifyFogRender(Camera camera, FogEnvironment environment, float distance, float partial, FogData fog) {
                  float density = (float)Util.map(Math.abs(fluid.getFluidType().getDensity()), 20000.0, 2.0);
                  fog.environmentalStart = 0.0F;
                  fog.environmentalEnd = density > 0.0F ? 1.0F / density : distance;
               }
            }, new FluidType[]{fluid.getFluidType()});
         }
      }
   }
}
