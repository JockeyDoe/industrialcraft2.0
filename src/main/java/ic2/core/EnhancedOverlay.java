package ic2.core;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public class EnhancedOverlay {
   private static final Map<Direction, EnhancedOverlay> SIDE_TO_OVERLAY = new EnumMap<>(Direction.class);
   private final Map<EnhancedOverlay.Segment, EnhancedOverlay.RawSegment> segmentMap;

   private EnhancedOverlay(Map<EnhancedOverlay.Segment, EnhancedOverlay.RawSegment> segmentMap) {
      this.segmentMap = segmentMap;
   }

   public static EnhancedOverlay forFace(Direction face) {
      return SIDE_TO_OVERLAY.get(face);
   }

   public static void transformToFace(PoseStack pose, Direction face) {
      pose.translate(0.5, 0.5, 0.5);
      switch (face) {
         case UP:
            pose.rotateDegrees(Axis.XP, 180.0F);
            break;
         case NORTH:
            pose.rotateDegrees(Axis.XP, 90.0F);
            break;
         case SOUTH:
            pose.rotateDegrees(Axis.XP, -90.0F);
            break;
         case WEST:
            pose.rotateDegrees(Axis.ZP, -90.0F);
            break;
         case EAST:
            pose.rotateDegrees(Axis.ZP, 90.0F);
      }

      pose.translate(0.0, -0.501, 0.0);
   }

   public List<Vec3> vertices(EnhancedOverlay.Segment segment) {
      ArrayList<Vec3> result = new ArrayList<>(4);
      this.segmentMap.get(segment).drawRaw((x, y, z) -> result.add(new Vec3(x, y, z)));
      return List.copyOf(result);
   }

   public static Direction spin(Direction side, EnhancedOverlay.Segment segment) {
      return switch (segment) {
         case TOP -> side.getAxis().isVertical() ? Direction.NORTH : Direction.UP;
         default -> side.getOpposite();
         case LEFT -> side.getAxis().isVertical() ? Direction.WEST : side.getClockWise();
         case CENTRE -> side;
         case RIGHT -> side.getAxis().isVertical() ? Direction.EAST : side.getCounterClockWise();
         case BOTTOM -> side.getAxis().isVertical() ? Direction.SOUTH : Direction.DOWN;
      };
   }

   static {
      Map<EnhancedOverlay.Segment, EnhancedOverlay.RawSegment> segmentMap = new EnumMap<>(EnhancedOverlay.Segment.class);
      SIDE_TO_OVERLAY.put(Direction.DOWN, new EnhancedOverlay(segmentMap));
      segmentMap.put(EnhancedOverlay.Segment.TOP_LEFT, EnhancedOverlay.RawSegment.C_BOX);
      segmentMap.put(EnhancedOverlay.Segment.TOP, EnhancedOverlay.RawSegment.B_BAR);
      segmentMap.put(EnhancedOverlay.Segment.TOP_RIGHT, EnhancedOverlay.RawSegment.A_BOX);
      segmentMap.put(EnhancedOverlay.Segment.LEFT, EnhancedOverlay.RawSegment.F_BAR);
      segmentMap.put(EnhancedOverlay.Segment.CENTRE, EnhancedOverlay.RawSegment.E_CENTRE);
      segmentMap.put(EnhancedOverlay.Segment.RIGHT, EnhancedOverlay.RawSegment.D_BAR);
      segmentMap.put(EnhancedOverlay.Segment.BOTTOM_LEFT, EnhancedOverlay.RawSegment.I_BOX);
      segmentMap.put(EnhancedOverlay.Segment.BOTTOM, EnhancedOverlay.RawSegment.H_BAR);
      segmentMap.put(EnhancedOverlay.Segment.BOTTOM_RIGHT, EnhancedOverlay.RawSegment.G_BOX);
      segmentMap = new EnumMap<>(EnhancedOverlay.Segment.class);
      SIDE_TO_OVERLAY.put(Direction.UP, new EnhancedOverlay(segmentMap));
      segmentMap.put(EnhancedOverlay.Segment.TOP_LEFT, EnhancedOverlay.RawSegment.I_BOX);
      segmentMap.put(EnhancedOverlay.Segment.TOP, EnhancedOverlay.RawSegment.H_BAR);
      segmentMap.put(EnhancedOverlay.Segment.TOP_RIGHT, EnhancedOverlay.RawSegment.G_BOX);
      segmentMap.put(EnhancedOverlay.Segment.LEFT, EnhancedOverlay.RawSegment.F_BAR);
      segmentMap.put(EnhancedOverlay.Segment.CENTRE, EnhancedOverlay.RawSegment.E_CENTRE);
      segmentMap.put(EnhancedOverlay.Segment.RIGHT, EnhancedOverlay.RawSegment.D_BAR);
      segmentMap.put(EnhancedOverlay.Segment.BOTTOM_LEFT, EnhancedOverlay.RawSegment.C_BOX);
      segmentMap.put(EnhancedOverlay.Segment.BOTTOM, EnhancedOverlay.RawSegment.B_BAR);
      segmentMap.put(EnhancedOverlay.Segment.BOTTOM_RIGHT, EnhancedOverlay.RawSegment.A_BOX);
      Map<EnhancedOverlay.Segment, EnhancedOverlay.RawSegment> var2 = new EnumMap<>(EnhancedOverlay.Segment.class);
      SIDE_TO_OVERLAY.put(Direction.NORTH, new EnhancedOverlay(var2));
      var2.put(EnhancedOverlay.Segment.TOP_LEFT, EnhancedOverlay.RawSegment.A_BOX);
      var2.put(EnhancedOverlay.Segment.TOP, EnhancedOverlay.RawSegment.B_BAR);
      var2.put(EnhancedOverlay.Segment.TOP_RIGHT, EnhancedOverlay.RawSegment.C_BOX);
      var2.put(EnhancedOverlay.Segment.LEFT, EnhancedOverlay.RawSegment.D_BAR);
      var2.put(EnhancedOverlay.Segment.CENTRE, EnhancedOverlay.RawSegment.E_CENTRE);
      var2.put(EnhancedOverlay.Segment.RIGHT, EnhancedOverlay.RawSegment.F_BAR);
      var2.put(EnhancedOverlay.Segment.BOTTOM_LEFT, EnhancedOverlay.RawSegment.G_BOX);
      var2.put(EnhancedOverlay.Segment.BOTTOM, EnhancedOverlay.RawSegment.H_BAR);
      var2.put(EnhancedOverlay.Segment.BOTTOM_RIGHT, EnhancedOverlay.RawSegment.I_BOX);
      Map<EnhancedOverlay.Segment, EnhancedOverlay.RawSegment> var3 = new EnumMap<>(EnhancedOverlay.Segment.class);
      SIDE_TO_OVERLAY.put(Direction.SOUTH, new EnhancedOverlay(var3));
      var3.put(EnhancedOverlay.Segment.TOP_LEFT, EnhancedOverlay.RawSegment.I_BOX);
      var3.put(EnhancedOverlay.Segment.TOP, EnhancedOverlay.RawSegment.H_BAR);
      var3.put(EnhancedOverlay.Segment.TOP_RIGHT, EnhancedOverlay.RawSegment.G_BOX);
      var3.put(EnhancedOverlay.Segment.LEFT, EnhancedOverlay.RawSegment.F_BAR);
      var3.put(EnhancedOverlay.Segment.CENTRE, EnhancedOverlay.RawSegment.E_CENTRE);
      var3.put(EnhancedOverlay.Segment.RIGHT, EnhancedOverlay.RawSegment.D_BAR);
      var3.put(EnhancedOverlay.Segment.BOTTOM_LEFT, EnhancedOverlay.RawSegment.C_BOX);
      var3.put(EnhancedOverlay.Segment.BOTTOM, EnhancedOverlay.RawSegment.B_BAR);
      var3.put(EnhancedOverlay.Segment.BOTTOM_RIGHT, EnhancedOverlay.RawSegment.A_BOX);
      Map<EnhancedOverlay.Segment, EnhancedOverlay.RawSegment> var4 = new EnumMap<>(EnhancedOverlay.Segment.class);
      SIDE_TO_OVERLAY.put(Direction.WEST, new EnhancedOverlay(var4));
      var4.put(EnhancedOverlay.Segment.TOP_LEFT, EnhancedOverlay.RawSegment.C_BOX);
      var4.put(EnhancedOverlay.Segment.TOP, EnhancedOverlay.RawSegment.F_BAR);
      var4.put(EnhancedOverlay.Segment.TOP_RIGHT, EnhancedOverlay.RawSegment.I_BOX);
      var4.put(EnhancedOverlay.Segment.LEFT, EnhancedOverlay.RawSegment.B_BAR);
      var4.put(EnhancedOverlay.Segment.CENTRE, EnhancedOverlay.RawSegment.E_CENTRE);
      var4.put(EnhancedOverlay.Segment.RIGHT, EnhancedOverlay.RawSegment.H_BAR);
      var4.put(EnhancedOverlay.Segment.BOTTOM_LEFT, EnhancedOverlay.RawSegment.A_BOX);
      var4.put(EnhancedOverlay.Segment.BOTTOM, EnhancedOverlay.RawSegment.D_BAR);
      var4.put(EnhancedOverlay.Segment.BOTTOM_RIGHT, EnhancedOverlay.RawSegment.G_BOX);
      Map<EnhancedOverlay.Segment, EnhancedOverlay.RawSegment> var5 = new EnumMap<>(EnhancedOverlay.Segment.class);
      SIDE_TO_OVERLAY.put(Direction.EAST, new EnhancedOverlay(var5));
      var5.put(EnhancedOverlay.Segment.TOP_LEFT, EnhancedOverlay.RawSegment.G_BOX);
      var5.put(EnhancedOverlay.Segment.TOP, EnhancedOverlay.RawSegment.D_BAR);
      var5.put(EnhancedOverlay.Segment.TOP_RIGHT, EnhancedOverlay.RawSegment.A_BOX);
      var5.put(EnhancedOverlay.Segment.LEFT, EnhancedOverlay.RawSegment.H_BAR);
      var5.put(EnhancedOverlay.Segment.CENTRE, EnhancedOverlay.RawSegment.E_CENTRE);
      var5.put(EnhancedOverlay.Segment.RIGHT, EnhancedOverlay.RawSegment.B_BAR);
      var5.put(EnhancedOverlay.Segment.BOTTOM_LEFT, EnhancedOverlay.RawSegment.I_BOX);
      var5.put(EnhancedOverlay.Segment.BOTTOM, EnhancedOverlay.RawSegment.F_BAR);
      var5.put(EnhancedOverlay.Segment.BOTTOM_RIGHT, EnhancedOverlay.RawSegment.C_BOX);
   }

   private static enum RawSegment {
      A_BOX {
         @Override
         void drawRaw(EnhancedOverlay.TripleDoubleConsumer buffer) {
            buffer.accept(0.5, 0.0, -0.5);
            buffer.accept(0.25, 0.0, -0.5);
            buffer.accept(0.25, 0.0, -0.25);
            buffer.accept(0.5, 0.0, -0.25);
         }
      },
      B_BAR {
         @Override
         void drawRaw(EnhancedOverlay.TripleDoubleConsumer buffer) {
            buffer.accept(-0.25, 0.0, -0.5);
            buffer.accept(0.25, 0.0, -0.5);
            buffer.accept(0.25, 0.0, -0.25);
            buffer.accept(-0.25, 0.0, -0.25);
         }
      },
      C_BOX {
         @Override
         void drawRaw(EnhancedOverlay.TripleDoubleConsumer buffer) {
            buffer.accept(-0.5, 0.0, -0.5);
            buffer.accept(-0.25, 0.0, -0.5);
            buffer.accept(-0.25, 0.0, -0.25);
            buffer.accept(-0.5, 0.0, -0.25);
         }
      },
      D_BAR {
         @Override
         void drawRaw(EnhancedOverlay.TripleDoubleConsumer buffer) {
            buffer.accept(0.5, 0.0, -0.25);
            buffer.accept(0.25, 0.0, -0.25);
            buffer.accept(0.25, 0.0, 0.25);
            buffer.accept(0.5, 0.0, 0.25);
         }
      },
      E_CENTRE {
         @Override
         void drawRaw(EnhancedOverlay.TripleDoubleConsumer buffer) {
            buffer.accept(0.25, 0.0, -0.25);
            buffer.accept(-0.25, 0.0, -0.25);
            buffer.accept(-0.25, 0.0, 0.25);
            buffer.accept(0.25, 0.0, 0.25);
         }
      },
      F_BAR {
         @Override
         void drawRaw(EnhancedOverlay.TripleDoubleConsumer buffer) {
            buffer.accept(-0.5, 0.0, -0.25);
            buffer.accept(-0.25, 0.0, -0.25);
            buffer.accept(-0.25, 0.0, 0.25);
            buffer.accept(-0.5, 0.0, 0.25);
         }
      },
      G_BOX {
         @Override
         void drawRaw(EnhancedOverlay.TripleDoubleConsumer buffer) {
            buffer.accept(0.5, 0.0, 0.5);
            buffer.accept(0.25, 0.0, 0.5);
            buffer.accept(0.25, 0.0, 0.25);
            buffer.accept(0.5, 0.0, 0.25);
         }
      },
      H_BAR {
         @Override
         void drawRaw(EnhancedOverlay.TripleDoubleConsumer buffer) {
            buffer.accept(-0.25, 0.0, 0.5);
            buffer.accept(0.25, 0.0, 0.5);
            buffer.accept(0.25, 0.0, 0.25);
            buffer.accept(-0.25, 0.0, 0.25);
         }
      },
      I_BOX {
         @Override
         void drawRaw(EnhancedOverlay.TripleDoubleConsumer buffer) {
            buffer.accept(-0.5, 0.0, 0.5);
            buffer.accept(-0.25, 0.0, 0.5);
            buffer.accept(-0.25, 0.0, 0.25);
            buffer.accept(-0.5, 0.0, 0.25);
         }
      };

      abstract void drawRaw(EnhancedOverlay.TripleDoubleConsumer var1);
   }

   public static enum Segment {
      TOP_LEFT,
      TOP,
      TOP_RIGHT,
      LEFT,
      CENTRE,
      RIGHT,
      BOTTOM_LEFT,
      BOTTOM,
      BOTTOM_RIGHT;

      public static EnhancedOverlay.Segment forRayTrace(HitResult ray) {
         assert ray.getType() == Type.BLOCK;

         Vec3i pos = ((BlockHitResult)ray).getBlockPos();
         Vec3 hit = ray.getLocation();
         return forHit(((BlockHitResult)ray).getDirection(), hit.x - pos.getX(), hit.y - pos.getY(), hit.z - pos.getZ());
      }

      public static EnhancedOverlay.Segment forHit(Direction face, double x, double y, double z) {
         switch (face) {
            case UP:
            case DOWN:
               return forHit(x, 1.0 - z);
            case NORTH:
               return forHit(1.0 - x, y);
            case SOUTH:
               return forHit(x, y);
            case WEST:
               return forHit(z, y);
            case EAST:
               return forHit(1.0 - z, y);
            default:
               throw new IllegalArgumentException("Unexpected face: " + face);
         }
      }

      public static EnhancedOverlay.Segment forHit(double hitX, double hitY) {
         if (hitX <= 0.25) {
            if (hitY <= 0.25) {
               return BOTTOM_LEFT;
            } else {
               return hitY >= 0.75 ? TOP_LEFT : LEFT;
            }
         } else if (hitX > 0.25 && hitX < 0.75) {
            if (hitY <= 0.25) {
               return BOTTOM;
            } else {
               return hitY >= 0.75 ? TOP : CENTRE;
            }
         } else if (hitX >= 0.75) {
            if (hitY <= 0.25) {
               return BOTTOM_RIGHT;
            } else {
               return hitY >= 0.75 ? TOP_RIGHT : RIGHT;
            }
         } else {
            throw new IllegalArgumentException("Unexpected hit values: [" + hitX + ", " + hitY + "]");
         }
      }
   }

   @FunctionalInterface
   private interface TripleDoubleConsumer {
      void accept(double var1, double var3, double var5);
   }

   private static enum XSegment {
      A_BOX {
         @Override
         void drawRaw(EnhancedOverlay.TripleDoubleConsumer buffer) {
            buffer.accept(0.25, 0.0, -0.25);
            buffer.accept(0.5, 0.0, -0.5);
            buffer.accept(0.25, 0.0, -0.5);
            buffer.accept(0.5, 0.0, -0.25);
         }
      },
      B_BAR {
         @Override
         void drawRaw(EnhancedOverlay.TripleDoubleConsumer buffer) {
            buffer.accept(-0.25, 0.0, -0.25);
            buffer.accept(0.25, 0.0, -0.5);
            buffer.accept(-0.25, 0.0, -0.5);
            buffer.accept(0.25, 0.0, -0.25);
         }
      },
      C_BOX {
         @Override
         void drawRaw(EnhancedOverlay.TripleDoubleConsumer buffer) {
            buffer.accept(-0.5, 0.0, -0.25);
            buffer.accept(-0.25, 0.0, -0.5);
            buffer.accept(-0.5, 0.0, -0.5);
            buffer.accept(-0.25, 0.0, -0.25);
         }
      },
      D_BAR {
         @Override
         void drawRaw(EnhancedOverlay.TripleDoubleConsumer buffer) {
            buffer.accept(0.25, 0.0, 0.25);
            buffer.accept(0.5, 0.0, -0.25);
            buffer.accept(0.25, 0.0, -0.25);
            buffer.accept(0.5, 0.0, 0.25);
         }
      },
      E_CENTRE {
         @Override
         void drawRaw(EnhancedOverlay.TripleDoubleConsumer buffer) {
            buffer.accept(-0.25, 0.0, 0.25);
            buffer.accept(0.25, 0.0, -0.25);
            buffer.accept(-0.25, 0.0, -0.25);
            buffer.accept(0.25, 0.0, 0.25);
         }
      },
      F_BAR {
         @Override
         void drawRaw(EnhancedOverlay.TripleDoubleConsumer buffer) {
            buffer.accept(-0.5, 0.0, 0.25);
            buffer.accept(-0.25, 0.0, -0.25);
            buffer.accept(-0.5, 0.0, -0.25);
            buffer.accept(-0.25, 0.0, 0.25);
         }
      },
      G_BOX {
         @Override
         void drawRaw(EnhancedOverlay.TripleDoubleConsumer buffer) {
            buffer.accept(0.25, 0.0, 0.5);
            buffer.accept(0.5, 0.0, 0.25);
            buffer.accept(0.25, 0.0, 0.25);
            buffer.accept(0.5, 0.0, 0.5);
         }
      },
      H_BAR {
         @Override
         void drawRaw(EnhancedOverlay.TripleDoubleConsumer buffer) {
            buffer.accept(-0.25, 0.0, 0.5);
            buffer.accept(0.25, 0.0, 0.25);
            buffer.accept(-0.25, 0.0, 0.25);
            buffer.accept(0.25, 0.0, 0.5);
         }
      },
      I_BOX {
         @Override
         void drawRaw(EnhancedOverlay.TripleDoubleConsumer buffer) {
            buffer.accept(-0.5, 0.0, 0.5);
            buffer.accept(-0.25, 0.0, 0.25);
            buffer.accept(-0.5, 0.0, 0.25);
            buffer.accept(-0.25, 0.0, 0.5);
         }
      };

      abstract void drawRaw(EnhancedOverlay.TripleDoubleConsumer var1);
   }
}
