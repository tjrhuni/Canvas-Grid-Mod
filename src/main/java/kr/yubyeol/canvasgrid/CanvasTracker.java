package kr.yubyeol.canvasgrid;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.map.MapState;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

/** 플레이어가 작업 중인 이젤 도화지와 시선이 가리키는 픽셀을 찾는다. */
public final class CanvasTracker {

  /** 지도를 든 액자와 그 방향, 그리고 클라이언트가 가진 지도 데이터. */
  public record Canvas(ItemFrameEntity frame, Direction facing, MapState mapState) {}

  private static final double SEAT_RANGE_SQUARED = 3.0 * 3.0;

  private final GridConfig config;

  private Canvas canvas;
  private int[] cursor;

  CanvasTracker(GridConfig config) {
    this.config = config;
  }

  /** 플레이어 앞의 도화지. 근처에 없으면 {@code null}. */
  public Canvas canvas() {
    return canvas;
  }

  /** 앉은 플레이어의 시선이 칠하게 될 픽셀. 도화지를 겨누고 있지 않으면 {@code null}. */
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
    boolean seated = canvas != null && isSeatedAt(player, canvas);
    if (config.seatedOnly && !seated) {
      canvas = null;
    }
    cursor = canvas != null && config.showCursor && seated
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
      // 플레이어가 바라보는 액자를 우선하고, 그중 가장 가까운 것을 고른다.
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

  /** 이젤 좌석(갑옷 거치대)에 타고 있고 그 자리가 도화지 바로 앞인지. */
  private static boolean isSeatedAt(ClientPlayerEntity player, Canvas canvas) {
    Entity vehicle = player.getVehicle();
    return vehicle instanceof ArmorStandEntity
        && vehicle.getPos().squaredDistanceTo(canvas.frame().getPos()) < SEAT_RANGE_SQUARED;
  }

  private static boolean holdsMap(ItemFrameEntity frame) {
    ItemStack stack = frame.getHeldItemStack();
    return !stack.isEmpty() && stack.contains(DataComponentTypes.MAP_ID);
  }
}
