package connectfour.db;

import org.h2.tools.RunScript;
import org.h2.tools.Server;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Az adatbázis előkészítése: létrehozza a memóriában futó H2 adatbázist a
 * {@code createDbScript.sql} szkriptből, és igény szerint elindítja a H2 webes konzolt,
 * ahol böngészőből is meg lehet nézni a táblát (http://localhost:8082).
 * <p>
 * A {@code DB_CLOSE_DELAY=-1} miatt az adatbázis addig él, amíg a program fut,
 * akkor is, ha épp nincs nyitott kapcsolat.
 */
public final class DbInit {

    /** A program által használt adatbázis címe. */
    public static final String DEFAULT_URL = "jdbc:h2:mem:connectfour;DB_CLOSE_DELAY=-1";
    public static final String USER = "sa";
    public static final String PASSWORD = "";

    private static final String SCRIPT = "/createDbScript.sql";

    private static Server webServer;

    private DbInit() {
    }

    /**
     * Lefuttatja a létrehozó szkriptet a megadott adatbázison.
     * A tesztek más-más URL-t adnak meg, hogy ne zavarják egymást.
     */
    public static void initDb(String jdbcUrl) throws SQLException {
        try (Connection conn = DriverManager.getConnection(jdbcUrl, USER, PASSWORD);
             InputStream is = DbInit.class.getResourceAsStream(SCRIPT)) {
            if (is == null) {
                throw new IllegalStateException("Nem található az adatbázis-szkript: " + SCRIPT);
            }
            RunScript.execute(conn, new InputStreamReader(is, StandardCharsets.UTF_8));
        } catch (java.io.IOException e) {
            throw new SQLException("Nem sikerült beolvasni az adatbázis-szkriptet.", e);
        }
    }

    /**
     * Elindítja a H2 webes konzolt. Ha nem sikerül (pl. foglalt a port), a játék
     * ettől még működik, csak figyelmeztetést írunk ki.
     */
    public static void startWebConsole(int port) {
        try {
            webServer = Server.createWebServer("-web", "-webPort", String.valueOf(port)).start();
            System.out.println("Az adatbázis böngészőből megnézhető: http://localhost:" + port
                    + "  (JDBC URL: " + DEFAULT_URL + ", felhasználó: sa, jelszó nincs)");
        } catch (SQLException e) {
            System.out.println("Figyelem: a H2 webkonzol nem indult el (" + e.getMessage()
                    + "). A játék enélkül is működik.");
        }
    }

    public static void stopWebConsole() {
        if (webServer != null) {
            webServer.stop();
            webServer = null;
        }
    }
}
