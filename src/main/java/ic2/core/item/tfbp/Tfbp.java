package ic2.core.item.tfbp;

import ic2.api.item.ITerraformingBP;
import ic2.core.IC2;
import ic2.core.block.state.IIdProvider;
import ic2.core.item.ItemDefaults;
import ic2.core.item.ItemMulti;
import ic2.core.ref.ItemName;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class Tfbp extends ItemMulti<Tfbp.TfbpType> implements ITerraformingBP {
   public static void init() {
      for (Tfbp.TfbpType tfbp : Tfbp.TfbpType.values()) {
         if (tfbp.logic != null) {
            tfbp.logic.init();
         }
      }
   }

   public Tfbp() {
      super(ItemName.tfbp, Tfbp.TfbpType.class);
      ItemDefaults.stackSize(this, 1);
   }

   @Override
   public double getConsume(ItemStack stack) {
      Tfbp.TfbpType type = this.getType(stack);
      return type == null ? 0.0 : type.consume;
   }

   @Override
   public int getRange(ItemStack stack) {
      Tfbp.TfbpType type = this.getType(stack);
      return type == null ? 0 : type.range;
   }

   @Override
   public boolean canInsert(ItemStack stack, Player player, Level world, BlockPos pos) {
      Tfbp.TfbpType type = this.getType(stack);
      if (type == null) {
         return false;
      } else {
         if (type == Tfbp.TfbpType.cultivation && world.dimension() == Level.END) {
            IC2.achievements.issueAchievement(player, "terraformEndCultivation");
         }

         return true;
      }
   }

   @Override
   public boolean terraform(ItemStack stack, Level world, BlockPos pos) {
      Tfbp.TfbpType type = this.getType(stack);
      if (type == null) {
         return false;
      } else {
         return type.logic == null ? false : type.logic.terraform(world, pos);
      }
   }

   public static enum TfbpType implements IIdProvider {
      blank(0.0, 0, null),
      chilling(2000.0, 50, new Chilling()),
      cultivation(4000.0, 40, new Cultivation()),
      desertification(2500.0, 40, new Desertification()),
      flatification(4000.0, 40, new Flatification()),
      irrigation(3000.0, 60, new Irrigation()),
      mushroom(8000.0, 25, new Mushroom());

      public final double consume;
      public final int range;
      final TerraformerBase logic;

      private TfbpType(double consume, int range, TerraformerBase logic) {
         this.consume = consume;
         this.range = range;
         this.logic = logic;
      }

      @Override
      public String getName() {
         return this.name();
      }

      @Override
      public int getId() {
         return this.ordinal();
      }
   }
}
