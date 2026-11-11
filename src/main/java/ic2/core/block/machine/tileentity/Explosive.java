package ic2.core.block.machine.tileentity;

import ic2.core.block.EntityIC2Explosive;
import ic2.core.block.TileEntityInventory;
import ic2.core.block.comp.Redstone;
import ic2.core.util.StackUtil;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;

public abstract class Explosive extends TileEntityInventory implements Redstone.IRedstoneChangeHandler {
   protected final Redstone redstone = this.addComponent(new Redstone(this));
   private boolean exploded;

   protected Explosive() {
      this.redstone.subscribe(this);
   }

   @Override
   protected SoundType getBlockSound(Entity entity) {
      return SoundType.GRASS;
   }

   @Override
   public void onRedstoneChange(int newLevel) {
      if (newLevel > 0) {
         this.explode(null, false);
      }
   }

   @Override
   protected boolean onActivated(Player player, InteractionHand hand, Direction side, float hitX, float hitY, float hitZ) {
      if (!StackUtil.consume(player, hand, StackUtil.sameItem(Items.FIRE_CHARGE), 1)
         && !StackUtil.damage(player, hand, StackUtil.sameItem(Items.FLINT_AND_STEEL), 1)) {
         return super.onActivated(player, hand, side, hitX, hitY, hitZ);
      } else {
         this.explode(player, false);
         return true;
      }
   }

   @Override
   protected void onExploded(Explosion explosion) {
      super.onExploded(explosion);
      this.explode(explosion.getIndirectSourceEntity(), true);
   }

   @Override
   protected boolean onRemovedByPlayer(Player player, boolean willHarvest) {
      if (this.explodeOnRemoval()) {
         this.explode(player, false);
         return true;
      } else {
         return super.onRemovedByPlayer(player, willHarvest);
      }
   }

   @Override
   protected void onEntityCollision(Entity entity) {
      if (!this.getLevel().isClientSide() && entity instanceof AbstractArrow && entity.isOnFire()) {
         AbstractArrow arrow = (AbstractArrow)entity;
         this.explode(arrow.getOwner() instanceof LivingEntity ? (LivingEntity)arrow.getOwner() : null, false);
      }
   }

   @Override
   protected ItemStack adjustDrop(ItemStack drop, boolean wrench) {
      return this.exploded ? null : super.adjustDrop(drop, wrench);
   }

   protected boolean explode(LivingEntity igniter, boolean shortFuse) {
      EntityIC2Explosive entity = this.getEntity(igniter);
      if (entity == null) {
         return false;
      } else {
         Level world = this.getLevel();
         if (world.isClientSide()) {
            return true;
         } else {
            entity.setIgniter(igniter);
            this.onIgnite(igniter);
            world.removeBlock(this.getBlockPos(), false);
            if (shortFuse) {
               entity.fuse = world.getRandom().nextInt(Math.max(1, entity.fuse / 4)) + entity.fuse / 8;
            }

            world.addFreshEntity(entity);
            world.playSound((Player)null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.TNT_PRIMED, SoundSource.BLOCKS, 1.0F, 1.0F);
            this.exploded = true;
            return true;
         }
      }
   }

   protected boolean explodeOnRemoval() {
      return false;
   }

   protected abstract EntityIC2Explosive getEntity(LivingEntity var1);

   protected void onIgnite(LivingEntity igniter) {
   }
}
