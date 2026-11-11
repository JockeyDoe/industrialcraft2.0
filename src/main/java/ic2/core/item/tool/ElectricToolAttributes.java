package ic2.core.item.tool;

import ic2.api.item.ElectricItem;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;

@EventBusSubscriber(modid = "ic2")
public final class ElectricToolAttributes {
   private ElectricToolAttributes() {
   }

   @SubscribeEvent
   public static void modify(ItemAttributeModifierEvent event) {
      ItemStack stack = event.getItemStack();
      Double damage = null;
      if (stack.getItem() instanceof ItemElectricToolChainsaw saw) {
         damage = ElectricItem.manager.canUse(stack, saw.operationEnergyCost) ? 9.0 : Double.NaN;
      } else if (stack.getItem() instanceof ItemNanoSaber) {
         damage = ItemNanoSaber.isActive(stack) && ElectricItem.manager.canUse(stack, 400.0) ? 20.0 : 4.0;
      }

      if (damage != null) {
         event.removeAllModifiersFor(Attributes.ATTACK_DAMAGE);
         event.removeAllModifiersFor(Attributes.ATTACK_SPEED);
         if (!Double.isNaN(damage)) {
            event.addModifier(
               Attributes.ATTACK_DAMAGE, new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, damage, Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND
            );
            event.addModifier(Attributes.ATTACK_SPEED, new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, -3.0, Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND);
         }
      }
   }
}
