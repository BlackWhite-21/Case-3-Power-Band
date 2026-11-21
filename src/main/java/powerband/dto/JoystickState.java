package powerband.dto;

/**
 * Lectura del joystick: 3 ejes normalizados 0..1 y los botones como máscara de bits
 * (bit 0 = botón 1 presionado, bit 1 = botón 2, ...).
 */
public record JoystickState(float x, float y, float z, int buttons) {

    public boolean isPressed(int pButtonIndex) {
        return (buttons & (1 << pButtonIndex)) != 0;
    }
}
