package ic2.core.model;

import ic2.core.block.state.ISkippableProperty;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.Property.Value;

public class ModelUtil {
   private static String variant(BlockState state, boolean skipInternal) {
      StringBuilder result = new StringBuilder();

      for (Value<?> entry : state.getValues().toList()) {
         Property<?> property = entry.property();
         if (!skipInternal || !(property instanceof ISkippableProperty)) {
            if (!result.isEmpty()) {
               result.append(',');
            }

            result.append(property.getName()).append('=').append(propertyValue(property, entry.value()));
         }
      }

      return result.isEmpty() ? "normal" : result.toString();
   }

   private static String propertyValue(Property property, Comparable value) {
      return property.getName(value);
   }

   public static ModelKey getModelLocation(Identifier loc, BlockState state) {
      return new ModelKey(loc, getVariant(state));
   }

   public static ModelKey getTEBlockModelLocation(Identifier loc, BlockState state) {
      return new ModelKey(loc, variant(state, true));
   }

   public static String getVariant(BlockState state) {
      return variant(state, false);
   }

   public static Ic2Geometry getMissingModel() {
      return new NativeGeometry(getModelManager().getBlockStateModelSet().get(Blocks.AIR.defaultBlockState()));
   }

   public static Ic2Geometry getModel(ModelKey loc) {
      return ModelRegistry.get(loc);
   }

   public static Ic2Geometry getBlockModel(BlockState state) {
      return new NativeGeometry(getModelManager().getBlockStateModelSet().get(state));
   }

   private static ModelManager getModelManager() {
      return Minecraft.getInstance().getModelManager();
   }
}
