package ic2.core.item.upgrade;

import com.google.common.base.Predicate;
import ic2.api.item.ElectricItem;
import ic2.api.item.ICustomDamageItem;
import ic2.api.item.IElectricItem;
import ic2.api.item.IItemHudInfo;
import ic2.api.upgrade.IFullUpgrade;
import ic2.api.upgrade.IUpgradableBlock;
import ic2.api.upgrade.UpgradableProperty;
import ic2.api.upgrade.UpgradeRegistry;
import ic2.core.IC2;
import ic2.core.IHasGui;
import ic2.core.block.state.IIdProvider;
import ic2.core.gui.dynamic.DynamicHandHeldContainer;
import ic2.core.init.Localization;
import ic2.core.item.IHandHeldSubInventory;
import ic2.core.item.ItemDefaults;
import ic2.core.item.ItemIC2;
import ic2.core.item.ItemMulti;
import ic2.core.item.tool.HandHeldInventory;
import ic2.core.model.ModelKey;
import ic2.core.model.ModelRegistry;
import ic2.core.profile.NotClassic;
import ic2.core.ref.ItemName;
import ic2.core.util.ItemData;
import ic2.core.util.ItemStacks;
import ic2.core.util.LiquidUtil;
import ic2.core.util.NbtChecks;
import ic2.core.util.StackUtil;
import ic2.core.util.Util;
import java.text.DecimalFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class ItemUpgradeModule extends ItemMulti<ItemUpgradeModule.UpgradeType> implements IFullUpgrade, IHandHeldSubInventory, IItemHudInfo {
   private static final DecimalFormat decimalformat = new DecimalFormat("0.##");
   private static final List<StackUtil.AdjacentInv> emptyInvList = Collections.emptyList();
   private static final List<LiquidUtil.AdjacentFluidHandler> emptyFhList = Collections.emptyList();

   public ItemUpgradeModule() {
      super(ItemName.upgrade, ItemUpgradeModule.UpgradeType.class);
      ItemDefaults.variants(this, true);
   }

   public void initializeUpgrades() {
      for (ItemUpgradeModule.UpgradeType type : ItemUpgradeModule.UpgradeType.values()) {
         UpgradeRegistry.register(ItemStacks.create(this, 1, type.getId()));
      }
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public void registerModels(final ItemName name) {
      ModelRegistry.setCustomMeshDefinition(
         this,
         new ModelRegistry.ItemMeshDefinition() {
            {
               Objects.requireNonNull(ItemUpgradeModule.this);
            }

            @Override
            public ModelKey func_178113_a(ItemStack stack) {
               ItemUpgradeModule.UpgradeType type = ItemUpgradeModule.this.getType(stack);
               if (type == null) {
                  return new ModelKey("builtin/missing", "missing");
               } else {
                  Direction dir;
                  return type.directional && (dir = ItemUpgradeModule.getDirection(stack)) != null
                     ? ItemIC2.getModelLocation(name, type.getName() + "_" + dir.getName())
                     : ItemIC2.getModelLocation(name, type.getName());
               }
            }
         }
      );

      for (ItemUpgradeModule.UpgradeType type : this.typeProperty.getAllowedValues()) {
         ModelRegistry.registerItemVariants(this, new ModelKey[]{getModelLocation(name, type.getName())});
         if (type.directional) {
            for (Direction dir : Direction.values()) {
               ModelRegistry.registerItemVariants(this, new ModelKey[]{getModelLocation(name, type.getName() + "_" + dir.getName())});
            }
         }
      }
   }

   @Override
   public List<String> getHudInfo(ItemStack stack, boolean advanced) {
      List<String> info = new LinkedList<>();
      info.add("Machine Upgrade");
      return info;
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public void func_77624_a(ItemStack stack, Level world, List<String> tooltip, TooltipFlag advanced) {
      super.func_77624_a(stack, world, tooltip, advanced);
      ItemUpgradeModule.UpgradeType type = this.getType(stack);
      if (type != null) {
         switch (type) {
            case overclocker:
               tooltip.add(
                  Localization.translate(
                     "ic2.tooltip.upgrade.overclocker.time",
                     decimalformat.format(100.0 * Math.pow(this.getProcessTimeMultiplier(stack, null), StackUtil.getSize(stack)))
                  )
               );
               tooltip.add(
                  Localization.translate(
                     "ic2.tooltip.upgrade.overclocker.power",
                     decimalformat.format(100.0 * Math.pow(this.getEnergyDemandMultiplier(stack, null), StackUtil.getSize(stack)))
                  )
               );
               break;
            case transformer:
               tooltip.add(Localization.translate("ic2.tooltip.upgrade.transformer", this.getExtraTier(stack, null) * StackUtil.getSize(stack)));
               break;
            case energy_storage:
               tooltip.add(Localization.translate("ic2.tooltip.upgrade.storage", this.getExtraEnergyStorage(stack, null) * StackUtil.getSize(stack)));
               break;
            case redstone_inverter:
               tooltip.add(Localization.translate("ic2.tooltip.upgrade.redstone"));
               break;
            case ejector:
            case advanced_ejector: {
               String side = getSideName(stack);
               tooltip.add(Localization.translate("ic2.tooltip.upgrade.ejector", Localization.translate(side)));
               break;
            }
            case pulling:
            case advanced_pulling: {
               String side = getSideName(stack);
               tooltip.add(Localization.translate("ic2.tooltip.upgrade.pulling", Localization.translate(side)));
               break;
            }
            case fluid_ejector: {
               String side = getSideName(stack);
               tooltip.add(Localization.translate("ic2.tooltip.upgrade.ejector", Localization.translate(side)));
               break;
            }
            case fluid_pulling: {
               String side = getSideName(stack);
               tooltip.add(Localization.translate("ic2.tooltip.upgrade.pulling", Localization.translate(side)));
               break;
            }
            case remote_interface:
               tooltip.add(Localization.translate("ic2.tooltip.upgrade.remote_interface", StackUtil.getSize(stack)));
         }
      }
   }

   private static String getSideName(ItemStack stack) {
      Direction dir = getDirection(stack);
      if (dir == null) {
         return "ic2.tooltip.upgrade.ejector.anyside";
      } else {
         switch (dir) {
            case WEST:
               return "ic2.dir.west";
            case EAST:
               return "ic2.dir.east";
            case DOWN:
               return "ic2.dir.bottom";
            case UP:
               return "ic2.dir.top";
            case NORTH:
               return "ic2.dir.north";
            case SOUTH:
               return "ic2.dir.south";
            default:
               throw new RuntimeException("invalid dir: " + dir);
         }
      }
   }

   @Override
   public InteractionResult func_180614_a(
      Player player, Level world, BlockPos pos, InteractionHand hand, Direction side, float xOffset, float yOffset, float zOffset
   ) {
      ItemStack stack = StackUtil.get(player, hand);
      ItemUpgradeModule.UpgradeType type = this.getType(stack);
      if (type == null) {
         return InteractionResult.PASS;
      } else if (type.directional) {
         int dir = 1 + side.ordinal();
         CompoundTag nbtData = StackUtil.getOrCreateNbtData(stack);
         if (nbtData.getByteOr("dir", (byte)0) == dir) {
            nbtData.putByte("dir", (byte)0);
            ItemData.commit(stack, nbtData);
         } else {
            nbtData.putByte("dir", (byte)dir);
            ItemData.commit(stack, nbtData);
         }

         if (IC2.platform.isRendering()) {
            switch (type) {
               case ejector:
               case advanced_ejector:
                  IC2.platform.messagePlayer(player, Localization.translate("ic2.tooltip.upgrade.ejector", Localization.translate(getSideName(stack))));
                  break;
               case pulling:
               case advanced_pulling:
                  IC2.platform.messagePlayer(player, Localization.translate("ic2.tooltip.upgrade.pulling", Localization.translate(getSideName(stack))));
                  break;
               case fluid_ejector:
                  IC2.platform.messagePlayer(player, Localization.translate("ic2.tooltip.upgrade.ejector", Localization.translate(getSideName(stack))));
                  break;
               case fluid_pulling:
                  IC2.platform.messagePlayer(player, Localization.translate("ic2.tooltip.upgrade.pulling", Localization.translate(getSideName(stack))));
            }
         }

         return InteractionResult.SUCCESS;
      } else {
         return InteractionResult.PASS;
      }
   }

   public boolean onDroppedByPlayer(ItemStack stack, Player player) {
      ItemUpgradeModule.UpgradeType type = this.getType(stack);
      if (type != null) {
         switch (type) {
            case advanced_ejector:
            case advanced_pulling:
               if (!player.level().isClientSide() && !StackUtil.isEmpty(stack) && player.containerMenu instanceof DynamicHandHeldContainer) {
                  HandHeldInventory base = (HandHeldInventory)((DynamicHandHeldContainer)player.containerMenu).base;
                  if (base instanceof HandHeldAdvancedUpgrade && base.isThisContainer(stack)) {
                     base.saveAsThrown(stack);
                     player.closeContainer();
                  }
               }
         }
      }

      return true;
   }

   @Override
   public boolean isSuitableFor(ItemStack stack, Set<UpgradableProperty> types) {
      ItemUpgradeModule.UpgradeType type = this.getType(stack);
      if (type == null) {
         return false;
      } else {
         switch (type) {
            case overclocker:
               return types.contains(UpgradableProperty.Processing) || types.contains(UpgradableProperty.Augmentable);
            case transformer:
               return types.contains(UpgradableProperty.Transformer);
            case energy_storage:
               return types.contains(UpgradableProperty.EnergyStorage);
            case redstone_inverter:
               return types.contains(UpgradableProperty.RedstoneSensitive);
            case ejector:
            case advanced_ejector:
               return types.contains(UpgradableProperty.ItemProducing);
            case pulling:
            case advanced_pulling:
               return types.contains(UpgradableProperty.ItemConsuming);
            case fluid_ejector:
               return types.contains(UpgradableProperty.FluidProducing);
            case fluid_pulling:
               return types.contains(UpgradableProperty.FluidConsuming);
            case remote_interface:
               return types.contains(UpgradableProperty.RemotelyAccessible);
            default:
               return false;
         }
      }
   }

   @Override
   public int getAugmentation(ItemStack stack, IUpgradableBlock parent) {
      ItemUpgradeModule.UpgradeType type = this.getType(stack);
      if (type == null) {
         return 0;
      } else {
         switch (type) {
            case overclocker:
               return 1;
            default:
               return 0;
         }
      }
   }

   @Override
   public int getExtraProcessTime(ItemStack stack, IUpgradableBlock parent) {
      return 0;
   }

   @Override
   public double getProcessTimeMultiplier(ItemStack stack, IUpgradableBlock parent) {
      ItemUpgradeModule.UpgradeType type = this.getType(stack);
      if (type == null) {
         return 1.0;
      } else {
         switch (type) {
            case overclocker:
               return 0.7;
            default:
               return 1.0;
         }
      }
   }

   @Override
   public int getExtraEnergyDemand(ItemStack stack, IUpgradableBlock parent) {
      return 0;
   }

   @Override
   public double getEnergyDemandMultiplier(ItemStack stack, IUpgradableBlock parent) {
      ItemUpgradeModule.UpgradeType type = this.getType(stack);
      if (type == null) {
         return 1.0;
      } else {
         switch (type) {
            case overclocker:
               return 1.6;
            default:
               return 1.0;
         }
      }
   }

   @Override
   public int getExtraEnergyStorage(ItemStack stack, IUpgradableBlock parent) {
      ItemUpgradeModule.UpgradeType type = this.getType(stack);
      if (type == null) {
         return 0;
      } else {
         switch (type) {
            case energy_storage:
               return 10000;
            default:
               return 0;
         }
      }
   }

   @Override
   public double getEnergyStorageMultiplier(ItemStack stack, IUpgradableBlock parent) {
      return 1.0;
   }

   @Override
   public int getExtraTier(ItemStack stack, IUpgradableBlock parent) {
      ItemUpgradeModule.UpgradeType type = this.getType(stack);
      if (type == null) {
         return 0;
      } else {
         switch (type) {
            case transformer:
               return 1;
            default:
               return 0;
         }
      }
   }

   @Override
   public boolean modifiesRedstoneInput(ItemStack stack, IUpgradableBlock parent) {
      ItemUpgradeModule.UpgradeType type = this.getType(stack);
      if (type == null) {
         return false;
      } else {
         switch (type) {
            case redstone_inverter:
               return true;
            default:
               return false;
         }
      }
   }

   @Override
   public int getRedstoneInput(ItemStack stack, IUpgradableBlock parent, int externalInput) {
      ItemUpgradeModule.UpgradeType type = this.getType(stack);
      if (type == null) {
         return externalInput;
      } else {
         switch (type) {
            case redstone_inverter:
               return 15 - externalInput;
            default:
               return externalInput;
         }
      }
   }

   @Override
   public int getRangeAmplification(ItemStack stack, IUpgradableBlock parent, int existingRange) {
      ItemUpgradeModule.UpgradeType type = this.getType(stack);
      if (type == null) {
         return existingRange;
      } else {
         switch (type) {
            case remote_interface:
               return existingRange << 1;
            default:
               return existingRange;
         }
      }
   }

   @Override
   public boolean onTick(ItemStack stack, IUpgradableBlock parent) {
      ItemUpgradeModule.UpgradeType type = this.getType(stack);
      if (type == null) {
         return false;
      } else {
         int size = StackUtil.getSize(stack);
         BlockEntity te = (BlockEntity)parent;
         boolean ret = false;
         switch (type) {
            case ejector:
               int amount = (int)Math.pow(4.0, Math.min(4, size - 1));

               for (StackUtil.AdjacentInv inv : getTargetInventories(stack, te)) {
                  StackUtil.transfer(te, inv.te, inv.dir, amount);
               }
               break;
            case advanced_ejector:
               int amount = (int)Math.pow(4.0, Math.min(4, size - 1));

               for (StackUtil.AdjacentInv inv : getTargetInventories(stack, te)) {
                  StackUtil.transfer(te, inv.te, inv.dir, amount, stackChecker(stack));
               }
               break;
            case pulling:
               int amount = (int)Math.pow(4.0, Math.min(4, size - 1));

               for (StackUtil.AdjacentInv inv : getTargetInventories(stack, te)) {
                  StackUtil.transfer(inv.te, te, inv.dir.getOpposite(), amount);
               }
               break;
            case advanced_pulling:
               int amount = (int)Math.pow(4.0, Math.min(4, size - 1));

               for (StackUtil.AdjacentInv inv : getTargetInventories(stack, te)) {
                  StackUtil.transfer(inv.te, te, inv.dir.getOpposite(), amount, stackChecker(stack));
               }
               break;
            case fluid_ejector:
               if (!LiquidUtil.isFluidTile(te, null)) {
                  return false;
               }

               int amount = (int)(50.0 * Math.pow(4.0, Math.min(4, size - 1)));

               for (LiquidUtil.AdjacentFluidHandler fh : getTargetFluidHandlers(stack, te)) {
                  LiquidUtil.transfer(te, fh.dir, fh.handler, amount);
               }
               break;
            case fluid_pulling:
               if (!LiquidUtil.isFluidTile(te, null)) {
                  return false;
               }

               int amount = (int)(50.0 * Math.pow(4.0, Math.min(4, size - 1)));

               for (LiquidUtil.AdjacentFluidHandler fh : getTargetFluidHandlers(stack, te)) {
                  LiquidUtil.transfer(fh.handler, fh.dir.getOpposite(), te, amount);
               }
               break;
            default:
               return false;
         }

         return ret;
      }
   }

   private static Predicate<ItemStack> stackChecker(final ItemStack stack) {
      return new Predicate<ItemStack>() {
         private boolean hasInitialised = false;
         private Set<ItemStack> filters;
         private Settings meta;
         private Settings damage;
         private Settings energy;
         private NbtSettings nbt;

         private void initalise() {
            assert !this.hasInitialised;

            CompoundTag tag = StackUtil.getOrCreateNbtData(stack);
            this.filters = this.getFilterStacks(tag);
            this.meta = new Settings(HandHeldAdvancedUpgrade.getTag(tag, "meta"));
            this.damage = null;
            this.nbt = NbtSettings.getFromNBT(HandHeldAdvancedUpgrade.getTag(tag, "nbt").getByteOr("type", (byte)0));
            this.energy = new Settings(HandHeldAdvancedUpgrade.getTag(tag, "energy"));
            this.hasInitialised = true;
         }

         private Set<ItemStack> getFilterStacks(CompoundTag nbt) {
            Set<ItemStack> ret = new HashSet<>();
            ListTag contentList = NbtChecks.getList(nbt, "Items", 10);

            for (int tag = 0; tag < contentList.size(); tag++) {
               CompoundTag slotNbt = contentList.getCompoundOrEmpty(tag);
               int slot = slotNbt.getByteOr("Slot", (byte)0);
               if (slot >= 0 && slot < 9) {
                  ItemStack filter = ItemStacks.read(slotNbt);
                  if (!StackUtil.isEmpty(filter)) {
                     ret.add(filter);
                  }
               }
            }

            return ret;
         }

         private boolean checkMeta(ItemStack stackx, ItemStack filter) {
            assert this.meta.active;

            assert this.meta.comparison == ComparisonType.DIRECT;

            return ItemStacks.rawMeta(stack) == ItemStacks.rawMeta(filter);
         }

         private boolean checkDamage(ItemStack stackx, ItemStack filter, boolean customStack) {
            assert this.damage.active;

            assert this.damage.comparison == ComparisonType.DIRECT;

            return customStack && filter.getItem() instanceof ICustomDamageItem
               ? ((ICustomDamageItem)stack.getItem()).getCustomDamage(stack) == ((ICustomDamageItem)filter.getItem()).getCustomDamage(filter)
               : ItemStacks.rawMeta(filter) == ItemStacks.rawMeta(stack);
         }

         private boolean checkNBT(ItemStack stackx, ItemStack filter) {
            switch (this.nbt) {
               case IGNORED:
                  return true;
               case FUZZY:
                  return StackUtil.checkNbtEquality(ItemData.read(stack), ItemData.read(filter));
               case EXACT:
                  return StackUtil.checkNbtEqualityStrict(stack, filter);
               default:
                  throw new IllegalStateException("Unexpected NBT state: " + this.nbt);
            }
         }

         private boolean checkEnergy(ItemStack stackx, ItemStack filter) {
            assert this.energy.active;

            assert this.energy.comparison == ComparisonType.DIRECT;

            return filter.getItem() instanceof IElectricItem && Util.isSimilar(ElectricItem.manager.getCharge(stack), ElectricItem.manager.getCharge(filter));
         }

         public boolean apply(ItemStack stackx) {
            if (!this.hasInitialised) {
               this.initalise();
            }

            boolean checkMeta;
            if (!this.meta.comparison.ignoreFilters()) {
               if (!this.meta.doComparison(ItemStacks.rawMeta(stack))) {
                  return false;
               }

               checkMeta = false;
            } else {
               checkMeta = this.meta.active;
            }

            boolean customStack = stack.getItem() instanceof ICustomDamageItem;
            boolean checkDamage = false;
            boolean checkEnergy;
            if (!this.energy.comparison.ignoreFilters()) {
               if (!(stack.getItem() instanceof IElectricItem) || !this.energy.doComparison((int)ElectricItem.manager.getCharge(stack))) {
                  return false;
               }

               checkEnergy = false;
            } else {
               checkEnergy = this.energy.active;
               if (checkEnergy && !(stack.getItem() instanceof IElectricItem)) {
                  return false;
               }
            }

            for (ItemStack filter : this.filters) {
               if (filter.getItem() == stack.getItem()
                  && (!checkMeta || this.checkMeta(stack, filter))
                  && (!checkDamage || this.checkDamage(stack, filter, customStack))
                  && this.checkNBT(stack, filter)
                  && (!checkEnergy || this.checkEnergy(stack, filter))) {
                  return true;
               }
            }

            return this.filters.isEmpty() && this.meta.active && !checkMeta && this.energy.active && !checkEnergy;
         }
      };
   }

   private static List<StackUtil.AdjacentInv> getTargetInventories(ItemStack stack, BlockEntity parent) {
      Direction dir = getDirection(stack);
      if (dir == null) {
         return StackUtil.getAdjacentInventories(parent);
      } else {
         StackUtil.AdjacentInv inv = StackUtil.getAdjacentInventory(parent, dir);
         return inv == null ? emptyInvList : Collections.singletonList(inv);
      }
   }

   private static List<LiquidUtil.AdjacentFluidHandler> getTargetFluidHandlers(ItemStack stack, BlockEntity parent) {
      Direction dir = getDirection(stack);
      if (dir == null) {
         return LiquidUtil.getAdjacentHandlers(parent);
      } else {
         LiquidUtil.AdjacentFluidHandler fh = LiquidUtil.getAdjacentHandler(parent, dir);
         return fh == null ? emptyFhList : Collections.singletonList(fh);
      }
   }

   @Override
   public Collection<ItemStack> onProcessEnd(ItemStack stack, IUpgradableBlock parent, Collection<ItemStack> output) {
      return output;
   }

   @Override
   public IHasGui getInventory(Player player, ItemStack stack) {
      ItemUpgradeModule.UpgradeType type = this.getType(stack);
      if (type == null) {
         return null;
      } else {
         switch (type) {
            case advanced_ejector:
            case advanced_pulling:
               return new HandHeldAdvancedUpgrade(player, stack);
            default:
               return null;
         }
      }
   }

   @Override
   public IHasGui getSubInventory(Player player, ItemStack stack, int ID) {
      ItemUpgradeModule.UpgradeType type = this.getType(stack);
      if (type == null) {
         return null;
      } else {
         switch (type) {
            case advanced_ejector:
            case advanced_pulling:
               return HandHeldAdvancedUpgrade.delegate(player, stack, ID);
            default:
               return null;
         }
      }
   }

   private static Direction getDirection(ItemStack stack) {
      int rawDir = StackUtil.getOrCreateNbtData(stack).getByteOr("dir", (byte)0);
      return rawDir >= 1 && rawDir <= 6 ? Direction.values()[rawDir - 1] : null;
   }

   public static enum UpgradeType implements IIdProvider {
      overclocker(false),
      transformer(false),
      energy_storage(false),
      redstone_inverter(false),
      ejector(true),
      @NotClassic
      advanced_ejector(true),
      pulling(true),
      @NotClassic
      advanced_pulling(true),
      fluid_ejector(true),
      fluid_pulling(true),
      @NotClassic
      remote_interface(false);

      public final boolean directional;

      private UpgradeType(boolean directional) {
         this.directional = directional;
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
