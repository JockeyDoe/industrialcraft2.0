package ic2.core.block.state;

import ic2.core.block.ITeBlock;
import ic2.core.ref.BlockMaterial;
import ic2.core.ref.IC2Material;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import net.minecraft.world.level.block.state.properties.Property;

public class MaterialProperty extends Property<MaterialProperty.WrappedMaterial> implements ISkippableProperty {
   private final int length;
   private final List<MaterialProperty.WrappedMaterial> values;

   public MaterialProperty(Collection<BlockMaterial> materials) {
      super("material", MaterialProperty.WrappedMaterial.class);
      this.values = new ArrayList<>(materials.size());

      for (BlockMaterial material : materials) {
         this.values.add(MaterialProperty.WrappedMaterial.get(material));
      }

      this.length = this.values.size();
   }

   public List<MaterialProperty.WrappedMaterial> getPossibleValues() {
      return this.values;
   }

   public int getInternalIndex(MaterialProperty.WrappedMaterial value) {
      return this.values.indexOf(value);
   }

   public Optional<MaterialProperty.WrappedMaterial> getValue(String value) {
      for (MaterialProperty.WrappedMaterial material : MaterialProperty.WrappedMaterial.MATERIAL_TO_WRAP.values()) {
         if (material.getName().equals(value)) {
            return Optional.of(material);
         }
      }

      return Optional.empty();
   }

   public String getName(MaterialProperty.WrappedMaterial value) {
      return value.getName();
   }

   public int getId(MaterialProperty.WrappedMaterial material) {
      assert this.values.contains(material);

      return this.values.indexOf(material);
   }

   public MaterialProperty.WrappedMaterial getMaterial(int ID) {
      assert ID >= 0 && ID < this.length;

      return this.values.get(ID % this.length);
   }

   public static final class WrappedMaterial implements Comparable<MaterialProperty.WrappedMaterial> {
      private final int id;
      private final String name;
      private final BlockMaterial material;
      private static int nextId;
      private static final Map<BlockMaterial, MaterialProperty.WrappedMaterial> MATERIAL_TO_WRAP = new HashMap<>();

      private WrappedMaterial(BlockMaterial material, String name) {
         if (material instanceof IC2Material) {
            name = ((IC2Material)material).name;
         }

         this.material = material;
         this.name = name.toLowerCase(Locale.ENGLISH);
         this.id = nextId++;
      }

      public BlockMaterial getMaterial() {
         return this.material;
      }

      public String getName() {
         return this.name;
      }

      public int compareTo(MaterialProperty.WrappedMaterial other) {
         return this.id - other.id;
      }

      public static MaterialProperty.WrappedMaterial get(BlockMaterial material) {
         MaterialProperty.WrappedMaterial ret = MATERIAL_TO_WRAP.get(material);
         if (ret == null) {
            ret = new MaterialProperty.WrappedMaterial(material, material.getClass().getName());
            MATERIAL_TO_WRAP.put(material, ret);
         }

         return ret;
      }

      public static boolean check(MaterialProperty.WrappedMaterial state, ITeBlock teBlock) {
         return teBlock.getMaterial() == state.getMaterial();
      }

      static {
         try {
            for (Field field : BlockMaterial.class.getFields()) {
               if (field.getType() == BlockMaterial.class) {
                  BlockMaterial material = (BlockMaterial)field.get(null);
                  MATERIAL_TO_WRAP.put(material, new MaterialProperty.WrappedMaterial(material, field.getName()));
               }
            }

            assert !MATERIAL_TO_WRAP.isEmpty();
         } catch (Exception var5) {
            throw new RuntimeException("Error building materials name map", var5);
         }
      }
   }
}
