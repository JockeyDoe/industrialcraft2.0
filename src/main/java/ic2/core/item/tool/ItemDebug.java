package ic2.core.item.tool;

import ic2.api.crops.CropCard;
import ic2.api.energy.EnergyNet;
import ic2.api.item.IBoxable;
import ic2.api.item.IDebuggable;
import ic2.api.item.IElectricItemManager;
import ic2.api.item.ISpecialElectricItem;
import ic2.api.reactor.IReactor;
import ic2.api.tile.IEnergyStorage;
import ic2.core.IC2;
import ic2.core.block.TileEntityBlock;
import ic2.core.block.comp.Energy;
import ic2.core.block.comp.Ic2FluidTank;
import ic2.core.block.comp.Redstone;
import ic2.core.block.comp.TileEntityComponent;
import ic2.core.block.generator.tileentity.TileEntityBaseGenerator;
import ic2.core.block.personal.IPersonalBlock;
import ic2.core.crop.TileEntityCrop;
import ic2.core.item.InfiniteElectricItemManager;
import ic2.core.item.ItemDefaults;
import ic2.core.item.ItemIC2;
import ic2.core.ref.ItemName;
import ic2.core.util.ItemData;
import ic2.core.util.ItemStacks;
import ic2.core.util.LevelIds;
import ic2.core.util.LogCategory;
import ic2.core.util.StackUtil;
import ic2.core.util.Util;
import ic2.core.util.WorldQueries;
import java.io.ByteArrayOutputStream;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

public class ItemDebug extends ItemIC2 implements ISpecialElectricItem, IBoxable {
   private static IElectricItemManager manager = null;

   public ItemDebug() {
      super(ItemName.debug_item);
      ItemDefaults.variants(this, false);
      if (!Util.inDev()) {
         ItemDefaults.hideFromCreative(this);
      }
   }

