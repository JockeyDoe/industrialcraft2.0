package ic2.core.block;

import ic2.api.util.FluidContainerOutputMode;
import ic2.core.item.ItemBooze;
import ic2.core.item.type.CropResItemType;
import ic2.core.ref.BlockName;
import ic2.core.ref.ItemName;
import ic2.core.util.ItemStacks;
import ic2.core.util.LiquidUtil;
import ic2.core.util.StackUtil;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.fluids.FluidStack;

public class TileEntityBarrel extends TileEntityBlock {
   private int type = 0;
   private int boozeAmount = 0;
   private int age = 0;
   private boolean opened;
   private byte hopsCount = 0;
   private byte wheatCount = 0;
   private byte solidRatio = 0;
   private byte hopsRatio = 0;
   private byte timeRatio = 0;

   public TileEntityBarrel() {
   }

   public TileEntityBarrel(int value) {
      this.type = ItemBooze.getTypeOfValue(value);
      if (this.type > 0) {
         this.boozeAmount = ItemBooze.getAmountOfValue(value);
      }

      if (this.type == 1) {
         this.opened = true;
         this.hopsRatio = (byte)ItemBooze.getHopsRatioOfBeerValue(value);
         this.solidRatio = (byte)ItemBooze.getSolidRatioOfBeerValue(value);
         this.timeRatio = (byte)ItemBooze.getTimeRatioOfBeerValue(value);
      }

      if (this.type == 2) {
         this.opened = false;
         this.age = this.timeNedForRum(this.boozeAmount) * ItemBooze.getProgressOfRumValue(value) / 100;
      }
   }

   @Override
   public void loadAdditional(CompoundTag nbt) {
      super.loadAdditional(nbt);
      this.type = nbt.getByteOr("type", (byte)0);
      this.boozeAmount = nbt.getByteOr("waterCount", (byte)0);
      this.age = nbt.getIntOr("age", 0);
      this.opened = nbt.getBooleanOr("opened", false);
      if (this.type == 1) {
         if (!this.opened) {
            this.hopsCount = nbt.getByteOr("hopsCount", (byte)0);
            this.wheatCount = nbt.getByteOr("wheatCount", (byte)0);
         }

         this.solidRatio = nbt.getByteOr("solidRatio", (byte)0);
         this.hopsRatio = nbt.getByteOr("hopsRatio", (byte)0);
         this.timeRatio = nbt.getByteOr("timeRatio", (byte)0);
      }
   }

   @Override
   public CompoundTag saveAdditional(CompoundTag nbt) {
      super.saveAdditional(nbt);
      nbt.putByte("type", (byte)this.type);
      nbt.putByte("waterCount", (byte)this.boozeAmount);
      nbt.putInt("age", this.age);
      nbt.putBoolean("opened", this.opened);
      if (this.type == 1) {
         if (!this.opened) {
            nbt.putByte("hopsCount", this.hopsCount);
            nbt.putByte("wheatCount", this.wheatCount);
         }

         nbt.putByte("solidRatio", this.solidRatio);
         nbt.putByte("hopsRatio", this.hopsRatio);
         nbt.putByte("timeRatio", this.timeRatio);
      }

      return nbt;
   }

   @Override
   protected void updateEntityServer() {
      super.updateEntityServer();
      if (!this.isEmpty() && !this.getActive()) {
         this.age++;
         if (this.type == 1 && this.timeRatio < 5) {
            int x = this.timeRatio;
            if (x == 4) {
               x += 2;
            }

            if (this.age >= 24000.0 * Math.pow(3.0, x)) {
               this.age = 0;
               this.timeRatio++;
            }
         }
      }
   }

   public boolean isEmpty() {
      return this.type == 0 || this.boozeAmount <= 0;
   }

