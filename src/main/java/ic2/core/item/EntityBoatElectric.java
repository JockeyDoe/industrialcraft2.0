package ic2.core.item;

import ic2.api.item.ElectricItem;
import ic2.core.IC2;
import ic2.core.ref.BlockMaterial;
import ic2.core.ref.ItemName;
import ic2.core.slot.ArmorSlot;
import ic2.core.util.StackUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class EntityBoatElectric extends EntityIC2Boat {
   private static final double euConsume = 4.0;
   private boolean accelerated = false;

   public EntityBoatElectric(Level world) {
      super(world, "electric_boat");
   }

   @Override
   protected ItemStack getItem() {
      return ItemName.boat.getItemStack(ItemIC2Boat.BoatType.electric);
   }

   @Override
   protected double getAccelerationFactor() {
      return this.accelerated ? 1.5 : 0.25;
   }

   @Override
   protected double getTopSpeed() {
      return 0.7;
   }

   @Override
   protected boolean isWater(BlockState block) {
      return BlockMaterial.of(block) == BlockMaterial.WATER || BlockMaterial.of(block) == BlockMaterial.LAVA;
   }

   @Override
   public String getTexture() {
      return "textures/models/boat_electric.png";
   }

   @Override
   public void tick() {
      this.clearFire();

      for (Entity e : this.getPassengers()) {
         e.clearFire();
      }

      this.accelerated = false;
      Entity driver = this.getControllingPassenger();
      if (driver instanceof Player && IC2.keyboard.isForwardKeyDown((Player)driver)) {
         for (ItemStack stack : ArmorSlot.getEquipment(((Player)driver).getInventory())) {
            if (!StackUtil.isEmpty(stack) && ElectricItem.manager.discharge(stack, 4.0, Integer.MAX_VALUE, true, true, true) == 4.0) {
               ElectricItem.manager.discharge(stack, 4.0, Integer.MAX_VALUE, true, true, false);
               this.accelerated = true;
               break;
            }
         }
      }

      super.tick();
   }
}
