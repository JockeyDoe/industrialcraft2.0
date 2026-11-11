package ic2.core.init;

import ic2.api.info.Info;
import ic2.core.IC2;
import ic2.core.IC2Potion;
import ic2.core.Ic2Fluid;
import ic2.core.block.BlockBase;
import ic2.core.block.BlockDynamite;
import ic2.core.block.BlockFoam;
import ic2.core.block.BlockIC2Door;
import ic2.core.block.BlockIC2Fence;
import ic2.core.block.BlockIC2Fluid;
import ic2.core.block.BlockOre;
import ic2.core.block.BlockRubWood;
import ic2.core.block.BlockScaffold;
import ic2.core.block.BlockSheet;
import ic2.core.block.BlockTexGlass;
import ic2.core.block.BlockWall;
import ic2.core.block.Ic2Leaves;
import ic2.core.block.Ic2Sapling;
import ic2.core.block.TeBlockRegistry;
import ic2.core.block.machine.BlockMiningPipe;
import ic2.core.block.steam.BlockRefractoryBricks;
import ic2.core.block.type.ResourceBlock;
import ic2.core.crop.ItemCrop;
import ic2.core.item.BehaviorScrapboxDispense;
import ic2.core.item.ItemBattery;
import ic2.core.item.ItemBatteryChargeHotbar;
import ic2.core.item.ItemBatterySU;
import ic2.core.item.ItemBooze;
import ic2.core.item.ItemClassicCell;
import ic2.core.item.ItemCoke;
import ic2.core.item.ItemContainmentbox;
import ic2.core.item.ItemCropSeed;
import ic2.core.item.ItemCrystalMemory;
import ic2.core.item.ItemFilledFuelCan;
import ic2.core.item.ItemFluidCell;
import ic2.core.item.ItemHandlers;
import ic2.core.item.ItemIC2Boat;
import ic2.core.item.ItemIodineTablet;
import ic2.core.item.ItemMug;
import ic2.core.item.ItemMulti;
import ic2.core.item.ItemNuclearResource;
import ic2.core.item.ItemTerraWart;
import ic2.core.item.ItemTinCan;
import ic2.core.item.ItemToolbox;
import ic2.core.item.armor.ItemArmorAdvBatpack;
import ic2.core.item.armor.ItemArmorBatpack;
import ic2.core.item.armor.ItemArmorCFPack;
import ic2.core.item.armor.ItemArmorEnergypack;
import ic2.core.item.armor.ItemArmorHazmat;
import ic2.core.item.armor.ItemArmorIC2;
import ic2.core.item.armor.ItemArmorJetpack;
import ic2.core.item.armor.ItemArmorJetpackElectric;
import ic2.core.item.armor.ItemArmorLappack;
import ic2.core.item.armor.ItemArmorNanoSuit;
import ic2.core.item.armor.ItemArmorNightvisionGoggles;
import ic2.core.item.armor.ItemArmorQuantumSuit;
import ic2.core.item.armor.ItemArmorSolarHelmet;
import ic2.core.item.armor.ItemArmorStaticBoots;
import ic2.core.item.block.BehaviorTeBlockDispense;
import ic2.core.item.block.ItemBarrel;
import ic2.core.item.block.ItemBlockTileEntity;
import ic2.core.item.block.ItemCable;
import ic2.core.item.block.ItemDynamite;
import ic2.core.item.block.ItemFluidPipe;
import ic2.core.item.crafting.BlockCuttingBlade;
import ic2.core.item.crafting.UpgradeKit;
import ic2.core.item.logistics.ItemPumpCover;
import ic2.core.item.reactor.ItemReactorCondensator;
import ic2.core.item.reactor.ItemReactorDepletedUranium;
import ic2.core.item.reactor.ItemReactorHeatStorage;
import ic2.core.item.reactor.ItemReactorHeatSwitch;
import ic2.core.item.reactor.ItemReactorHeatpack;
import ic2.core.item.reactor.ItemReactorIridiumReflector;
import ic2.core.item.reactor.ItemReactorLithiumCell;
import ic2.core.item.reactor.ItemReactorMOX;
import ic2.core.item.reactor.ItemReactorPlating;
import ic2.core.item.reactor.ItemReactorReflector;
import ic2.core.item.reactor.ItemReactorUranium;
import ic2.core.item.reactor.ItemReactorVent;
import ic2.core.item.reactor.ItemReactorVentSpread;
import ic2.core.item.resources.ItemWindRotor;
import ic2.core.item.tfbp.Tfbp;
import ic2.core.item.tool.HarvestLevel;
import ic2.core.item.tool.Ic2Axe;
import ic2.core.item.tool.Ic2Hoe;
import ic2.core.item.tool.Ic2Pickaxe;
import ic2.core.item.tool.Ic2Shovel;
import ic2.core.item.tool.Ic2Sword;
import ic2.core.item.tool.ItemClassicSprayer;
import ic2.core.item.tool.ItemCropnalyzer;
import ic2.core.item.tool.ItemDebug;
import ic2.core.item.tool.ItemDrill;
import ic2.core.item.tool.ItemDrillIridium;
import ic2.core.item.tool.ItemElectricToolChainsaw;
import ic2.core.item.tool.ItemElectricToolHoe;
import ic2.core.item.tool.ItemFrequencyTransmitter;
import ic2.core.item.tool.ItemNanoSaber;
import ic2.core.item.tool.ItemObscurator;
import ic2.core.item.tool.ItemRemote;
import ic2.core.item.tool.ItemScanner;
import ic2.core.item.tool.ItemScannerAdv;
import ic2.core.item.tool.ItemSprayer;
import ic2.core.item.tool.ItemToolCrowbar;
import ic2.core.item.tool.ItemToolCutter;
import ic2.core.item.tool.ItemToolHammer;
import ic2.core.item.tool.ItemToolMeter;
import ic2.core.item.tool.ItemToolMiningLaser;
import ic2.core.item.tool.ItemToolPainter;
import ic2.core.item.tool.ItemToolWrench;
import ic2.core.item.tool.ItemToolWrenchElectric;
import ic2.core.item.tool.ItemToolWrenchNew;
import ic2.core.item.tool.ItemTreetap;
import ic2.core.item.tool.ItemTreetapElectric;
import ic2.core.item.tool.ItemWeedingTrowel;
import ic2.core.item.tool.ItemWindmeter;
import ic2.core.item.type.CasingResourceType;
import ic2.core.item.type.CellType;
import ic2.core.item.type.CraftingItemType;
import ic2.core.item.type.CropResItemType;
import ic2.core.item.type.DustResourceType;
import ic2.core.item.type.IngotResourceType;
import ic2.core.item.type.MiscResourceType;
import ic2.core.item.type.NuclearResourceType;
import ic2.core.item.type.OreResourceType;
import ic2.core.item.type.PlateResourceType;
import ic2.core.item.upgrade.ItemUpgradeModule;
import ic2.core.recipe.RecipeTags;
import ic2.core.ref.BlockMaterial;
import ic2.core.ref.BlockName;
import ic2.core.ref.FluidName;
import ic2.core.ref.ItemName;
import ic2.core.ref.TeBlock;
import java.util.Objects;
import java.util.function.Supplier;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.capabilities.Capabilities.Fluid;
import net.neoforged.neoforge.registries.NeoForgeRegistries.Keys;

