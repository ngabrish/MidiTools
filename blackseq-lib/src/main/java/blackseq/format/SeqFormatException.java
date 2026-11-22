package blackseq.format;

/** The bytes are not a .SEQ file that this library knows. */
public class SeqFormatException extends Exception {
    private static final long serialVersionUID = 1L;

    public SeqFormatException(String message) {
        super(message);
    }
}
