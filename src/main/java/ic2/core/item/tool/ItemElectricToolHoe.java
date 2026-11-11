package ic2.core.item.tool;

import ic2.api.item.ElectricItem;
import ic2.core.IC2;
import ic2.core.ref.ItemName;
import ic2.core.util.StackUtil;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.ItemAbility;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent.BlockToolModificationEvent;

public class ItemElectricToolHoe extends ItemElectricTool {
   public ItemElectricToolHoe() {
      super(ItemName.electric_hoe, 50, HarvestLevel.Iron, EnumSet.of(ToolClass.Hoe));
      this.maxCharge = 10000;
      this.transferLimit = 100;
      this.tier = 1;
      this.efficiency = 16.0F;
   }

   public boolean onBlockStartBreak(ItemStack stack, BlockPos pos, Player player) {
      ElectricItem.manager.use(stack, this.operationEnergyCost, player);
      return false;
   }

   @Override
   public InteractionResult func_180614_a(Player player, Level world, BlockPos pos, InteractionHand hand, Direction side, float hitX, float hitY, float hitZ) {
      ItemStack stack = StackUtil.get(player, hand);
      if (!player.mayUseItemAt(pos, side, stack)) {
         return InteractionResult.PASS;
      } else if (!ElectricItem.manager.canUse(stack, this.operationEnergyCost)) {
         return InteractionResult.PASS;
      } else {
         BlockToolModificationEvent modification = (BlockToolModificationEvent)NeoForge.EVENT_BUS
            .post(
               new BlockToolModificationEvent(
                  world.getBlockState(pos),
                  new UseOnContext(player, hand, new BlockHitResult(new Vec3(pos.getX() + hitX, pos.getY() + hitY, pos.getZ() + hitZ), side, pos, false)),
                  ItemAbility.get("hoe_till"),
                  false
               )
            );
         if (modification.isCanceled()) {
            return InteractionResult.PASS;
         } else {
            BlockState state = world.getBlockState(pos);
            Block block = state.getBlock();
            if (side != Direction.DOWN && world.isEmptyBlock(pos.above()) && (block == Blocks.MYCELIUM || block == Blocks.GRASS_BLOCK || block == Blocks.DIRT)) {
               block = Blocks.FARMLAND;
               SoundType stepSound = block.getSoundType(state, world, pos, player);
               world.playSound(
                  null,
                  pos.getX() + 0.5,
                  pos.getY() + 0.5,
                  pos.getZ() + 0.5,
                  stepSound.getStepSound(),
                  SoundSource.BLOCKS,
                  (stepSound.getVolume() + 1.0F) / 2.0F,
                  stepSound.getPitch() * 0.8F
               );
               if (IC2.platform.isSimulating()) {
                  world.setBlock(pos, modification.getFinalState() == state ? block.defaultBlockState() : modification.getFinalState(), 3);
                  ElectricItem.manager.use(stack, this.operationEnergyCost, player);
               }

               return InteractionResult.SUCCESS;
            } else {
               return super.func_180614_a(player, world, pos, hand, side, hitX, hitY, hitZ);
            }
         }
      }
   }
}
