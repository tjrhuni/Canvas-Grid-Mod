package kr.yubyeol.canvasgrid;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.map.MapState;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

/** Finds the easel canvas the player is working on and the pixel their view points at. */
public final class CanvasTracker {

  /** An item frame holding a filled map, with the frame's facing and the map's client-side data. */
  public record Canvas(ItemFrameEntity frame, Direction facing, MapState mapState) {}

  private final GridConfig config;

  private Canvas canvas;
  private int[] cursor;

  CanvasTracker(GridConfig config) {
    this.config = config;
  }

  /** The canvas in front of the player, or {@code null} when there is none nearby. */
  public Canvas canvas() {
    return canvas;
  }

  /** The pixel the seated player's view would paint, or {@code null} when not aiming at one. */
  public int[] cursor() {
    return cursor;
  }

  void update(MinecraftClient client) {
    ClientPlayerEntity player = client.player;
    if (!config.enabled || player == null || client.world == null) {
      canvas = null;
      cursor = null;
      return;
    }
    canvas = findCanvas(client, player);
    cursor = canvas != null && config.showCursor && player.hasVehicle()
        ? ArtMapCursor.pixelAt(player.getYaw(), player.getPitch(),
            ArtMapCursor.yawOffset(canvas.facing()))
        : null;
  }

  private Canvas findCanvas(MinecraftClient client, ClientPlayerEntity player) {
    Vec3d eye = player.getEyePos();
    Vec3d look = player.getRotationVector();
    double radiusSquared = config.searchRadius * config.searchRadius;
    Canvas best = null;
    double bestScore = Double.MAX_VALUE;
    for (Entity entity : client.world.getEntities()) {
      if (!(entity instanceof ItemFrameEntity frame) || !holdsMap(frame)) {
        continue;
      }
      Vec3d offset = frame.getPos().subtract(eye);
      double distanceSquared = offset.lengthSquared();
      if (distanceSquared > radiusSquared) {
        continue;
      }
      // Prefer the frame the player is facing; among those, the nearest one.
      double alignment = offset.normalize().dotProduct(look);
      double score = alignment > 0.7 ? distanceSquared : 1000 + (1 - alignment);
      if (score < bestScore) {
        bestScore = score;
        MapIdComponent mapId = frame.getHeldItemStack().get(DataComponentTypes.MAP_ID);
        best = new Canvas(frame, frame.getHorizontalFacing(), client.world.getMapState(mapId));
      }
    }
    return best;
  }

  private static boolean holdsMap(ItemFrameEntity frame) {
    ItemStack stack = frame.getHeldItemStack();
    return !stack.isEmpty() && stack.contains(DataComponentTypes.MAP_ID);
  }
}
