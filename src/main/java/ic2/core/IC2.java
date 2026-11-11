package ic2.core;

import ic2.api.energy.EnergyNet;
import ic2.api.info.Info;
import ic2.api.item.ElectricItem;
import ic2.api.recipe.IRecipeInput;
import ic2.api.recipe.Recipes;
import ic2.api.tile.ExplosionWhitelist;
import ic2.api.util.Keys;
import ic2.core.apihelper.ApiHelper;
import ic2.core.audio.AudioManager;
import ic2.core.block.comp.Components;
import ic2.core.block.comp.Obscuration;
import ic2.core.block.generator.tileentity.TileEntitySemifluidGenerator;
import ic2.core.block.heatgenerator.tileentity.TileEntityFluidHeatGenerator;
import ic2.core.block.machine.tileentity.TileEntityBlastFurnace;
import ic2.core.block.machine.tileentity.TileEntityBlockCutter;
import ic2.core.block.machine.tileentity.TileEntityCanner;
import ic2.core.block.machine.tileentity.TileEntityCentrifuge;
import ic2.core.block.machine.tileentity.TileEntityCompressor;
import ic2.core.block.machine.tileentity.TileEntityElectrolyzer;
import ic2.core.block.machine.tileentity.TileEntityExtractor;
import ic2.core.block.machine.tileentity.TileEntityFermenter;
import ic2.core.block.machine.tileentity.TileEntityLiquidHeatExchanger;
import ic2.core.block.machine.tileentity.TileEntityMacerator;
import ic2.core.block.machine.tileentity.TileEntityMatter;
import ic2.core.block.machine.tileentity.TileEntityMetalFormer;
import ic2.core.block.machine.tileentity.TileEntityOreWashing;
import ic2.core.block.machine.tileentity.TileEntityRecycler;
import ic2.core.block.steam.TileEntityCokeKiln;
import ic2.core.crop.IC2Crops;
import ic2.core.energy.grid.EnergyNetGlobal;
import ic2.core.entity.Ic2EntityTypes;
import ic2.core.init.BlocksItems;
import ic2.core.init.Ic2Loot;
import ic2.core.init.MainConfig;
import ic2.core.init.OreValues;
import ic2.core.init.Rezepte;
import ic2.core.init.VanillaBlockPolicy;
import ic2.core.item.ElectricItemManager;
import ic2.core.item.EntityIC2Boat;
import ic2.core.item.GatewayElectricItemManager;
import ic2.core.item.Ic2Fuel;
import ic2.core.item.armor.jetpack.JetpackAttachmentRecipe;
import ic2.core.item.armor.jetpack.JetpackHandler;
import ic2.core.item.logistics.ItemPumpCover;
import ic2.core.item.tfbp.Tfbp;
import ic2.core.item.type.CellType;
import ic2.core.item.type.CraftingItemType;
import ic2.core.item.upgrade.ItemUpgradeModule;
import ic2.core.network.NetworkManager;
import ic2.core.profile.ProfileManager;
import ic2.core.profile.Version;
import ic2.core.recipe.NativeRecipePolicy;
import ic2.core.recipe.OreDictionaryEntries;
import ic2.core.recipe.RecipeCodecs;
import ic2.core.recipe.RecipeInputFactory;
import ic2.core.recipe.RecipeTags;
import ic2.core.recipe.ScrapboxRecipeManager;
import ic2.core.ref.BlockName;
import ic2.core.ref.ItemName;
import ic2.core.ref.TeBlock;
import ic2.core.util.ConfigUtil;
import ic2.core.util.FluidTransfer;
import ic2.core.util.ItemInfo;
import ic2.core.util.Keyboard;
import ic2.core.util.Log;
import ic2.core.util.LogCategory;
import ic2.core.util.PriorityExecutor;
import ic2.core.util.SideGateway;
import ic2.core.util.StackUtil;
import ic2.core.uu.UuIndex;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.component.DataComponentInitializers.PendingComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.neoforged.neoforge.event.level.LevelEvent.Unload;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import org.apache.logging.log4j.LogManager;