   public InteractionResult onItemUseFirst(Player player, Level world, BlockPos pos, Direction side, float hitX, float hitY, float hitZ, InteractionHand hand) {
      CompoundTag nbtData = StackUtil.getOrCreateNbtData(StackUtil.get(player, hand));
      int modeIdx = nbtData.getIntOr("mode", 0);
      if (modeIdx < 0 || modeIdx >= ItemDebug.Mode.modes.length) {
         modeIdx = 0;
      }

      ItemDebug.Mode mode = ItemDebug.Mode.modes[modeIdx];
      if (IC2.keyboard.isModeSwitchKeyDown(player)) {
         if (!world.isClientSide()) {
            mode = ItemDebug.Mode.modes[(mode.ordinal() + 1) % ItemDebug.Mode.modes.length];
            nbtData.putInt("mode", mode.ordinal());
            ItemData.commit(StackUtil.get(player, hand), nbtData);
            IC2.platform.messagePlayer(player, "Debug Item Mode: " + mode.getName());
            return InteractionResult.SUCCESS;
         } else {
            return InteractionResult.PASS;
         }
      } else {
         BlockEntity tileentity = world.getBlockEntity(pos);
         if (tileentity instanceof IDebuggable) {
            if (world.isClientSide()) {
               return InteractionResult.PASS;
            } else {
               IDebuggable dbg = (IDebuggable)tileentity;
               if (dbg.isDebuggable() && !world.isClientSide()) {
                  IC2.platform.messagePlayer(player, dbg.getDebugText());
               }

               return (InteractionResult)(world.isClientSide() ? InteractionResult.PASS : InteractionResult.SUCCESS);
            }
         } else {
            ByteArrayOutputStream consoleBuffer = new ByteArrayOutputStream();
            PrintStream console = new PrintStream(consoleBuffer);
            ByteArrayOutputStream chatBuffer = new ByteArrayOutputStream();
            PrintStream chat = new PrintStream(chatBuffer);
            switch (mode) {
               case InterfacesFields:
               case InterfacesFieldsRetrace:
                  HitResult position;
                  if (mode == ItemDebug.Mode.InterfacesFields) {
                     position = new BlockHitResult(new Vec3(pos.getX() + hitX, pos.getY() + hitY, pos.getZ() + hitZ), side, pos, false);
                  } else {
                     position = getPlayerPOVHitResult(world, player, Fluid.ANY);
                     if (position == null) {
                        return InteractionResult.PASS;
                     }

                     HitResult entityPosition = Util.traceEntities(player, position.getLocation(), true);
                     if (entityPosition != null) {
                        position = entityPosition;
                     }
                  }

                  String plat;
                  if (FMLEnvironment.getDist().isClient()) {
                     if (!world.isClientSide()) {
                        plat = "sp server";
                     } else if (ServerLifecycleHooks.getCurrentServer() == null) {
                        plat = "mp client";
                     } else {
                        plat = "sp client";
                     }
                  } else {
                     plat = "mp server";
                  }

                  if (position.getType() == Type.BLOCK) {
                     pos = ((BlockHitResult)position).getBlockPos();
                     BlockState state = world.getBlockState(pos);
                     Block block = state.getBlock();
                     BlockEntity te = world.getBlockEntity(pos);
                     String message = String.format(
                        "[%s] block state: %s%nname: %s%ncls: %s%nte: %s", plat, state, block.getDescriptionId(), block.getClass().getName(), te
                     );
                     chat.println(message);
                     console.println(message);
                     if (te != null) {
                        message = "[" + plat + "] interfaces:";
                        Class<?> c = te.getClass();

                        do {
                           for (Class<?> i : c.getInterfaces()) {
                              message = message + " " + i.getName();
                           }

                           c = c.getSuperclass();
                        } while (c != null);

                        chat.println(message);
                        console.println(message);
                     }

                     console.println("block fields:");
                     dumpObjectFields(console, block);
                     if (te != null) {
                        console.println();
                        console.println("tile entity fields:");
                        dumpObjectFields(console, te);
                     }
                  } else {
                     if (position.getType() != Type.ENTITY) {
                        return InteractionResult.PASS;
                     }

                     String messagex = "[" + plat + "] entity: " + ((EntityHitResult)position).getEntity();
                     chat.println(messagex);
                     console.println(messagex);
                     if (((EntityHitResult)position).getEntity() instanceof ItemEntity) {
                        ItemStack entStack = ((ItemEntity)((EntityHitResult)position).getEntity()).getItem();
                        String name = Util.getName(entStack.getItem()).toString();
                        messagex = "["
                           + plat
                           + "] item id: "
                           + name
                           + " meta: "
                           + ItemStacks.rawMeta(entStack)
                           + " size: "
                           + StackUtil.getSize(entStack)
                           + " name: "
                           + entStack.getItem().getDescriptionId();
                        chat.println(messagex);
                        console.println(messagex);
                        console.println("NBT: " + ItemData.read(entStack));
                     }
                  }
                  break;
               case TileData:
                  if (world.isClientSide()) {
                     return InteractionResult.PASS;
                  }

                  BlockEntity tileEntity = world.getBlockEntity(pos);
                  if (tileEntity instanceof TileEntityBlock tex) {
                     chat.println("Block: Active=" + tex.getActive() + " Facing=" + tex.getFacing());

                     for (TileEntityComponent comp : tex.getComponents()) {
                        if (comp instanceof Energy energy) {
                           chat.printf("Energy: %.2f / %.2f%n", energy.getEnergy(), energy.getCapacity());
                        } else if (comp instanceof Redstone redstone) {
                           chat.printf("Redstone: %d%n", redstone.getRedstoneInput());
                        }
                     }
                  }

                  if (tileEntity instanceof TileEntityBaseGenerator tex) {
                     chat.println("BaseGen: Fuel=" + tex.fuel);
                  }

                  if (tileEntity instanceof IEnergyStorage tex) {
                     chat.println("EnergyStorage: Stored=" + tex.getStored());
                  }

                  if (tileEntity instanceof IReactor tex) {
                     chat.println(
                        "Reactor: Heat="
                           + tex.getHeat()
                           + " MaxHeat="
                           + tex.getMaxHeat()
                           + " HEM="
                           + tex.getHeatEffectModifier()
                           + " Output="
                           + tex.getReactorEnergyOutput()
                     );
                  }

                  if (tileEntity instanceof IPersonalBlock tex) {
                     chat.println("PersonalBlock: CanAccess=" + tex.permitsAccess(player.getGameProfile()));
                  }

                  if (tileEntity instanceof TileEntityCrop tex) {
                     CropCard crop = tex.getCrop();
                     String id = crop != null ? crop.getOwner() + ":" + crop.getId() : "none";
                     chat.printf(
                        "Crop: Crop=%s Size=%d Growth=%d Gain=%d Resistance=%d Nutrients=%d Water=%d GrowthPoints=%d%n Cross=%b",
                        id,
                        tex.getCurrentSize(),
                        tex.getStatGrowth(),
                        tex.getStatGain(),
                        tex.getStatResistance(),
                        tex.getStorageNutrients(),
                        tex.getStorageWater(),
                        tex.getGrowthPoints(),
                        tex.isCrossingBase()
                     );
                  }
                  break;
               case EnergyNet:
                  if (world.isClientSide()) {
                     return InteractionResult.PASS;
                  }

                  if (!EnergyNet.instance.dumpDebugInfo(world, pos, console, chat)) {
                     return InteractionResult.PASS;
                  }
                  break;
               case Accelerate:
               case AccelerateX100:
                  if (world.isClientSide()) {
                     return InteractionResult.PASS;
                  }

                  BlockEntity tex = world.getBlockEntity(pos);
                  int count = mode == ItemDebug.Mode.Accelerate ? 1000 : 100000;
                  if (tex == null) {
                     BlockState statex = world.getBlockState(pos);
                     if (statex.isRandomlyTicking()) {
                        chat.println("Running" + count + " ticks on " + statex.getBlock() + "(" + pos + ").");

                        int i;
                        for (i = 0; i < count && world.getBlockState(pos) == statex; i++) {
                           statex.randomTick((ServerLevel)world, pos, world.getRandom());
                        }

                        if (i != count) {
                           chat.println("Ran " + i + " ticks before a state change.");
                        }
                     }
                  } else if (WorldQueries.hasTicker(world, tex)) {
                     BlockEntity tickable = tex;
                     chat.println("Running " + count + " ticks on " + tex + ".");
                     int changes = 0;
                     int interruptCount = -1;

                     for (int ix = 0; ix < count; ix++) {
                        if (tex.isRemoved()) {
                           changes++;
                           tex = world.getBlockEntity(pos);
                           if (!WorldQueries.hasTicker(world, tex) || tex.isRemoved()) {
                              interruptCount = ix;
                              break;
                           }

                           tickable = tex;
                        }

                        WorldQueries.tickBlockEntity(world, tickable);
                     }

                     if (changes > 0) {
                        if (interruptCount != -1) {
                           chat.println("The tile entity changed " + changes + " time(s), interrupted after " + interruptCount + " updates.");
                        } else {
                           chat.println("The tile entity changed " + changes + " time(s).");
                        }
                     }
                  }
            }

            console.flush();
            chat.flush();
            if (world.isClientSide()) {
               try {
                  consoleBuffer.writeTo(new FileOutputStream(FileDescriptor.out));
               } catch (IOException var29) {
                  IC2.log.warn(LogCategory.Item, var29, "Stdout write failed.");
               }

               for (String line : chatBuffer.toString().split("[\\r\\n]+")) {
                  IC2.platform.messagePlayer(player, line);
               }
            } else if (player instanceof ServerPlayer) {
               try {
                  IC2.network.get(true).sendConsole((ServerPlayer)player, consoleBuffer.toString("UTF-8"));
                  IC2.network.get(true).sendChat((ServerPlayer)player, chatBuffer.toString("UTF-8"));
               } catch (UnsupportedEncodingException var28) {
                  IC2.log.warn(LogCategory.Item, var28, "String encoding failed.");
               }
            }

            return (InteractionResult)(world.isClientSide() ? InteractionResult.PASS : InteractionResult.SUCCESS);
         }
      }
   }

