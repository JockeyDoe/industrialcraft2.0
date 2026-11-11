package ic2.core.slot;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class SlotArmor extends Slot {
   private final EquipmentSlot armorType;

   public SlotArmor(Inventory inventory, EquipmentSlot armorType, int x, int y) {
      super(inventory, 36 + armorType.getIndex(), x, y);
      this.armorType = armorType;
   }

   public boolean mayPlace(ItemStack stack) {
      Item item = stack.getItem();
      return stack.canEquip(this.armorType, ((Inventory)this.container).player);
   }

   @OnlyIn(Dist.CLIENT)
   public String func_178171_c() {
      return "minecraft:container/slot/" + switch (this.armorType) {
         case HEAD -> "helmet";
         case CHEST -> "chestplate";
         case LEGS -> "leggings";
         case FEET -> "boots";
         default -> throw new IllegalStateException("Not an armor slot");
      };
   }
}
