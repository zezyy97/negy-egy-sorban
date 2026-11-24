package connectfour.db;

/**
 * Adatbázis-hiba, nem ellenőrzött (unchecked) kivételbe csomagolva. Így a parancsoknak
 * nem kell mindenhol {@code throws SQLException}-t írniuk, de a hiba nem vész el:
 * az eredeti {@code SQLException} a {@link #getCause()}-ban megmarad.
 */
public class DataAccessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
