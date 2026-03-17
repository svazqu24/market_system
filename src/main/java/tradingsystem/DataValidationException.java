package tradingsystem;

/** Thrown when domain object validation fails (invalid user ID, bad symbol, etc.). */
public class DataValidationException extends Exception {

    public DataValidationException(String message) {
        super(message);
    }
}
