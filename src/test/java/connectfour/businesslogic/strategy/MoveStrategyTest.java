package connectfour.businesslogic.strategy;

import connectfour.TestBoards;
import connectfour.businesslogic.BoardService;
import connectfour.model.Board;
import connectfour.model.Disc;
import connectfour.model.Move;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MoveStrategyTest {

    private final SmartMoveStrategy smart = new SmartMoveStrategy(new Random(42));
    private final RandomMoveStrategy random = new RandomMoveStrategy(new Random(42));

    // Okos strat

    @Test
    @DisplayName("Okos: ha nyerni tud, nyer")
    void smartTakesWinningMove() {
        Board board = TestBoards.of(
                ".......",
                ".......",
                ".......",
                ".......",
                "YYY....",
                "RRR....");
        assertEquals(3, smart.chooseColumn(board, Disc.RED));
    }

    @Test
    @DisplayName("Okos: blokkolja az ellenfél nyerő lépését")
    void smartBlocksOpponent() {
        Board board = TestBoards.of(
                ".......",
                ".......",
                ".......",
                "...Y...",
                "...Y...",
                "RR.Y..R");
        assertEquals(3, smart.chooseColumn(board, Disc.RED));
    }

    @Test
    @DisplayName("Okos: a saját győzelem fontosabb, mint a blokkolás")
    void smartPrefersWinOverBlock() {
        Board board = TestBoards.of(
                ".......",
                ".......",
                ".......",
                "......Y",
                "......Y",
                "RRR...Y");
        // a piros a 4. oszloppal (index 3) nyer, a sárga a 7. oszloppal (index 6) nyerne
        assertEquals(3, smart.chooseColumn(board, Disc.RED));
    }

    @Test
    @DisplayName("Okos: nem dob az ellenfél nyerő mezője alá")
    void smartDoesNotSetUpOpponentWin() {
        // A sárga a 2. sorban (alulról) hármast épített: ha a piros a 4. oszlopba (index 3) dobna,
        // a korongja fölé a sárga beférne, és nyerne. A középre húzás ezt választaná – az okos nem.
        Board board = TestBoards.of(
                ".......",
                ".......",
                ".......",
                ".......",
                "YYY....",
                "RRY.R.R");
        int chosen = smart.chooseColumn(board, Disc.RED);
        assertFalse(chosen == 3, "a 4. oszlop ajándék lenne a sárgának");
        assertTrue(Set.of(2, 4).contains(chosen), "a középhez legközelebbi biztonságos oszlopot várjuk");
    }

    @Test
    @DisplayName("Okos: üres táblán középre dob")
    void smartPrefersCenterOnEmptyBoard() {
        assertEquals(3, smart.chooseColumn(Board.standard(), Disc.RED));
    }

    // Véletlen strat

    @Test
    @DisplayName("Véletlen: mindig nem teli oszlopot választ")
    void randomAlwaysPicksFreeColumn() {
        Board board = TestBoards.of(
                "R.YR",
                "Y.RY",
                "R.YR",
                "Y.RY");
        for (int i = 0; i < 50; i++) {
            assertEquals(1, random.chooseColumn(board, Disc.RED));
        }
    }

    @Test
    @DisplayName("Véletlen: rögzített seed-del megismételhető")
    void randomIsReproducibleWithSeed() {
        RandomMoveStrategy a = new RandomMoveStrategy(new Random(7));
        RandomMoveStrategy b = new RandomMoveStrategy(new Random(7));
        for (int i = 0; i < 20; i++) {
            assertEquals(a.chooseColumn(Board.standard(), Disc.RED), b.chooseColumn(Board.standard(), Disc.RED));
        }
    }

    // Közös

    @Test
    @DisplayName("Teli táblán egyik stratégia sem tud lépni")
    void fullBoardThrows() {
        Board full = TestBoards.of(
                "RYRY",
                "RYRY",
                "YRYR",
                "YRYR");
        assertThrows(IllegalStateException.class, () -> smart.chooseColumn(full, Disc.RED));
        assertThrows(IllegalStateException.class, () -> random.chooseColumn(full, Disc.RED));
    }

    @Test
    @DisplayName("Okos a véletlen ellen: 20 játszmából legalább 18-at megnyer")
    void smartBeatsRandom() {
        int smartWins = 0;
        for (int game = 0; game < 20; game++) {
            Disc smartDisc = game % 2 == 0 ? Disc.RED : Disc.YELLOW;   // felváltva kezd
            SmartMoveStrategy s = new SmartMoveStrategy(new Random(game));
            RandomMoveStrategy r = new RandomMoveStrategy(new Random(1000 + game));
            BoardService bs = new BoardService(Board.standard());
            while (!bs.evaluate().isOver()) {
                Disc next = bs.nextToMove();
                MoveStrategy who = next == smartDisc ? s : r;
                bs.doMove(new Move(next, who.chooseColumn(bs.getBoard(), next)));
            }
            if (bs.evaluate().winner() == smartDisc) smartWins++;
        }
        assertTrue(smartWins >= 18, "az okos stratégia csak " + smartWins + " játszmát nyert");
    }
}
