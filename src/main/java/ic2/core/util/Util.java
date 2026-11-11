package ic2.core.util;

import ic2.core.IC2;
import ic2.core.Ic2Player;
import ic2.core.init.NativeRegistration;
import ic2.core.recipe.RecipeTags;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.EmptyLevelChunk;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;

public final class Util {
   private static final Map<Class<? extends BlockGetter>, Field> worldFieldCache = new IdentityHashMap<>();
   public static Set<Direction> noFacings = Collections.emptySet();
   public static Set<Direction> onlyNorth = Collections.unmodifiableSet(EnumSet.of(Direction.NORTH));
   public static Set<Direction> horizontalFacings = Collections.unmodifiableSet(EnumSet.copyOf(Arrays.asList(DirectionOrder.HORIZONTAL)));
   public static Set<Direction> verticalFacings = Collections.unmodifiableSet(EnumSet.of(Direction.DOWN, Direction.UP));
   public static Set<Direction> downSideFacings = Collections.unmodifiableSet(EnumSet.complementOf(EnumSet.of(Direction.UP)));
   public static Set<Direction> allFacings = Collections.unmodifiableSet(EnumSet.allOf(Direction.class));
   private static final boolean inDev = System.getProperty("INDEV") != null;
   private static final boolean includeWorldHash = System.getProperty("ic2.debug.includeworldhash") != null;
   private static final Map<Class<?>, Boolean> checkedClasses = new IdentityHashMap<>();

   public static int roundToNegInf(float x) {
      int ret = (int)x;
      if (ret > x) {
         ret--;
      }

      return ret;
   }

   public static int roundToNegInf(double x) {
      int ret = (int)x;
      if (ret > x) {
         ret--;
      }

      return ret;
   }

   public static int saturatedCast(double x) {
      if (x > 2.147483647E9) {
         return Integer.MAX_VALUE;
      } else {
         return x < -2.1474836E9F ? Integer.MIN_VALUE : (int)x;
      }
   }

   public static int limit(int value, int min, int max) {
      if (value <= min) {
         return min;
      } else {
         return value >= max ? max : value;
      }
   }

   public static float limit(float value, float min, float max) {
      if (!Float.isNaN(value) && !(value <= min)) {
         return value >= max ? max : value;
      } else {
         return min;
      }
   }

   public static double limit(double value, double min, double max) {
      if (!Double.isNaN(value) && !(value <= min)) {
         return value >= max ? max : value;
      } else {
         return min;
      }
   }

   public static double map(double value, double srcMax, double dstMax) {
      if (value < 0.0 || Double.isNaN(value)) {
         value = 0.0;
      }

      if (value > srcMax) {
         value = srcMax;
      }

      return value / srcMax * dstMax;
   }

   public static double lerp(double start, double end, double fraction) {
      assert fraction >= 0.0 && fraction <= 1.0;

      return start + (end - start) * fraction;
   }

   public static float lerp(float start, float end, float fraction) {
      assert fraction >= 0.0F && fraction <= 1.0F;

      return start + (end - start) * fraction;
   }

   public static int square(int x) {
      return x * x;
   }

   public static float square(float x) {
      return x * x;
   }

   public static double square(double x) {
      return x * x;
   }

   public static boolean isSimilar(float a, float b) {
      return Math.abs(a - b) < 1.0E-5F;
   }

   public static boolean isSimilar(double a, double b) {
      return Math.abs(a - b) < 1.0E-5;
   }

   public static int countInArray(Object[] oa, Class<?>... clsz) {
      int ret = 0;

      for (Object o : oa) {
         for (Class<?> cls : clsz) {
            if (cls.isAssignableFrom(o.getClass())) {
               ret++;
            }
         }
      }

      return ret;
   }

   public static int countInArray(Object[] oa, Class<?> cls) {
      int ret = 0;

      for (Object o : oa) {
         if (cls.isAssignableFrom(o.getClass())) {
            ret++;
         }
      }

      return ret;
   }

   public static boolean inDev() {
      return inDev;
   }

   public static boolean hasAssertions() {
      boolean ret = false;
      if (!$assertionsDisabled) {
         ret = true;
         if (false) {
            throw new AssertionError();
         }
      }

      return ret;
   }