   private static void dumpObjectFields(PrintStream ps, Object o) {
      Class<?> fieldDeclaringClass = o.getClass();

      do {
         Field[] fields = fieldDeclaringClass.getDeclaredFields();

         for (Field field : fields) {
            if ((field.getModifiers() & 8) == 0 || fieldDeclaringClass != Block.class && fieldDeclaringClass != BlockEntity.class) {
               boolean accessible = field.isAccessible();
               field.setAccessible(true);

               Object value;
               try {
                  value = field.get(o);
               } catch (IllegalAccessException var13) {
                  value = "<can't access>";
               }

               ps.println(field.getName() + " class: " + fieldDeclaringClass.getName() + " type: " + field.getType());
               ps.printf(
                  "  identity hash: %x hash: %x modifiers: %x%n", System.identityHashCode(value), value == null ? 0 : value.hashCode(), field.getModifiers()
               );
               if (value != null && field.getType().isArray()) {
                  List<Object> array = new ArrayList<>();

                  for (int i = 0; i < Array.getLength(value); i++) {
                     array.add(Array.get(value, i));
                  }

                  value = array;
               }

               if (value instanceof Iterable) {
                  ps.println("  values (" + (value instanceof Collection ? ((Collection)value).size() : "?") + "):");
                  int i = 0;

                  for (Object o2 : (Iterable)value) {
                     ps.print("    [" + i++ + "] ");
                     dumpValueString(o2, field, "      ", ps);
                  }
               } else if (value instanceof Map) {
                  ps.println("  values (" + ((Map)value).size() + "):");

                  for (Entry<?, ?> entry : ((Map)value).entrySet()) {
                     ps.print("    " + entry.getKey() + ": ");
                     dumpValueString(entry.getValue(), field, "      ", ps);
                  }
               } else {
                  ps.print("  value: ");
                  dumpValueString(value, field, "    ", ps);
               }

               field.setAccessible(accessible);
            }
         }

         fieldDeclaringClass = fieldDeclaringClass.getSuperclass();
      } while (fieldDeclaringClass != null);
   }

