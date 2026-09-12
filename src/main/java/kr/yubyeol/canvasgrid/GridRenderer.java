package kr.yubyeol.canvasgrid;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.block.MapColor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.item.map.MapState;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

/** 이젤 액자 면 위에 모눈, 눈금 숫자, 커서 테두리를 그린다. */
public final class GridRenderer {

  private static final int SIZE = ArtMapCursor.SIZE;
  private static final float CELL = 1f / SIZE;
  private static final int MAP_SIZE = 128;
  private static final int MAP_PIXELS_PER_CANVAS_PIXEL = MAP_SIZE / SIZE;
  /**
   * 바닐라가 액자 엔티티 위치를 기준으로 지도를 그리는 깊이(액자가 바라보는 방향 기준). 렌더러는
   * 벽에서 0.46875 나온 뒤 보이지 않는 액자는 0.5, 보이는 액자는 0.4375 만큼 되돌아가고, 지도는
   * 거기서 1/128 앞에 그린다.
   */
  private static final float INVISIBLE_FRAME_MAP_OFFSET = 0.46875f - 0.5f + 1f / 128f;
  private static final float VISIBLE_FRAME_MAP_OFFSET = 0.46875f - 0.4375f + 1f / 128f;
  private static final float SURFACE_CLEARANCE = 0.001f;
  /** 선 굵기 설정 한 단계당 얇은 선 반폭. */
  private static final float HALF_WIDTH_PER_STEP = 0.00125f;
  private static final int BOLD_EXTRA_ALPHA = 90;
  private static final float HIGHLIGHT_HALF_WIDTH_PER_STEP = 0.002f;
  private static final float LABEL_SCALE = 0.007f;
  private static final float LABEL_GAP = 0.05f;
  private static final int LABEL_COLOR = 0xFF202020;

  private final GridConfig config;
  private final CanvasTracker tracker;

  GridRenderer(GridConfig config, CanvasTracker tracker) {
    this.config = config;
    this.tracker = tracker;
  }

  void render(WorldRenderContext context) {
    CanvasTracker.Canvas canvas = tracker.canvas();
    if (!config.enabled || canvas == null) {
      return;
    }
    MatrixStack matrices = context.matrixStack();
    VertexConsumerProvider consumers = context.consumers();
    if (matrices == null || consumers == null) {
      return;
    }

    Vec3d camera = context.camera().getPos();
    Plane plane = Plane.of(canvas.frame(), canvas.facing(), (float) config.surfaceOffset);

    matrices.push();
    matrices.translate(-camera.x, -camera.y, -camera.z);
    Matrix4f matrix = matrices.peek().getPositionMatrix();

    VertexConsumer quads = consumers.getBuffer(OverlayRenderLayer.QUADS);
    if (config.crispCanvas && canvas.mapState() != null) {
      drawCanvasPixels(quads, matrix, plane, canvas.mapState());
    }
    drawGrid(quads, matrix, plane);
    int[] cursor = tracker.cursor();
    if (cursor != null) {
      drawHighlight(quads, matrix, plane, cursor[0], cursor[1]);
    }
    if (config.showLabels) {
      drawLabels(matrices, consumers, plane);
    }
    matrices.pop();
  }

  /**
   * 클라이언트의 지도 데이터로 32x32 캔버스를 픽셀마다 사각형 하나씩 다시 그린다. 필터링된 지도
   * 텍스처 대신 또렷한 정사각형이 모눈과 정확히 맞물린다.
   */
  private static void drawCanvasPixels(VertexConsumer quads, Matrix4f matrix, Plane plane,
      MapState mapState) {
    byte[] colors = mapState.colors;
    for (int py = 0; py < SIZE; py++) {
      for (int px = 0; px < SIZE; px++) {
        int mapIndex = (px * MAP_PIXELS_PER_CANVAS_PIXEL + 2)
            + (py * MAP_PIXELS_PER_CANVAS_PIXEL + 2) * MAP_SIZE;
        int colorByte = colors[mapIndex] & 0xFF;
        if (colorByte == 0) {
          continue;
        }
        int rgb = MapColor.getRenderColor(colorByte) & 0xFFFFFF;
        float left = -0.5f + px * CELL;
        float top = 0.5f - py * CELL;
        fillQuad(quads, matrix, plane, left, top - CELL, left + CELL, top, 0f, rgb, 255);
      }
    }
  }

  private void drawGrid(VertexConsumer quads, Matrix4f matrix, Plane plane) {
    int color = config.gridColor.rgb;
    int thinAlpha = config.lineOpacity * 255 / 100;
    int boldAlpha = Math.min(255, thinAlpha + BOLD_EXTRA_ALPHA);
    float thinHalfWidth = HALF_WIDTH_PER_STEP * config.lineWidth;
    for (int i = 0; i <= SIZE; i++) {
      boolean bold = i == 0 || i == SIZE || (config.boldLines && i % config.boldEvery == 0);
      int alpha = bold ? boldAlpha : thinAlpha;
      float halfWidth = bold ? thinHalfWidth * 2 : thinHalfWidth;
      float offset = -0.5f + i * CELL;
      // 선은 픽셀 경계에 중심을 맞추되, 바깥 테두리 두 줄은 캔버스 안쪽으로만 그린다.
      float near = i == 0 ? offset : offset - halfWidth;
      float far = i == SIZE ? offset : offset + halfWidth;
      fillQuad(quads, matrix, plane, near, -0.5f, far, 0.5f, 0f, color, alpha);
      fillQuad(quads, matrix, plane, -0.5f, near, 0.5f, far, 0f, color, alpha);
    }
  }

