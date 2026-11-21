package powerband.exception;

/**
 * Falla al grabar, indexar, buscar o reproducir una música.
 */
public class RecordingException extends PowerBandException {

    private static final long serialVersionUID = 1L;

    public RecordingException(String pMessage) {
        super(pMessage);
    }

    public RecordingException(String pMessage, Throwable pCause) {
        super(pMessage, pCause);
    }
}