   private static void dumpValueString(Object o, Field parentField, String prefix, PrintStream out) {
      if (o == null) {
         out.println("<null>");
      } else {
         String ret;
         if (o.getClass().isArray()) {
            ret = "";

            for (int i = 0; i < Array.getLength(o); i++) {
               Object val = Array.get(o, i);
               String valStr;
               if (val == null) {
                  valStr = "<null>";
               } else {
                  valStr = val.toString();
                  if (valStr.length() > 32) {
                     valStr = valStr.substring(0, 20) + "... (" + (valStr.length() - 20) + " more)";
                  }
               }

               ret = ret + " [" + i + "] " + valStr;
            }
         } else {
            ret = o.toString();
         }

         if (ret.length() > 100) {
            ret = ret.substring(0, 90) + "... (" + (ret.length() - 90) + " more)";
         }

         out.println(ret);
         if (o instanceof Ic2FluidTank && IC2.platform.isSimulating()) {
            System.out.println();
         }

         if (!Modifier.isStatic(parentField.getModifiers())
            && !parentField.isSynthetic()
            && !o.getClass().isArray()
            && !o.getClass().isEnum()
            && !o.getClass().isPrimitive()
            && !(o instanceof Iterable)
            && !(o instanceof Class)
            && !(o instanceof String)) {
            if (o instanceof Level) {
               out.println(prefix + " dim: " + LevelIds.id((Level)o));
            } else if (!(o instanceof StateDefinition)
               && !(o instanceof Block)
               && !(o instanceof BlockEntity)
               && !(o instanceof Item)
               && !(o instanceof ItemStack)
               && !(o instanceof Vec3i)
               && !(o instanceof Vec3)
               && !(o instanceof Tag)
               && !o.getClass().getName().startsWith("java.")) {
               for (Class<?> fieldDeclaringClass = o.getClass();
                  fieldDeclaringClass != null && fieldDeclaringClass != Object.class;
                  fieldDeclaringClass = fieldDeclaringClass.getSuperclass()
               ) {
                  for (Field field : fieldDeclaringClass.getDeclaredFields()) {
                     if (!field.isSynthetic() && !Modifier.isStatic(field.getModifiers())) {
                        Object val;
                        try {
                           field.setAccessible(true);
                           val = field.get(o);
                        } catch (Exception var12) {
                           val = "<can't access>";
                        }

                        String valStr;
                        if (val == o) {
                           valStr = "<parent>";
                        } else {
                           valStr = toStringLimited(val, 100);
                        }

                        out.println(prefix + field.getName() + ": " + valStr);
                     }
                  }
               }
            }
         }
      }
   }

   private static String toStringLimited(Object o, int limit) {
      if (o == null) {
         return "<null>";
      } else {
         int extra = 12;
         limit = Math.max(limit, 12);
         String ret = o.toString();
         if (ret.length() > limit) {
            int newLimit = limit - 12;
            return ret.substring(0, newLimit) + "... (" + (ret.length() - newLimit) + " more)";
         } else {
            return ret;
         }
      }
   }

   @Override
   public IElectricItemManager getManager(ItemStack stack) {
      if (manager == null) {
         manager = new InfiniteElectricItemManager();
      }

      return manager;
   }

   @Override
   public boolean canBeStoredInToolbox(ItemStack itemstack) {
      return true;
   }

   private static enum Mode {
      InterfacesFields("Interfaces and Fields"),
      InterfacesFieldsRetrace("Interfaces and Fields (liquid/entity)"),
      TileData("Tile Data"),
      EnergyNet("Energy Net"),
      Accelerate("Accelerate"),
      AccelerateX100("Accelerate x100");

      static final ItemDebug.Mode[] modes = values();
      private final String name;

      private Mode(String name) {
         this.name = name;
      }

      String getName() {
         return this.name;
      }
   }
}
