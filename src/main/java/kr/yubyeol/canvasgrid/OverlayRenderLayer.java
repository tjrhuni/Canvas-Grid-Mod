package kr.yubyeol.canvasgrid;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;

/**
 * Alpha-blended, untextured quads drawn without a depth test, so the overlay always shows on top
 * of the map even though it lies in the same plane. The layer is not flagged translucent: that
 * keeps the quads in submission order (pixels, then grid, then highlight) instead of sorting them
 * by distance, which would let coplanar quads overwrite each other at random.
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