public class BlocksItems {
   public static void init() {
      initPotions();
      initBlocks();
      initFluids();
      initItems();
      initMigration();
   }

   private static void initPotions() {
      Info.POTION_RADIATION = IC2Potion.radiation = new IC2Potion("radiation", true, 5149489);
   }

   private static void initBlocks() {
      TeBlockRegistry.addAll(TeBlock.class, TeBlock.invalid.getIdentifier());
      TeBlockRegistry.addCreativeRegisterer(TeBlock.invalid);
      TeBlock.reactor_chamber.setPlaceHandler(ItemHandlers.reactorChamberPlace);
      TeBlockRegistry.buildBlocks();
      ItemBlockTileEntity itemTeBlock = TeBlockRegistry.get(TeBlock.itnt.getIdentifier()).getItem();
      DispenserBlock.registerBehavior(itemTeBlock, new BehaviorTeBlockDispense());
      BlockOre.create();
      BlockName.resource.<BlockBase>getInstance().setHarvestLevel("pickaxe", 1, BlockName.resource.getBlockState(ResourceBlock.copper_ore));
      BlockName.resource.<BlockBase>getInstance().setHarvestLevel("pickaxe", 1, BlockName.resource.getBlockState(ResourceBlock.lead_ore));
      BlockName.resource.<BlockBase>getInstance().setHarvestLevel("pickaxe", 1, BlockName.resource.getBlockState(ResourceBlock.tin_ore));
      BlockName.resource.<BlockBase>getInstance().setHarvestLevel("pickaxe", 2, BlockName.resource.getBlockState(ResourceBlock.uranium_ore));
      BlockName.resource.<BlockBase>getInstance().setHarvestLevel("pickaxe", 2, BlockName.resource.getBlockState(ResourceBlock.reinforced_stone));
      new Ic2Leaves();
      BlockRubWood rubberWood = new BlockRubWood();
      RecipeTags.deferOre("woodRubber", () -> new ItemStack(rubberWood));
      BlockName.rubber_wood.<BlockBase>getInstance().setHarvestLevel("axe", 0);
      new Ic2Sapling();
      BlockScaffold.create();
      BlockIC2Fence.create();
      BlockSheet.create();
      BlockTexGlass.create();
      BlockFoam.create();
      BlockWall.create();
      BlockMiningPipe.create();
      new BlockIC2Door();
      new BlockDynamite();
      new BlockRefractoryBricks();
   }

