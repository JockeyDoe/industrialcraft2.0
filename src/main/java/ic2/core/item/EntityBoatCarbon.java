package ic2.core.item;

import ic2.core.ref.ItemName;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class EntityBoatCarbon extends EntityIC2Boat {
   public EntityBoatCarbon(Level world) {
      super(world, "carbon_boat");
   }

   @Override
   protected ItemStack getItem() {
      return ItemName.boat.getItemStack(ItemIC2Boat.BoatType.carbon);
   }

   @Override
   public String getTexture() {
      return "textures/models/boat_carbon.png";
   }
}
