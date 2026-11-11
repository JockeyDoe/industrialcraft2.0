package ic2.core.item.tool;

import ic2.api.item.ElectricItem;
import ic2.core.IC2;
import ic2.core.audio.PositionSpec;
import ic2.core.item.ItemIC2;
import ic2.core.item.armor.ItemArmorNanoSuit;
import ic2.core.item.armor.ItemArmorQuantumSuit;
import ic2.core.model.ModelKey;
import ic2.core.model.ModelRegistry;
import ic2.core.ref.ItemName;
import ic2.core.slot.ArmorSlot;
import ic2.core.util.ItemData;
import ic2.core.util.StackUtil;
import java.util.EnumSet;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class ItemNanoSaber extends ItemElectricTool {
   public static int ticker = 0;
   private int soundTicker = 0;

   public ItemNanoSaber() {
      super(ItemName.nano_saber, 10, HarvestLevel.Diamond, EnumSet.of(ToolClass.Sword));
      this.maxCharge = 160000;
      this.transferLimit = 500;
      this.tier = 3;
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public void registerModels(final ItemName name) {
      String activeSuffix = "active";
      ModelRegistry.setCustomMeshDefinition(this, new ModelRegistry.ItemMeshDefinition() {
         {
            Objects.requireNonNull(ItemNanoSaber.this);
         }

         @Override
         public ModelKey func_178113_a(ItemStack stack) {
            return ItemIC2.getModelLocation(name, ItemNanoSaber.isActive(stack) ? "active" : null);
         }
      });
      ModelRegistry.registerItemVariants(this, new ModelKey[]{ItemIC2.getModelLocation(name, null)});
      ModelRegistry.registerItemVariants(this, new ModelKey[]{ItemIC2.getModelLocation(name, "active")});
   }

   @Override
   public float getDestroySpeed(ItemStack stack, BlockState state) {
      if (isActive(stack)) {
         this.soundTicker++;
         if (IC2.platform.isRendering() && this.soundTicker % 4 == 0) {
            IC2.platform.playSoundSp(this.getRandomSwingSound(), 1.0F, 1.0F);
         }

         return state.getBlock() == Blocks.COBWEB ? 50.0F : 4.0F;
      } else {
         return 1.0F;
      }
   }

   @Override
   public boolean func_77644_a(ItemStack stack, LivingEntity target, LivingEntity source) {
      if (!isActive(stack)) {
         return true;
      } else {
         if (IC2.platform.isSimulating()) {
            drainSaber(stack, 400.0, source);
            if (!(source instanceof ServerPlayer) || !(target instanceof Player) || ((ServerPlayer)source).canHarmPlayer((Player)target)) {
               for (EquipmentSlot slot : ArmorSlot.getAll()) {
                  if (!ElectricItem.manager.canUse(stack, 2000.0)) {
                     break;
                  }

                  ItemStack armor = target.getItemBySlot(slot);
                  if (armor != null) {
                     double amount = 0.0;
                     if (armor.getItem() instanceof ItemArmorNanoSuit) {
                        amount = 48000.0;
                     } else if (armor.getItem() instanceof ItemArmorQuantumSuit) {
                        amount = 300000.0;
                     }

                     if (amount > 0.0) {
                        ElectricItem.manager.discharge(armor, amount, this.tier, true, false, false);
                        if (!ElectricItem.manager.canUse(armor, 1.0)) {
                           target.setItemSlot(slot, null);
                        }

                        drainSaber(stack, 2000.0, source);
                     }
                  }
               }
            }
         }

         if (IC2.platform.isRendering()) {
            IC2.platform.playSoundSp(this.getRandomSwingSound(), 1.0F, 1.0F);
         }

         return true;
      }
   }

   public String getRandomSwingSound() {
      switch (IC2.random.nextInt(3)) {
         case 1:
            return "Tools/Nanosabre/NanosabreSwing2.ogg";
         case 2:
            return "Tools/Nanosabre/NanosabreSwing3.ogg";
         default:
            return "Tools/Nanosabre/NanosabreSwing1.ogg";
      }
   }

   public boolean canDestroyBlockInCreative(Level world, BlockPos pos, ItemStack stack, Player player) {
      return false;
   }

   public boolean onBlockStartBreak(ItemStack stack, BlockPos pos, Player player) {
      if (isActive(stack)) {
         drainSaber(stack, 80.0, player);
      }

      return false;
   }

   public boolean func_77662_d() {
      return true;
   }

   public static void drainSaber(ItemStack stack, double amount, LivingEntity entity) {
      if (!ElectricItem.manager.use(stack, amount, entity)) {
         CompoundTag nbt = StackUtil.getOrCreateNbtData(stack);
         setActive(nbt, false);
         ItemData.commit(stack, nbt);
      }
   }

   @Override
   public InteractionResult use(Level world, Player player, InteractionHand hand) {
      ItemStack stack = StackUtil.get(player, hand);
      if (world.isClientSide()) {
         return InteractionResult.PASS;
      } else {
         CompoundTag nbt = StackUtil.getOrCreateNbtData(stack);
         if (isActive(nbt)) {
            setActive(nbt, false);
            ItemData.commit(stack, nbt);
            return InteractionResult.SUCCESS.heldItemTransformedTo(stack);
         } else if (ElectricItem.manager.canUse(stack, 16.0)) {
            setActive(nbt, true);
            ItemData.commit(stack, nbt);
            return InteractionResult.SUCCESS.heldItemTransformedTo(stack);
         } else {
            return super.use(world, player, hand);
         }
      }
   }

   @Override
   public void func_77663_a(ItemStack stack, Level world, Entity entity, int slot, boolean par5) {
      super.func_77663_a(stack, world, entity, slot, par5 && isActive(stack));
      if (isActive(stack) && ticker % 16 == 0 && entity instanceof ServerPlayer) {
         if (slot < 9) {
            drainSaber(stack, 64.0, (Player)entity);
         } else if (ticker % 64 == 0) {
            drainSaber(stack, 16.0, (Player)entity);
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public Rarity func_77613_e(ItemStack stack) {
      return Rarity.UNCOMMON;
   }

   public static boolean isActive(ItemStack stack) {
      CompoundTag nbt = StackUtil.getOrCreateNbtData(stack);
      return isActive(nbt);
   }

   private static boolean isActive(CompoundTag nbt) {
      return nbt.getBooleanOr("active", false);
   }

   private static void setActive(CompoundTag nbt, boolean active) {
      nbt.putBoolean("active", active);
   }

   public boolean onEntitySwing(LivingEntity entity, ItemStack stack) {
      if (IC2.platform.isRendering() && isActive(stack)) {
         IC2.audioManager.playOnce(entity, PositionSpec.Hand, this.getRandomSwingSound(), true, IC2.audioManager.getDefaultVolume());
      }

      return false;
   }

   @Override
   protected String getIdleSound(LivingEntity player, ItemStack stack) {
      return "Tools/Nanosabre/NanosabreIdle.ogg";
   }

   @Override
   protected String getStartSound(LivingEntity player, ItemStack stack) {
      return "Tools/Nanosabre/NanosabrePowerup.ogg";
   }
}
