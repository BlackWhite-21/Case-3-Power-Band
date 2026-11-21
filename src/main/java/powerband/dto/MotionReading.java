package powerband.dto;

/**
 * Lectura de la cámara: cuánto se movió la imagen (0..1) y dónde (centro del movimiento, 0..1).
 */
public record MotionReading(float amount, float x, float y) {
}