   private static void initFluids() {
      BlockMaterial steam = BlockMaterial.liquid("steam", MapColor.COLOR_LIGHT_GRAY);
      registerIC2fluid(FluidName.uu_matter, BlockMaterial.WATER, 3867955, 3000, 3000, 0, 300, false);
      registerIC2fluid(FluidName.construction_foam, BlockMaterial.WATER, 2105376, 10000, 50000, 0, 300, false);
      registerIC2fluid(FluidName.coolant, BlockMaterial.WATER, 1333866, 1000, 3000, 0, 300, false);
      registerIC2fluid(FluidName.creosote, BlockMaterial.WATER, 4012298, 10000, 50000, 0, 300, false);
      registerIC2fluid(FluidName.hot_coolant, BlockMaterial.WATER, 11872308, 1000, 3000, 0, 1200, false);
      registerIC2fluid(FluidName.pahoehoe_lava, BlockMaterial.WATER, 8090732, 50000, 250000, 10, 1200, false);
      registerIC2fluid(FluidName.biomass, BlockMaterial.WATER, 3632933, 1000, 3000, 0, 300, false);
      registerIC2fluid(FluidName.biogas, BlockMaterial.WATER, 10983500, 1000, 3000, 0, 300, true);
      registerIC2fluid(FluidName.distilled_water, BlockMaterial.WATER, 4413173, 1000, 1000, 0, 300, false);
      registerIC2fluid(FluidName.superheated_steam, steam, 13291985, -3000, 100, 0, 600, true);
      registerIC2fluid(FluidName.steam, steam, 12369084, -800, 300, 0, 420, true);
      registerIC2fluid(FluidName.hot_water, BlockMaterial.WATER, 4644607, 1000, 1000, 0, 350, false);
      registerIC2fluid(FluidName.weed_ex, BlockMaterial.WATER, 478996, 1000, 1000, 0, 300, false);
      registerIC2fluid(FluidName.air, steam, 14474460, 0, 500, 0, 300, true);
      registerIC2fluid(FluidName.hydrogen, steam, 14474460, 0, 500, 0, 300, true);
      registerIC2fluid(FluidName.oxygen, steam, 14474460, 0, 500, 0, 300, true);
      registerIC2fluid(FluidName.heavy_water, BlockMaterial.WATER, 4413173, 1000, 1000, 0, 300, false);
      registerIC2fluid(FluidName.milk, BlockMaterial.WATER, 16579836, 1050, 1000, 0, 300, false);
   }