   @Override
   protected boolean onActivated(Player player, InteractionHand hand, Direction side, float hitX, float hitY, float hitZ) {
      ItemStack stack = StackUtil.get(player, hand);
      if (stack == null) {
         return false;
      } else if (side.getAxis() != Axis.Y && !this.getActive() && StackUtil.consume(player, hand, StackUtil.sameStack(ItemName.treetap.getItemStack()), 1)) {
         if (!this.getLevel().isClientSide()) {
            this.setActive(true);
         }

         if (this.getFacing() != side) {
            this.setFacing(side);
         }

         return true;
      } else {
         if (!this.opened) {
            if (this.type == 0 || this.type == 1) {
               int minAmount = 1000;
               int space = (32 - this.boozeAmount) * 1000;
               if (player.isShiftKeyDown()) {
                  space = Math.min(space, 1000);
               }

               FluidStack fs;
               if (space >= 1000
                  && (fs = LiquidUtil.drainContainer(player, hand, Fluids.WATER, space, FluidContainerOutputMode.InPlacePreferred, true)) != null
                  && fs.getAmount() >= 1000) {
                  int amount = fs.getAmount() / 1000 * 1000;
                  fs = LiquidUtil.drainContainer(player, hand, Fluids.WATER, amount, FluidContainerOutputMode.InPlacePreferred, true);
                  if (fs.getAmount() != amount) {
                     return false;
                  }

                  LiquidUtil.drainContainer(player, hand, Fluids.WATER, amount, FluidContainerOutputMode.InPlacePreferred, false);
                  this.type = 1;
                  this.boozeAmount += amount / 1000;
                  return true;
               }

               if (stack.getItem() == Items.WHEAT) {
                  this.type = 1;
                  int amount = StackUtil.getSize(stack);
                  if (player.isShiftKeyDown()) {
                     amount = 1;
                  }

                  if (amount > 64 - this.wheatCount) {
                     amount = 64 - this.wheatCount;
                  }

                  if (amount <= 0) {
                     return false;
                  }

                  this.wheatCount = (byte)(this.wheatCount + amount);
                  StackUtil.consumeOrError(player, hand, amount);
                  this.alterComposition();
                  return true;
               }

               if (StackUtil.checkItemEquality(stack, ItemName.crop_res.getItemStack(CropResItemType.hops))) {
                  this.type = 1;
                  int amountx = StackUtil.getSize(stack);
                  if (player.isShiftKeyDown()) {
                     amountx = 1;
                  }

                  if (amountx > 64 - this.hopsCount) {
                     amountx = 64 - this.hopsCount;
                  }

                  if (amountx <= 0) {
                     return false;
                  }

                  this.hopsCount = (byte)(this.hopsCount + amountx);
                  StackUtil.consumeOrError(player, hand, amountx);
                  this.alterComposition();
                  return true;
               }
            }

            if ((this.type == 0 || this.type == 2) && stack.getItem() == Items.SUGAR_CANE) {
               if (this.age > 600) {
                  return false;
               }

               this.type = 2;
               int amountxx = StackUtil.getSize(stack);
               if (player.isShiftKeyDown()) {
                  amountxx = 1;
               }

               if (this.boozeAmount + amountxx > 32) {
                  amountxx = 32 - this.boozeAmount;
               }

               if (amountxx <= 0) {
                  return false;
               }

               this.boozeAmount += amountxx;
               StackUtil.consumeOrError(player, hand, amountxx);
               return true;
            }
         }

         return false;
      }
   }

   @Override
   protected void onClicked(Player player) {
      super.onClicked(player);
      Level world = this.getLevel();
      if (this.getActive()) {
         if (!world.isClientSide()) {
            StackUtil.dropAsEntity(world, this.getBlockPos(), ItemName.treetap.getItemStack());
            this.setActive(false);
         }

         this.drainLiquid(1);
      } else {
         if (!world.isClientSide()) {
            StackUtil.dropAsEntity(world, this.getBlockPos(), ItemStacks.create(ItemName.barrel.getInstance(), 1, this.calculateMetaValue()));
         }

         world.setBlock(this.getBlockPos(), BlockName.scaffold.getBlockState(BlockScaffold.ScaffoldType.wood), 3);
      }
   }