   public static boolean matchesOD(ItemStack stack, Object match) {
      if (!(match instanceof ItemStack)) {
         if (match instanceof String) {
            if (StackUtil.isEmpty(stack)) {
               return false;
            } else {
               for (int oreId : RecipeTags.getOreIDs(stack)) {
                  if (RecipeTags.getOreName(oreId).equals(match)) {
                     return true;
                  }
               }

               return false;
            }
         } else {
            return stack == match;
         }
      } else {
         return !StackUtil.isEmpty(stack) && ItemStack.isSameItemSameComponents(stack, (ItemStack)match);
      }
   }

   public static String toString(BlockEntity te) {
      return te == null ? "null" : toString(te, te.getLevel(), te.getBlockPos());
   }

   public static String toString(Object o, BlockGetter world, BlockPos pos) {
      return toString(o, world, pos.getX(), pos.getY(), pos.getZ());
   }

   public static String toString(Object o, BlockGetter world, int x, int y, int z) {
      StringBuilder ret = new StringBuilder(64);
      if (o == null) {
         ret.append("null");
      } else {
         ret.append(o.getClass().getName());
         ret.append('@');
         ret.append(Integer.toHexString(System.identityHashCode(o)));
      }

      ret.append(" (");
      ret.append(formatPosition(world, x, y, z));
      ret.append(")");
      return ret.toString();
   }

   public static String formatPosition(BlockEntity te) {
      return formatPosition(te.getLevel(), te.getBlockPos());
   }

   public static String formatPosition(BlockGetter world, BlockPos pos) {
      return formatPosition(world, pos.getX(), pos.getY(), pos.getZ());
   }

   public static String formatPosition(BlockGetter world, int x, int y, int z) {
      int dimId;
      if (world instanceof Level && ((Level)world).dimension() != null) {
         dimId = LevelIds.id((Level)world);
      } else {
         dimId = Integer.MIN_VALUE;
      }

      return !includeWorldHash ? formatPosition(dimId, x, y, z) : String.format("dim %d (@%x): %d/%d/%d", dimId, System.identityHashCode(world), x, y, z);
   }

   public static String formatPosition(int dimId, int x, int y, int z) {
      return "dim " + dimId + ": " + x + "/" + y + "/" + z;
   }

   public static String formatPosition(BlockPos pos) {
      return formatPosition(pos.getX(), pos.getY(), pos.getZ());
   }

   public static String formatPosition(int x, int y, int z) {
      return x + "/" + y + "/" + z;
   }

   public static String toSiString(double value, int digits) {
      if (value == 0.0) {
         return "0 ";
      } else if (Double.isNaN(value)) {
         return "NaN ";
      } else {
         String ret = "";
         if (value < 0.0) {
            ret = "-";
            value = -value;
         }

         if (Double.isInfinite(value)) {
            return ret + "Ã¢Ë†Å¾ ";
         } else {
            double log = Math.log10(value);
            double mul;
            String si;
            if (log >= 0.0) {
               int reduce = (int)Math.floor(log / 3.0);
               mul = 1.0 / Math.pow(10.0, reduce * 3);
               switch (reduce) {
                  case 0:
                     si = "";
                     break;
                  case 1:
                     si = "k";
                     break;
                  case 2:
                     si = "M";
                     break;
                  case 3:
                     si = "G";
                     break;
                  case 4:
                     si = "T";
                     break;
                  case 5:
                     si = "P";
                     break;
                  case 6:
                     si = "E";
                     break;
                  case 7:
                     si = "Z";
                     break;
                  case 8:
                     si = "Y";
                     break;
                  default:
                     si = "E" + reduce * 3;
               }
            } else {
               int expand = (int)Math.ceil(-log / 3.0);
               mul = Math.pow(10.0, expand * 3);
               switch (expand) {
                  case 0:
                     si = "";
                     break;
                  case 1:
                     si = "m";
                     break;
                  case 2:
                     si = "Ã‚Âµ";
                     break;
                  case 3:
                     si = "n";
                     break;
                  case 4:
                     si = "p";
                     break;
                  case 5:
                     si = "f";
                     break;
                  case 6:
                     si = "a";
                     break;
                  case 7:
                     si = "z";
                     break;
                  case 8:
                     si = "y";
                     break;
                  default:
                     si = "E-" + expand * 3;
               }
            }

            value *= mul;
            int iVal = (int)Math.floor(value);
            value -= iVal;
            int iDigits = 1;
            if (iVal > 0) {
               iDigits = (int)(iDigits + Math.floor(Math.log10(iVal)));
            }

            mul = Math.pow(10.0, digits - iDigits);
            int dVal = (int)Math.round(value * mul);
            if (dVal >= mul) {
               iVal++;
               dVal = (int)(dVal - mul);
               iDigits = 1;
               if (iVal > 0) {
                  iDigits = (int)(iDigits + Math.floor(Math.log10(iVal)));
               }
            }

            ret = ret + Integer.toString(iVal);
            if (digits > iDigits && dVal != 0) {
               ret = ret + String.format(".%0" + (digits - iDigits) + "d", dVal);
            }

            ret = ret.replaceFirst("(\\.\\d*?)0+$", "$1");
            return ret + " " + si;
         }
      }
   }

