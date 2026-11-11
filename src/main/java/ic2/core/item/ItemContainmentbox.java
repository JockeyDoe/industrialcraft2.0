package ic2.core.item;

import ic2.core.IC2;
import ic2.core.IHasGui;
import ic2.core.item.tool.ContainerContainmentbox;
import ic2.core.item.tool.HandHeldContainmentbox;
import ic2.core.profile.NotClassic;
import ic2.core.ref.ItemName;
import ic2.core.util.StackUtil;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@NotClassic
public class ItemContainmentbox extends ItemIC2 implements IHandHeldInventory {
   public ItemContainmentbox() {
      super(ItemName.containment_box);
      ItemDefaults.stackSize(this, 1);
   }

   public InteractionResult use(Level world, Player player, InteractionHand hand) {
      ItemStack stack = StackUtil.get(player, hand);
      if (!world.isClientSide()) {
         IC2.platform.launchGui(player, this.getInventory(player, stack));
      }

      return InteractionResult.SUCCESS.heldItemTransformedTo(stack);
   }

   public boolean onDroppedByPlayer(ItemStack stack, Player player) {
      if (!player.level().isClientSide() && !StackUtil.isEmpty(stack) && player.containerMenu instanceof ContainerContainmentbox) {
         HandHeldContainmentbox containmentBox = ((ContainerContainmentbox)player.containerMenu).base;
         if (containmentBox.isThisContainer(stack)) {
            containmentBox.saveAsThrown(stack);
            player.closeContainer();
         }
      }

      return true;
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public Rarity func_77613_e(ItemStack stack) {
      return Rarity.UNCOMMON;
   }

   @Override
   public IHasGui getInventory(Player player, ItemStack stack) {
      return new HandHeldContainmentbox(player, stack, 12);
   }
}
