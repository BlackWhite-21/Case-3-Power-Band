package powerband.exception;

/**
 * Ya existe una grabación con ese nombre.
 */
public class DuplicateNameException extends RecordingException {

    private static final long serialVersionUID = 1L;

    public DuplicateNameException(String pMessage) {
        super(pMessage);
    }

    public DuplicateNameException(String pMessage, Throwable pCause) {
        super(pMessage, pCause);
    }
}
