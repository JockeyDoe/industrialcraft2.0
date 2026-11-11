package ic2.core.model;

import com.google.common.reflect.TypeToken;
import ic2.core.block.EntityDynamite;
import ic2.core.block.EntityItnt;
import ic2.core.block.EntityNuke;
import ic2.core.block.EntityStickyDynamite;
import ic2.core.block.KineticGeneratorRenderer;
import ic2.core.block.NativeBlockEntities;
import ic2.core.block.RenderExplosiveBlock;
import ic2.core.block.TileEntityBlock;
import ic2.core.block.beam.EntityParticle;
import ic2.core.block.beam.RenderBeam;
import ic2.core.block.generator.tileentity.TileEntityWaterGenerator;
import ic2.core.block.generator.tileentity.TileEntityWindGenerator;
import ic2.core.block.kineticgenerator.tileentity.TileEntityWaterKineticGenerator;
import ic2.core.block.kineticgenerator.tileentity.TileEntityWindKineticGenerator;
import ic2.core.block.personal.TileEntityPersonalChest;
import ic2.core.block.personal.TileEntityPersonalChestRenderer;
import ic2.core.entity.Ic2EntityTypes;
import ic2.core.item.EntityBoatCarbon;
import ic2.core.item.EntityBoatElectric;
import ic2.core.item.EntityBoatRubber;
import ic2.core.item.ItemIC2Boat;
import ic2.core.item.RenderIC2Boat;
import ic2.core.item.armor.jetpack.JetpackHandler;
import ic2.core.item.armor.jetpack.LayerJetpackOverride;
import ic2.core.item.tool.EntityMiningLaser;
import ic2.core.item.tool.RenderCrossed;
import java.util.LinkedHashSet;
import java.util.Set;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent.AddLayers;
import net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;

@EventBusSubscriber(modid = "ic2", value = Dist.CLIENT)
public final class NativeRendererRegistration {
   public static final ContextKey<Boolean> ATTACHED_JETPACK = new ContextKey(Identifier.parse("ic2:attached_jetpack"));
   private static final Set<Class<? extends BlockEntity>> rotorClasses = new LinkedHashSet<>();
   private static final Set<BlockEntityType<?>> rotorTypes = new LinkedHashSet<>();

   @SubscribeEvent
   public static void renderState(RegisterRenderStateModifiersEvent event) {
      event.registerEntityModifier(
         new TypeToken<LivingEntityRenderer<?, ?, ?>>() {},
         (entity, state) -> state.setRenderData(ATTACHED_JETPACK, JetpackHandler.hasJetpackAttached(entity.getItemBySlot(EquipmentSlot.CHEST)))
      );
   }

   public static void rotor(Class<? extends BlockEntity> type) {
      rotorClasses.add(type);
   }

   public static void rotor(BlockEntityType<?> type) {
      rotorTypes.add(type);
   }

   @SubscribeEvent
   public static void layers(AddLayers event) {
      for (PlayerModelType skin : event.getSkins()) {
         addJetpack(event.getPlayerRenderer(skin), event.getContext());
         addJetpack(event.getMannequinRenderer(skin), event.getContext());
      }

      for (EntityType<?> type : event.getEntityTypes()) {
         addJetpack(event.getRenderer(type), event.getContext());
      }
   }

   private static void addJetpack(EntityRenderer<?, ?> renderer, Context context) {
      if (renderer instanceof LivingEntityRenderer living && living.getModel() instanceof HumanoidModel) {
         living.addLayer(new LayerJetpackOverride(living, context));
      }
   }

   @SubscribeEvent
   public static void register(RegisterRenderers event) {
      for (Class<? extends BlockEntity> clazz : rotorClasses) {
         if (TileEntityBlock.class.isAssignableFrom(clazz)) {
            rotorTypes.add(NativeBlockEntities.type(clazz));
         } else {
            boolean found = false;

            for (BlockEntityType<?> type : BuiltInRegistries.BLOCK_ENTITY_TYPE) {
               for (Block block : type.getValidBlocks()) {
                  BlockEntity sample = type.create(BlockPos.ZERO, block.defaultBlockState());
                  if (clazz.isInstance(sample)) {
                     rotorTypes.add(type);
                     found = true;
                     break;
                  }
               }
            }

            if (!found) {
               throw new IllegalStateException("No native block entity type for rotor " + clazz.getName() + "; register its BlockEntityType with RotorRegistry");
            }
         }
      }

      for (BlockEntityType<?> type : rotorTypes) {
         event.registerBlockEntityRenderer(type, context -> new KineticGeneratorRenderer());
      }

      event.registerBlockEntityRenderer(NativeBlockEntities.rendererType(TileEntityPersonalChest.class), context -> new TileEntityPersonalChestRenderer());
      event.registerBlockEntityRenderer(NativeBlockEntities.rendererType(TileEntityWindKineticGenerator.class), context -> new KineticGeneratorRenderer());
      event.registerBlockEntityRenderer(NativeBlockEntities.rendererType(TileEntityWaterKineticGenerator.class), context -> new KineticGeneratorRenderer());
      event.registerBlockEntityRenderer(NativeBlockEntities.rendererType(TileEntityWindGenerator.class), context -> new KineticGeneratorRenderer());
      event.registerBlockEntityRenderer(NativeBlockEntities.rendererType(TileEntityWaterGenerator.class), context -> new KineticGeneratorRenderer());
      event.registerEntityRenderer(Ic2EntityTypes.get("itnt", EntityItnt.class), RenderExplosiveBlock::new);
      event.registerEntityRenderer(Ic2EntityTypes.get("nuke", EntityNuke.class), RenderExplosiveBlock::new);
      event.registerEntityRenderer(
         Ic2EntityTypes.get("mining_laser", EntityMiningLaser.class),
         context -> new RenderCrossed(context, Identifier.fromNamespaceAndPath("ic2", "textures/models/laser.png"))
      );
      event.registerEntityRenderer(
         Ic2EntityTypes.get("rubber_boat", EntityBoatRubber.class), context -> new RenderIC2Boat(context, "textures/models/boat_rubber.png")
      );
      event.registerEntityRenderer(
         Ic2EntityTypes.get("carbon_boat", EntityBoatCarbon.class), context -> new RenderIC2Boat(context, "textures/models/boat_carbon.png")
      );
      event.registerEntityRenderer(
         Ic2EntityTypes.get("electric_boat", EntityBoatElectric.class), context -> new RenderIC2Boat(context, "textures/models/boat_electric.png")
      );
      event.registerEntityRenderer(Ic2EntityTypes.get("dynamite", EntityDynamite.class), ThrownItemRenderer::new);
      event.registerEntityRenderer(Ic2EntityTypes.get("sticky_dynamite", EntityStickyDynamite.class), ThrownItemRenderer::new);
      event.registerEntityRenderer(Ic2EntityTypes.get("fireproof_item", ItemIC2Boat.FireproofItem.class), ItemEntityRenderer::new);
      event.registerEntityRenderer(Ic2EntityTypes.get("beam", EntityParticle.class), RenderBeam::new);
      event.registerEntityRenderer(Ic2EntityTypes.get("particle", ic2.core.item.tool.EntityParticle.class), NoopRenderer::new);
   }
}
