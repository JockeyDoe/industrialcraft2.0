package ic2.core.item.tool;

import ic2.api.item.ElectricItem;
import ic2.core.IC2;
import ic2.core.IHitSoundOverride;
import ic2.core.ref.ItemName;
import ic2.core.util.ItemData;
import ic2.core.util.StackUtil;
import java.util.EnumSet;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.IShearable;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.EntityInteract;

public class ItemElectricToolChainsaw extends ItemElectricTool implements IHitSoundOverride {
   public ItemElectricToolChainsaw() {
      super(ItemName.chainsaw, 100, HarvestLevel.Iron, EnumSet.of(ToolClass.Axe, ToolClass.Sword, ToolClass.Shears));
      this.maxCharge = 30000;
      this.transferLimit = 100;
      this.tier = 1;
      this.efficiency = 12.0F;
      NeoForge.EVENT_BUS.register(this);
   }

   @Override
   public InteractionResult use(Level world, Player player, InteractionHand hand) {
      if (world.isClientSide()) {
         return super.use(world, player, hand);
      } else {
         if (IC2.keyboard.isModeSwitchKeyDown(player)) {
            CompoundTag compoundTag = StackUtil.getOrCreateNbtData(StackUtil.get(player, hand));
            if (compoundTag.getBooleanOr("disableShear", false)) {
               compoundTag.putBoolean("disableShear", false);
               ItemData.commit(StackUtil.get(player, hand), compoundTag);
               IC2.platform.messagePlayer(player, "ic2.tooltip.mode", "ic2.tooltip.mode.normal");
            } else {
               compoundTag.putBoolean("disableShear", true);
               ItemData.commit(StackUtil.get(player, hand), compoundTag);
               IC2.platform.messagePlayer(player, "ic2.tooltip.mode", "ic2.tooltip.mode.noShear");
            }
         }

         return super.use(world, player, hand);
      }
   }

   @Override
   public boolean func_77644_a(ItemStack itemstack, LivingEntity entityliving, LivingEntity attacker) {
      ElectricItem.manager.use(itemstack, this.operationEnergyCost, attacker);
      if (attacker instanceof Player && entityliving instanceof Creeper && entityliving.getHealth() <= 0.0F) {
         IC2.achievements.issueAchievement((Player)attacker, "killCreeperChainsaw");
      }

      return true;
   }

   @SubscribeEvent
   public void onEntityInteract(EntityInteract event) {
      if (IC2.platform.isSimulating()) {
         Entity entity = event.getTarget();
         Player player = event.getEntity();
         ItemStack itemstack = player.getInventory().getItem(player.getInventory().getSelectedSlot());
         if (itemstack != null
            && itemstack.getItem() == this
            && entity instanceof IShearable
            && !StackUtil.getOrCreateNbtData(itemstack).getBooleanOr("disableShear", false)
            && ElectricItem.manager.use(itemstack, this.operationEnergyCost, player)) {
            IShearable target = (IShearable)entity;
            Level world = entity.level();
            BlockPos pos = entity.blockPosition();
            if (target.isShearable(player, itemstack, world, pos)) {
               for (ItemStack stack : target.onSheared(player, itemstack, world, pos)) {
                  ItemEntity ent = entity.spawnAtLocation((ServerLevel)world, stack, 1.0F);
                  if (ent != null) {
                     ent.setDeltaMovement(
                        new Vec3(ent.getDeltaMovement().x, ent.getDeltaMovement().y + world.getRandom().nextFloat() * 0.05F, ent.getDeltaMovement().z)
                     );
                     ent.setDeltaMovement(
                        new Vec3(
                           ent.getDeltaMovement().x + (world.getRandom().nextFloat() - world.getRandom().nextFloat()) * 0.1F,
                           ent.getDeltaMovement().y,
                           ent.getDeltaMovement().z
                        )
                     );
                     ent.setDeltaMovement(
                        new Vec3(
                           ent.getDeltaMovement().x,
                           ent.getDeltaMovement().y,
                           ent.getDeltaMovement().z + (world.getRandom().nextFloat() - world.getRandom().nextFloat()) * 0.1F
                        )
                     );
                  }
               }
            }
         }
      }
   }

   public boolean onBlockStartBreak(ItemStack itemstack, BlockPos pos, Player player) {
      if (!IC2.platform.isSimulating()) {
         return false;
      } else if (StackUtil.getOrCreateNbtData(itemstack).getBooleanOr("disableShear", false)) {
         return false;
      } else {
         Level world = player.level();
         BlockState state = world.getBlockState(pos);
         Block block = state.getBlock();
         if (block instanceof IShearable target
            && target.isShearable(player, itemstack, world, pos)
            && ElectricItem.manager.use(itemstack, this.operationEnergyCost, player)) {
            for (ItemStack stack : target.onSheared(player, itemstack, world, pos)) {
               StackUtil.dropAsEntity(world, pos, stack);
            }

            player.awardStat(Stats.BLOCK_MINED.get(block), 1);
            world.setBlock(pos, Blocks.AIR.defaultBlockState(), 11);
            return true;
         } else {
            return false;
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public String getHitSoundForBlock(LocalPlayer player, Level world, BlockPos pos, ItemStack stack) {
      return null;
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public String getBreakSoundForBlock(LocalPlayer player, Level world, BlockPos pos, ItemStack stack) {
      return null;
   }

   @Override
   protected String getIdleSound(LivingEntity player, ItemStack stack) {
      return "Tools/Chainsaw/ChainsawIdle.ogg";
   }

   @Override
   protected String getStopSound(LivingEntity player, ItemStack stack) {
      return "Tools/Chainsaw/ChainsawStop.ogg";
   }
}