   private static void initItems() {
      ArmorMaterial bronzeArmorMaterial = NativeMaterials.BRONZE_ARMOR;
      ArmorMaterial alloyArmorMaterial = NativeMaterials.ALLOY_ARMOR;
      new ItemArmorAdvBatpack();
      new ItemArmorIC2(
         ItemName.alloy_chestplate,
         alloyArmorMaterial,
         "alloy",
         EquipmentSlot.CHEST,
         (Supplier<ItemStack>)() -> ItemName.crafting.getItemStack(CraftingItemType.alloy)
      );
      new ItemArmorBatpack();
      new ItemArmorIC2(ItemName.bronze_boots, bronzeArmorMaterial, "bronze", EquipmentSlot.FEET, "ingotBronze");
      new ItemArmorIC2(ItemName.bronze_chestplate, bronzeArmorMaterial, "bronze", EquipmentSlot.CHEST, "ingotBronze");
      new ItemArmorIC2(ItemName.bronze_helmet, bronzeArmorMaterial, "bronze", EquipmentSlot.HEAD, "ingotBronze");
      new ItemArmorIC2(ItemName.bronze_leggings, bronzeArmorMaterial, "bronze", EquipmentSlot.LEGS, "ingotBronze");
      new ItemArmorCFPack();
      new ItemArmorEnergypack();
      new ItemArmorHazmat(ItemName.hazmat_chestplate, EquipmentSlot.CHEST);
      new ItemArmorHazmat(ItemName.hazmat_helmet, EquipmentSlot.HEAD);
      new ItemArmorHazmat(ItemName.hazmat_leggings, EquipmentSlot.LEGS);
      new ItemArmorJetpack();
      new ItemArmorJetpackElectric();
      new ItemArmorLappack();
      new ItemArmorNanoSuit(ItemName.nano_boots, EquipmentSlot.FEET);
      new ItemArmorNanoSuit(ItemName.nano_chestplate, EquipmentSlot.CHEST);
      new ItemArmorNanoSuit(ItemName.nano_helmet, EquipmentSlot.HEAD);
      new ItemArmorNanoSuit(ItemName.nano_leggings, EquipmentSlot.LEGS);
      new ItemArmorNightvisionGoggles();
      new ItemArmorQuantumSuit(ItemName.quantum_boots, EquipmentSlot.FEET);
      new ItemArmorQuantumSuit(ItemName.quantum_chestplate, EquipmentSlot.CHEST);
      new ItemArmorQuantumSuit(ItemName.quantum_helmet, EquipmentSlot.HEAD);
      new ItemArmorQuantumSuit(ItemName.quantum_leggings, EquipmentSlot.LEGS);
      new ItemArmorHazmat(ItemName.rubber_boots, EquipmentSlot.FEET);
      new ItemArmorSolarHelmet();
      new ItemArmorStaticBoots();
      new ItemIC2Boat();
      new ItemBarrel();
      new ItemMug();
      new ItemBooze();
      ItemMulti.create(ItemName.crushed, OreResourceType.class);
      ItemMulti.create(ItemName.purified, OreResourceType.class);
      ItemMulti.create(ItemName.dust, DustResourceType.class);
      ItemMulti.create(ItemName.ingot, IngotResourceType.class);
      ItemMulti.create(ItemName.plate, PlateResourceType.class);
      ItemMulti.create(ItemName.casing, CasingResourceType.class);
      ItemMulti<NuclearResourceType> nuclearResource = new ItemNuclearResource();
      nuclearResource.setUpdateHandler(null, ItemHandlers.radioactiveUpdate);
      ItemMulti<MiscResourceType> miscResource = ItemMulti.create(ItemName.misc_resource, MiscResourceType.class);
      miscResource.setRarity(MiscResourceType.matter, Rarity.RARE);
      miscResource.setRarity(MiscResourceType.iridium_ore, Rarity.RARE);
      miscResource.setRarity(MiscResourceType.iridium_shard, Rarity.UNCOMMON);
      miscResource.setUseHandler(MiscResourceType.resin, ItemHandlers.resinUse);
      miscResource.setUseHandler(MiscResourceType.water_sheet, ItemHandlers.getFluidPlacer(Blocks.WATER));
      miscResource.setUseHandler(MiscResourceType.lava_sheet, ItemHandlers.getFluidPlacer(Blocks.LAVA));
      ItemMulti<CraftingItemType> crafting = ItemMulti.create(ItemName.crafting, CraftingItemType.class);
      crafting.setRarity(CraftingItemType.advanced_circuit, Rarity.UNCOMMON);
      crafting.setRarity(CraftingItemType.iridium, Rarity.RARE);
      crafting.setRightClickHandler(CraftingItemType.cf_powder, ItemHandlers.cfPowderApply);
      crafting.setRightClickHandler(CraftingItemType.scrap_box, ItemHandlers.scrapBoxUnpack);
      DispenserBlock.registerBehavior(crafting, new BehaviorScrapboxDispense());
      new BlockCuttingBlade();
      new UpgradeKit();
      new ItemCrop();
      ItemMulti.create(ItemName.crop_res, CropResItemType.class);
      new ItemTerraWart();
      new ItemCropnalyzer();
      new ItemBattery(ItemName.re_battery, 10000.0, 100.0, 1);
      new ItemBattery(ItemName.advanced_re_battery, 100000.0, 256.0, 2) {
         @Override
         protected boolean isEnabled() {
            return !IC2.version.isClassic();
         }
      };
      new ItemBattery(ItemName.energy_crystal, 1000000.0, 2048.0, 3);
      new ItemBattery(ItemName.lapotron_crystal, 1.0E7, 8092.0, 4).setRarity(Rarity.UNCOMMON);
      new ItemBatterySU(ItemName.single_use_battery, 1200, 1);
      new ItemBatteryChargeHotbar(ItemName.charging_re_battery, 40000.0, 128.0, 1);
      new ItemBatteryChargeHotbar(ItemName.advanced_charging_re_battery, 400000.0, 1024.0, 2);
      new ItemBatteryChargeHotbar(ItemName.charging_energy_crystal, 4000000.0, 8192.0, 3);
      new ItemBatteryChargeHotbar(ItemName.charging_lapotron_crystal, 4.0E7, 32768.0, 4).setRarity(Rarity.UNCOMMON);
      new ItemReactorHeatStorage(ItemName.heat_storage, 10000);
      new ItemReactorHeatStorage(ItemName.tri_heat_storage, 30000);
      new ItemReactorHeatStorage(ItemName.hex_heat_storage, 60000);
      new ItemReactorPlating(ItemName.plating, 1000, 0.95F);
      new ItemReactorPlating(ItemName.heat_plating, 2000, 0.99F);
      new ItemReactorPlating(ItemName.containment_plating, 500, 0.9F);
      new ItemReactorHeatSwitch(ItemName.heat_exchanger, 2500, 12, 4);
      new ItemReactorHeatSwitch(ItemName.reactor_heat_exchanger, 5000, 0, 72);
      new ItemReactorHeatSwitch(ItemName.component_heat_exchanger, 5000, 36, 0);
      new ItemReactorHeatSwitch(ItemName.advanced_heat_exchanger, 10000, 24, 8);
      new ItemReactorVent(ItemName.heat_vent, 1000, 6, 0);
      new ItemReactorVent(ItemName.reactor_heat_vent, 1000, 5, 5);
      new ItemReactorVent(ItemName.overclocked_heat_vent, 1000, 20, 36);
      new ItemReactorVentSpread(ItemName.component_heat_vent, 4);
      new ItemReactorVent(ItemName.advanced_heat_vent, 1000, 12, 0);
      new ItemReactorReflector(ItemName.neutron_reflector, 30000);
      new ItemReactorReflector(ItemName.thick_neutron_reflector, 120000);
      new ItemReactorIridiumReflector(ItemName.iridium_reflector);
      new ItemReactorCondensator(ItemName.rsh_condensator, 20000);
      new ItemReactorCondensator(ItemName.lzh_condensator, 100000);
      new ItemReactorHeatpack(1000, 1);
      new ItemReactorUranium(ItemName.uranium_fuel_rod, 1);
      new ItemReactorUranium(ItemName.dual_uranium_fuel_rod, 2);
      new ItemReactorUranium(ItemName.quad_uranium_fuel_rod, 4);
      new ItemReactorMOX(ItemName.mox_fuel_rod, 1);
      new ItemReactorMOX(ItemName.dual_mox_fuel_rod, 2);
      new ItemReactorMOX(ItemName.quad_mox_fuel_rod, 4);
      new ItemReactorLithiumCell();
      new ItemReactorDepletedUranium();
      new Tfbp();
      ToolMaterial bronzeToolMaterial = NativeMaterials.BRONZE_TOOL;
      new Ic2Axe(bronzeToolMaterial);
      new Ic2Hoe(bronzeToolMaterial);
      new Ic2Pickaxe(bronzeToolMaterial);
      new Ic2Shovel(bronzeToolMaterial);
      new Ic2Sword(bronzeToolMaterial);
      new ItemToolCutter();
      new ItemDebug();
      if (!IC2.version.isClassic()) {
         new ItemSprayer();
      } else {
         new ItemClassicSprayer();
      }

      new ItemToolHammer();
      new ItemFrequencyTransmitter();
      new ItemToolMeter();
      new ItemToolbox();
      new ItemTreetap();
      new ItemToolWrench();
      new ItemToolWrenchNew();
      new ItemToolCrowbar();
      new ItemContainmentbox();
      new ItemWeedingTrowel();
      new ItemCropSeed();
      new ItemScannerAdv();
      new ItemElectricToolChainsaw();
      new ItemDrill(ItemName.diamond_drill, 80, HarvestLevel.Diamond, 30000, 100, 1, 16.0F);
      new ItemDrill(ItemName.drill, 50, HarvestLevel.Iron, 30000, 100, 1, 8.0F);
      new ItemElectricToolHoe();
      new ItemTreetapElectric();
      new ItemToolWrenchElectric();
      new ItemDrillIridium();
      new ItemToolMiningLaser();
      new ItemNanoSaber();
      new ItemObscurator();
      new ItemScanner();
      new ItemWindmeter();
      new ItemToolPainter();
      new ItemFluidCell();
      ItemMulti<CellType> cell = new ItemClassicCell();
      cell.setUseHandler(CellType.empty, ItemHandlers.emptyCellFill);
      cell.addCapability(Fluid.ITEM, (stack, access) -> {
         CellType type = cell.getType(stack);
         return type.isFluidContainer() ? new CellType.CellFluidHandler(stack, access, cell::getType) : null;
      });
      new ItemCable();
      ItemMulti<ItemUpgradeModule.UpgradeType> upgrade = new ItemUpgradeModule();
      upgrade.setRightClickHandler(ItemUpgradeModule.UpgradeType.advanced_ejector, ItemHandlers.openAdvancedUpgradeGUI);
      upgrade.setRightClickHandler(ItemUpgradeModule.UpgradeType.advanced_pulling, ItemHandlers.openAdvancedUpgradeGUI);
      new ItemTinCan();
      new ItemFilledFuelCan();
      new ItemIodineTablet();
      new ItemCrystalMemory();
      new ItemWindRotor(ItemName.rotor_wood, 5, 10800, 0.25F, 10, 60, Identifier.fromNamespaceAndPath("ic2", "textures/items/rotor/wood_rotor_model.png"));
      new ItemWindRotor(ItemName.rotor_bronze, 7, 86400, 0.5F, 14, 75, Identifier.fromNamespaceAndPath("ic2", "textures/items/rotor/bronze_rotor_model.png"));
      new ItemWindRotor(ItemName.rotor_iron, 7, 86400, 0.5F, 14, 75, Identifier.fromNamespaceAndPath("ic2", "textures/items/rotor/iron_rotor_model.png"));
      new ItemWindRotor(ItemName.rotor_steel, 9, 172800, 0.75F, 17, 90, Identifier.fromNamespaceAndPath("ic2", "textures/items/rotor/steel_rotor_model.png"));
      new ItemWindRotor(ItemName.rotor_carbon, 11, 604800, 1.0F, 20, 110, Identifier.fromNamespaceAndPath("ic2", "textures/items/rotor/carbon_rotor_model.png"));
      new ItemDynamite(ItemName.dynamite);
      new ItemDynamite(ItemName.dynamite_sticky);
      new ItemRemote();
      new ItemFluidPipe();
      new ItemPumpCover();
      new ItemCoke();
   }