@Mod("ic2")
public class IC2 {
   public static final String VERSION = "2.8.222-ex112";
   public static final String MODID = "ic2";
   public static final String RESOURCE_DOMAIN = "ic2";
   private static IC2 instance;
   public static Platform platform;
   public static SideGateway<NetworkManager> network;
   public static Keyboard keyboard;
   public static AudioManager audioManager;
   public static Log log = new Log(LogManager.getLogger("IC2"));
   public static IC2Achievements achievements;
   public static TickHandler tickHandler;
   public static Random random;
   public static boolean suddenlyHoes;
   public static boolean seasonal;
   public static boolean initialized;
   private static boolean serverRecipesReady;
   private boolean loadStarted;
   public static Version version;
   public static final CreativeTabIC2 tabIC2;
   public static final int setBlockNotify = 1;
   public static final int setBlockUpdate = 2;
   public static final int setBlockNoUpdateFromClient = 4;
   public final PriorityExecutor threadPool = new PriorityExecutor(Math.max(Runtime.getRuntime().availableProcessors(), 2));

   public IC2(IEventBus modBus) {
      instance = this;
      Info.ic2ModInstance = this;
      boolean client = FMLEnvironment.getDist().isClient();
      platform = client ? clientObject("ic2.core.PlatformClient", Platform.class) : new Platform();
      keyboard = client ? clientObject("ic2.core.util.KeyboardClient", Keyboard.class) : new Keyboard();
      audioManager = client ? clientObject("ic2.core.audio.AudioManagerClient", AudioManager.class) : new AudioManager();
      RecipeCodecs.init();
      Ic2Fuel.init();
      modBus.addListener(EventPriority.HIGHEST, event -> {
         if (!this.loadStarted) {
            this.loadStarted = true;
            this.load();
         }
      });
      modBus.addListener(event -> event.enqueueWork(() -> {
         Provider lookup = VanillaRegistries.createWorldLookup();
         BuiltInRegistries.DATA_COMPONENT_INITIALIZERS.build(lookup).forEach(PendingComponents::apply);
         this.init();
         this.modsLoaded();
      }));
      if (client) {
         modBus.addListener(event -> event.enqueueWork(audioManager::initialize));
      }
   }

   public static IC2 getInstance() {
      return instance;
   }

   public void load() {
      long startTime = System.nanoTime();
      log.debug(LogCategory.General, "Starting pre-init.");
      MainConfig.load();
      ProfileManager.init();
      tickHandler = new TickHandler();
      if (!FMLEnvironment.getDist().isClient()) {
         audioManager.initialize();
      }

      Recipes.inputFactory = new RecipeInputFactory();
      ElectricItem.manager = new GatewayElectricItemManager();
      ElectricItem.rawManager = new ElectricItemManager();
      Info.itemInfo = new ItemInfo();
      Keys.instance = keyboard;
      Components.init();
      BlocksItems.init();
      Ic2WorldDecorator.registerAttachment();
      Ic2EntityTypes.registerAll();
      TeBlock.registerTeMappings();
      initialized = true;
      log.debug(LogCategory.General, "Finished pre-init after %d ms.", (System.nanoTime() - startTime) / 1000000L);
   }

