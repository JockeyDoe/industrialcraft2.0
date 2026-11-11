package ic2.core.block.machine.tileentity;

import ic2.api.upgrade.IUpgradableBlock;
import ic2.api.upgrade.UpgradableProperty;
import ic2.core.ContainerBase;
import ic2.core.IHasGui;
import ic2.core.block.invslot.InvSlot;
import ic2.core.block.invslot.InvSlotUpgrade;
import ic2.core.block.machine.container.ContainerCropHarvester;
import ic2.core.block.machine.gui.GuiCropHarvester;
import ic2.core.crop.TileEntityCrop;
import ic2.core.profile.NotClassic;
import ic2.core.util.StackUtil;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@NotClassic
public class TileEntityCropHarvester extends TileEntityElectricMachine implements IHasGui, IUpgradableBlock {
   public final InvSlot contentSlot;
   public final InvSlotUpgrade upgradeSlot;
   public int scanX = -4;
   public int scanY = -1;
   public int scanZ = -4;

   public TileEntityCropHarvester() {
      super(10000, 1, false);
      this.contentSlot = new InvSlot(this, "content", InvSlot.Access.IO, 15);
      this.upgradeSlot = new InvSlotUpgrade(this, "upgrade", 4);
   }

   @Override
   protected void updateEntityServer() {
      super.updateEntityServer();
      this.upgradeSlot.tick();
      if (this.getLevel().getGameTime() % 10L == 0L && this.energy.getEnergy() >= 21.0) {
         this.scan();
      }
   }

   public void scan() {
      this.scanX++;
      if (this.scanX > 4) {
         this.scanX = -4;
         this.scanZ++;
         if (this.scanZ > 4) {
            this.scanZ = -4;
            this.scanY++;
            if (this.scanY > 1) {
               this.scanY = -1;
            }
         }
      }

      this.energy.useEnergy(1.0);
      Level world = this.getLevel();
      BlockEntity tileEntity = world.getBlockEntity(this.getBlockPos().offset(this.scanX, this.scanY, this.scanZ));
      if (tileEntity instanceof TileEntityCrop && !this.isInvFull()) {
         TileEntityCrop crop = (TileEntityCrop)tileEntity;
         if (crop.getCrop() != null) {
            List<ItemStack> drops = null;
            if (crop.getCurrentSize() == crop.getCrop().getOptimalHarvestSize(crop)) {
               drops = crop.performHarvest();
            } else if (crop.getCurrentSize() == crop.getCrop().getMaxSize()) {
               drops = crop.performHarvest();
            }

            if (drops != null) {
               drops.forEach(drop -> {
                  if (StackUtil.putInInventory(this, Direction.WEST, drop, true) == 0) {
                     StackUtil.dropAsEntity(world, this.getBlockPos(), drop);
                  } else {
                     StackUtil.putInInventory(this, Direction.WEST, drop, false);
                  }

                  this.energy.useEnergy(20.0);
               });
            }
         }
      }
   }

   private boolean isInvFull() {
      for (int i = 0; i < this.contentSlot.size(); i++) {
         ItemStack stack = this.contentSlot.get(i);
         if (StackUtil.isEmpty(stack) || StackUtil.getSize(stack) < Math.min(stack.getMaxStackSize(), this.contentSlot.getStackSizeLimit())) {
            return false;
         }
      }

      return true;
   }

   @Override
   public double getEnergy() {
      return this.energy.getEnergy();
   }

   @Override
   public boolean useEnergy(double amount) {
      return this.energy.useEnergy(amount);
   }

   @Override
   public Set<UpgradableProperty> getUpgradableProperties() {
      return EnumSet.of(UpgradableProperty.Transformer, UpgradableProperty.EnergyStorage, UpgradableProperty.ItemProducing);
   }

   @Override
   public ContainerBase<TileEntityCropHarvester> getGuiContainer(Player player) {
      return new ContainerCropHarvester(player, this);
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public Screen getGui(Player player, boolean isAdmin) {
      return new GuiCropHarvester(new ContainerCropHarvester(player, this));
   }

   @Override
   public void onGuiClosed(Player player) {
   }
}