   private void alterComposition() {
      if (this.timeRatio <= 0) {
         this.age = 0;
      } else if (this.timeRatio == 1) {
         Level world = this.getLevel();
         if (world.getRandom().nextBoolean()) {
            this.timeRatio = 0;
         } else if (world.getRandom().nextBoolean()) {
            this.timeRatio = 5;
         }
      } else if (this.timeRatio == 2) {
         if (this.getLevel().getRandom().nextBoolean()) {
            this.timeRatio = 5;
         }
      } else {
         this.timeRatio = 5;
      }
   }

   public boolean drainLiquid(int amount) {
      if (this.isEmpty()) {
         return false;
      } else if (amount > this.boozeAmount) {
         return false;
      } else {
         this.open();
         if (this.type == 2) {
            int progress = this.age * 100 / this.timeNedForRum(this.boozeAmount);
            this.boozeAmount -= amount;
            this.age = progress / 100 * this.timeNedForRum(this.boozeAmount);
         } else {
            this.boozeAmount -= amount;
         }

         if (this.boozeAmount <= 0) {
            if (this.type == 1) {
               this.hopsCount = 0;
               this.wheatCount = 0;
               this.hopsRatio = 0;
               this.solidRatio = 0;
               this.timeRatio = 0;
            }

            this.type = 0;
            this.opened = false;
            this.boozeAmount = 0;
         }

         return true;
      }
   }

   private void open() {
      if (!this.opened) {
         this.opened = true;
         if (this.type == 1) {
            float ratio;
            if (this.hopsCount <= 0) {
               ratio = 0.0F;
            } else {
               ratio = (float)this.hopsCount / this.wheatCount;
            }

            if (ratio <= 0.25F) {
               this.hopsRatio = 0;
            } else if (ratio <= 0.33333334F) {
               this.hopsRatio = 1;
            } else if (ratio <= 0.5F) {
               this.hopsRatio = 2;
            } else if (ratio < 2.0F) {
               this.hopsRatio = 3;
            } else {
               this.hopsRatio = (byte)Math.min(6.0, Math.floor(ratio) + 2.0);
               if (ratio >= 5.0F) {
                  this.timeRatio = 5;
               }
            }

            if (this.boozeAmount <= 0) {
               ratio = Float.POSITIVE_INFINITY;
            } else {
               ratio = (float)(this.hopsCount + this.wheatCount) / this.boozeAmount;
            }

            if (ratio <= 0.41666666F) {
               this.solidRatio = 0;
            } else if (ratio <= 0.5F) {
               this.solidRatio = 1;
            } else if (ratio < 1.0F) {
               this.solidRatio = 2;
            } else if (ratio == 1.0F) {
               this.solidRatio = 3;
            } else if (ratio < 2.0F) {
               this.solidRatio = 4;
            } else if (ratio < 2.4F) {
               this.solidRatio = 5;
            } else {
               this.solidRatio = 6;
               if (ratio >= 4.0F) {
                  this.timeRatio = 5;
               }
            }
         }
      }
   }

   public int calculateMetaValue() {
      if (this.isEmpty()) {
         return 0;
      } else if (this.type == 1) {
         this.open();
         int value = 0;
         value |= this.timeRatio;
         value <<= 3;
         value |= this.hopsRatio;
         value <<= 3;
         value |= this.solidRatio;
         value <<= 5;
         value |= this.boozeAmount - 1;
         value <<= 2;
         return value | this.type;
      } else if (this.type == 2) {
         this.open();
         int value = 0;
         int progress = this.age * 100 / this.timeNedForRum(this.boozeAmount);
         if (progress > 100) {
            progress = 100;
         }

         value |= progress;
         value <<= 5;
         value |= this.boozeAmount - 1;
         value <<= 2;
         return value | this.type;
      } else {
         return 0;
      }
   }

   public int timeNedForRum(int amount) {
      return (int)(1200 * amount * Math.pow(0.95, amount - 1));
   }

   @Override
   protected ItemStack getPickBlock(Player player, HitResult target) {
      return BlockName.scaffold.getItemStack(BlockScaffold.ScaffoldType.wood);
   }

   @Override
   protected List<ItemStack> getAuxDrops(int fortune) {
      List<ItemStack> ret = new ArrayList<>(super.getAuxDrops(fortune));
      ret.add(ItemName.barrel.getItemStack());
      return ret;
   }
}