   private void initializeGameplay() {
      ItemName.upgrade.<ItemUpgradeModule>getInstance().initializeUpgrades();
      ItemName.cover.<ItemPumpCover>getInstance().initializeCovers();
      RecipeTags.initializeDeclaredEntries();
      VanillaBlockPolicy.resistance(Blocks.OBSIDIAN, 60.0F);
      VanillaBlockPolicy.resistance(Blocks.ENCHANTING_TABLE, 60.0F);
      VanillaBlockPolicy.resistance(Blocks.ENDER_CHEST, 60.0F);
      VanillaBlockPolicy.resistance(Blocks.ANVIL, 60.0F);
      VanillaBlockPolicy.resistance(Blocks.WATER, 30.0F);
      VanillaBlockPolicy.resistance(Blocks.WATER, 30.0F);
      VanillaBlockPolicy.resistance(Blocks.LAVA, 30.0F);
      ExplosionWhitelist.addWhitelistedBlock(Blocks.BEDROCK);
      ScrapboxRecipeManager.setup();
      Tfbp.init();
      TileEntityCanner.init();
      TileEntityCompressor.init();
      TileEntityExtractor.init();
      TileEntityMacerator.init();
      TileEntityRecycler.init();
      TileEntityCentrifuge.init();
      TileEntityMatter.init();
      TileEntityMetalFormer.init();
      TileEntitySemifluidGenerator.init();
      TileEntityOreWashing.init();
      TileEntityFluidHeatGenerator.init();
      TileEntityBlockCutter.init();
      TileEntityBlastFurnace.init();
      TileEntityLiquidHeatExchanger.init();
      TileEntityFermenter.init();
      TileEntityElectrolyzer.init();
      TileEntityCokeKiln.init();
      EntityIC2Boat.init();
      NeoForge.EVENT_BUS.register(this);
      Rezepte.registerWithSorter();

      for (String oreName : RecipeTags.getOreNames()) {
         for (ItemStack ore : RecipeTags.getOres(oreName)) {
            this.registerOre(new RecipeTags.EntryAdded(oreName, ore));
         }
      }

      OreDictionaryEntries.load();
      EnergyNet.instance = EnergyNetGlobal.create();
      IC2Crops.init();
      IC2Potion.init();
      ApiHelper.preload();
      achievements = new IC2Achievements();
      Ic2Loot.init();
      int d = Integer.parseInt(new SimpleDateFormat("Mdd").format(new Date()));
      suddenlyHoes = d > Math.cbrt(6.4E7) && d < Math.cbrt(6.5939264E7);
      seasonal = d > Math.cbrt(1.089547389E9) && d < Math.cbrt(1.338273208E9);
      new Ic2WorldDecorator();
      NeoForge.EVENT_BUS.register(new IC2BucketHandler());
      Obscuration.ObscurationComponentEventHandler.init();
   }

   public void init() {
      long startTime = System.nanoTime();
      log.debug(LogCategory.General, "Starting init.");
      this.initializeGameplay();
      platform.preInit();
      Rezepte.registerRecipes();
      ScrapboxRecipeManager.load();
      new ChunkLoaderLogic();
      TeBlock.buildDummies();
      IC2Crops.ensureInit();
      log.debug(LogCategory.General, "Finished init after %d ms.", (System.nanoTime() - startTime) / 1000000L);
   }

   public void modsLoaded() {
      long startTime = System.nanoTime();
      log.debug(LogCategory.General, "Starting post-init.");
      if (!initialized) {
         platform.displayError("IndustrialCraft 2 has failed to initialize properly.");
      }

      Rezepte.loadFailedRecipes();

      for (IRecipeInput input : ConfigUtil.asRecipeInputList(MainConfig.get(), "misc/additionalValuableOres")) {
         for (ItemStack stack : input.getInputs()) {
            OreValues.add(stack, 1);
         }
      }

      if (loadSubModule("bcIntegration")) {
         log.debug(LogCategory.SubModule, "BuildCraft integration module loaded.");
      }

      List<IRecipeInput> purgedRecipes = new ArrayList<>();
      purgedRecipes.addAll(ConfigUtil.asRecipeInputList(MainConfig.get(), "recipes/purge"));
      if (ConfigUtil.getBool(MainConfig.get(), "balance/disableEnderChest")) {
         purgedRecipes.add(Recipes.inputFactory.forStack(new ItemStack(Blocks.ENDER_CHEST)));
      }

      NativeRecipePolicy.configure(purgedRecipes, ConfigUtil.getBool(MainConfig.get(), "recipes/smeltToIc2Items"));
      TileEntityRecycler.initLate();
      JetpackAttachmentRecipe.init();
      JetpackHandler.init();
      UuIndex.instance.init();
      platform.onPostInit();
      log.debug(LogCategory.General, "Finished post-init after %d ms.", (System.nanoTime() - startTime) / 1000000L);
      log.info(LogCategory.General, "%s version %s loaded.", "ic2", "2.8.222-ex112");
   }

