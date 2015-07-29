package framework.patcher;

public class PatcherException extends Exception {
    private static final long serialVersionUID = 5763139899274286757L;

    public PatcherException() {
    }

    public PatcherException(String message) {
        super(message);
    }

    public PatcherException(Throwable cause) {
        super(cause);
    }

    public PatcherException(String message, Throwable cause) {
        super(message, cause);
    }

    public PatcherException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
