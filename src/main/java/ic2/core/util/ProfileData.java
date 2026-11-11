package ic2.core.util;

import com.google.common.collect.ArrayListMultimap;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.util.ExtraCodecs;

public final class ProfileData {
   public static final UUID UNKNOWN = new UUID(0L, 0L);

   private ProfileData() {
   }

   public static GameProfile read(CompoundTag tag) {
      if (tag.contains("id")) {
         return (GameProfile)ExtraCodecs.STORED_GAME_PROFILE.codec().parse(NbtOps.INSTANCE, tag).getOrThrow();
      } else {
         UUID id = UNKNOWN;
         String stored = tag.getStringOr("Id", "");
         if (!stored.isEmpty()) {
            try {
               id = UUID.fromString(stored);
            } catch (IllegalArgumentException var11) {
            }
         }

         String name = tag.getStringOr("Name", "");
         ArrayListMultimap<String, Property> properties = ArrayListMultimap.create();
         CompoundTag grouped = tag.getCompoundOrEmpty("Properties");

         for (String key : grouped.keySet()) {
            for (Tag entry : grouped.getListOrEmpty(key)) {
               if (entry instanceof CompoundTag property) {
                  properties.put(key, new Property(key, property.getStringOr("Value", ""), (String)property.getString("Signature").orElse(null)));
               }
            }
         }

         return new GameProfile(id, name, new PropertyMap(properties));
      }
   }

   public static CompoundTag write(CompoundTag tag, GameProfile profile) {
      if (!profile.id().equals(UNKNOWN)) {
         tag.putString("Id", profile.id().toString());
      }

      if (!profile.name().isEmpty()) {
         tag.putString("Name", profile.name());
      }

      CompoundTag properties = new CompoundTag();

      for (String key : profile.properties().keySet()) {
         ListTag entries = new ListTag();

         for (Property property : profile.properties().get(key)) {
            CompoundTag value = new CompoundTag();
            value.putString("Value", property.value());
            if (property.hasSignature()) {
               value.putString("Signature", property.signature());
            }

            entries.add(value);
         }

         properties.put(key, entries);
      }

      if (!properties.isEmpty()) {
         tag.put("Properties", properties);
      }

      return tag;
   }

   public static boolean matches(GameProfile owner, GameProfile player) {
      return owner.id().equals(UNKNOWN) ? owner.name().equals(player.name()) : owner.id().equals(player.id());
   }
}
