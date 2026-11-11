package ic2.core.item;

import ic2.core.IC2;
import ic2.core.init.BlocksItems;
import ic2.core.init.Localization;
import ic2.core.model.ModelKey;
import ic2.core.model.ModelRegistry;
import ic2.core.profile.Version;
import ic2.core.ref.IItemModelProvider;
import ic2.core.ref.ItemName;
import ic2.core.util.ItemStacks;
import ic2.core.util.StackUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.capabilities.ItemCapability;
import net.neoforged.neoforge.transfer.access.ItemAccess;

public class ItemIC2 extends Item implements IItemModelProvider {
   private Rarity rarity = Rarity.COMMON;

   public void func_150895_a(CreativeModeTab tab, NonNullList<ItemStack> items) {
      if (this.func_194125_a(tab)) {
         items.add(new ItemStack(this));
      }
   }

   public InteractionResult useOn(UseOnContext context) {
      Player player = context.getPlayer();
      if (player == null) {
         return super.useOn(context);
      } else {
         BlockPos pos = context.getClickedPos();
         Vec3 hit = context.getClickLocation();
         return this.func_180614_a(
            player,
            context.getLevel(),
            pos,
            context.getHand(),
            context.getClickedFace(),
            (float)(hit.x - pos.getX()),
            (float)(hit.y - pos.getY()),
            (float)(hit.z - pos.getZ())
         );
      }
   }

   public InteractionResult func_180614_a(Player player, Level level, BlockPos pos, InteractionHand hand, Direction face, float x, float y, float z) {
      return super.useOn(new UseOnContext(player, hand, new BlockHitResult(new Vec3(pos.getX() + x, pos.getY() + y, pos.getZ() + z), face, pos, false)));
   }

   protected BlockHitResult func_77621_a(Level level, Player player, boolean liquids) {
      BlockHitResult hit = getPlayerPOVHitResult(level, player, liquids ? Fluid.SOURCE_ONLY : Fluid.NONE);
      return hit.getType() == Type.MISS ? null : hit;
   }

   public void func_77637_a(CreativeModeTab tab) {
      if (tab == null) {
         ItemDefaults.hideFromCreative(this);
      }
   }

   public ItemIC2(ItemName name) {
      this(name, new Properties());
   }

   protected ItemIC2(ItemName name, Properties properties) {
      super(properties.setId(ResourceKey.create(Registries.ITEM, IC2.getIdentifier(Objects.requireNonNull(name).name()))));
      if (name != null) {
         BlocksItems.registerItem(this, IC2.getIdentifier(name.name()));
         name.setInstance(this);
      }
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public void registerModels(ItemName name) {
      this.registerModel(0, name, null);
   }

   @OnlyIn(Dist.CLIENT)
   protected void registerModel(int meta, ItemName name) {
      registerModel(this, meta, name, null);
   }

   @OnlyIn(Dist.CLIENT)
   protected void registerModel(int meta, ItemName name, String extraName) {
      registerModel(this, meta, name, extraName);
   }

   @OnlyIn(Dist.CLIENT)
   public static void registerModel(Item item, int meta, ItemName name, String extraName) {
      ModelRegistry.setCustomModelResourceLocation(item, meta, getModelLocation(name, extraName));
   }

   @OnlyIn(Dist.CLIENT)
   public static ModelKey getModelLocation(ItemName name, String extraName) {
      StringBuilder loc = new StringBuilder();
      loc.append("ic2");
      loc.append(':');
      loc.append(name.getPath(extraName));
      return new ModelKey(loc.toString(), null);
   }

   @OnlyIn(Dist.CLIENT)
   public int getItemColor(ItemStack stack) {
      return 16777215;
   }

   public Component getName(ItemStack stack) {
      return Component.translatable(this.func_77667_c(stack));
   }

   public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> output, TooltipFlag flag) {
      super.appendHoverText(stack, context, display, output, flag);
      List<String> lines = new ArrayList<>();
      this.func_77624_a(stack, context.level(), lines, flag);
      lines.forEach(line -> output.accept(Component.literal(line)));
   }

   public void func_77624_a(ItemStack stack, Level level, List<String> lines, TooltipFlag flag) {
   }

   public String func_77658_a() {
      return this.getDescriptionId();
   }

   public boolean hasContainerItem(ItemStack stack) {
      return super.getCraftingRemainder(stack) != null;
   }

   public ItemStack getContainerItem(ItemStack stack) {
      ItemStackTemplate remainder = super.getCraftingRemainder(stack);
      return remainder == null ? ItemStack.EMPTY : remainder.create();
   }

   public ItemStackTemplate getCraftingRemainder(ItemInstance input) {
      ItemStack stack = input instanceof ItemStack value ? value.copy() : (input instanceof ItemStackTemplate template ? template.create() : null);
      if (stack == null) {
         throw new IllegalArgumentException("Unsupported item instance " + input.getClass());
      } else if (!this.hasContainerItem(stack)) {
         return null;
      } else {
         ItemStack result = this.getContainerItem(stack);
         return result.isEmpty() ? null : ItemStackTemplate.fromNonEmptyStack(result);
      }
   }

   public String func_77667_c(ItemStack stack) {
      return this.func_77658_a();
   }

   public String func_77657_g(ItemStack stack) {
      return this.func_77667_c(stack);
   }

   public String func_77653_i(ItemStack stack) {
      return Localization.translate(this.func_77667_c(stack));
   }

   protected boolean func_194125_a(CreativeModeTab tab) {
      return this.isEnabled() && tab == IC2.tabIC2;
   }

   protected boolean isEnabled() {
      return Version.shouldEnable(this.getClass());
   }

   public ItemIC2 setRarity(Rarity rarity) {
      if (rarity == null) {
         throw new NullPointerException("null rarity");
      } else {
         this.rarity = rarity;
         return this;
      }
   }

   public Rarity func_77613_e(ItemStack stack) {
      return stack.isEnchanted() && this.rarity != Rarity.EPIC ? Rarity.RARE : this.rarity;
   }

   public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
      return shouldReequip(oldStack, newStack, slotChanged);
   }

   public static boolean shouldReequip(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
      if (!StackUtil.checkItemEquality(newStack, oldStack)) {
         return true;
      } else if (oldStack == null) {
         return false;
      } else {
         return StackUtil.getSize(oldStack) != StackUtil.getSize(newStack) ? true : slotChanged && StackUtil.checkItemEqualityStrict(oldStack, newStack);
      }
   }

   protected static int getRemainingUses(ItemStack stack) {
      return stack.getMaxDamage() - ItemStacks.rawMeta(stack) + 1;
   }

   public <T> void addCapability(ItemCapability<T, ItemAccess> cap, BiFunction<ItemStack, ItemAccess, T> lookup) {
      ItemCapabilities.add(this, cap, lookup);
   }
}
