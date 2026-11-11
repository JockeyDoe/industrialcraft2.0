package ic2.core.block.personal;

import ic2.api.upgrade.IUpgradableBlock;
import ic2.api.upgrade.UpgradableProperty;
import ic2.core.ContainerBase;
import ic2.core.IHasGui;
import ic2.core.block.TileEntityInventory;
import ic2.core.block.invslot.InvSlotUpgrade;
import java.util.Collections;
import java.util.Set;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class TileEntityTradingTerminal extends TileEntityInventory implements IHasGui, IUpgradableBlock {
   protected int range;
   public final InvSlotUpgrade rangeUpgrade = new InvSlotUpgrade(this, "range", 1);

   public TileEntityTradingTerminal() {
      this.rangeUpgrade.setStackSizeLimit(16);
   }

   @Override
   protected void onLoaded() {
      super.onLoaded();
      this.range = this.rangeUpgrade.getRemoteRange(512);
   }

   @Override
   public void setChanged() {
      super.setChanged();
      if (!this.getLevel().isClientSide()) {
         this.range = this.rangeUpgrade.getRemoteRange(512);
      }
   }

   @Override
   protected void updateEntityServer() {
      super.updateEntityServer();
      this.rangeUpgrade.tick();
   }

   @Override
   public ContainerBase<?> getGuiContainer(Player player) {
      return new ContainerTradingTerminal(player, this);
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public Screen getGui(Player player, boolean isAdmin) {
      return new GuiTradingTerminal(new ContainerTradingTerminal(player, this));
   }

   @Override
   public void onGuiClosed(Player player) {
   }

   @Override
   public Set<UpgradableProperty> getUpgradableProperties() {
      return Collections.singleton(UpgradableProperty.RemotelyAccessible);
   }

   @Override
   public double getEnergy() {
      return 0.0;
   }

   @Override
   public boolean useEnergy(double amount) {
      return false;
   }
}
