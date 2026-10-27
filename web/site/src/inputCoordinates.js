// Convert viewport/pointer coordinates into the canvas backing-store space.
// The mobile browser stage is CSS-scaled and can be taller/shorter than 800.
export function canvasPoint(clientX, clientY, rect, canvasWidth, canvasHeight) {
  if (!rect || rect.width <= 0 || rect.height <= 0) return null;
  return {
    x: ((clientX - rect.left) * canvasWidth) / rect.width,
    y: ((clientY - rect.top) * canvasHeight) / rect.height,
  };
}
