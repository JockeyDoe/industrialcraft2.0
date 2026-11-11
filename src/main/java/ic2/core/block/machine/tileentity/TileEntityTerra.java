package ic2.core.block.machine.tileentity;

import com.google.common.base.Predicate;
import ic2.api.item.ITerraformingBP;
import ic2.core.IC2;
import ic2.core.audio.AudioSource;
import ic2.core.audio.PositionSpec;
import ic2.core.block.invslot.InvSlotConsumableClass;
import ic2.core.util.Ic2BlockPos;
import ic2.core.util.StackUtil;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class TileEntityTerra extends TileEntityElectricMachine {
   public int failedAttempts = 0;
   private BlockPos lastPos;
   public AudioSource audioSource;
   public int inactiveTicks = 0;
   public final InvSlotConsumableClass tfbpSlot = new InvSlotConsumableClass(this, "tfbp", 1, ITerraformingBP.class);

   public TileEntityTerra() {
      super(100000, 4);
   }

   @Override
   protected void onUnloaded() {
      if (IC2.platform.isRendering() && this.audioSource != null) {
         IC2.audioManager.removeSources(this);
         this.audioSource = null;
      }

      super.onUnloaded();
   }

   @Override
   protected void updateEntityServer() {
      super.updateEntityServer();
      boolean newActive = false;
      ItemStack stack = this.tfbpSlot.get();
      if (!StackUtil.isEmpty(stack)) {
         ITerraformingBP tfbp = (ITerraformingBP)stack.getItem();
         if (this.energy.getEnergy() >= tfbp.getConsume(stack)) {
            newActive = true;
            Level world = this.getLevel();
            BlockPos nextPos;
            if (this.lastPos != null) {
               int range = tfbp.getRange(stack) / 10;
               nextPos = new BlockPos(
                  this.lastPos.getX() - world.getRandom().nextInt(range + 1) + world.getRandom().nextInt(range + 1),
                  this.getBlockPos().getY(),
                  this.lastPos.getZ() - world.getRandom().nextInt(range + 1) + world.getRandom().nextInt(range + 1)
               );
            } else {
               if (this.failedAttempts > 4) {
                  this.failedAttempts = 4;
               }

               int range = tfbp.getRange(stack) * (this.failedAttempts + 1) / 5;
               nextPos = new BlockPos(
                  this.getBlockPos().getX() - world.getRandom().nextInt(range + 1) + world.getRandom().nextInt(range + 1),
                  this.getBlockPos().getY(),
                  this.getBlockPos().getZ() - world.getRandom().nextInt(range + 1) + world.getRandom().nextInt(range + 1)
               );
            }

            if (tfbp.terraform(stack, world, nextPos)) {
               this.energy.useEnergy(tfbp.getConsume(stack));
               this.failedAttempts = 0;
               this.lastPos = nextPos;
            } else {
               this.energy.useEnergy(tfbp.getConsume(stack) / 10.0);
               this.failedAttempts++;
               this.lastPos = null;
            }
         }
      }

      if (newActive) {
         this.inactiveTicks = 0;
         this.setActive(true);
      } else if (!newActive && this.getActive() && this.inactiveTicks++ > 30) {
         this.setActive(false);
      }
   }

   @Override
   public boolean onActivated(final Player player, InteractionHand hand, Direction side, float hitX, float hitY, float hitZ) {
      final Level world = this.getLevel();
      if (!player.isShiftKeyDown() && !world.isClientSide()) {
         if (this.ejectBlueprint()) {
            return true;
         }

         ItemStack stack = StackUtil.consumeAndGet(player, hand, new Predicate<ItemStack>() {
            {
               Objects.requireNonNull(TileEntityTerra.this);
            }

            public boolean apply(ItemStack input) {
               Item item = input.getItem();
               return item instanceof ITerraformingBP && ((ITerraformingBP)item).canInsert(input, player, world, TileEntityTerra.this.getBlockPos());
            }
         }, 1);
         if (!StackUtil.isEmpty(stack)) {
            this.insertBlueprint(stack);
            return true;
         }
      }

      return true;
   }

   private boolean ejectBlueprint() {
      ItemStack stack = this.tfbpSlot.get();
      if (StackUtil.isEmpty(stack)) {
         return false;
      } else {
         StackUtil.dropAsEntity(this.getLevel(), this.getBlockPos(), stack);
         this.tfbpSlot.clear();
         return true;
      }
   }

   private void insertBlueprint(ItemStack tfbp) {
      if (!this.tfbpSlot.isEmpty()) {
         throw new IllegalStateException("not empty");
      } else {
         this.tfbpSlot.put(tfbp);
      }
   }

   public static BlockPos getFirstSolidBlockFrom(Level world, BlockPos pos, int yOffset) {
      Ic2BlockPos ret = new Ic2BlockPos(pos.getX(), pos.getY() + yOffset, pos.getZ());

      while (ret.getY() >= 0) {
         if (world.getBlockState(ret).isSolidRender()) {
            return ret.immutable();
         }

         ret.moveDown();
      }

      return null;
   }

   public static BlockPos getFirstBlockFrom(Level world, BlockPos pos, int yOffset) {
      MutableBlockPos ret = new MutableBlockPos(pos.getX(), pos.getY() + yOffset, pos.getZ());

      while (ret.getY() >= 0) {
         if (!world.isEmptyBlock(ret)) {
            return ret.immutable();
         }

         ret.set(ret.getX(), ret.getY() - 1, ret.getZ());
      }

      return null;
   }

   public static boolean switchGround(Level world, BlockPos pos, Block from, BlockState to, boolean upwards) {
      MutableBlockPos cPos = new MutableBlockPos(pos.getX(), pos.getY(), pos.getZ());

      while (cPos.getY() >= 0) {
         Block block = world.getBlockState(cPos).getBlock();
         if (upwards && block != from || !upwards && block == from) {
            break;
         }

         cPos.set(cPos.getX(), cPos.getY() - 1, cPos.getZ());
      }

      if ((!upwards || cPos.getY() != pos.getY()) && (upwards || cPos.getY() >= 0)) {
         world.setBlock(upwards ? cPos.above() : cPos.immutable(), to, 3);
         return true;
      } else {
         return false;
      }
   }

   @Override
   public void onNetworkUpdate(String field) {
      if (field.equals("active")) {
         if (this.audioSource == null) {
            this.audioSource = IC2.audioManager
               .createSource(this, PositionSpec.Center, "Terraformers/TerraformerGenericloop.ogg", true, false, IC2.audioManager.getDefaultVolume());
         }

         if (this.getActive()) {
            if (this.audioSource != null) {
               this.audioSource.play();
            }
         } else if (this.audioSource != null) {
            this.audioSource.stop();
         }
      }

      super.onNetworkUpdate(field);
   }
}
