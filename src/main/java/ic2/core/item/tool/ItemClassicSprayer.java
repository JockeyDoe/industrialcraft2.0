package ic2.core.item.tool;

import ic2.core.IC2;
import ic2.core.block.BlockBase;
import ic2.core.block.BlockFoam;
import ic2.core.block.BlockScaffold;
import ic2.core.block.wiring.TileEntityCable;
import ic2.core.item.ItemDefaults;
import ic2.core.item.ItemGradualInt;
import ic2.core.item.armor.ItemArmorClassicCFPack;
import ic2.core.ref.BlockName;
import ic2.core.ref.ItemName;
import ic2.core.slot.ArmorSlot;
import ic2.core.util.StackUtil;
import java.util.ArrayDeque;
import java.util.LinkedHashSet;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class ItemClassicSprayer extends ItemGradualInt {
   public ItemClassicSprayer() {
      super(ItemName.foam_sprayer, 1602);
      ItemDefaults.stackSize(this, 1);
   }

   @Override
   public InteractionResult func_180614_a(Player player, Level world, BlockPos pos, InteractionHand hand, Direction facing, float hitX, float hitY, float hitZ) {
      if (!IC2.platform.isSimulating()) {
         return InteractionResult.SUCCESS;
      } else {
         ItemStack stack = StackUtil.get(player, hand);
         ItemStack pack = ArmorSlot.getEquipment(player.getInventory()).get(2);
         boolean pulledFromCFPack = StackUtil.check(pack)
            && pack.getItem() == ItemName.cf_pack.getInstance()
            && ((ItemArmorClassicCFPack)pack.getItem()).getCFPellet(player, pack);
         if (!pulledFromCFPack && this.getCustomDamage(stack) < 100) {
            return InteractionResult.FAIL;
         } else if (world.getBlockState(pos).getBlock() == BlockName.scaffold.getInstance()) {
            this.sprayFoam(world, pos, calculateDirectionsFromPlayer(player), true);
            if (!pulledFromCFPack) {
               this.applyCustomDamage(stack, 100, player);
            }

            return InteractionResult.SUCCESS;
         } else if (this.sprayFoam(world, pos.relative(facing), calculateDirectionsFromPlayer(player), false)) {
            if (!pulledFromCFPack) {
               this.applyCustomDamage(stack, 100, player);
            }

            return InteractionResult.SUCCESS;
         } else {
            return InteractionResult.PASS;
         }
      }
   }

   private static boolean[] calculateDirectionsFromPlayer(Player player) {
      float yaw = player.getYRot() % 360.0F;
      float pitch = player.getXRot();
      boolean[] r = new boolean[]{true, true, true, true, true, true};
      if (pitch >= -65.0F && pitch <= 65.0F) {
         if (yaw >= 300.0F && yaw <= 360.0F || yaw >= 0.0F && yaw <= 60.0F) {
            r[2] = false;
         }

         if (yaw >= 30.0F && yaw <= 150.0F) {
            r[5] = false;
         }

         if (yaw >= 120.0F && yaw <= 240.0F) {
            r[3] = false;
         }

         if (yaw >= 210.0F && yaw <= 330.0F) {
            r[4] = false;
         }
      }

      if (pitch <= -40.0F) {
         r[0] = false;
      }

      if (pitch >= 40.0F) {
         r[1] = false;
      }

      return r;
   }

   public boolean sprayFoam(Level world, BlockPos start, boolean[] directions, boolean scaffold) {
      if (!canFoam(world, start, scaffold)) {
         return false;
      } else {
         Queue<BlockPos> check = new ArrayDeque<>();
         Set<BlockPos> place = new LinkedHashSet<>();
         int foamcount = getSprayMass();
         check.add(start);

         BlockPos set;
         while ((set = check.poll()) != null && foamcount > 0) {
            if (canFoam(world, set, scaffold) && place.add(set)) {
               for (int i : generateRngSpread(IC2.random)) {
                  if (scaffold || directions[i]) {
                     check.add(set.relative(Direction.from3DDataValue(i)));
                  }
               }

               foamcount--;
            }
         }

         for (BlockPos pos : place) {
            BlockState state = world.getBlockState(pos);
            Block targetBlock = state.getBlock();
            if (targetBlock == BlockName.scaffold.getInstance()) {
               BlockScaffold block = (BlockScaffold)targetBlock;
               switch ((BlockScaffold.ScaffoldType)state.getValue(block.getTypeProperty())) {
                  case wood:
                  case reinforced_wood:
                     block.func_176226_b(world, pos, state, 0);
                     world.setBlock(pos, BlockName.foam.getBlockState(BlockFoam.FoamType.normal), 3);
                  case reinforced_iron:
                  case iron:
               }
            } else if (targetBlock == BlockName.te.getInstance()) {
               BlockEntity te = world.getBlockEntity(pos);
               if (te instanceof TileEntityCable) {
                  ((TileEntityCable)te).foam();
               }
            } else {
               world.setBlock(pos, BlockName.foam.getBlockState(BlockFoam.FoamType.normal), 3);
            }
         }

         return true;
      }
   }

   private static boolean canFoam(Level world, BlockPos pos, boolean scaffold) {
      if (scaffold) {
         return world.getBlockState(pos).getBlock() == BlockName.scaffold.getInstance();
      } else if (BlockName.foam.<BlockBase>getInstance().func_176198_a(world, pos, Direction.DOWN)) {
         return true;
      } else if (world.getBlockState(pos).getBlock() != BlockName.te.getInstance()) {
         return false;
      } else {
         BlockEntity te = world.getBlockEntity(pos);
         return te instanceof TileEntityCable && !((TileEntityCable)te).isFoamed();
      }
   }

   private static int[] generateRngSpread(Random random) {
      int[] re = new int[]{0, 1, 2, 3, 4, 5};

      for (int i = 0; i < 16; i++) {
         int first = random.nextInt(6);
         int second = random.nextInt(6);
         int temp = re[first];
         re[first] = re[second];
         re[second] = temp;
      }

      return re;
   }

   public static int getSprayMass() {
      return 13;
   }

   @Override
   public double getDurabilityForDisplay(ItemStack stack) {
      return 1.0 - super.getDurabilityForDisplay(stack);
   }
}
