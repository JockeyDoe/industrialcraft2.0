package ic2.core.item.upgrade;

import com.google.common.base.Predicate;
import com.google.common.base.Supplier;
import ic2.api.network.ClientModifiable;
import ic2.core.ContainerBase;
import ic2.core.IC2;
import ic2.core.gui.EnumCycleHandler;
import ic2.core.gui.GuiDefaultBackground;
import ic2.core.gui.IClickHandler;
import ic2.core.gui.IEnableHandler;
import ic2.core.gui.MouseButton;
import ic2.core.gui.SlotGrid;
import ic2.core.gui.Text;
import ic2.core.gui.TextBox;
import ic2.core.gui.VanillaButton;
import ic2.core.gui.dynamic.TextProvider;
import ic2.core.init.Localization;
import ic2.core.item.ContainerHandHeldInventory;
import ic2.core.slot.SlotHologramSlot;
import java.util.Objects;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class HandHeldValueConfig extends HandHeldUpgradeOption {
   protected final ComparisonType initialComparisonType;
   protected final String initialNormalBox;
   protected final String initialExtraBox;
   protected final ComparisonSettings initialNormalSetting;
   protected final ComparisonSettings initialExtraSetting;

   public HandHeldValueConfig(HandHeldAdvancedUpgrade upgradeGUI, String type) {
      super(upgradeGUI, type);
      Settings settings = new Settings(this.getNBT());
      this.initialComparisonType = settings.comparison;
      this.initialNormalBox = settings.mainBox;
      this.initialExtraBox = settings.extraBox;
      this.initialNormalSetting = settings.main;
      this.initialExtraSetting = settings.extra;
   }

   @Override
   public ContainerBase<?> getGuiContainer(Player player) {
      return new HandHeldValueConfig.ContainerValueConfig();
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public Screen getGui(Player player, boolean isAdmin) {
      return new HandHeldValueConfig.GuiValueConfig();
   }

   public class ContainerValueConfig extends ContainerHandHeldInventory<HandHeldValueConfig> {
      @ClientModifiable
      protected ComparisonType comparisonType;
      @ClientModifiable
      protected String normalBox;
      @ClientModifiable
      protected String extraBox;
      @ClientModifiable
      protected ComparisonSettings normalSetting;
      @ClientModifiable
      protected ComparisonSettings extraSetting;

      public ContainerValueConfig() {
         Objects.requireNonNull(HandHeldValueConfig.this);
         super(HandHeldValueConfig.this);
         this.comparisonType = HandHeldValueConfig.this.initialComparisonType;
         this.normalBox = HandHeldValueConfig.this.initialNormalBox;
         this.extraBox = HandHeldValueConfig.this.initialExtraBox;
         this.normalSetting = HandHeldValueConfig.this.initialNormalSetting;
         this.extraSetting = HandHeldValueConfig.this.initialExtraSetting;
         this.addPlayerInventorySlots(HandHeldValueConfig.this.player, 166);

         for (byte slot = 0; slot < 9; slot++) {
            this.addSlot(new SlotHologramSlot(HandHeldValueConfig.this.inventory, slot, 8 + 18 * slot, 8, 1, HandHeldValueConfig.this.makeSaveCallback()));
         }
      }

      @Override
      public void removed(Player player) {
         CompoundTag nbt = HandHeldValueConfig.this.getNBT();
         nbt.putBoolean("active", this.comparisonType.enabled());
         ComparisonType saveType = this.comparisonType;
         switch (this.comparisonType) {
            case COMPARISON:
               if (this.normalBox.isEmpty()) {
                  saveType = ComparisonType.DIRECT;
               } else {
                  nbt.putString("normal", this.normalBox);
                  nbt.putByte("normalComp", this.normalSetting.getForNBT());
               }
            case RANGE:
               if (this.normalBox.isEmpty()) {
                  if (this.extraBox.isEmpty()) {
                     saveType = ComparisonType.DIRECT;
                  } else {
                     saveType = ComparisonType.COMPARISON;
                     nbt.putString("normal", this.extraBox);
                     nbt.putByte("normalComp", this.extraSetting.getForNBT());
                  }
               } else {
                  nbt.putString("normal", this.normalBox);
                  nbt.putByte("normalComp", this.normalSetting.getForNBT());
                  if (this.extraBox.isEmpty()) {
                     saveType = ComparisonType.COMPARISON;
                  } else {
                     nbt.putString("extra", this.extraBox);
                     nbt.putByte("extraComp", this.extraSetting.getForNBT());
                  }
               }
            default:
               nbt.putByte("type", saveType.getForNBT());
               HandHeldValueConfig.this.saveNBT(nbt);
               super.removed(player);
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   public class GuiValueConfig extends GuiDefaultBackground<HandHeldValueConfig.ContainerValueConfig> {
      public GuiValueConfig() {
         Objects.requireNonNull(HandHeldValueConfig.this);
         super(HandHeldValueConfig.this.new ContainerValueConfig());
         this.addElement(HandHeldValueConfig.this.getBackButton(this, 10, 62));
         this.addElement(new VanillaButton(this, 10, 25, 75, 15, new EnumCycleHandler<ComparisonType>(ComparisonType.VALUES, this.container.comparisonType) {
            {
               Objects.requireNonNull(GuiValueConfig.this);
            }

            @Override
            public void onClick(MouseButton button) {
               super.onClick(button);
               GuiValueConfig.this.container.comparisonType = this.getCurrentValue();
               IC2.network.get(false).sendContainerField(GuiValueConfig.this.container, "comparisonType");
            }
         }).withText(new Supplier<String>() {
            {
               Objects.requireNonNull(GuiValueConfig.this);
            }

            public String get() {
               return Localization.translate(GuiValueConfig.this.container.comparisonType.name);
            }
         }).withTooltip(new Supplier<String>() {
            private final String name;

            {
               Objects.requireNonNull(GuiValueConfig.this);
               this.name = Localization.translate("ic2.upgrade.advancedGUI." + HandHeldValueConfig.this.getName());
            }

            public String get() {
               return Localization.translate(GuiValueConfig.this.container.comparisonType.name + ".desc", this.name);
            }
         }));
         IEnableHandler rangeEnabled = new IEnableHandler() {
            {
               Objects.requireNonNull(GuiValueConfig.this);
            }

            @Override
            public boolean isEnabled() {
               return GuiValueConfig.this.container.comparisonType == ComparisonType.RANGE;
            }
         };
         IEnableHandler filtersEnabled = new IEnableHandler() {
            {
               Objects.requireNonNull(GuiValueConfig.this);
            }

            @Override
            public boolean isEnabled() {
               return !GuiValueConfig.this.container.comparisonType.ignoreFilters();
            }
         };
         this.addElement(
            new MoveableButton(
                  this,
                  75,
                  43,
                  60,
                  43,
                  17,
                  15,
                  new EnumCycleHandler<ComparisonSettings>(ComparisonSettings.VALUES, this.container.normalSetting) {
                     {
                        Objects.requireNonNull(GuiValueConfig.this);
                     }

                     @Override
                     public void onClick(MouseButton button) {
                        super.onClick(button);
                        GuiValueConfig.this.container.normalSetting = this.getCurrentValue();
                        IC2.network.get(false).sendContainerField(GuiValueConfig.this.container, "normalSetting");
                        switch ((ComparisonSettings)this.getCurrentValue()) {
                           case LESS:
                           case LESS_OR_EQUAL:
                              if (GuiValueConfig.this.container.extraSetting != ComparisonSettings.LESS
                                 && GuiValueConfig.this.container.extraSetting != ComparisonSettings.LESS_OR_EQUAL) {
                                 GuiValueConfig.this.container.extraSetting = ComparisonSettings.LESS;
                                 IC2.network.get(false).sendContainerField(GuiValueConfig.this.container, "extraSetting");
                              }
                              break;
                           case GREATER:
                           case GREATER_OR_EQUAL:
                              if (GuiValueConfig.this.container.extraSetting != ComparisonSettings.GREATER
                                 && GuiValueConfig.this.container.extraSetting != ComparisonSettings.GREATER_OR_EQUAL) {
                                 GuiValueConfig.this.container.extraSetting = ComparisonSettings.GREATER;
                                 IC2.network.get(false).sendContainerField(GuiValueConfig.this.container, "extraSetting");
                              }
                              break;
                           default:
                              throw new IllegalStateException("Unexpected other setting: " + this.getCurrentValue());
                        }
                     }
                  }
               )
               .withMoveHandler(rangeEnabled)
               .withEnableHandler(filtersEnabled)
               .withText(new Supplier<String>() {
                  {
                     Objects.requireNonNull(GuiValueConfig.this);
                  }

                  public String get() {
                     return GuiValueConfig.this.container.normalSetting.symbol;
                  }
               })
               .withTooltip(new Supplier<String>() {
                  {
                     Objects.requireNonNull(GuiValueConfig.this);
                  }

                  public String get() {
                     return Localization.translate(GuiValueConfig.this.container.normalSetting.name);
                  }
               })
         );
         this.addElement(new VanillaButton(this, 105, 43, 17, 15, new IClickHandler() {
            {
               Objects.requireNonNull(GuiValueConfig.this);
            }

            @Override
            public void onClick(MouseButton button) {
               if (button == MouseButton.left || button == MouseButton.right) {
                  switch (GuiValueConfig.this.container.normalSetting) {
                     case LESS:
                     case LESS_OR_EQUAL:
                        if (GuiValueConfig.this.container.extraSetting == ComparisonSettings.LESS) {
                           GuiValueConfig.this.container.extraSetting = ComparisonSettings.LESS_OR_EQUAL;
                        } else {
                           GuiValueConfig.this.container.extraSetting = ComparisonSettings.LESS;
                        }
                        break;
                     case GREATER:
                     case GREATER_OR_EQUAL:
                        if (GuiValueConfig.this.container.extraSetting == ComparisonSettings.GREATER) {
                           GuiValueConfig.this.container.extraSetting = ComparisonSettings.GREATER_OR_EQUAL;
                        } else {
                           GuiValueConfig.this.container.extraSetting = ComparisonSettings.GREATER;
                        }
                        break;
                     default:
                        throw new IllegalStateException("Unexpected other setting: " + GuiValueConfig.this.container.normalSetting);
                  }

                  IC2.network.get(false).sendContainerField(GuiValueConfig.this.container, "extraSetting");
               }
            }
         }).withEnableHandler(rangeEnabled).withText(new Supplier<String>() {
            {
               Objects.requireNonNull(GuiValueConfig.this);
            }

            public String get() {
               return GuiValueConfig.this.container.extraSetting.symbol;
            }
         }).withTooltip(new Supplier<String>() {
            {
               Objects.requireNonNull(GuiValueConfig.this);
            }

            public String get() {
               return Localization.translate(GuiValueConfig.this.container.extraSetting.name);
            }
         }));
         Predicate<String> numberOnly = new Predicate<String>() {
            {
               Objects.requireNonNull(GuiValueConfig.this);
            }

            public boolean apply(String input) {
               try {
                  return Integer.parseInt(input) >= 0;
               } catch (NumberFormatException var3) {
                  return input.isEmpty();
               }
            }
         };
         final MoveableTextBox textBox = new MoveableTextBox(this, 40, 43, 25, 43, 30, 15, this.container.normalBox);
         this.addElement(textBox.withMoveHandler(rangeEnabled).withTextWatcher(new TextBox.ITextBoxWatcher() {
            {
               Objects.requireNonNull(GuiValueConfig.this);
            }

            @Override
            public void onChanged(String oldValue, String newValue) {
               GuiValueConfig.this.container.normalBox = newValue;
               IC2.network.get(false).sendContainerField(GuiValueConfig.this.container, "normalBox");
            }
         }).withTextValidator(numberOnly).withEnableHandler(filtersEnabled));
         this.addElement(new TextBox(this, 125, 43, 30, 15, this.container.extraBox).withTextWatcher(new TextBox.ITextBoxWatcher() {
            {
               Objects.requireNonNull(GuiValueConfig.this);
            }

            @Override
            public void onChanged(String oldValue, String newValue) {
               GuiValueConfig.this.container.extraBox = newValue;
               IC2.network.get(false).sendContainerField(GuiValueConfig.this.container, "extraBox");
            }
         }).withTextValidator(numberOnly).withEnableHandler(rangeEnabled));
         this.addElement(
            Text.create(this, 100, 47, TextProvider.ofTranslated("ic2.upgrade.advancedGUI." + HandHeldValueConfig.this.getName()), 4210752, false)
               .withEnableHandler(new IEnableHandler() {
                  {
                     Objects.requireNonNull(GuiValueConfig.this);
                  }

                  @Override
                  public boolean isEnabled() {
                     return textBox.isEnabled() && !textBox.isMoved();
                  }
               })
         );
         this.addElement(
            Text.create(this, 80, 47, TextProvider.ofTranslated("ic2.upgrade.advancedGUI." + HandHeldValueConfig.this.getName()), 4210752, false)
               .withEnableHandler(new IEnableHandler() {
                  {
                     Objects.requireNonNull(GuiValueConfig.this);
                  }

                  @Override
                  public boolean isEnabled() {
                     return textBox.isEnabled() && textBox.isMoved();
                  }
               })
         );
         this.addElement(new SlotGrid(this, 7, 7, 9, 1, SlotGrid.SlotStyle.Normal));
         this.addElement(new SlotGrid(this, 7, 83, 9, 3, SlotGrid.SlotStyle.Normal));
         this.addElement(new SlotGrid(this, 7, 141, 9, 1, SlotGrid.SlotStyle.Normal));
      }
   }
}
