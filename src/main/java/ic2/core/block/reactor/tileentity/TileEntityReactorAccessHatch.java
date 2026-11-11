package ic2.core.block.reactor.tileentity;

import ic2.core.profile.NotClassic;
import ic2.core.util.BlockCapabilities;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities.Item;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper;

@NotClassic
public class TileEntityReactorAccessHatch extends TileEntityReactorVessel implements Container {
   private ResourceHandler<ItemResource> itemHandler;

   @Override
   protected boolean onActivated(Player player, InteractionHand hand, Direction side, float hitX, float hitY, float hitZ) {
      TileEntityNuclearReactorElectric reactor = this.getReactorInstance();
      if (reactor != null) {
         Level world = this.getLevel();
         return reactor.getBlockType()
            .func_180639_a(world, reactor.getBlockPos(), world.getBlockState(reactor.getBlockPos()), player, hand, side, hitX, hitY, hitZ);
      } else {
         return false;
      }
   }

   public String getName() {
      TileEntityNuclearReactorElectric reactor = this.getReactorInstance();
      return reactor != null ? reactor.getName() : "<null>";
   }

   public boolean hasCustomName() {
      TileEntityNuclearReactorElectric reactor = this.getReactorInstance();
      return reactor != null ? reactor.hasCustomName() : false;
   }

   public Component getDisplayName() {
      TileEntityNuclearReactorElectric reactor = this.getReactorInstance();
      return (Component)(reactor != null ? reactor.getDisplayName() : Component.literal("<null>"));
   }

   public int getContainerSize() {
      TileEntityNuclearReactorElectric reactor = this.getReactorInstance();
      return reactor != null ? reactor.getContainerSize() : 0;
   }

   public boolean isEmpty() {
      TileEntityNuclearReactorElectric reactor = this.getReactorInstance();
      return reactor != null ? reactor.isEmpty() : true;
   }

   public ItemStack getItem(int index) {
      TileEntityNuclearReactorElectric reactor = this.getReactorInstance();
      return reactor != null ? reactor.getItem(index) : null;
   }

   public ItemStack removeItem(int index, int count) {
      TileEntityNuclearReactorElectric reactor = this.getReactorInstance();
      return reactor != null ? reactor.removeItem(index, count) : null;
   }

   public ItemStack removeItemNoUpdate(int index) {
      TileEntityNuclearReactorElectric reactor = this.getReactorInstance();
      return reactor != null ? reactor.removeItemNoUpdate(index) : null;
   }

   public void setItem(int index, ItemStack stack) {
      TileEntityNuclearReactorElectric reactor = this.getReactorInstance();
      if (reactor != null) {
         reactor.setItem(index, stack);
      }
   }

   public int getMaxStackSize() {
      TileEntityNuclearReactorElectric reactor = this.getReactorInstance();
      return reactor != null ? reactor.getMaxStackSize() : 0;
   }

   public boolean stillValid(Player player) {
      TileEntityNuclearReactorElectric reactor = this.getReactorInstance();
      return reactor != null ? reactor.stillValid(player) : false;
   }

   public void startOpen(Player player) {
      TileEntityNuclearReactorElectric reactor = this.getReactorInstance();
      if (reactor != null) {
         reactor.startOpen(player);
      }
   }

   public void stopOpen(Player player) {
      TileEntityNuclearReactorElectric reactor = this.getReactorInstance();
      if (reactor != null) {
         reactor.stopOpen(player);
      }
   }

   public boolean canPlaceItem(int index, ItemStack stack) {
      TileEntityNuclearReactorElectric reactor = this.getReactorInstance();
      return reactor != null ? reactor.canPlaceItem(index, stack) : false;
   }

   public int func_174887_a_(int id) {
      TileEntityNuclearReactorElectric reactor = this.getReactorInstance();
      return reactor != null ? reactor.func_174887_a_(id) : 0;
   }

   public void func_174885_b(int id, int value) {
      TileEntityNuclearReactorElectric reactor = this.getReactorInstance();
      if (reactor != null) {
         reactor.func_174885_b(id, value);
      }
   }

   public int func_174890_g() {
      TileEntityNuclearReactorElectric reactor = this.getReactorInstance();
      return reactor != null ? reactor.func_174890_g() : 0;
   }

   public void clearContent() {
      TileEntityNuclearReactorElectric reactor = this.getReactorInstance();
      if (reactor != null) {
         reactor.clearContent();
      }
   }

   @Override
   public boolean hasCapability(BlockCapability<?, Direction> capability, Direction facing) {
      return capability == Item.BLOCK || super.hasCapability(capability, facing);
   }

   @Override
   public <T> T getCapability(BlockCapability<T, Direction> capability, Direction facing) {
      if (capability == Item.BLOCK) {
         if (this.itemHandler == null) {
            this.itemHandler = VanillaContainerWrapper.of(this);
         }

         return BlockCapabilities.cast(Item.BLOCK, this.itemHandler);
      } else {
         return super.getCapability(capability, facing);
      }
   }
}
