package framework.services.plugins;

/**
 * The default exception for dashboards
 * @author Pierre-Yves Cloux
 */
public class DashboardException extends Exception {
    private static final long serialVersionUID = 1L;

    public DashboardException() {
    }

    public DashboardException(String message) {
        super(message);
    }

    public DashboardException(Throwable cause) {
        super(cause);
    }

    public DashboardException(String message, Throwable cause) {
        super(message, cause);
    }

    public DashboardException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
