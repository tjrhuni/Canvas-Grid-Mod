package kr.yubyeol.canvasgrid;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;

/**
 * 깊이 검사 없이 알파 블렌딩으로 그리는 무텍스처 사각형 레이어. 지도와 같은 평면에 있어도 항상
 * 지도 위에 보이게 한다. 반투명 레이어로 등록하지 않는 이유는 제출 순서(픽셀 → 모눈 → 테두리)를
 * 유지하기 위해서다. 거리순 정렬이 켜지면 같은 평면의 사각형들이 무작위로 서로를 덮어 버린다.
 */
final class OverlayRenderLayer extends RenderLayer {

  static final RenderLayer QUADS = RenderLayer.of(
      CanvasGridMod.MOD_ID + "_overlay",
      VertexFormats.POSITION_COLOR,
      VertexFormat.DrawMode.QUADS,
      4096,
      false,
      false,
      MultiPhaseParameters.builder()
          .program(POSITION_COLOR_PROGRAM)
          .transparency(TRANSLUCENT_TRANSPARENCY)
          .depthTest(ALWAYS_DEPTH_TEST)
          .writeMaskState(COLOR_MASK)
          .cull(DISABLE_CULLING)
          .build(false));

  private OverlayRenderLayer(String name, VertexFormat format, VertexFormat.DrawMode mode,
      int size, boolean crumbling, boolean translucent, Runnable start, Runnable end) {
    super(name, format, mode, size, crumbling, translucent, start, end);
  }
}
