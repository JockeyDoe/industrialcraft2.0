package ic2.core.item.upgrade;

import com.google.common.base.Supplier;
import ic2.api.network.ClientModifiable;
import ic2.core.ContainerBase;
import ic2.core.IC2;
import ic2.core.IHasGui;
import ic2.core.gui.EnumCycleHandler;
import ic2.core.gui.MouseButton;
import ic2.core.gui.VanillaButton;
import ic2.core.gui.dynamic.DynamicGui;
import ic2.core.gui.dynamic.DynamicHandHeldContainer;
import ic2.core.gui.dynamic.GuiParser;
import ic2.core.gui.dynamic.IGuiConditionProvider;
import ic2.core.gui.dynamic.IHolographicSlotProvider;
import ic2.core.init.Localization;
import ic2.core.item.ContainerHandHeldInventory;
import ic2.core.item.tool.HandHeldInventory;
import ic2.core.network.GuiSynced;
import ic2.core.util.ItemData;
import ic2.core.util.LogCategory;
import ic2.core.util.ReflectionUtil;
import ic2.core.util.StackUtil;
import ic2.core.util.Util;
import java.io.IOException;
import java.util.Objects;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.xml.sax.SAXException;

public class HandHeldAdvancedUpgrade extends HandHeldInventory implements IHolographicSlotProvider, IGuiConditionProvider {
   @GuiSynced
   protected boolean meta;
   @GuiSynced
   protected boolean energy;
   @ClientModifiable
   protected NbtSettings nbt;
   private static final byte META_GUI = 0;
   private static final byte DAMAGE_GUI = 1;
   private static final byte ENERGY_GUI = 2;
   private static final byte ORE_GUI = 3;
   private static final Identifier GUI_XML = Identifier.fromNamespaceAndPath("ic2", "guidef/advanced_upgrade.xml");

   private static ItemStack checkContainerStack(Player player, ItemStack containerStack) {
      if (!player.level().isClientSide()
         && player.containerMenu instanceof ContainerHandHeldInventory
         && ((ContainerHandHeldInventory)player.containerMenu).base instanceof HandHeldUpgradeOption) {
         addMaintainedPlayer(player);
         return ReflectionUtil.getFieldValue(
            ReflectionUtil.getField(HandHeldInventory.class, ItemStack.class), ((ContainerHandHeldInventory)player.containerMenu).base
         );
      } else {
         return containerStack;
      }
   }

   public HandHeldAdvancedUpgrade(Player player, ItemStack containerStack) {
      super(player, checkContainerStack(player, containerStack), 9);
      CompoundTag nbt = StackUtil.getOrCreateNbtData(containerStack);
      this.meta = readTag(nbt, "meta");
      this.nbt = NbtSettings.getFromNBT(getTag(nbt, "nbt").getByteOr("type", (byte)0));
      this.energy = readTag(nbt, "energy");
   }

   @Override
   protected void save() {
      super.save();
      if (IC2.platform.isSimulating()) {
         CompoundTag nbt = ItemData.readOrEmpty(this.containerStack);

         assert nbt != null;

         writeTag(nbt, "meta", this.meta);
         CompoundTag tag = getTag(nbt, "nbt");
         tag.putBoolean("active", this.nbt.enabled());
         tag.putByte("type", this.nbt.getForNBT());
         nbt.put("nbtSettings", tag);
         ItemData.commit(this.containerStack, nbt);
         writeTag(nbt, "energy", this.energy);
         ItemData.commit(this.containerStack, nbt);
      }
   }

   public static CompoundTag getTag(CompoundTag nbt, String name) {
      return nbt.getCompoundOrEmpty(name + "Settings");
   }

   protected static boolean readTag(CompoundTag nbt, String name) {
      return getTag(nbt, name).getBooleanOr("active", false);
   }

   protected static void writeTag(CompoundTag nbt, String name, boolean active) {
      CompoundTag tag = getTag(nbt, name);
      tag.putBoolean("active", active);
      nbt.put(name + "Settings", tag);
   }

   static IHasGui delegate(Player player, ItemStack stack, int ID) {
      switch (ID) {
         case 0:
            return new HandHeldValueConfig(new HandHeldAdvancedUpgrade(player, stack), "meta");
         case 1:
            return null;
         case 2:
            return new HandHeldValueConfig(new HandHeldAdvancedUpgrade(player, stack), "energy");
         case 3:
            return new HandHeldOre(new HandHeldAdvancedUpgrade(player, stack));
         default:
            IC2.log.warn(LogCategory.Network, "Unexpected delegate ID: " + ID);
            return null;
      }
   }

