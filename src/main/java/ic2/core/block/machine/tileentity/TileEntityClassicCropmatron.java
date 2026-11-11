package ic2.core.block.machine.tileentity;

import ic2.core.ContainerBase;
import ic2.core.IHasGui;
import ic2.core.block.invslot.InvSlotConsumable;
import ic2.core.block.invslot.InvSlotConsumableItemStack;
import ic2.core.block.machine.container.ContainerClassicCropmatron;
import ic2.core.block.machine.gui.GuiClassicCropmatron;
import ic2.core.crop.TileEntityCrop;
import ic2.core.item.type.CellType;
import ic2.core.item.type.CropResItemType;
import ic2.core.ref.ItemName;
import ic2.core.ref.TeBlock;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@TeBlock.Delegated(current = TileEntityCropmatron.class, old = TileEntityClassicCropmatron.class)
public class TileEntityClassicCropmatron extends TileEntityElectricMachine implements IHasGui {
   public int scanX = -4;
   public int scanY = -1;
   public int scanZ = -4;
   public final InvSlotConsumable fertilizerSlot = new InvSlotConsumableItemStack(
      this, "fertilizer", 3, ItemName.crop_res.getItemStack(CropResItemType.fertilizer)
   );
   public final InvSlotConsumable hydrationSlot = new InvSlotConsumableItemStack(this, "hydration", 3, ItemName.cell.getItemStack(CellType.hydration));
   public final InvSlotConsumable weedExSlot = new InvSlotConsumableItemStack(this, "weedEx", 3, ItemName.cell.getItemStack(CellType.weed_ex));

   public TileEntityClassicCropmatron() {
      super(1000, 1);
   }

   @Override
   protected void updateEntityServer() {
      super.updateEntityServer();
      this.fertilizerSlot.organize();
      this.hydrationSlot.organize();
      this.weedExSlot.organize();
      if (this.energy.getEnergy() >= 31.0) {
         this.scan();
      }
   }

   public void scan() {
      this.scanX++;
      if (this.scanX > 5) {
         this.scanX = -5;
         this.scanZ++;
         if (this.scanZ > 5) {
            this.scanZ = -5;
            this.scanY++;
            if (this.scanY > 1) {
               this.scanY = -1;
            }
         }
      }

      this.energy.useEnergy(1.0);
      BlockPos scan = this.getBlockPos().offset(this.scanX, this.scanY, this.scanZ);
      if (this.getLevel().getBlockEntity(scan) instanceof TileEntityCrop crop) {
         if (!this.fertilizerSlot.isEmpty() && crop.applyFertilizer(false)) {
            this.energy.useEnergy(10.0);
            this.fertilizerSlot.consume(1);
         }

         if (!this.hydrationSlot.isEmpty()
            && CellType.hydration.doCropAction(this.hydrationSlot.get(0), stack -> this.hydrationSlot.put(0, stack), crop, false) == InteractionResult.SUCCESS) {
            this.energy.useEnergy(10.0);
         }

         if (!this.weedExSlot.isEmpty()
            && CellType.weed_ex.doCropAction(this.weedExSlot.get(0), stack -> this.weedExSlot.put(0, stack), crop, false) == InteractionResult.SUCCESS) {
            this.energy.useEnergy(10.0);
         }
      }
   }

   @Override
   public ContainerBase<?> getGuiContainer(Player player) {
      return new ContainerClassicCropmatron(player, this);
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public Screen getGui(Player player, boolean isAdmin) {
      return new GuiClassicCropmatron(new ContainerClassicCropmatron(player, this));
   }

   @Override
   public void onGuiClosed(Player entityPlayer) {
   }
}
