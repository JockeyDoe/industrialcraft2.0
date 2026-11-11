package ic2.core.block.generator.tileentity;

import ic2.api.energy.EnergyNet;
import ic2.api.energy.tile.IEnergyAcceptor;
import ic2.api.energy.tile.IEnergySource;
import ic2.core.ContainerBase;
import ic2.core.IHasGui;
import ic2.core.block.TileEntityInventory;
import ic2.core.gui.dynamic.DynamicContainer;
import ic2.core.gui.dynamic.DynamicGui;
import ic2.core.gui.dynamic.GuiParser;
import ic2.core.network.GuiSynced;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public abstract class TileEntityConversionGenerator extends TileEntityInventory implements IHasGui, IEnergySource {
   private static final NumberFormat FORMAT = new DecimalFormat("#.#");
   @GuiSynced
   private double lastProduction;
   @GuiSynced
   private double maxProduction;
   private double production;
   private boolean registeredToEnet;

   @Override
   protected void updateEntityServer() {
      super.updateEntityServer();
      this.lastProduction = this.production;
      this.production = 0.0;
      this.setActive(this.maxProduction > 0.0);
   }

   @Override
   protected void onUnloaded() {
      super.onUnloaded();
      if (this.registeredToEnet && !this.getLevel().isClientSide()) {
         EnergyNet.instance.removeTile(this);
         this.registeredToEnet = false;
      }
   }

   @Override
   protected void onLoaded() {
      super.onLoaded();
      if (!this.registeredToEnet && !this.getLevel().isClientSide()) {
         EnergyNet.instance.addTile(this);
         this.registeredToEnet = true;
      }
   }

   public String getProduction() {
      return FORMAT.format(this.lastProduction);
   }

   public String getMaxProduction() {
      return FORMAT.format(this.maxProduction);
   }

   @Override
   public ContainerBase<TileEntityConversionGenerator> getGuiContainer(Player player) {
      return DynamicContainer.create(this, player, GuiParser.parse(this.teBlock));
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public Screen getGui(Player player, boolean isAdmin) {
      return DynamicGui.<TileEntityConversionGenerator>create(this, player, GuiParser.parse(this.teBlock));
   }

   @Override
   public void onGuiClosed(Player player) {
   }

   protected abstract int getEnergyAvailable();

   protected abstract void drawEnergyAvailable(int var1);

   protected abstract double getMultiplier();

   @Override
   public double getOfferedEnergy() {
      return this.maxProduction = this.getEnergyAvailable() * this.getMultiplier();
   }

   @Override
   public void drawEnergy(double amount) {
      this.production += amount;
      this.drawEnergyAvailable((int)Math.ceil(amount / this.getMultiplier()));
   }

   @Override
   public int getSourceTier() {
      return Math.max(EnergyNet.instance.getTierFromPower(this.maxProduction), 2);
   }

   @Override
   public boolean emitsEnergyTo(IEnergyAcceptor receiver, Direction side) {
      return side != this.getFacing();
   }
}
