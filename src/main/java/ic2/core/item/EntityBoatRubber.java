package ic2.core.item;

import ic2.core.ref.ItemName;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class EntityBoatRubber extends EntityIC2Boat {
   public EntityBoatRubber(Level world) {
      super(world, "rubber_boat");
   }

   @Override
   protected ItemStack getItem() {
      return ItemName.boat.getItemStack(ItemIC2Boat.BoatType.rubber);
   }

   @Override
   protected ItemStack getBrokenItem() {
      this.playSound(SoundEvents.ITEM_PICKUP, 16.0F, 8.0F);
      return ItemName.boat.getItemStack(ItemIC2Boat.BoatType.broken_rubber);
   }

   @Override
   public String getTexture() {
      return "textures/models/boat_rubber.png";
   }
}
