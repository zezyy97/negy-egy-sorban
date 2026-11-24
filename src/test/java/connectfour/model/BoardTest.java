package connectfour.model;

import connectfour.TestBoards;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BoardTest {

    @Test
    @DisplayName("Az új tábla üres, és a megadott méretű")
    void newBoardIsEmpty() {
        Board board = new Board(5, 6);
        assertEquals(5, board.getRows());
        assertEquals(6, board.getCols());
        assertEquals(0, board.count(Disc.RED));
        assertEquals(0, board.count(Disc.YELLOW));
        assertFalse(board.isFull());
    }

    @Test
    @DisplayName("A szabványos tábla 6×7-es")
    void standardBoardIsSixBySeven() {
        Board board = Board.standard();
        assertEquals(6, board.getRows());
        assertEquals(7, board.getCols());
    }

    @Test
    @DisplayName("A bedobott korong a legalsó sorba esik")
    void dropLandsOnBottomRow() {
        Board board = Board.standard().drop(3, Disc.RED);
        assertEquals(Disc.RED, board.getDisc(5, 3));
        assertNull(board.getDisc(4, 3));
    }

    @Test
    @DisplayName("A korongok egymásra rakódnak")
    void dropsStackUpwards() {
        Board board = Board.standard().drop(0, Disc.RED).drop(0, Disc.YELLOW).drop(0, Disc.RED);
        assertEquals(Disc.RED, board.getDisc(5, 0));
        assertEquals(Disc.YELLOW, board.getDisc(4, 0));
        assertEquals(Disc.RED, board.getDisc(3, 0));
        assertEquals(2, board.landingRow(0));
        assertEquals(3, board.topDiscRow(0));
    }

    @Test
    @DisplayName("A drop nem változtatja meg az eredeti táblát (immutable)")
    void dropDoesNotModifyOriginal() {
        Board original = Board.standard();
        Board after = original.drop(2, Disc.YELLOW);
        assertNull(original.getDisc(5, 2));
        assertEquals(Disc.YELLOW, after.getDisc(5, 2));
        assertNotEquals(original, after);
    }

    @Test
    @DisplayName("Teli oszlopba nem lehet dobni")
    void dropIntoFullColumnThrows() {
        Board board = new Board(4, 4);
        for (int i = 0; i < 4; i++) {
            board = board.drop(1, i % 2 == 0 ? Disc.RED : Disc.YELLOW);
        }
        assertTrue(board.isColumnFull(1));
        assertEquals(-1, board.landingRow(1));
        Board full = board;
        assertThrows(IllegalArgumentException.class, () -> full.drop(1, Disc.RED));
    }

    @Test
    @DisplayName("Nem létező oszlopba nem lehet dobni")
    void dropIntoMissingColumnThrows() {
        Board board = Board.standard();
        assertThrows(IllegalArgumentException.class, () -> board.drop(7, Disc.RED));
        assertThrows(IllegalArgumentException.class, () -> board.drop(-1, Disc.RED));
    }

    @Test
    @DisplayName("A removeTop a legfelső korongot veszi ki")
    void removeTopTakesUppermostDisc() {
        Board board = Board.standard().drop(4, Disc.RED).drop(4, Disc.YELLOW);
        Board after = board.removeTop(4);
        assertEquals(Disc.RED, after.getDisc(5, 4));
        assertNull(after.getDisc(4, 4));
        assertEquals(Disc.YELLOW, board.getDisc(4, 4), "az eredeti tábla nem változhat");
    }

    @Test
    @DisplayName("Üres oszlopból nem lehet korongot kivenni")
    void removeTopFromEmptyColumnThrows() {
        assertThrows(IllegalArgumentException.class, () -> Board.standard().removeTop(0));
    }

    @Test
    @DisplayName("Túl kicsi vagy túl nagy tábla nem hozható létre")
    void invalidSizesAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new Board(3, 7));
        assertThrows(IllegalArgumentException.class, () -> new Board(6, 11));
    }

    @Test
    @DisplayName("Mentési szöveg → tábla → mentési szöveg ugyanazt adja")
    void descriptionRoundTrip() {
        Board board = TestBoards.of(
                "....",
                "....",
                ".Y..",
                "RRY.");
        assertEquals("........" + ".Y.." + "RRY.", board.toDescription());
        assertEquals(board, Board.fromDescription(4, 4, board.toDescription()));
    }

    @Test
    @DisplayName("Lebegő korongot tartalmazó leírás elutasítva")
    void floatingDiscIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> TestBoards.of(
                "....",
                ".R..",
                "....",
                "...."));
    }

    @Test
    @DisplayName("Rossz hosszú vagy ismeretlen karaktert tartalmazó leírás elutasítva")
    void badDescriptionIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> Board.fromDescription(4, 4, "...."));
        assertThrows(IllegalArgumentException.class, () -> Board.fromDescription(4, 4, "X".repeat(16)));
    }

    @Test
    @DisplayName("A kirajzolás zárójellel emeli ki a megadott mezőket")
    void renderHighlightsPositions() {
        Board board = Board.standard().drop(0, Disc.RED);
        String plain = board.toString();
        String marked = board.render(Set.of(new Position(5, 0)));
        assertTrue(plain.contains("| R "));
        assertTrue(marked.contains("|(R)"));
        assertTrue(plain.startsWith("  1  2  3  4  5  6  7"));
    }

    @Test
    @DisplayName("Két azonos tartalmú tábla egyenlő, és a hash-kódjuk is egyezik")
    void equalsAndHashCode() {
        Board a = Board.standard().drop(3, Disc.RED);
        Board b = Board.standard().drop(3, Disc.RED);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    @DisplayName("A tábla teli, ha minden oszlop teli")
    void isFullWhenEveryColumnFull() {
        Board board = TestBoards.of(
                "RYRY",
                "RYRY",
                "YRYR",
                "YRYR");
        assertTrue(board.isFull());
        assertEquals(8, board.count(Disc.RED));
    }
}
