package ic2.core.block;

import ic2.core.IC2;
import ic2.core.block.type.ResourceBlock;
import ic2.core.init.BlocksItems;
import ic2.core.item.block.ItemBlockIC2;
import ic2.core.profile.Version;
import ic2.core.ref.BlockMaterial;
import ic2.core.ref.BlockName;
import ic2.core.ref.FluidName;
import ic2.core.ref.IBlockModelProvider;
import ic2.core.util.LiquidUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.redstone.Orientation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class BlockIC2Fluid extends LiquidBlock implements IBlockModelProvider {
   protected final Fluid fluid;
   private final int color;

   public BlockIC2Fluid(FluidName name, Fluid fluid, BlockMaterial material, int color) {
      super(
         (FlowingFluid)fluid,
         material.properties().noCollision().liquid().strength(100.0F).setId(ResourceKey.create(Registries.BLOCK, IC2.getIdentifier(name.name())))
      );
      this.fluid = fluid;
      this.color = color;
      Identifier regName = IC2.getIdentifier(name.name());
      BlocksItems.registerBlock(this, regName);
      BlocksItems.registerItem(new ItemBlockIC2(this), regName);
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public void registerModels(BlockName name) {
      BlockBase.registerDefaultItemModel(this);
   }

   public void func_149666_a(CreativeModeTab tab, NonNullList<ItemStack> items) {
      boolean defaultState = Version.shouldEnable(FluidName.class);

      try {
         if (Version.shouldEnable(FluidName.class.getField(super.getDescriptionId().substring(5)), defaultState)) {
            items.add(new ItemStack(this));
         }
      } catch (NoSuchFieldException var5) {
         throw new RuntimeException("Impossible missing enum field!", var5);
      }
   }

   protected void tick(BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {
      super.tick(state, world, pos, random);
      if (!world.isClientSide()) {
         if (this.fluid == FluidName.pahoehoe_lava.getInstance()) {
            if (this.isSourceBlock(world, pos) && world.getMaxLocalRawBrightness(pos) >= world.getRandom().nextInt(120)) {
               world.setBlock(pos, BlockName.resource.getBlockState(ResourceBlock.basalt), 3);
            } else if (!this.hardenFromNeighbors(world, pos)) {
               world.scheduleTick(pos, this, this.fluid.getTickDelay(world));
            }
         } else if (this.fluid == FluidName.hot_water.getInstance()) {
            if (this.isSourceBlock(world, pos)
               && !isLavaBlock(world.getBlockState(pos.below(2)).getBlock())
               && world.getBlockState(pos.below()).getBlock() != this
               && world.getRandom().nextInt(60) == 0) {
               world.setBlock(pos, Blocks.WATER.defaultBlockState(), 3);
            } else {
               world.scheduleTick(pos, this, this.fluid.getTickDelay(world));
            }
         }
      }
   }

   private static boolean isLavaBlock(Block block) {
      return block == Blocks.LAVA || block == Blocks.LAVA;
   }

   protected void neighborChanged(BlockState state, Level world, BlockPos pos, Block block, Orientation orientation, boolean movedByPiston) {
      super.neighborChanged(state, world, pos, block, orientation, movedByPiston);
      this.hardenFromNeighbors(world, pos);
   }

   protected void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean movedByPiston) {
      super.onPlace(state, world, pos, oldState, movedByPiston);
      this.hardenFromNeighbors(world, pos);
   }

   public void setPlacedBy(Level world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
      if (!world.isClientSide() && (this.fluid == FluidName.biogas.getInstance() || this.fluid == FluidName.air.getInstance())) {
         world.removeBlock(pos, false);
         world.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.0F, IC2.random.nextFloat() * 0.4F + 0.8F);
      }
   }

   protected void entityInside(BlockState state, Level worldIn, BlockPos pos, Entity entityIn, InsideBlockEffectApplier effects) {
      this.func_176199_a(worldIn, pos, entityIn);
   }

   public void func_176199_a(Level world, BlockPos pos, Entity entity) {
      if (!world.isClientSide()) {
         if (this.fluid == FluidName.pahoehoe_lava.getInstance()) {
            entity.igniteForSeconds(10.0F);
         } else if (this.fluid == FluidName.hot_coolant.getInstance()) {
            entity.igniteForSeconds(30.0F);
         }

         if (entity instanceof LivingEntity living) {
            if (this.fluid == FluidName.construction_foam.getInstance()) {
               addPotion(living, MobEffects.SLOWNESS, 300, 2);
            } else if (this.fluid == FluidName.uu_matter.getInstance()) {
               addPotion(living, MobEffects.REGENERATION, 100, 1);
            } else if (this.fluid == FluidName.steam.getInstance() || this.fluid == FluidName.superheated_steam.getInstance()) {
               addPotion(living, MobEffects.BLINDNESS, 300, 0);
            } else if (this.fluid == FluidName.hot_water.getInstance()) {
               Holder<MobEffect> potion;
               if (((LivingEntity)entity).getType().builtInRegistryHolder().is(EntityTypeTags.UNDEAD)) {
                  potion = MobEffects.WITHER;
               } else {
                  potion = MobEffects.REGENERATION;
               }

               addPotion(living, potion, 100, IC2.random.nextInt(2));
            }
         }
      }
   }

   private static void addPotion(LivingEntity entity, Holder<MobEffect> potion, int duration, int amplifier) {
      if (!entity.hasEffect(potion)) {
         entity.addEffect(new MobEffectInstance(potion, duration, amplifier, true, true));
      }
   }

   public String getFluidDescriptionId() {
      return "ic2." + super.getDescriptionId().substring(5);
   }

   public Fluid getFluid() {
      return this.fluid;
   }

   private boolean isSourceBlock(Level world, BlockPos pos) {
      return world.getFluidState(pos).isSource();
   }

   public int getColor() {
      return this.color;
   }

   private boolean hardenFromNeighbors(Level world, BlockPos pos) {
      if (world.isClientSide()) {
         return false;
      } else {
         if (this.fluid == FluidName.pahoehoe_lava.getInstance()) {
            for (Direction dir : Direction.values()) {
               LiquidUtil.LiquidData data = LiquidUtil.getLiquid(world, pos.relative(dir));
               if (data != null && data.liquid.getFluidType().getTemperature() <= this.fluid.getFluidType().getTemperature() / 4) {
                  if (this.isSourceBlock(world, pos)) {
                     world.setBlock(pos, BlockName.resource.getBlockState(ResourceBlock.basalt), 3);
                  } else {
                     world.removeBlock(pos, false);
                  }

                  return true;
               }
            }
         }

         return false;
      }
   }
}
