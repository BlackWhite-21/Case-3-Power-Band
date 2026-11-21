package powerband.exception;

/**
 * Falla al comunicarse con SuperCollider (OSC/UDP).
 */
public class SynthException extends PowerBandException {

    private static final long serialVersionUID = 1L;

    public SynthException(String pMessage) {
        super(pMessage);
    }

    public SynthException(String pMessage, Throwable pCause) {
        super(pMessage, pCause);
    }
}
