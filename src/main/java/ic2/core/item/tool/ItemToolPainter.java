package ic2.core.item.tool;

import ic2.api.item.IBoxable;
import ic2.core.IC2;
import ic2.core.audio.PositionSpec;
import ic2.core.block.state.EnumProperty;
import ic2.core.item.ItemDefaults;
import ic2.core.item.ItemIC2;
import ic2.core.model.ModelKey;
import ic2.core.model.ModelRegistry;
import ic2.core.ref.IMultiItem;
import ic2.core.ref.ItemName;
import ic2.core.util.BlockColors;
import ic2.core.util.Ic2Color;
import ic2.core.util.ItemData;
import ic2.core.util.ItemStacks;
import ic2.core.util.StackUtil;
import ic2.core.util.Util;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.EntityInteract;

public class ItemToolPainter extends ItemIC2 implements IMultiItem<Ic2Color>, IBoxable {
   private static final EnumProperty<Ic2Color> typeProperty = new EnumProperty<>("type", Ic2Color.class);
   private static final int maxDamage = 32;

   public ItemToolPainter() {
      super(ItemName.painter);
      ItemDefaults.durability(this, 31);
      ItemDefaults.stackSize(this, 1);
      ItemDefaults.variants(this, true);
      NeoForge.EVENT_BUS.register(this);
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public void registerModels(final ItemName name) {
      ModelRegistry.setCustomMeshDefinition(this, new ModelRegistry.ItemMeshDefinition() {
         {
            Objects.requireNonNull(ItemToolPainter.this);
         }

         @Override
         public ModelKey func_178113_a(ItemStack stack) {
            Ic2Color color = ItemToolPainter.this.getColor(stack);
            return ItemIC2.getModelLocation(name, color != null ? color.getName() : null);
         }
      });
      ModelRegistry.registerItemVariants(this, new ModelKey[]{getModelLocation(name, null)});

      for (Ic2Color type : typeProperty.getAllowedValues()) {
         ModelRegistry.registerItemVariants(this, new ModelKey[]{getModelLocation(name, type.getName())});
      }
   }

   public int getDamage(ItemStack stack) {
      int rawDamage = super.getDamage(stack);
      return rawDamage == 0 ? 0 : (rawDamage - 1) / Ic2Color.values.length;
   }

   public boolean isDamaged(ItemStack stack) {
      return this.getDamage(stack) > 0;
   }

   public void setDamage(ItemStack stack, int damage) {
      int oldRawDamage = super.getDamage(stack);
      if (oldRawDamage != 0) {
         int oldDamage = this.getDamage(stack);
         int newDamage = Util.limit(damage, 0, 32);
         super.setDamage(stack, oldRawDamage + (newDamage - oldDamage) * Ic2Color.values.length);
      }
   }

   public int getMetadata(ItemStack stack) {
      int rawDamage = super.getDamage(stack);
      return rawDamage != 0 && rawDamage != 32767 ? (rawDamage - 1) % Ic2Color.values.length + 1 : rawDamage;
   }

   public Ic2Color getColor(ItemStack stack) {
      int meta = this.getMetadata(stack);
      return meta == 0 ? null : Ic2Color.values[meta - 1];
   }

   public InteractionResult onItemUseFirst(Player player, Level world, BlockPos pos, Direction side, float hitX, float hitY, float hitZ, InteractionHand hand) {
      ItemStack stack = StackUtil.get(player, hand);
      Ic2Color color = this.getColor(stack);
      if (color == null) {
         return InteractionResult.PASS;
      } else {
         BlockState state = world.getBlockState(pos);
         Block block = state.getBlock();
         if (!this.colorBlock(world, pos, block, state, color.mcColor)) {
            return InteractionResult.PASS;
         } else {
            this.damagePainter(player, hand, color);
            if (world.isClientSide()) {
               IC2.audioManager.playOnce(player, PositionSpec.Hand, "Tools/Painter.ogg", true, IC2.audioManager.getDefaultVolume());
            }

            return (InteractionResult)(world.isClientSide() ? InteractionResult.PASS : InteractionResult.SUCCESS);
         }
      }
   }

   private boolean colorBlock(Level world, BlockPos pos, Block block, BlockState state, DyeColor newColor) {
      for (Property<?> property : state.getProperties()) {
         if (property.getValueClass() == DyeColor.class) {
            DyeColor oldColor = (DyeColor)state.getValue(property);
            if (oldColor != newColor && property.getPossibleValues().contains(newColor)) {
               world.setBlock(pos, (BlockState)state.setValue(property, newColor), 3);
               return true;
            }

            return false;
         }
      }

      Block replacement = BlockColors.recolor(block, newColor);
      if (replacement != null && replacement != block) {
         world.setBlock(pos, replacement.withPropertiesOf(state), 3);
         return true;
      } else if (block == Blocks.TERRACOTTA) {
         world.setBlock(pos, ((Block)Blocks.DYED_TERRACOTTA.pick(newColor)).defaultBlockState(), 3);
         return true;
      } else if (block == Blocks.GLASS) {
         world.setBlock(pos, ((Block)Blocks.STAINED_GLASS.pick(newColor)).defaultBlockState(), 3);
         return true;
      } else if (block == Blocks.GLASS_PANE) {
         world.setBlock(pos, ((Block)Blocks.STAINED_GLASS_PANE.pick(newColor)).defaultBlockState(), 3);
         return true;
      } else {
         return false;
      }
   }

   @SubscribeEvent
   public void onEntityInteract(EntityInteract event) {
      Player player = event.getEntity();
      if (!player.level().isClientSide()) {
         Entity entity = event.getTarget();
         ItemStack stack = player.getItemInHand(event.getHand());
         if (!StackUtil.isEmpty(stack) && stack.getItem() == this) {
            Ic2Color color = this.getColor(stack);
            if (color != null && entity instanceof Sheep sheep && sheep.getColor() != color.mcColor) {
               ((Sheep)entity).setColor(color.mcColor);
               this.damagePainter(player, event.getHand(), color);
               event.setCanceled(true);
            }
         }
      }
   }

   public InteractionResult use(Level world, Player player, InteractionHand hand) {
      ItemStack stack = StackUtil.get(player, hand);
      if (!world.isClientSide() && IC2.keyboard.isModeSwitchKeyDown(player)) {
         CompoundTag nbtData = StackUtil.getOrCreateNbtData(stack);
         boolean newValue = !nbtData.getBooleanOr("autoRefill", false);
         nbtData.putBoolean("autoRefill", newValue);
         ItemData.commit(stack, nbtData);
         if (newValue) {
            IC2.platform.messagePlayer(player, "Painter automatic refill mode enabled");
         } else {
            IC2.platform.messagePlayer(player, "Painter automatic refill mode disabled");
         }

         return InteractionResult.SUCCESS.heldItemTransformedTo(stack);
      } else {
         return InteractionResult.PASS;
      }
   }

   @Override
   public String func_77667_c(ItemStack stack) {
      Ic2Color color = this.getColor(stack);
      return color == null ? this.func_77658_a() : this.func_77658_a() + "." + color.getName();
   }

   @Override
   public final void func_150895_a(CreativeModeTab tab, NonNullList<ItemStack> subItems) {
      if (this.func_194125_a(tab)) {
         subItems.add(this.getItemStackUnchecked(null));

         for (Ic2Color type : typeProperty.getAllowedValues()) {
            subItems.add(this.getItemStackUnchecked(type));
         }
      }
   }

   @Override
   public Set<Ic2Color> getAllTypes() {
      return EnumSet.allOf(Ic2Color.class);
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public void func_77624_a(ItemStack stack, Level world, List<String> tooltip, TooltipFlag advanced) {
      Ic2Color color = this.getColor(stack);
      if (color != null) {
         ItemStack dyeStack = new ItemStack((ItemLike)Items.DYE.pick(color.mcColor), 1);
         tooltip.add(dyeStack.getHoverName().getString());
      }
   }

   public void damagePainter(Player player, InteractionHand hand, Ic2Color color) {
      assert color != null;

      ItemStack stack = StackUtil.get(player, hand);
      if (ItemStacks.rawMeta(stack) >= stack.getMaxDamage()) {
         CompoundTag nbtData = StackUtil.getOrCreateNbtData(stack);
         if (nbtData.getBooleanOr("autoRefill", false) && StackUtil.consumeFromPlayerInventory(player, StackUtil.oreDict(color.oreDictDyeName), 1, false)) {
            this.setDamage(stack, 0);
         } else {
            super.setDamage(stack, 0);
         }
      } else {
         stack.hurtAndBreak(1, player, hand);
      }
   }

   public ItemStack getItemStack(Ic2Color type) {
      if (type != null && !typeProperty.getAllowedValues().contains(type)) {
         throw new IllegalArgumentException("invalid property value " + type + " for property " + typeProperty);
      } else {
         return this.getItemStackUnchecked(type);
      }
   }

   private ItemStack getItemStackUnchecked(Ic2Color type) {
      return type == null ? new ItemStack(this) : ItemStacks.create(this, 1, 1 + type.getId());
   }

   @Override
   public ItemStack getItemStack(String variant) {
      Ic2Color type;
      if (variant != null && !variant.isEmpty()) {
         type = typeProperty.getValueByName(variant);
         if (type == null) {
            throw new IllegalArgumentException("invalid variant " + variant + " for " + this);
         }
      } else {
         type = null;
      }

      return this.getItemStackUnchecked(type);
   }

   @Override
   public String getVariant(ItemStack stack) {
      if (stack == null) {
         throw new NullPointerException("null stack");
      } else if (stack.getItem() != this) {
         throw new IllegalArgumentException("The stack " + stack + " doesn't match " + this);
      } else {
         Ic2Color color = this.getColor(stack);
         return color == null ? null : color.getName();
      }
   }

   @Override
   public boolean canBeStoredInToolbox(ItemStack itemstack) {
      return true;
   }
}
