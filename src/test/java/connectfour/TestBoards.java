package connectfour;

import connectfour.db.DbInit;
import connectfour.model.Board;

import java.sql.SQLException;
import java.util.UUID;

/**
 * Segédmetódusok a tesztekhez: táblák "rajzolása" szövegből, és saját adatbázis
 * minden tesztnek (hogy a tesztek ne zavarják egymást).
 */
public final class TestBoards {

    private TestBoards() {
    }

    /**
     * Tábla soronként megadva, felülről lefelé. Példa:
     * <pre>
     * TestBoards.of(
     *     ".......",
     *     "...R...",
     *     "..YRY..");
     * </pre>
     */
    public static Board of(String... rows) {
        return Board.fromDescription(rows.length, rows[0].length(), String.join("", rows));
    }

    /** Új, üres (a szkripttel feltöltött) memóriabeli adatbázis egyedi névvel. */
    public static String freshDatabase() {
        String url = "jdbc:h2:mem:test-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1";
        try {
            DbInit.initDb(url);
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
        return url;
    }
}
