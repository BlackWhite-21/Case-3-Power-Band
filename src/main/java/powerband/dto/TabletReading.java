package powerband.dto;

/**
 * Huion tablet coordinates and pressure, normalized to the inclusive range 0..1.
 * A pressure of zero means that the pen is not touching the tablet.
 */
public record TabletReading(float x, float y, float pressure) {
}
