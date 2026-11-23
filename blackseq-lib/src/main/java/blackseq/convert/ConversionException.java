package blackseq.convert;

/** The conversion cannot continue: a clip is too long for the policy, or a plan has a conflict. */
public class ConversionException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public ConversionException(String message) {
        super(message);
    }
}
