package powerband.exception;

/**
 * Falla al abrir o leer un dispositivo físico (joystick, micrófono o tableta).
 */
public class DeviceException extends PowerBandException {

    private static final long serialVersionUID = 1L;

    public DeviceException(String pMessage) {
        super(pMessage);
    }

    public DeviceException(String pMessage, Throwable pCause) {
        super(pMessage, pCause);
    }
}
