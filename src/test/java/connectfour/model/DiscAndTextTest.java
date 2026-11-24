package connectfour.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiscAndTextTest {

    @Test
    @DisplayName("A piros ellenfele a sárga és fordítva")
    void opponent() {
        assertEquals(Disc.YELLOW, Disc.RED.opponent());
        assertEquals(Disc.RED, Disc.YELLOW.opponent());
    }

    @Test
    @DisplayName("Angol és magyar betűjel is elfogadott, kis- és nagybetűvel")
    void fromLetter() {
        assertEquals(Disc.RED, Disc.fromLetter('R'));
        assertEquals(Disc.RED, Disc.fromLetter('p'));
        assertEquals(Disc.YELLOW, Disc.fromLetter('y'));
        assertEquals(Disc.YELLOW, Disc.fromLetter('S'));
        assertNull(Disc.fromLetter('.'));
    }

    @Test
    @DisplayName("A lépéshez kötelező a szín")
    void moveNeedsDisc() {
        assertThrows(IllegalArgumentException.class, () -> new Move(null, 3));
        assertEquals("Piros → 4. oszlop", new Move(Disc.RED, 3).toString());
    }

    @Test
    @DisplayName("A GameResult megmondja a győztest")
    void gameResultWinner() {
        assertEquals(Disc.RED, GameResult.RED_WON.winner());
        assertEquals(Disc.YELLOW, GameResult.wonBy(Disc.YELLOW).winner());
        assertNull(GameResult.DRAW.winner());
        assertTrue(GameResult.DRAW.isOver());
        assertFalse(GameResult.IN_PROGRESS.isOver());
    }

    @Test
    @DisplayName("Helyes névelő az oszlopszám előtt: az 1., a 4., az 5., a 10.")
    void hungarianArticle() {
        assertEquals("az 1. oszlop", HungarianText.column(1));
        assertEquals("a 4. oszlop", HungarianText.column(4));
        assertEquals("az 5. oszlop", HungarianText.column(5));
        assertEquals("a 10. oszlop", HungarianText.column(10));
        assertEquals("Az 5. oszlop", HungarianText.columnCapitalized(5));
    }

    @Test
    @DisplayName("Position: lépés irányba és tábla határainak vizsgálata")
    void positionStepAndInside() {
        Position p = new Position(2, 2);
        assertEquals(new Position(1, 3), p.step(LineDirection.DIAGONAL_UP));
        assertTrue(p.isInside(3, 3));
        assertFalse(new Position(3, 0).isInside(3, 3));
        assertFalse(new Position(0, -1).isInside(3, 3));
    }
}
