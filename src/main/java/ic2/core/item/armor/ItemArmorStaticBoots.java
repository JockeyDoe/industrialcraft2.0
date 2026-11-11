package ic2.core.item.armor;

import ic2.api.item.ElectricItem;
import ic2.core.ref.ItemName;
import ic2.core.slot.ArmorSlot;
import ic2.core.util.ItemData;
import ic2.core.util.StackUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class ItemArmorStaticBoots extends ItemArmorUtility {
   public ItemArmorStaticBoots() {
      super(ItemName.static_boots, "rubber", EquipmentSlot.FEET);
   }

   public void onArmorTick(Level world, Player player, ItemStack stack) {
      if (!StackUtil.isEmpty(ArmorSlot.getEquipment(player.getInventory()).get(2))) {
         boolean ret = false;
         CompoundTag compound = StackUtil.getOrCreateNbtData(stack);
         boolean isNotWalking = player.getVehicle() != null || player.isInWater();
         if (!compound.contains("x") || isNotWalking) {
            compound.putInt("x", (int)player.getX());
            ItemData.commit(stack, compound);
         }

         if (!compound.contains("z") || isNotWalking) {
            compound.putInt("z", (int)player.getZ());
            ItemData.commit(stack, compound);
         }

         double distance = Math.sqrt(
            (compound.getIntOr("x", 0) - (int)player.getX()) * (compound.getIntOr("x", 0) - (int)player.getX())
               + (compound.getIntOr("z", 0) - (int)player.getZ()) * (compound.getIntOr("z", 0) - (int)player.getZ())
         );
         if (distance >= 5.0) {
            compound.putInt("x", (int)player.getX());
            ItemData.commit(stack, compound);
            compound.putInt("z", (int)player.getZ());
            ItemData.commit(stack, compound);
            ret = ElectricItem.manager
                  .charge(ArmorSlot.getEquipment(player.getInventory()).get(2), Math.min(3.0, distance / 5.0), Integer.MAX_VALUE, true, false)
               > 0.0;
         }

         if (ret) {
            player.inventoryMenu.broadcastChanges();
         }
      }
   }
}
