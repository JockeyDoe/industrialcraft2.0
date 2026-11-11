package ic2.core.item.tool;

import ic2.api.item.ElectricItem;
import ic2.api.item.IBoxable;
import ic2.core.IC2;
import ic2.core.IHasGui;
import ic2.core.Ic2Player;
import ic2.core.audio.PositionSpec;
import ic2.core.init.Localization;
import ic2.core.init.OreValues;
import ic2.core.item.BaseElectricItem;
import ic2.core.item.IHandHeldInventory;
import ic2.core.ref.ItemName;
import ic2.core.util.ItemComparableItemStack;
import ic2.core.util.StackUtil;
import ic2.core.util.Tuple;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Map.Entry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class ItemScanner extends BaseElectricItem implements IBoxable, IHandHeldInventory {
   public ItemScanner() {
      this(ItemName.scanner, 100000.0, 128.0, 1);
   }

   public ItemScanner(ItemName name, double maxCharge, double transferLimit, int tier) {
      super(name, maxCharge, transferLimit, tier);
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public void func_77624_a(ItemStack stack, Level world, List<String> tooltip, TooltipFlag advanced) {
      super.func_77624_a(stack, world, tooltip, advanced);
      tooltip.add(Localization.translate("ic2.scanner.range", this.getScanRange() + ""));
   }

   public InteractionResult use(Level world, Player player, InteractionHand hand) {
      ItemStack stack = StackUtil.get(player, hand);
      if ((this.tier != 1 || ElectricItem.manager.use(stack, 50.0, player)) && (this.tier != 2 || ElectricItem.manager.use(stack, 250.0, player))) {
         if (!world.isClientSide()) {
            if (IC2.platform.launchGui(player, this.getInventory(player, stack)) && player.containerMenu instanceof ContainerToolScanner container) {
               Map<ItemComparableItemStack, Integer> scanResult = this.scan(player.level(), player.blockPosition(), this.getScanRange());
               container.setResults(this.scanMapToSortedList(scanResult));
            }
         } else {
            IC2.audioManager.playOnce(player, PositionSpec.Hand, "Tools/ODScanner.ogg", true, IC2.audioManager.getDefaultVolume());
         }

         return InteractionResult.SUCCESS.heldItemTransformedTo(stack);
      } else {
         return InteractionResult.FAIL;
      }
   }

   public boolean onDroppedByPlayer(ItemStack stack, Player player) {
      if (!player.level().isClientSide() && !StackUtil.isEmpty(stack) && player.containerMenu instanceof ContainerToolScanner) {
         HandHeldScanner scanner = ((ContainerToolScanner)player.containerMenu).base;
         if (scanner.isThisContainer(stack)) {
            scanner.saveAsThrown(stack);
            player.closeContainer();
         }
      }

      return true;
   }

   public int startLayerScan(ItemStack stack) {
      return ElectricItem.manager.use(stack, 50.0, null) ? this.getScanRange() / 2 : 0;
   }

   public int getScanRange() {
      return 6;
   }

   @Override
   public boolean canBeStoredInToolbox(ItemStack itemstack) {
      return true;
   }

   @Override
   public IHasGui getInventory(Player player, ItemStack stack) {
      return new HandHeldScanner(player, stack);
   }

   private Map<ItemComparableItemStack, Integer> scan(Level world, BlockPos center, int range) {
      Map<ItemComparableItemStack, Integer> ret = new HashMap<>();
      PathNavigationRegion cache = new PathNavigationRegion(world, center.offset(-range, -range, -range), center.offset(range, range, range));
      Player player = Ic2Player.get(world);
      MutableBlockPos tmpPos = new MutableBlockPos();

      for (int y = center.getY() - range; y <= center.getY() + range; y++) {
         for (int z = center.getZ() - range; z <= center.getZ() + range; z++) {
            for (int x = center.getX() - range; x <= center.getX() + range; x++) {
               tmpPos.set(x, y, z);
               BlockState state = cache.getBlockState(tmpPos);
               if (!state.isAir()) {
                  ItemStack pickStack = StackUtil.getPickStack(world, tmpPos, state, player);
                  List<ItemStack> drops;
                  if (pickStack != null && OreValues.get(pickStack) > 0) {
                     drops = Arrays.asList(pickStack);
                  } else {
                     drops = StackUtil.getDrops(cache, tmpPos, state, 0);
                     if (drops.isEmpty() || OreValues.get(drops) <= 0) {
                        continue;
                     }
                  }

                  for (ItemStack drop : drops) {
                     ItemComparableItemStack key = new ItemComparableItemStack(drop, true);
                     Integer count = ret.get(key);
                     if (count == null) {
                        count = 0;
                     }

                     count = count + StackUtil.getSize(drop);
                     ret.put(key, count);
                  }
               }
            }
         }
      }

      return ret;
   }

   private List<Tuple.T2<ItemStack, Integer>> scanMapToSortedList(Map<ItemComparableItemStack, Integer> map) {
      List<Tuple.T2<ItemStack, Integer>> ret = new ArrayList<>(map.size());

      for (Entry<ItemComparableItemStack, Integer> entry : map.entrySet()) {
         ret.add(new Tuple.T2<>(entry.getKey().toStack(), entry.getValue()));
      }

      Collections.sort(ret, new Comparator<Tuple.T2<ItemStack, Integer>>() {
         {
            Objects.requireNonNull(ItemScanner.this);
         }

         public int compare(Tuple.T2<ItemStack, Integer> a, Tuple.T2<ItemStack, Integer> b) {
            return b.b - a.b;
         }
      });
      return ret;
   }
}
