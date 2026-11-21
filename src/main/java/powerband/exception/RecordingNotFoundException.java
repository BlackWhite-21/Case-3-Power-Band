package powerband.exception;

/**
 * La búsqueda binaria no encontró el nombre en recordings.index.
 */
public class RecordingNotFoundException extends RecordingException {

    private static final long serialVersionUID = 1L;

    public RecordingNotFoundException(String pMessage) {
        super(pMessage);
    }

    public RecordingNotFoundException(String pMessage, Throwable pCause) {
        super(pMessage, pCause);
    }
}
