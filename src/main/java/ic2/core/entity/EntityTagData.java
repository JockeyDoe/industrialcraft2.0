package ic2.core.entity;

import com.mojang.serialization.Codec;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class EntityTagData {
   private EntityTagData() {
   }

   public static void write(ValueOutput output, CompoundTag tag) {
      output.store(tag);
   }

   public static CompoundTag read(ValueInput input) {
      CompoundTag tag = new CompoundTag();

      for (String key : input.keySet()) {
         input.read(key, Codec.PASSTHROUGH).ifPresent(value -> {
            if (value.convert(NbtOps.INSTANCE).getValue() instanceof Tag entry) {
               tag.put(key, entry);
            }
         });
      }

      return tag;
   }
}