   public static void exit(int status) {
      Method exit = null;

      try {
         exit = Class.forName("java.lang.Shutdown").getDeclaredMethod("exit", int.class);
         exit.setAccessible(true);
      } catch (Exception var6) {
         IC2.log.warn(LogCategory.General, var6, "Method lookup failed.");

         try {
            Field security = System.class.getDeclaredField("security");
            security.setAccessible(true);
            security.set(null, null);
            exit = System.class.getMethod("exit", int.class);
         } catch (Exception var5) {
            throw new Error(var5);
         }
      }

      try {
         exit.invoke(null, status);
      } catch (Exception var4) {
         throw new Error(var4);
      }
   }

   public static Vector3 getEyePosition(Entity entity) {
      return new Vector3(entity.getX(), entity.getY() + entity.getEyeHeight(), entity.getZ());
   }

   public static Vector3 getLook(Entity entity) {
      return new Vector3(entity.getLookAngle());
   }

   public static Vector3 getLookScaled(Entity entity) {
      return getLook(entity).scale(getReachDistance(entity));
   }

   public static double getReachDistance(Entity entity) {
      return entity instanceof ServerPlayer ? ((ServerPlayer)entity).blockInteractionRange() : 5.0;
   }

   public static HitResult traceBlocks(Player player, boolean liquid) {
      return traceBlocks(player, liquid, !liquid, false);
   }

   public static HitResult traceBlocks(Player player, boolean liquid, boolean ignoreBlockWithoutBoundingBox, boolean returnLastUncollidableBlock) {
      Vector3 start = getEyePosition(player);
      Vector3 end = getLookScaled(player).add(start);
      return player.level()
         .clip(
            new ClipContext(
               start.toVec3(), end.toVec3(), ignoreBlockWithoutBoundingBox ? Block.COLLIDER : Block.OUTLINE, liquid ? Fluid.SOURCE_ONLY : Fluid.NONE, player
            )
         );
   }

   public static HitResult traceEntities(Player player, boolean alwaysCollide) {
      Vector3 start = getEyePosition(player);
      return traceEntities(player.level(), start.toVec3(), getLookScaled(player).add(start).toVec3(), player, alwaysCollide);
   }

   public static HitResult traceEntities(Player player, Vec3 end, boolean alwaysCollide) {
      return traceEntities(player.level(), getEyePosition(player).toVec3(), end, player, alwaysCollide);
   }

   public static HitResult traceEntities(Level world, Vec3 start, Vec3 end, Entity exclude, boolean alwaysCollide) {
      AABB aabb = new AABB(
         Math.min(start.x, end.x),
         Math.min(start.y, end.y),
         Math.min(start.z, end.z),
         Math.max(start.x, end.x),
         Math.max(start.y, end.y),
         Math.max(start.z, end.z)
      );
      List<Entity> entities = world.getEntities(exclude, aabb);
      HitResult closest = null;
      double minDist = Double.POSITIVE_INFINITY;

      for (Entity entity : entities) {
         if (alwaysCollide || entity.isPickable()) {
            EntityHitResult pos = entity.getBoundingBox().clip(start, end).map(point -> new EntityHitResult(entity, point)).orElse(null);
            if (pos != null) {
               double distance = start.distanceToSqr(pos.getLocation());
               if (distance < minDist) {
                  minDist = distance;
                  closest = pos;
               }
            }
         }
      }

      return closest;
   }

   public static boolean isFakePlayer(Player entity, boolean fuzzy) {
      if (entity == null) {
         return false;
      } else if (!(entity instanceof ServerPlayer)) {
         return true;
      } else {
         return fuzzy ? entity instanceof FakePlayer : entity.getClass() != ServerPlayer.class;
      }
   }

