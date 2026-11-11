package ic2.core.util;

import ic2.core.IC2;
import java.util.Optional;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

public final class ItemStacks {
   public static final String VARIANT = "ic2:variant";

   private ItemStacks() {
   }

   public static Provider registries() {
      MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
      if (server != null) {
         return server.registryAccess();
      } else {
         Player player = IC2.platform == null ? null : IC2.platform.getPlayerInstance();
         return (Provider)(player != null ? player.level().registryAccess() : RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY));
      }
   }

   public static ItemStack read(CompoundTag tag) {
      RegistryOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, registries());
      if (!tag.contains("count") && !tag.contains("components")) {
         String id = tag.getStringOr("id", "");
         Identifier identifier = Identifier.tryParse(id);
         if (identifier == null) {
            return ItemStack.EMPTY;
         } else {
            int meta = tag.getShortOr("Damage", (short)0);
            Item variant = VanillaVariants.legacyItem(id, meta);
            Optional<Item> item = variant == null ? BuiltInRegistries.ITEM.getOptional(identifier) : Optional.of(variant);
            if (item.isEmpty()) {
               return ItemStack.EMPTY;
            } else {
               ItemStack stack = new ItemStack((ItemLike)item.get(), Math.max(0, tag.getByteOr("Count", (byte)0)));
               ItemData.store(stack, tag.getCompoundOrEmpty("tag"));
               if (variant == null) {
                  setRawMeta(stack, meta);
               }

               return stack;
            }
         }
      } else {
         return (ItemStack)ItemStack.OPTIONAL_CODEC.parse(ops, tag).getOrThrow();
      }
   }

   public static CompoundTag write(ItemStack stack, CompoundTag destination) {
      RegistryOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, registries());
      CompoundTag encoded = (CompoundTag)ItemStack.OPTIONAL_CODEC.encodeStart(ops, stack).getOrThrow();
      destination.merge(encoded);
      return destination;
   }

   public static ItemStack create(ItemLike item, int count, int meta) {
      ItemStack result = new ItemStack(item, count);
      setRawMeta(result, meta);
      return result;
   }

   public static ItemStack craftingRemainder(ItemStack input) {
      ItemStackTemplate result = input.getCraftingRemainder();
      return result == null ? ItemStack.EMPTY : result.create();
   }

   public static EquipmentSlot equipmentSlot(ItemStack stack) {
      Equippable equippable = (Equippable)stack.get(DataComponents.EQUIPPABLE);
      return equippable == null ? EquipmentSlot.MAINHAND : equippable.slot();
   }

   public static int rawMeta(ItemStack stack) {
      CompoundTag data = ItemData.read(stack);
      return data != null && data.contains("ic2:variant") ? data.getIntOr("ic2:variant", 0) : stack.getDamageValue();
   }

   public static void setRawMeta(ItemStack stack, int meta) {
      if (meta < 0) {
         throw new IllegalArgumentException("Negative metadata");
      } else {
         CompoundTag data = ItemData.read(stack);
         if (stack.isDamageableItem() && (data == null || !data.contains("ic2:variant"))) {
            stack.setDamageValue(meta);
         } else {
            ItemData.put(stack, "ic2:variant", IntTag.valueOf(meta));
         }
      }
   }
}
