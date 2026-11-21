package powerband.exception;

/**
 * Raíz de todas las excepciones propias de Power Band (checked).
 */
public class PowerBandException extends Exception {

    private static final long serialVersionUID = 1L;

    public PowerBandException(String pMessage) {
        super(pMessage);
    }

    public PowerBandException(String pMessage, Throwable pCause) {
        super(pMessage, pCause);
    }
}