   public static Level getWorld(BlockGetter world) {
      if (world == null) {
         return null;
      } else if (world instanceof Level) {
         return (Level)world;
      } else {
         Class<? extends BlockGetter> cls = world.getClass();
         Field field;
         synchronized (worldFieldCache) {
            field = worldFieldCache.get(cls);
            if (field == null && !worldFieldCache.containsKey(cls)) {
               field = ReflectionUtil.getFieldRecursive(world.getClass(), Level.class, false);
               worldFieldCache.put(cls, field);
            }
         }

         if (field != null) {
            try {
               return (Level)field.get(world);
            } catch (Exception var5) {
               throw new RuntimeException(var5);
            }
         } else {
            return null;
         }
      }
   }

   public static LevelChunk getLoadedChunk(Level world, int chunkX, int chunkZ) {
      LevelChunk chunk = world.getChunkSource().getChunk(chunkX, chunkZ, false);
      return chunk instanceof EmptyLevelChunk ? null : chunk;
   }

   public static boolean checkMcCoordBounds(int x, int y, int z) {
      return checkMcCoordBounds(x, z) && y >= 0 && y < 256;
   }

   public static boolean checkMcCoordBounds(int x, int z) {
      return x >= -30000000 && z >= -30000000 && x < 30000000 && z < 30000000;
   }

   public static boolean checkInterfaces(Class<?> cls) {
      Boolean cached = checkedClasses.get(cls);
      if (cached != null) {
         return cached;
      } else {
         Set<Class<?>> interfaces = Collections.newSetFromMap(new IdentityHashMap<>());
         Class<?> c = cls;

         do {
            for (Class<?> i : c.getInterfaces()) {
               interfaces.add(i);
            }

            c = c.getSuperclass();
         } while (c != null);

         boolean result = true;

         for (Class<?> iface : interfaces) {
            for (Method method : iface.getMethods()) {
               boolean found = false;
               c = cls;

               do {
                  try {
                     Method match = c.getDeclaredMethod(method.getName(), method.getParameterTypes());
                     if (method.getReturnType().isAssignableFrom(match.getReturnType())) {
                        found = true;
                        break;
                     }
                  } catch (NoSuchMethodException var13) {
                  }

                  c = c.getSuperclass();
               } while (c != null);

               if (!found) {
                  IC2.log.info(LogCategory.General, "Can't find method %s.%s in %s.", method.getDeclaringClass().getName(), method.getName(), cls.getName());
                  result = false;
               }
            }
         }

         checkedClasses.put(cls, result);
         return result;
      }
   }

   public static BlockState getBlockState(BlockGetter world, BlockPos pos) {
      return world.getBlockState(pos);
   }

   public static net.minecraft.world.level.block.Block getBlock(String name) {
      if (name == null) {
         throw new NullPointerException("null name");
      } else {
         return getBlock(Identifier.parse(name));
      }
   }

   public static net.minecraft.world.level.block.Block getBlock(Identifier loc) {
      net.minecraft.world.level.block.Block ret = (net.minecraft.world.level.block.Block)BuiltInRegistries.BLOCK.getValue(loc);
      if (ret != Blocks.AIR) {
         return ret;
      } else {
         return loc.getNamespace().equals("minecraft") && loc.getPath().equals("air") ? ret : null;
      }
   }

   public static Identifier getName(net.minecraft.world.level.block.Block block) {
      Identifier pending = NativeRegistration.name(block);
      return pending != null ? pending : BuiltInRegistries.BLOCK.getKey(block);
   }

   public static Item getItem(String name) {
      if (name == null) {
         throw new NullPointerException("null name");
      } else {
         return getItem(Identifier.parse(name));
      }
   }

   public static Item getItem(Identifier loc) {
      return (Item)BuiltInRegistries.ITEM.getValue(loc);
   }

   public static Identifier getName(Item item) {
      Identifier pending = NativeRegistration.name(item);
      return pending != null ? pending : BuiltInRegistries.ITEM.getKey(item);
   }

   public static boolean harvestBlock(Level world, BlockPos pos) {
      if (world.isClientSide()) {
         return false;
      } else {
         BlockState state = world.getBlockState(pos);
         net.minecraft.world.level.block.Block block = state.getBlock();
         BlockEntity te = world.getBlockEntity(pos);
         Player player = Ic2Player.get(world);
         boolean canHarvest = block.canHarvestBlock(state, world, pos, player);
         block.playerWillDestroy(world, pos, state, player);
         boolean removed = state.onDestroyedByPlayer(world, pos, player, player.getMainHandItem(), canHarvest, state.getFluidState());
         if (canHarvest && removed) {
            block.playerDestroy((ServerLevel)world, (ServerPlayer)player, pos, state, te, new ItemStack(Items.DIAMOND_PICKAXE));
         }

         return removed;
      }
   }
}
