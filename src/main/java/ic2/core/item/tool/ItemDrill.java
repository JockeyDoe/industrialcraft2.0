package ic2.core.item.tool;

import ic2.api.item.IMiningDrill;
import ic2.core.IC2;
import ic2.core.IHitSoundOverride;
import ic2.core.ref.ItemName;
import ic2.core.util.StackUtil;
import java.util.EnumSet;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

public class ItemDrill extends ItemElectricTool implements IMiningDrill, IHitSoundOverride {
   public ItemDrill(ItemName name, int operationEnergyCost, HarvestLevel harvestLevel, int maxCharge, int transferLimit, int tier, float efficiency) {
      super(name, operationEnergyCost, harvestLevel, EnumSet.of(ToolClass.Pickaxe, ToolClass.Shovel));
      this.maxCharge = maxCharge;
      this.transferLimit = transferLimit;
      this.tier = tier;
      this.efficiency = efficiency;
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public String getHitSoundForBlock(LocalPlayer player, Level world, BlockPos pos, ItemStack stack) {
      return null;
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public String getBreakSoundForBlock(LocalPlayer player, Level world, BlockPos pos, ItemStack stack) {
      if (player.getAbilities().instabuild) {
         return null;
      } else {
         BlockState state = world.getBlockState(pos);
         float hardness = state.getDestroySpeed(world, pos);
         return !(hardness > 1.0F) && !(hardness < 0.0F) ? "Tools/Drill/DrillSoft.ogg" : "Tools/Drill/DrillHard.ogg";
      }
   }

   @Override
   public float getDestroySpeed(ItemStack stack, BlockState state) {
      float speed = super.getDestroySpeed(stack, state);
      Player player = getPlayerHoldingItem(stack);
      if (player != null) {
         if (player.isEyeInFluid(FluidTags.WATER) && player.getAttributeValue(Attributes.SUBMERGED_MINING_SPEED) < 1.0) {
            speed *= 5.0F;
         }

         if (!player.onGround()) {
            speed *= 5.0F;
         }
      }

      return speed;
   }

   private static Player getPlayerHoldingItem(ItemStack stack) {
      if (IC2.platform.isRendering()) {
         Player player = IC2.platform.getPlayerInstance();
         if (player != null && player.getInventory().getSelectedItem() == stack) {
            return player;
         }
      } else {
         for (Player player : ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()) {
            if (player.getInventory().getSelectedItem() == stack) {
               return player;
            }
         }
      }

      return null;
   }

   @Override
   public int energyUse(ItemStack stack, Level world, BlockPos pos, BlockState state) {
      if (stack.getItem() == ItemName.drill.getInstance()) {
         return 6;
      } else if (stack.getItem() == ItemName.diamond_drill.getInstance()) {
         return 20;
      } else if (stack.getItem() == ItemName.iridium_drill.getInstance()) {
         return 200;
      } else {
         throw new IllegalArgumentException("Invalid drill: " + StackUtil.toStringSafe(stack));
      }
   }

   @Override
   public int breakTime(ItemStack stack, Level world, BlockPos pos, BlockState state) {
      if (stack.getItem() == ItemName.drill.getInstance()) {
         return 200;
      } else if (stack.getItem() == ItemName.diamond_drill.getInstance()) {
         return 50;
      } else if (stack.getItem() == ItemName.iridium_drill.getInstance()) {
         return 20;
      } else {
         throw new IllegalArgumentException("Invalid drill: " + StackUtil.toStringSafe(stack));
      }
   }

   @Override
   public boolean breakBlock(ItemStack stack, Level world, BlockPos pos, BlockState state) {
      if (stack.getItem() == ItemName.drill.getInstance()) {
         return this.tryUsePower(stack, 50.0);
      } else if (stack.getItem() == ItemName.diamond_drill.getInstance()) {
         return this.tryUsePower(stack, 80.0);
      } else if (stack.getItem() == ItemName.iridium_drill.getInstance()) {
         return this.tryUsePower(stack, 800.0);
      } else {
         throw new IllegalArgumentException("Invalid drill: " + StackUtil.toStringSafe(stack));
      }
   }
}
