package ic2.core.block.reactor.tileentity;

import com.google.common.base.Supplier;
import ic2.api.reactor.IReactorChamber;
import ic2.api.upgrade.IUpgradableBlock;
import ic2.api.upgrade.UpgradableProperty;
import ic2.core.ContainerBase;
import ic2.core.IHasGui;
import ic2.core.block.TileEntityInventory;
import ic2.core.block.comp.FluidReactorLookup;
import ic2.core.block.comp.Fluids;
import ic2.core.block.invslot.InvSlotUpgrade;
import ic2.core.gui.dynamic.DynamicContainer;
import ic2.core.gui.dynamic.DynamicGui;
import ic2.core.gui.dynamic.GuiParser;
import ic2.core.profile.NotClassic;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@NotClassic
public class TileEntityReactorFluidPort extends TileEntityInventory implements IHasGui, IUpgradableBlock, IReactorChamber {
   public final InvSlotUpgrade upgradeSlot = new InvSlotUpgrade(this, "upgrade", 1);
   private final FluidReactorLookup lookup = this.addComponent(new FluidReactorLookup(this));
   protected final Fluids fluids = this.addComponent(new Fluids(this));

   public TileEntityReactorFluidPort() {
      this.fluids.addUnmanagedTankHook(new Supplier<Collection<Fluids.InternalFluidTank>>() {
         {
            Objects.requireNonNull(TileEntityReactorFluidPort.this);
         }

         public Collection<Fluids.InternalFluidTank> get() {
            TileEntityNuclearReactorElectric reactor = TileEntityReactorFluidPort.this.getReactorInstance();
            return (Collection<Fluids.InternalFluidTank>)(reactor == null ? Collections.emptySet() : Arrays.asList(reactor.inputTank, reactor.outputTank));
         }
      });
   }

   @Override
   protected void updateEntityServer() {
      super.updateEntityServer();
      this.upgradeSlot.tick();
   }

   @Override
   public ContainerBase<TileEntityReactorFluidPort> getGuiContainer(Player player) {
      return DynamicContainer.create(this, player, GuiParser.parse(this.teBlock));
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public Screen getGui(Player player, boolean isAdmin) {
      return DynamicGui.<TileEntityReactorFluidPort>create(this, player, GuiParser.parse(this.teBlock));
   }

   @Override
   public void onGuiClosed(Player player) {
   }

   @Override
   public Set<UpgradableProperty> getUpgradableProperties() {
      return EnumSet.of(UpgradableProperty.FluidConsuming, UpgradableProperty.FluidProducing);
   }

   @Override
   public double getEnergy() {
      return 40.0;
   }

   @Override
   public boolean useEnergy(double amount) {
      return true;
   }

   public TileEntityNuclearReactorElectric getReactorInstance() {
      return this.lookup.getReactor();
   }

   @Override
   public boolean isWall() {
      return true;
   }
}
