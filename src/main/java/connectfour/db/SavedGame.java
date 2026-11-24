package connectfour.db;

import connectfour.model.Board;
import connectfour.model.Disc;

import java.time.LocalDateTime;

/**
 * Egy adatbázisból beolvasott mentés.
 *
 * @param id        az adatbázis-azonosító (LOAD &lt;id&gt;)
 * @param name      a mentés neve
 * @param board     a visszaállított tábla
 * @param humanDisc a játékos színe mentéskor, vagy {@code null}
 * @param savedAt   a mentés időpontja
 */
public record SavedGame(int id, String name, Board board, Disc humanDisc, LocalDateTime savedAt) {
}