   @Override
   public ContainerBase<?> getGuiContainer(Player player) {
      return DynamicHandHeldContainer.create(this, player, this.getNode());
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public Screen getGui(Player player, boolean isAdmin) {
      final DynamicGui<?> gui = DynamicGui.create(this, player, this.getNode());
      if (Util.inDev()) {
         gui.addElement(
            new VanillaButton(gui, 10, 62, 50, 20, new EnumCycleHandler<NbtSettings>(NbtSettings.VALUES, this.nbt) {
                  {
                     Objects.requireNonNull(HandHeldAdvancedUpgrade.this);
                  }

                  @Override
                  public void onClick(MouseButton button) {
                     super.onClick(button);
                     HandHeldAdvancedUpgrade.this.nbt = this.getCurrentValue();
                     IC2.network.get(false).sendHandHeldInvField(gui.getContainer(), "nbt");
                  }
               })
               .withText("ic2.upgrade.advancedGUI.nbt")
               .withTooltip(
                  new Supplier<String>() {
                     private final String NBT;

                     {
                        Objects.requireNonNull(HandHeldAdvancedUpgrade.this);
                        this.NBT = Localization.translate("ic2.upgrade.advancedGUI.nbt");
                     }

                     public String get() {
                        return Localization.translate(
                           "ic2.upgrade.advancedGUI.nbt.desc",
                           Localization.translate(HandHeldAdvancedUpgrade.this.nbt.name),
                           ChatFormatting.GRAY,
                           Localization.translate(HandHeldAdvancedUpgrade.this.nbt.name + ".desc", this.NBT)
                        );
                     }
                  }
               )
         );
      }

      return gui;
   }

   protected GuiParser.GuiNode getNode() {
      try {
         return GuiParser.parse(GUI_XML, HandHeldAdvancedUpgrade.class);
      } catch (SAXException var2) {
         throw new RuntimeException("XML Exception opening Advanced Upgrade GUI", var2);
      } catch (IOException var3) {
         throw new RuntimeException("IO Exception opening Advanced Upgrade GUI", var3);
      }
   }

   public boolean hasCustomName() {
      return false;
   }

   @Override
   public String getName() {
      return this.containerStack.getItem().getDescriptionId();
   }

   Player getPlayer() {
      return this.player;
   }

   ItemStack getContainerStack() {
      return this.containerStack;
   }

   @Override
   public ItemStack[] getStacksForName(String name) {
      if ("filter".equals(name)) {
         return this.inventory;
      } else {
         throw new IllegalArgumentException("Unexpected stack array name requested: " + name);
      }
   }

   @Override
   public boolean getGuiState(String name) {
      if ("meta".equals(name)) {
         return this.meta;
      } else if ("nbt".equals(name)) {
         return this.nbt.enabled();
      } else if (!"energy".equals(name)) {
         if ("dev".equals(name)) {
            return Util.inDev();
         } else {
            throw new IllegalArgumentException("Unexpected conditional name requested: " + name);
         }
      } else {
         return this.energy || this.nbt == NbtSettings.EXACT;
      }
   }

   @Override
   public void onEvent(String event) {
      boolean dev = false;
      if (Util.inDev() && event.endsWith("Dev")) {
         dev = true;
         event = event.substring(0, event.lastIndexOf("Dev"));
      }

      if ("meta".equals(event)) {
         if (!dev) {
            this.meta = !this.meta;
         } else {
            this.launchGUI(new HandHeldValueConfig(this, "meta"), 0);
         }
      } else if ("energy".equals(event)) {
         if (!dev) {
            this.energy = !this.energy;
         } else {
            this.launchGUI(new HandHeldValueConfig(this, "energy"), 2);
         }
      } else if ("ore".equals(event)) {
         assert dev;

         this.launchGUI(new HandHeldOre(this), 3);
      } else {
         super.onEvent(event);
      }
   }

   protected void launchGUI(IHasGui gui, int ID) {
      if (!this.player.level().isClientSide()) {
         HandHeldInventory.addMaintainedPlayer(this.player);
         IC2.platform.launchSubGui(this.player, gui, ID);
      }
   }
}
