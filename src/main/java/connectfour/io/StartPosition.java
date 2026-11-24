package connectfour.io;

import connectfour.model.Board;
import connectfour.model.Disc;

/**
 * A külső fájlból beolvasott kezdőállás.
 *
 * @param board     a beolvasott tábla
 * @param humanDisc a játékos színe, vagy {@code null}, ha a fájl nem adja meg
 */
public record StartPosition(Board board, Disc humanDisc) {
}
