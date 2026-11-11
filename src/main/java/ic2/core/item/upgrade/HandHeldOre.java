package ic2.core.item.upgrade;

import ic2.core.ContainerBase;
import ic2.core.gui.GuiDefaultBackground;
import ic2.core.gui.MouseButton;
import ic2.core.gui.ScrollableList;
import ic2.core.gui.SlotGrid;
import ic2.core.item.ContainerHandHeldInventory;
import ic2.core.slot.SlotHologramSlot;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class HandHeldOre extends HandHeldUpgradeOption {
   public HandHeldOre(HandHeldAdvancedUpgrade upgradeGUI) {
      super(upgradeGUI, "ore");
   }

   @Override
   public ContainerBase<?> getGuiContainer(Player player) {
      return new HandHeldOre.ContainerEditOre();
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public Screen getGui(Player player, boolean isAdmin) {
      return new HandHeldOre.GuiEditOre();
   }

   public class ContainerEditOre extends ContainerHandHeldInventory<HandHeldOre> {
      static final int HEIGHT = 200;

      public ContainerEditOre() {
         Objects.requireNonNull(HandHeldOre.this);
         super(HandHeldOre.this);
         this.addPlayerInventorySlots(HandHeldOre.this.player, 200);

         for (byte slot = 0; slot < 9; slot++) {
            this.addSlot(new SlotHologramSlot(HandHeldOre.this.inventory, slot, 8 + 18 * slot, 8, 1, HandHeldOre.this.makeSaveCallback()));
         }
      }

      @Override
      public void removed(Player player) {
         super.removed(player);
      }
   }

   @OnlyIn(Dist.CLIENT)
   public class GuiEditOre extends GuiDefaultBackground<HandHeldOre.ContainerEditOre> {
      public GuiEditOre() {
         super(HandHeldOre.this.new ContainerEditOre(), 200);
         this.addElement(HandHeldOre.this.getBackButton(this, 10, 96));
         List<ScrollableList.IListItem> items = new ArrayList<>();

         for (String name : new String[]{"One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine", "Ten"}) {
            items.add(new HandHeldOre.GuiEditOre.ListItem(name));
         }

         this.addElement(new ScrollableList(this, 10, 30, 120, 60, items));
         this.addElement(new SlotGrid(this, 7, 7, 9, 1, SlotGrid.SlotStyle.Normal));
         this.addElement(new SlotGrid(this, 7, 117, 9, 3, SlotGrid.SlotStyle.Normal));
         this.addElement(new SlotGrid(this, 7, 175, 9, 1, SlotGrid.SlotStyle.Normal));
      }

      public class ListItem implements ScrollableList.IListItem {
         private final String number;

         public ListItem(String number) {
            Objects.requireNonNull(GuiEditOre.this);
            super();
            this.number = number;
         }

         @Override
         public void onClick(MouseButton button) {
            System.out.println(this.number + " clicked with " + button);
         }

         public String getSerializedName() {
            return "Thing " + this.number;
         }
      }
   }
}
