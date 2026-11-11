package ic2.core.block.machine.tileentity;

import ic2.api.item.IBlockCuttingBlade;
import ic2.api.recipe.IRecipeInput;
import ic2.api.recipe.MachineRecipeResult;
import ic2.api.recipe.Recipes;
import ic2.api.upgrade.UpgradableProperty;
import ic2.core.ContainerBase;
import ic2.core.block.invslot.InvSlotConsumableClass;
import ic2.core.block.invslot.InvSlotProcessableGeneric;
import ic2.core.gui.dynamic.DynamicContainer;
import ic2.core.gui.dynamic.DynamicGui;
import ic2.core.gui.dynamic.GuiParser;
import ic2.core.network.GuiSynced;
import ic2.core.profile.NotClassic;
import ic2.core.recipe.BasicMachineRecipeManager;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@NotClassic
public class TileEntityBlockCutter extends TileEntityStandardMachine<IRecipeInput, Collection<ItemStack>, ItemStack> {
   @GuiSynced
   private boolean bladeTooWeak = false;
   public final InvSlotConsumableClass cutterSlot;

   public TileEntityBlockCutter() {
      super(4, 450, 1);
      this.inputSlot = new InvSlotProcessableGeneric(this, "input", 1, Recipes.blockcutter);
      this.cutterSlot = new InvSlotConsumableClass(this, "cutterInputSlot", 1, IBlockCuttingBlade.class);
   }

   public static void init() {
      Recipes.blockcutter = new BasicMachineRecipeManager();
   }

   @Override
   public MachineRecipeResult<IRecipeInput, Collection<ItemStack>, ItemStack> getOutput() {
      if (this.cutterSlot.isEmpty()) {
         if (!this.bladeTooWeak) {
            this.bladeTooWeak = true;
         }

         return null;
      } else {
         if (this.bladeTooWeak) {
            this.bladeTooWeak = false;
         }

         MachineRecipeResult<IRecipeInput, Collection<ItemStack>, ItemStack> ret = super.getOutput();
         if (ret != null && ret.getRecipe().getMetaData() != null) {
            ItemStack bladeStack = this.cutterSlot.get();
            IBlockCuttingBlade blade = (IBlockCuttingBlade)bladeStack.getItem();
            if (ret.getRecipe().getMetaData().getIntOr("hardness", 0) > blade.getHardness(bladeStack)) {
               if (!this.bladeTooWeak) {
                  this.bladeTooWeak = true;
               }

               return null;
            } else {
               if (this.bladeTooWeak) {
                  this.bladeTooWeak = false;
               }

               return ret;
            }
         } else {
            return null;
         }
      }
   }

   @Override
   public ContainerBase<TileEntityBlockCutter> getGuiContainer(Player player) {
      return DynamicContainer.create(this, player, GuiParser.parse(this.teBlock));
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public Screen getGui(Player player, boolean isAdmin) {
      return DynamicGui.<TileEntityBlockCutter>create(this, player, GuiParser.parse(this.teBlock));
   }

   @Override
   public boolean getGuiState(String name) {
      return "isBladeTooWeak".equals(name) ? this.bladeTooWeak : super.getGuiState(name);
   }

   @Override
   public Set<UpgradableProperty> getUpgradableProperties() {
      return EnumSet.of(
         UpgradableProperty.Processing,
         UpgradableProperty.Transformer,
         UpgradableProperty.EnergyStorage,
         UpgradableProperty.ItemConsuming,
         UpgradableProperty.ItemProducing
      );
   }
}
