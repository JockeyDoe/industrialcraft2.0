package ic2.core.block.machine.gui;

import com.google.common.base.Supplier;
import ic2.core.GuiIC2;
import ic2.core.block.machine.container.ContainerPatternStorage;
import ic2.core.block.machine.tileentity.TileEntityPatternStorage;
import ic2.core.gui.CustomButton;
import ic2.core.gui.IEnableHandler;
import ic2.core.gui.ItemImage;
import ic2.core.gui.Text;
import ic2.core.gui.dynamic.TextProvider;
import ic2.core.init.Localization;
import ic2.core.util.Util;
import java.util.Objects;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GuiPatternStorage extends GuiIC2<ContainerPatternStorage> {
   private static final Identifier background = Identifier.fromNamespaceAndPath("ic2", "textures/gui/guipatternstorage.png");

   public GuiPatternStorage(final ContainerPatternStorage container) {
      super(container);
      this.addElement(new CustomButton(this, 7, 19, 9, 18, this.createEventSender(0)).withTooltip("ic2.PatternStorage.gui.info.last"));
      this.addElement(new CustomButton(this, 36, 19, 9, 18, this.createEventSender(1)).withTooltip("ic2.PatternStorage.gui.info.next"));
      this.addElement(new CustomButton(this, 10, 37, 16, 8, this.createEventSender(2)).withTooltip("ic2.PatternStorage.gui.info.export"));
      this.addElement(new CustomButton(this, 26, 37, 16, 8, this.createEventSender(3)).withTooltip("ic2.PatternStorage.gui.info.import"));
      this.addElement(Text.create(this, this.imageWidth / 2, 30, TextProvider.of(new Supplier<String>() {
         {
            Objects.requireNonNull(GuiPatternStorage.this);
         }

         public String get() {
            TileEntityPatternStorage te = container.base;
            return Math.min(te.index + 1, te.maxIndex) + " / " + te.maxIndex;
         }
      }), 4210752, false, true, false));
      this.addElement(Text.create(this, 10, 48, TextProvider.ofTranslated("ic2.generic.text.Name"), 16777215, false));
      this.addElement(Text.create(this, 10, 59, TextProvider.ofTranslated("ic2.generic.text.UUMatte"), 16777215, false));
      this.addElement(Text.create(this, 10, 70, TextProvider.ofTranslated("ic2.generic.text.Energy"), 16777215, false));
      IEnableHandler patternInfoEnabler = new IEnableHandler() {
         {
            Objects.requireNonNull(GuiPatternStorage.this);
         }

         @Override
         public boolean isEnabled() {
            return container.base.pattern != null;
         }
      };
      this.addElement(Text.create(this, 80, 48, TextProvider.of(new Supplier<String>() {
         {
            Objects.requireNonNull(GuiPatternStorage.this);
         }

         public String get() {
            ItemStack pattern = container.base.pattern;
            return pattern != null ? pattern.getDisplayName().getString() : null;
         }
      }), 16777215, false).withEnableHandler(patternInfoEnabler));
      this.addElement(Text.create(this, 80, 59, TextProvider.of(new Supplier<String>() {
         {
            Objects.requireNonNull(GuiPatternStorage.this);
         }

         public String get() {
            return Util.toSiString(container.base.patternUu, 4) + Localization.translate("ic2.generic.text.bucketUnit");
         }
      }), 16777215, false).withEnableHandler(patternInfoEnabler));
      this.addElement(Text.create(this, 80, 70, TextProvider.of(new Supplier<String>() {
         {
            Objects.requireNonNull(GuiPatternStorage.this);
         }

         public String get() {
            return Util.toSiString(container.base.patternEu, 4) + Localization.translate("ic2.generic.text.EU");
         }
      }), 16777215, false).withEnableHandler(patternInfoEnabler));
      this.addElement(new ItemImage(this, 152, 29, new Supplier<ItemStack>() {
         {
            Objects.requireNonNull(GuiPatternStorage.this);
         }

         public ItemStack get() {
            return container.base.pattern;
         }
      }));
   }

   @Override
   protected Identifier getTexture() {
      return background;
   }
}
