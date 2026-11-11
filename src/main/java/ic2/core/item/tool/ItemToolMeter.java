package ic2.core.item.tool;

import ic2.api.energy.EnergyNet;
import ic2.api.energy.tile.IEnergyConductor;
import ic2.api.energy.tile.IEnergySink;
import ic2.api.energy.tile.IEnergySource;
import ic2.api.energy.tile.IEnergyTile;
import ic2.api.item.IBoxable;
import ic2.core.IC2;
import ic2.core.IHasGui;
import ic2.core.item.IHandHeldInventory;
import ic2.core.item.ItemDefaults;
import ic2.core.item.ItemIC2;
import ic2.core.ref.ItemName;
import ic2.core.util.StackUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class ItemToolMeter extends ItemIC2 implements IBoxable, IHandHeldInventory {
   public ItemToolMeter() {
      super(ItemName.meter);
      ItemDefaults.stackSize(this, 1);
      ItemDefaults.durability(this, 0);
   }

   public InteractionResult onItemUseFirst(Player player, Level world, BlockPos pos, Direction side, float hitX, float hitY, float hitZ, InteractionHand hand) {
      if (world.isClientSide()) {
         return InteractionResult.PASS;
      } else {
         IEnergyTile tile = EnergyNet.instance.getTile(world, pos);
         if (!(tile instanceof IEnergySource) && !(tile instanceof IEnergyConductor) && !(tile instanceof IEnergySink)) {
            IC2.platform.messagePlayer(player, "Not an energy net tile");
         } else if (IC2.platform.launchGui(player, this.getInventory(player, StackUtil.get(player, hand)))) {
            ContainerMeter container = (ContainerMeter)player.containerMenu;
            container.setUut(tile);
            return InteractionResult.SUCCESS;
         }

         return InteractionResult.SUCCESS;
      }
   }

   public boolean onDroppedByPlayer(ItemStack stack, Player player) {
      if (!player.level().isClientSide() && !StackUtil.isEmpty(stack) && player.containerMenu instanceof ContainerMeter) {
         HandHeldMeter euReader = ((ContainerMeter)player.containerMenu).base;
         if (euReader.isThisContainer(stack)) {
            euReader.saveAsThrown(stack);
            player.closeContainer();
         }
      }

      return true;
   }

   @Override
   public boolean canBeStoredInToolbox(ItemStack itemstack) {
      return true;
   }

   @Override
   public IHasGui getInventory(Player player, ItemStack stack) {
      return new HandHeldMeter(player, stack);
   }
}
