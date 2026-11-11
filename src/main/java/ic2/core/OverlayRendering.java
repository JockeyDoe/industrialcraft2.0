package ic2.core;

import ic2.api.item.IEnhancedOverlayProvider;
import ic2.api.tile.IWrenchable;
import ic2.core.util.RotationUtil;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ExtractBlockOutlineRenderStateEvent;

@EventBusSubscriber(modid = "ic2", value = Dist.CLIENT)
public final class OverlayRendering {
   private static final List<Vec3> full = List.of(new Vec3(0.5, 0.0, -0.5), new Vec3(-0.5, 0.0, -0.5), new Vec3(-0.5, 0.0, 0.5), new Vec3(0.5, 0.0, 0.5));

   @SubscribeEvent
   public static void extract(ExtractBlockOutlineRenderStateEvent event) {
      LocalPlayer player = Minecraft.getInstance().player;
      if (player != null) {
         ItemStack stack = player.getItemInHand(InteractionHand.MAIN_HAND);
         ClientLevel world = event.getLevel();
         BlockPos pos = event.getBlockPos();
         Direction side = event.getHitResult().getDirection();
         if (stack.getItem() instanceof IEnhancedOverlayProvider provider && provider.providesEnhancedOverlay(world, pos, side, player, stack)) {
            List<OverlayRendering.Fill> fills = new ArrayList<>();
            EnhancedOverlay overlay = EnhancedOverlay.forFace(side);
            if (event.getBlockState().getBlock() instanceof IWrenchable wrench) {
               Direction var31 = RotationUtil.rotateByRay(event.getHitResult());
               ArrayList skipped = new ArrayList();
               EnumSet extraRendered = EnumSet.noneOf(Direction.class);

               for (EnhancedOverlay.Segment segment : EnhancedOverlay.Segment.values()) {
                  Direction spin = EnhancedOverlay.spin(side, segment);
                  if (!wrench.canSetFacing(world, pos, spin, player)) {
                     skipped.add(segment);
                  } else {
                     int color = var31 == spin ? 2130771712 : 2130706687;
                     fills.add(new OverlayRendering.Fill(side, overlay.vertices(segment), color));
                     if (var31 == spin && extraRendered.add(spin)) {
                        if (spin != side.getOpposite()) {
                           if (segment != EnhancedOverlay.Segment.CENTRE) {
                              fills.add(new OverlayRendering.Fill(spin, full, color));
                           }
                        } else {
                           Direction[] edges = switch (side.getAxis()) {
                              case X -> new Direction[]{Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH};
                              case Y -> new Direction[0];
                              case Z -> new Direction[]{Direction.WEST, Direction.EAST};
                              default -> throw new MatchException(null, null);
                           };

                           Direction[] sides = switch (side.getAxis()) {
                              case X -> new Direction[0];
                              case Y -> new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};
                              case Z -> new Direction[]{Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH};
                              default -> throw new MatchException(null, null);
                           };

                           for (Direction face : edges) {
                              for (EnhancedOverlay.Segment part : new EnhancedOverlay.Segment[]{
                                 EnhancedOverlay.Segment.TOP_LEFT,
                                 EnhancedOverlay.Segment.TOP,
                                 EnhancedOverlay.Segment.TOP_RIGHT,
                                 EnhancedOverlay.Segment.BOTTOM_LEFT,
                                 EnhancedOverlay.Segment.BOTTOM,
                                 EnhancedOverlay.Segment.BOTTOM_RIGHT
                              }) {
                                 fills.add(new OverlayRendering.Fill(face, EnhancedOverlay.forFace(face).vertices(part), color));
                              }
                           }

                           for (Direction face : sides) {
                              for (EnhancedOverlay.Segment part : new EnhancedOverlay.Segment[]{
                                 EnhancedOverlay.Segment.TOP_LEFT,
                                 EnhancedOverlay.Segment.LEFT,
                                 EnhancedOverlay.Segment.BOTTOM_LEFT,
                                 EnhancedOverlay.Segment.TOP_RIGHT,
                                 EnhancedOverlay.Segment.RIGHT,
                                 EnhancedOverlay.Segment.BOTTOM_RIGHT
                              }) {
                                 fills.add(new OverlayRendering.Fill(face, EnhancedOverlay.forFace(face).vertices(part), color));
                              }
                           }
                        }
                     }
                  }
               }

               if (var31 == side) {
                  for (EnhancedOverlay.Segment segmentx : skipped) {
                     fills.add(new OverlayRendering.Fill(side, overlay.vertices(segmentx), 2130771712));
                  }
               }
            } else {
               fills.add(new OverlayRendering.Fill(side, overlay.vertices(EnhancedOverlay.Segment.forRayTrace(event.getHitResult())), 2130706432));
            }

            List<OverlayRendering.Fill> geometry = List.copyOf(fills);
            event.addCustomRenderer((outline, collector, poses, level) -> {
               Vec3 camera = level.cameraRenderState.pos;
               poses.pushPose();

               try {
                  poses.translate(pos.getX() - camera.x, pos.getY() - camera.y, pos.getZ() - camera.z);

                  for (OverlayRendering.Fill fill : geometry) {
                     poses.pushPose();

                     try {
                        EnhancedOverlay.transformToFace(poses, fill.face);
                        collector.submitCustomGeometry(poses, RenderTypes.debugQuads(), (pose, buffer) -> {
                           for (Vec3 v : fill.vertices) {
                              buffer.addVertex(pose, (float)v.x, (float)v.y, (float)v.z).setColor(fill.color);
                           }
                        });
                     } finally {
                        poses.popPose();
                     }
                  }

                  poses.pushPose();

                  try {
                     EnhancedOverlay.transformToFace(poses, side);
                     collector.submitCustomGeometry(poses, RenderTypes.linesTranslucentNoDepthWrite(), (pose, buffer) -> {
                        for (float coordinate : new float[]{-0.5F, -0.25F, 0.25F, 0.5F}) {
                           buffer.addVertex(pose, -0.5F, 0.0F, coordinate).setColor(2130706432).setNormal(pose, 1.0F, 0.0F, 0.0F).setLineWidth(2.0F);
                           buffer.addVertex(pose, 0.5F, 0.0F, coordinate).setColor(2130706432).setNormal(pose, 1.0F, 0.0F, 0.0F).setLineWidth(2.0F);
                           buffer.addVertex(pose, coordinate, 0.0F, -0.5F).setColor(2130706432).setNormal(pose, 0.0F, 0.0F, 1.0F).setLineWidth(2.0F);
                           buffer.addVertex(pose, coordinate, 0.0F, 0.5F).setColor(2130706432).setNormal(pose, 0.0F, 0.0F, 1.0F).setLineWidth(2.0F);
                        }
                     });
                  } finally {
                     poses.popPose();
                  }
               } finally {
                  poses.popPose();
               }

               return false;
            });
         }
      }
   }

   private record Fill(Direction face, List<Vec3> vertices, int color) {
   }
}
