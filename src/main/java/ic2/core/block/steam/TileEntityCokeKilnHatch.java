package ic2.core.block.steam;

import ic2.core.ContainerBase;
import ic2.core.IHasGui;
import ic2.core.block.TileEntityInventory;
import ic2.core.block.invslot.InvSlot;
import ic2.core.gui.dynamic.DynamicContainer;
import ic2.core.gui.dynamic.DynamicGui;
import ic2.core.gui.dynamic.GuiParser;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class TileEntityCokeKilnHatch extends TileEntityInventory implements IHasGui {
   protected final InvSlot inventory = new InvSlot(this, "inventory", InvSlot.Access.I, 1, InvSlot.InvSide.ANY);

   @Override
   public boolean canPlaceItemThroughFace(int index, ItemStack stack, Direction side) {
      return side != this.getFacing() ? false : super.canPlaceItemThroughFace(index, stack, side);
   }

   @Override
   public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction side) {
      return false;
   }

   @Override
   public ContainerBase<TileEntityCokeKilnHatch> getGuiContainer(Player player) {
      return DynamicContainer.create(this, player, GuiParser.parse(this.teBlock));
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public Screen getGui(Player player, boolean isAdmin) {
      return DynamicGui.<TileEntityCokeKilnHatch>create(this, player, GuiParser.parse(this.teBlock));
   }

   @Override
   public void onGuiClosed(Player player) {
   }
}
