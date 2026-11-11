package ic2.core.item;

import ic2.api.item.ElectricItem;
import ic2.api.item.IBoxable;
import ic2.api.item.IItemHudInfo;
import ic2.core.ref.ItemName;
import ic2.core.util.StackUtil;
import ic2.core.util.Util;
import java.util.LinkedList;
import java.util.List;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class ItemBatterySU extends ItemIC2 implements IBoxable, IItemHudInfo {
   public int capacity;
   public int tier;

   public ItemBatterySU(ItemName internalName, int capacity1, int tier1) {
      super(internalName);
      this.capacity = capacity1;
      this.tier = tier1;
   }

   public InteractionResult use(Level world, Player player, InteractionHand hand) {
      ItemStack stack = StackUtil.get(player, hand);
      double energy = this.capacity;

      for (int i = 0; i < 9 && energy > 0.0; i++) {
         ItemStack target = (ItemStack)player.getInventory().getNonEquipmentItems().get(i);
         if (target != null && target != stack) {
            energy -= ElectricItem.manager.charge(target, energy, this.tier, true, false);
         }
      }

      if (!Util.isSimilar(energy, (double)this.capacity)) {
         stack = StackUtil.decSize(stack);
         return InteractionResult.SUCCESS.heldItemTransformedTo(stack);
      } else {
         return InteractionResult.PASS;
      }
   }

   @Override
   public boolean canBeStoredInToolbox(ItemStack itemstack) {
      return true;
   }

   @Override
   public List<String> getHudInfo(ItemStack stack, boolean advanced) {
      List<String> info = new LinkedList<>();
      info.add(this.capacity + " EU");
      return info;
   }
}