  private void drawHighlight(VertexConsumer quads, Matrix4f matrix, Plane plane, int px,
      int py) {
    float left = -0.5f + px * CELL;
    float right = left + CELL;
    float top = 0.5f - py * CELL;
    float bottom = top - CELL;
    float w = HIGHLIGHT_HALF_WIDTH_PER_STEP * config.pointerWidth;
    float lift = 0.002f;
    int color = config.highlightColor.rgb;
    fillQuad(quads, matrix, plane, left - w, top - w, right + w, top + w, lift, color, 255);
    fillQuad(quads, matrix, plane, left - w, bottom - w, right + w, bottom + w, lift, color, 255);
    fillQuad(quads, matrix, plane, left - w, bottom, left + w, top, lift, color, 255);
    fillQuad(quads, matrix, plane, right - w, bottom, right + w, top, lift, color, 255);
  }

  /**
   * 평면 좌표 {@code (x0, y0)}~{@code (x1, y1)} 사각형을 채운다. x 는 보는 사람의 오른쪽, y 는
   * 위쪽이며 액자 한 변이 -0.5~0.5 다.
   */
  private static void fillQuad(VertexConsumer quads, Matrix4f matrix, Plane plane, float x0,
      float y0, float x1, float y1, float lift, int rgb, int alpha) {
    Vec3d a = plane.point(x0, y0, lift);
    Vec3d b = plane.point(x1, y0, lift);
    Vec3d c = plane.point(x1, y1, lift);
    Vec3d d = plane.point(x0, y1, lift);
    int r = (rgb >> 16) & 0xFF;
    int g = (rgb >> 8) & 0xFF;
    int bl = rgb & 0xFF;
    for (Vec3d v : new Vec3d[] {a, b, c, d, d, c, b, a}) {
      quads.vertex(matrix, (float) v.x, (float) v.y, (float) v.z).color(r, g, bl, alpha);
    }
  }

  private void drawLabels(MatrixStack matrices, VertexConsumerProvider consumers, Plane plane) {
    TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
    float textYaw = viewYawTowards(plane.facing);
    for (int i = config.boldEvery; i < SIZE; i += config.boldEvery) {
      String label = Integer.toString(i);
      // 픽셀 번호는 왼쪽 위가 1 이므로 열은 오른쪽으로, 행은 아래로 커진다.
      float column = -0.5f + i * CELL;
      float row = 0.5f - i * CELL;
      drawLabel(matrices, consumers, textRenderer, textYaw, label,
          plane.point(column, 0.5f + LABEL_GAP, 0f));
      drawLabel(matrices, consumers, textRenderer, textYaw, label,
          plane.point(-0.5f - LABEL_GAP, row, 0f));
    }
  }

  /** 액자 앞에 서서 액자를 바라보는 플레이어의 yaw. */
  private static float viewYawTowards(Direction facing) {
    return switch (facing) {
      case NORTH -> 0f;
      case EAST -> 90f;
      case SOUTH -> 180f;
      default -> 270f;
    };
  }

  private static void drawLabel(MatrixStack matrices, VertexConsumerProvider consumers,
      TextRenderer textRenderer, float yaw, String text, Vec3d position) {
    matrices.push();
    matrices.translate(position.x, position.y, position.z);
    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-yaw));
    matrices.scale(-LABEL_SCALE, -LABEL_SCALE, LABEL_SCALE);
    float x = -textRenderer.getWidth(text) / 2f;
    float y = -textRenderer.fontHeight / 2f;
    textRenderer.draw(text, x, y, LABEL_COLOR, false, matrices.peek().getPositionMatrix(),
        consumers, TextRenderer.TextLayerType.NORMAL, 0,
        LightmapTextureManager.MAX_LIGHT_COORDINATE);
    matrices.pop();
  }

  /** 액자 속 지도의 평면. 원점과 오른쪽·위·바깥 단위 벡터로 이루어진다. */
  private record Plane(Vec3d origin, Vec3d right, Vec3d up, Vec3d out, Direction facing) {

    static Plane of(ItemFrameEntity frame, Direction facing, float extraOffset) {
      Vec3d out = Vec3d.of(facing.getVector());
      Vec3d right = Vec3d.of(facing.rotateYCounterclockwise().getVector());
      Vec3d up = new Vec3d(0, 1, 0);
      float mapOffset =
          frame.isInvisible() ? INVISIBLE_FRAME_MAP_OFFSET : VISIBLE_FRAME_MAP_OFFSET;
      Vec3d origin =
          frame.getPos().add(out.multiply(mapOffset + SURFACE_CLEARANCE + extraOffset));
      return new Plane(origin, right, up, out, facing);
    }

    Vec3d point(float x, float y, float lift) {
      return origin.add(right.multiply(x)).add(up.multiply(y)).add(out.multiply(lift));
    }
  }
}