   private static boolean loadSubModule(String name) {
      log.debug(LogCategory.SubModule, "Loading %s submodule: %s.", "ic2", name);

      try {
         Class<?> subModuleClass = IC2.class.getClassLoader().loadClass("ic2." + name + ".SubModule");
         return (Boolean)subModuleClass.getMethod("init").invoke(null);
      } catch (Throwable var2) {
         log.debug(LogCategory.SubModule, "Submodule %s not loaded.", name);
         return false;
      }
   }

   @SubscribeEvent
   public void serverStarted(ServerStartedEvent event) {
      UuIndex.instance.refresh(true);
      serverRecipesReady = true;
   }

   @SubscribeEvent
   public void serverStopped(ServerStoppedEvent event) {
      serverRecipesReady = false;
   }

   @SubscribeEvent
   public void recipesReloaded(OnDatapackSyncEvent event) {
      if (serverRecipesReady && event.getPlayer() == null) {
         UuIndex.instance.refresh(true);
      }
   }

   private static <T> T clientObject(String name, Class<T> type) {
      try {
         return type.cast(Class.forName(name).getConstructor().newInstance());
      } catch (ReflectiveOperationException var3) {
         throw new IllegalStateException("Cannot initialize IC2 client service: " + name, var3);
      }
   }

   public int getBurnTime(ItemStack stack) {
      if (!BlockName.sapling.hasItemStack()) {
         return 0;
      } else {
         if (stack != null) {
            Item item = stack.getItem();
            if (StackUtil.checkItemEquality(stack, BlockName.sapling.getItemStack())) {
               return 80;
            }

            if (item == Items.SUGAR_CANE) {
               return 50;
            }

            if (item == Item.byBlock(Blocks.CACTUS)) {
               return 50;
            }

            if (StackUtil.checkItemEquality(stack, ItemName.crafting.getItemStack(CraftingItemType.scrap))) {
               return 350;
            }

            if (StackUtil.checkItemEquality(stack, ItemName.crafting.getItemStack(CraftingItemType.scrap_box))) {
               return 3150;
            }

            if (item == ItemName.fluid_cell.getInstance()) {
               FluidStack fs = FluidTransfer.getFluidContained(stack);
               if (fs != null && fs.getFluid() == Fluids.LAVA) {
                  int ret = 20000;
                  return ret * fs.getAmount() / 1000;
               }
            } else if (StackUtil.checkItemEquality(stack, ItemName.cell.getItemStack(CellType.lava))) {
               return 20000;
            }
         }

         return 0;
      }
   }

   @SubscribeEvent
   public void onPlayerLogin(PlayerLoggedInEvent event) {
   }

   @SubscribeEvent
   public void onPlayerLogout(PlayerLoggedOutEvent event) {
      if (platform.isSimulating()) {
         keyboard.removePlayerReferences(event.getEntity());
      }
   }

   @SubscribeEvent
   public void onWorldUnload(Unload event) {
      if (event.getLevel() instanceof Level level) {
         WorldData.onWorldUnload(level);
      }
   }

   public static void explodeMachineAt(Level world, int x, int y, int z, boolean noDrop) {
      ExplosionIC2 explosion = new ExplosionIC2(world, null, 0.5 + x, 0.5 + y, 0.5 + z, 2.5F, 0.75F);
      explosion.destroy(x, y, z, noDrop);
      explosion.doExplosion();
   }

   public static int getSeaLevel(Level world) {
      return world.getSeaLevel();
   }

   public static int getWorldHeight(Level world) {
      return world.getHeight();
   }

