package powerband.exception;

/**
 * El nombre de la grabación no cumple las reglas.
 */
public class InvalidNameException extends RecordingException {

    private static final long serialVersionUID = 1L;

    public InvalidNameException(String pMessage) {
        super(pMessage);
    }

    public InvalidNameException(String pMessage, Throwable pCause) {
        super(pMessage, pCause);
    }
}
