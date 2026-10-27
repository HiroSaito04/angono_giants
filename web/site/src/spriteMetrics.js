// Shared by renderer and hit detection so the tappable area tracks the visible giant.
// Keep the original bitmap scaling unchanged.
export const SPRITE_SCALE = 0.68;
export const GIANT_DRAW_SIZE = 90 * SPRITE_SCALE;
// Extra room accommodates finger occlusion and the 15-degree sprite tilt,
// without making giants hittable from far away.
export function giantHitRadius(pointerType = "touch") {
  return GIANT_DRAW_SIZE / 2 + (pointerType === "mouse" ? 8 : 13);
}