   @SubscribeEvent
   public void registerOre(RecipeTags.EntryAdded event) {
      String oreClass = event.getName();
      ItemStack ore = event.getOre();
      if (ore.getItem() instanceof BlockItem) {
         int multiplier = 1;
         if (oreClass.startsWith("dense")) {
            multiplier *= 3;
            oreClass = oreClass.substring("dense".length());
         }

         int value = 0;
         if (oreClass.equals("oreCoal")) {
            value = 1;
         } else if (oreClass.equals("oreCopper") || oreClass.equals("oreTin") || oreClass.equals("oreLead") || oreClass.equals("oreQuartz")) {
            value = 2;
         } else if (oreClass.equals("oreIron")
            || oreClass.equals("oreGold")
            || oreClass.equals("oreRedstone")
            || oreClass.equals("oreLapis")
            || oreClass.equals("oreSilver")) {
            value = 3;
         } else if (oreClass.equals("oreUranium")
            || oreClass.equals("oreGemRuby")
            || oreClass.equals("oreGemGreenSapphire")
            || oreClass.equals("oreGemSapphire")
            || oreClass.equals("oreRuby")
            || oreClass.equals("oreGreenSapphire")
            || oreClass.equals("oreSapphire")) {
            value = 4;
         } else if (oreClass.equals("oreDiamond") || oreClass.equals("oreEmerald") || oreClass.equals("oreTungsten")) {
            value = 5;
         } else if (oreClass.startsWith("ore")) {
            value = 1;
         }

         if (value > 0 && multiplier >= 1) {
            OreValues.add(ore, value * multiplier);
         }
      }
   }

   @SubscribeEvent
   public void onLivingSpecialSpawn(EntityJoinLevelEvent event) {
      if (seasonal
         && !event.getLevel().isClientSide()
         && !event.loadedFromDisk()
         && (event.getEntity() instanceof Zombie || event.getEntity() instanceof Skeleton)
         && event.getEntity().level().getRandom().nextFloat() < 0.1F) {
         Mob entity = (Mob)event.getEntity();

         for (EquipmentSlot slot : EquipmentSlot.values()) {
            entity.setDropChance(slot, Float.NEGATIVE_INFINITY);
         }

         if (entity instanceof Zombie) {
            entity.setItemSlot(EquipmentSlot.MAINHAND, ItemName.nano_saber.getItemStack());
         }

         if (entity.level().getRandom().nextFloat() < 0.1F) {
            entity.setItemSlot(EquipmentSlot.HEAD, ItemName.quantum_helmet.getItemStack());
            entity.setItemSlot(EquipmentSlot.CHEST, ItemName.quantum_chestplate.getItemStack());
            entity.setItemSlot(EquipmentSlot.LEGS, ItemName.quantum_leggings.getItemStack());
            entity.setItemSlot(EquipmentSlot.FEET, ItemName.quantum_boots.getItemStack());
         } else {
            entity.setItemSlot(EquipmentSlot.HEAD, ItemName.nano_helmet.getItemStack());
            entity.setItemSlot(EquipmentSlot.CHEST, ItemName.nano_chestplate.getItemStack());
            entity.setItemSlot(EquipmentSlot.LEGS, ItemName.nano_leggings.getItemStack());
            entity.setItemSlot(EquipmentSlot.FEET, ItemName.nano_boots.getItemStack());
         }
      }
   }

   public static Identifier getIdentifier(String name) {
      return Identifier.fromNamespaceAndPath("ic2", name);
   }

   static {
      try {
         new BlockPos(1, 2, 3).offset(2, 3, 4);
      } catch (Throwable var1) {
         throw new Error("IC2 is incompatible with this environment, use the normal IC2 version, not the dev one.", var1);
      }

      instance = null;
      network = new SideGateway<>("ic2.core.network.NetworkManager", "ic2.core.network.NetworkManagerClient");
      random = new Random();
      suddenlyHoes = false;
      seasonal = false;
      initialized = false;
      version = ProfileManager.selected.style;
      tabIC2 = new CreativeTabIC2();
   }
}
