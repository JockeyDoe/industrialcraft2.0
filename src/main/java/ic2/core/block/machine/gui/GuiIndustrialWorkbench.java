package ic2.core.block.machine.gui;

import com.google.common.base.Predicate;
import com.google.common.base.Supplier;
import ic2.core.GuiIC2;
import ic2.core.IC2;
import ic2.core.IHasGui;
import ic2.core.block.machine.container.ContainerIndustrialWorkbench;
import ic2.core.block.machine.tileentity.TileEntityIndustrialWorkbench;
import ic2.core.block.personal.IPersonalBlock;
import ic2.core.gui.Area;
import ic2.core.gui.CustomButton;
import ic2.core.gui.GuiElement;
import ic2.core.gui.IClickHandler;
import ic2.core.gui.IEnableHandler;
import ic2.core.gui.Image;
import ic2.core.gui.MouseButton;
import ic2.core.gui.VanillaButton;
import ic2.core.init.Localization;
import java.util.Objects;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ScreenEvent.Opening;
import net.neoforged.neoforge.common.NeoForge;

@OnlyIn(Dist.CLIENT)
public class GuiIndustrialWorkbench extends GuiIC2<ContainerIndustrialWorkbench> {
   public static Predicate<Screen> jeiScreenRecipesGuiCheck;
   private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("ic2", "textures/gui/guiindustrialworkbench.png");

   public GuiIndustrialWorkbench(ContainerIndustrialWorkbench container) {
      super(container, 194, 228);
      this.addElement((new Area(this, 173, 3, 18, 108) {
         {
            Objects.requireNonNull(GuiIndustrialWorkbench.this);
         }

         @Override
         protected boolean suppressTooltip(int mouseX, int mouseY) {
            for (GuiElement<?> element : GuiIndustrialWorkbench.this.elements) {
               if (element.isEnabled() && element != this && element.contains(mouseX, mouseY)) {
                  return true;
               }
            }

            return false;
         }
      }).withTooltip("ic2.IndustrialWorkbench.gui.adjacent"));

      for (final Direction side : Direction.values()) {
         this.addElement(
            new CustomButton(
                  this,
                  173,
                  3 + (side.get3DDataValue() + 5) % 6 * 18,
                  18,
                  18,
                  new IClickHandler() {
                     private boolean firstOpen;
                     private boolean jei;

                     {
                        Objects.requireNonNull(GuiIndustrialWorkbench.this);
                        this.firstOpen = true;
                        this.jei = false;
                     }

                     @Override
                     public void onClick(MouseButton button) {
                        TileEntityIndustrialWorkbench base = GuiIndustrialWorkbench.this.container.base;

                        assert base.hasLevel();

                        BlockEntity neighbour = base.getLevel().getBlockEntity(base.getBlockPos().relative(side));

                        assert neighbour instanceof IHasGui;

                        if (neighbour instanceof IPersonalBlock
                           && !((IPersonalBlock)neighbour).permitsAccess(GuiIndustrialWorkbench.this.container.player.getGameProfile())) {
                           IC2.platform
                              .messagePlayer(GuiIndustrialWorkbench.this.container.player, "Owned by " + ((IPersonalBlock)neighbour).getOwner().name());
                        } else {
                           IC2.network.get(false).requestGUI((IHasGui)neighbour);
                           NeoForge.EVENT_BUS.register(this);
                        }
                     }

                     @SubscribeEvent
                     public void waitForClose(Opening event) {
                        if (!this.keepOpen(event.getScreen())) {
                           if (!this.firstOpen) {
                              IC2.network.get(false).requestGUI(GuiIndustrialWorkbench.this.container.base);
                              event.setNewScreen(GuiIndustrialWorkbench.this);
                              NeoForge.EVENT_BUS.unregister(this);
                           } else {
                              this.firstOpen = false;
                           }
                        }
                     }

                     private boolean keepOpen(Screen screen) {
                        if (GuiIndustrialWorkbench.jeiScreenRecipesGuiCheck == null) {
                           return false;
                        } else if (GuiIndustrialWorkbench.jeiScreenRecipesGuiCheck.apply(screen)) {
                           this.jei = true;
                           return true;
                        } else if (this.jei) {
                           this.jei = false;
                           return true;
                        } else {
                           return false;
                        }
                     }
                  }
               )
               .withEnableHandler(new IEnableHandler() {
                  {
                     Objects.requireNonNull(GuiIndustrialWorkbench.this);
                  }

                  @Override
                  public boolean isEnabled() {
                     TileEntityIndustrialWorkbench base = GuiIndustrialWorkbench.this.container.base;
                     return base.hasLevel() && base.getLevel().getBlockEntity(base.getBlockPos().relative(side)) instanceof IHasGui;
                  }
               })
               .withIcon(new Supplier<ItemStack>() {
                  {
                     Objects.requireNonNull(GuiIndustrialWorkbench.this);
                  }

                  public ItemStack get() {
                     TileEntityIndustrialWorkbench base = GuiIndustrialWorkbench.this.container.base;

                     assert base.hasLevel();

                     BlockPos pos = base.getBlockPos().relative(side);
                     BlockState state = base.getLevel().getBlockState(pos);
                     return state.getCloneItemStack(pos, base.getLevel(), false, GuiIndustrialWorkbench.this.container.player);
                  }
               })
               .withTooltip(new Supplier<String>() {
                  {
                     Objects.requireNonNull(GuiIndustrialWorkbench.this);
                  }

                  private String getSideName() {
                     switch (side) {
                        case WEST:
                           return "ic2.dir.West";
                        case EAST:
                           return "ic2.dir.East";
                        case DOWN:
                           return "ic2.dir.Bottom";
                        case UP:
                           return "ic2.dir.Top";
                        case NORTH:
                           return "ic2.dir.North";
                        case SOUTH:
                           return "ic2.dir.South";
                        default:
                           throw new IllegalStateException("Unexpected direction: " + side);
                     }
                  }

                  public String get() {
                     TileEntityIndustrialWorkbench base = GuiIndustrialWorkbench.this.container.base;

                     assert base.hasLevel();

                     BlockEntity neighbour = base.getLevel().getBlockEntity(base.getBlockPos().relative(side));

                     assert neighbour instanceof IHasGui;

                     return Localization.translate(((IHasGui)neighbour).getName())
                        + "\n"
                        + ChatFormatting.DARK_GRAY
                        + Localization.translate(this.getSideName());
                  }
               })
         );
      }

      int cancelX = 93;
      int cancelY = 42;
      this.addElement(new VanillaButton(this, 93, 42, 16, 16, new IClickHandler() {
         {
            Objects.requireNonNull(GuiIndustrialWorkbench.this);
         }

         @Override
         public void onClick(MouseButton button) {
            IC2.network.get(false).sendContainerEvent(GuiIndustrialWorkbench.this.container, "clear");
         }
      }).withTooltip("Clear"));
      this.addElement(Image.create(this, 94, 43, 14, 14, GuiElement.commonTexture, 256, 256, 210, 47, 224, 61));
   }

   @Override
   protected Identifier getTexture() {
      return TEXTURE;
   }
}
