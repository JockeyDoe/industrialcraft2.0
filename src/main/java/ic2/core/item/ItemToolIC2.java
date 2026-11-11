package ic2.core.item;

import ic2.api.item.IBoxable;
import ic2.core.IC2;
import ic2.core.block.BlockBase;
import ic2.core.init.Localization;
import ic2.core.item.tool.HarvestLevel;
import ic2.core.item.tool.HarvestRules;
import ic2.core.item.tool.IToolClass;
import ic2.core.profile.Version;
import ic2.core.ref.BlockMaterial;
import ic2.core.ref.IItemModelProvider;
import ic2.core.ref.ItemName;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.component.Tool.Rule;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public abstract class ItemToolIC2 extends ItemIC2 implements IItemModelProvider, IBoxable {
   protected Rarity rarity = Rarity.COMMON;
   protected float efficiency;
   protected final Set<? extends IToolClass> toolClasses;
   private final int legacyHarvestLevel;

   public int harvestLevel(String name) {
      return this.toolClasses.stream().anyMatch(type -> Objects.equals(name, type.getName())) ? this.legacyHarvestLevel : -1;
   }

   protected ItemToolIC2(ItemName name, HarvestLevel harvestLevel, Set<? extends IToolClass> toolClasses) {
      this(name, harvestLevel, toolClasses, new HashSet<>());
   }

   protected ItemToolIC2(ItemName name, HarvestLevel harvestLevel, Set<? extends IToolClass> toolClasses, Set<Block> mineableBlocks) {
      this(name, 0.0F, 0.0F, harvestLevel, toolClasses, mineableBlocks);
   }

   protected ItemToolIC2(ItemName name, float damage, float speed, HarvestLevel harvestLevel, Set<? extends IToolClass> toolClasses, Set<Block> mineableBlocks) {
      super(name, toolProperties(harvestLevel.toolMaterial, toolClasses, mineableBlocks, damage, speed));
      this.toolClasses = toolClasses;
      this.legacyHarvestLevel = harvestLevel.level;
      this.efficiency = harvestLevel.toolMaterial.speed();
   }

   private static Properties toolProperties(ToolMaterial material, Set<? extends IToolClass> classes, Set<Block> blocks, float damage, float speed) {
      List<Rule> rules = new ArrayList<>();
      HolderGetter<Block> lookup = BuiltInRegistries.acquireBootstrapRegistrationLookup(BuiltInRegistries.BLOCK);
      rules.add(Rule.deniesDrops(lookup.getOrThrow(material.incorrectBlocksForDrops())));

      for (IToolClass toolClass : classes) {
         String name = toolClass.getName();
         if (name != null && Set.of("axe", "pickaxe", "shovel", "hoe").contains(name)) {
            TagKey<Block> tag = TagKey.create(Registries.BLOCK, Identifier.withDefaultNamespace("mineable/" + name));
            rules.add(Rule.minesAndDrops(lookup.getOrThrow(tag), material.speed()));
         }
      }

      if (!blocks.isEmpty()) {
         rules.add(Rule.minesAndDrops(HolderSet.direct(blocks.stream().map(Block::builtInRegistryHolder).toList()), material.speed()));
      }

      ItemAttributeModifiers attributes = ItemAttributeModifiers.builder()
         .add(
            Attributes.ATTACK_DAMAGE,
            new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, damage + material.attackDamageBonus(), Operation.ADD_VALUE),
            EquipmentSlotGroup.MAINHAND
         )
         .add(Attributes.ATTACK_SPEED, new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, speed, Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
         .build();
      return new Properties()
         .durability(material.durability())
         .repairable(material.repairItems())
         .enchantable(material.enchantmentValue())
         .attributes(attributes)
         .component(DataComponents.TOOL, new Tool(rules, 1.0F, 1, true));
   }

   @Override
   public String func_77658_a() {
      return this.getDescriptionId();
   }

   @Override
   public String func_77667_c(ItemStack itemStack) {
      return this.func_77658_a();
   }

   @Override
   public String func_77657_g(ItemStack itemStack) {
      return this.func_77667_c(itemStack);
   }

   @Override
   public String func_77653_i(ItemStack itemStack) {
      return Localization.translate(this.func_77667_c(itemStack));
   }

   @Override
   protected boolean func_194125_a(CreativeModeTab tab) {
      return this.isEnabled() && tab == IC2.tabIC2;
   }

   @Override
   public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
      return ItemIC2.shouldReequip(oldStack, newStack, slotChanged);
   }

   public boolean canHarvestBlock(BlockState state, ItemStack itemStack) {
      if (state.getBlock() instanceof BlockBase block && block.harvestRequirement(state) != null) {
         return HarvestRules.allows(itemStack, block.harvestRequirement(state));
      } else {
         BlockMaterial material = BlockMaterial.of(state);

         for (IToolClass toolClass : this.toolClasses) {
            if (toolClass.getBlacklist().contains(state.getBlock())) {
               return false;
            }

            if (toolClass.getBlacklist().contains(material)) {
               return false;
            }

            if (toolClass.getWhitelist().contains(state.getBlock())) {
               return true;
            }

            if (toolClass.getWhitelist().contains(material)) {
               return true;
            }
         }

         return super.isCorrectToolForDrops(itemStack, state);
      }
   }

   public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
      return this.canHarvestBlock(state, stack);
   }

   public float getDestroySpeed(ItemStack itemStack, BlockState state) {
      return this.canHarvestBlock(state, itemStack) ? this.efficiency : super.getDestroySpeed(itemStack, state);
   }

   @Override
   public Rarity func_77613_e(ItemStack stack) {
      return stack.isEnchanted() && this.rarity != Rarity.EPIC ? Rarity.RARE : this.rarity;
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public void registerModels(ItemName name) {
      ItemIC2.registerModel(this, 0, name, null);
   }

   @Override
   public boolean canBeStoredInToolbox(ItemStack itemStack) {
      return true;
   }

   @OnlyIn(Dist.CLIENT)
   public int getItemColor(ItemStack stack, int tintIndex) {
      return 16777215;
   }

   public ItemToolIC2 setRarity(Rarity rarity) {
      if (rarity == null) {
         throw new NullPointerException("Rarity cannot be null");
      } else {
         this.rarity = rarity;
         return this;
      }
   }

   @Override
   protected boolean isEnabled() {
      return Version.shouldEnable(this.getClass());
   }
}