   private static void initMigration() {
   }

   private static void registerIC2fluid(
      FluidName name, BlockMaterial material, int color, int density, int viscosity, int luminosity, int temperature, boolean isGaseous
   ) {
      Ic2Fluid fluid = new Ic2Fluid(name)
         .setDensity(density)
         .setViscosity(viscosity)
         .setLuminosity(luminosity)
         .setTemperature(temperature)
         .setGaseous(isGaseous);
      Identifier id = IC2.getIdentifier(name.name());
      NativeRegistration.add(Keys.FLUID_TYPES, id, fluid.getFluidType());
      NativeRegistration.add(Registries.FLUID, id, fluid);
      NativeRegistration.add(Registries.FLUID, IC2.getIdentifier("flowing_" + name.name()), fluid.getFlowing());
      BlockIC2Fluid block = new BlockIC2Fluid(name, fluid, material, color);
      fluid.setBlock(block);
      name.setInstance(fluid);
      fluid.registerModels(name);
   }

   public static <T extends Item> T registerItem(T item, Identifier id) {
      if (item instanceof BlockItem blockItem) {
         blockItem.registerBlocks(Item.BY_BLOCK, item);
      }

      return NativeRegistration.add(Registries.ITEM, id, item);
   }

   public static <T extends Item> T registerItem(T item) {
      return registerItem(item, Objects.requireNonNull((Identifier)item.components().get(DataComponents.ITEM_MODEL)));
   }

   public static <T extends Block> T registerBlock(T block, Identifier id) {
      return NativeRegistration.add(Registries.BLOCK, id, block);
   }

   public static <T extends Block> T registerBlock(T block) {
      return registerBlock(block, Objects.requireNonNull(NativeRegistration.name(block)));
   }
}
