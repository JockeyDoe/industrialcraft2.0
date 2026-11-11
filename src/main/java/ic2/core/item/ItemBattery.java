package ic2.core.item;

import ic2.api.item.ElectricItem;
import ic2.core.model.ModelKey;
import ic2.core.model.ModelRegistry;
import ic2.core.ref.ItemName;
import ic2.core.util.ItemStacks;
import ic2.core.util.StackUtil;
import ic2.core.util.Util;
import java.util.Objects;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class ItemBattery extends BaseElectricItem {
   private static int maxLevel = 4;

   public ItemBattery(ItemName name, double maxCharge, double transferLimit, int tier) {
      super(name, maxCharge, transferLimit, tier);
      ItemDefaults.stackSize(this, 16);
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public void registerModels(final ItemName name) {
      ModelRegistry.setCustomMeshDefinition(this, new ModelRegistry.ItemMeshDefinition() {
         {
            Objects.requireNonNull(ItemBattery.this);
         }

         @Override
         public ModelKey func_178113_a(ItemStack stack) {
            int damage = ItemStacks.rawMeta(stack);
            int maxDamage = stack.getMaxDamage() - 1;
            int level;
            if (maxDamage > 0) {
               level = Util.limit((damage * ItemBattery.maxLevel + maxDamage / 2) / maxDamage, 0, ItemBattery.maxLevel);
            } else {
               level = 0;
            }

            return ItemIC2.getModelLocation(name, Integer.toString(ItemBattery.maxLevel - level));
         }
      });

      for (int level = 0; level <= maxLevel; level++) {
         ModelRegistry.registerItemVariants(this, new ModelKey[]{getModelLocation(name, Integer.toString(level))});
      }
   }

   @Override
   public boolean canProvideEnergy(ItemStack stack) {
      return true;
   }

   public InteractionResult use(Level world, Player player, InteractionHand hand) {
      ItemStack stack = StackUtil.get(player, hand);
      if (!world.isClientSide() && StackUtil.getSize(stack) == 1) {
         if (ElectricItem.manager.getCharge(stack) > 0.0) {
            boolean transferred = false;

            for (int i = 0; i < 9; i++) {
               ItemStack target = (ItemStack)player.getInventory().getNonEquipmentItems().get(i);
               if (target != null
                  && target != stack
                  && !(ElectricItem.manager.discharge(target, Double.POSITIVE_INFINITY, Integer.MAX_VALUE, true, true, true) > 0.0)) {
                  double transfer = ElectricItem.manager.discharge(stack, 2.0 * this.transferLimit, Integer.MAX_VALUE, true, true, true);
                  if (!(transfer <= 0.0)) {
                     transfer = ElectricItem.manager.charge(target, transfer, this.tier, true, false);
                     if (!(transfer <= 0.0)) {
                        ElectricItem.manager.discharge(stack, transfer, Integer.MAX_VALUE, true, true, false);
                        transferred = true;
                     }
                  }
               }
            }

            if (transferred && !world.isClientSide()) {
               player.containerMenu.broadcastChanges();
            }
         }

         return InteractionResult.SUCCESS.heldItemTransformedTo(stack);
      } else {
         return InteractionResult.PASS;
      }
   }
}
