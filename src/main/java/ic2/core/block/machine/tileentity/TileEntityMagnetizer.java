package ic2.core.block.machine.tileentity;

import ic2.api.upgrade.IUpgradableBlock;
import ic2.api.upgrade.UpgradableProperty;
import ic2.core.ContainerBase;
import ic2.core.IHasGui;
import ic2.core.block.comp.Redstone;
import ic2.core.block.invslot.InvSlotUpgrade;
import ic2.core.block.machine.container.ContainerMagnetizer;
import ic2.core.block.machine.gui.GuiMagnetizer;
import java.util.EnumSet;
import java.util.Set;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class TileEntityMagnetizer extends TileEntityElectricMachine implements IHasGui, IUpgradableBlock {
   public InvSlotUpgrade upgradeSlot = new InvSlotUpgrade(this, "upgrade", 4);
   public static final int defaultMaxEnergy = 100;
   public static final int defaultTier = 1;
   private static final double boostEnergy = 2.0;
   protected final Redstone redstone = this.addComponent(new Redstone(this));

   public TileEntityMagnetizer() {
      super(100, 1);
   }

   @Override
   public void setChanged() {
      super.setChanged();
      if (!this.getLevel().isClientSide()) {
         this.setOverclockRates();
      }
   }

   public void setOverclockRates() {
      this.upgradeSlot.onChanged();
      int tier = this.upgradeSlot.getTier(1);
      this.energy.setSinkTier(tier);
      this.dischargeSlot.setTier(tier);
      this.energy.setCapacity(this.upgradeSlot.getEnergyStorage(100, 0, 0));
   }

   private int distance() {
      return 20 + this.upgradeSlot.augmentation;
   }

   @Override
   public ContainerBase<?> getGuiContainer(Player player) {
      return new ContainerMagnetizer(player, this);
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public Screen getGui(Player player, boolean isAdmin) {
      return new GuiMagnetizer(new ContainerMagnetizer(player, this));
   }

   @Override
   public void onGuiClosed(Player player) {
   }

   @Override
   public double getEnergy() {
      return this.energy.getEnergy();
   }

   @Override
   public boolean useEnergy(double amount) {
      return this.energy.useEnergy(amount);
   }

   public boolean canBoost() {
      return this.energy.getEnergy() >= 2.0;
   }

   public void boost(double multiplier) {
      this.energy.useEnergy(2.0 * multiplier);
   }

   @Override
   public Set<UpgradableProperty> getUpgradableProperties() {
      return EnumSet.of(UpgradableProperty.Augmentable, UpgradableProperty.RedstoneSensitive, UpgradableProperty.Transformer, UpgradableProperty.EnergyStorage);
   }
}
