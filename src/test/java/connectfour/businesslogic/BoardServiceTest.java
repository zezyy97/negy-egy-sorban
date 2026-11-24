package connectfour.businesslogic;

import connectfour.TestBoards;
import connectfour.model.Board;
import connectfour.model.Disc;
import connectfour.model.GameResult;
import connectfour.model.Move;
import connectfour.model.Position;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BoardServiceTest {

    // Ki jön?

    @Test
    @DisplayName("Üres táblán a piros kezd, utána felváltva jönnek")
    void redStartsThenAlternates() {
        BoardService bs = new BoardService(Board.standard());
        assertEquals(Disc.RED, bs.nextToMove());
        bs.doMove(new Move(Disc.RED, 3));
        assertEquals(Disc.YELLOW, bs.nextToMove());
        bs.doMove(new Move(Disc.YELLOW, 3));
        assertEquals(Disc.RED, bs.nextToMove());
    }

    @Test
    @DisplayName("A doMove új táblát állít be, a régi nem változik")
    void doMoveReplacesBoard() {
        Board before = Board.standard();
        BoardService bs = new BoardService(before);
        bs.doMove(new Move(Disc.RED, 0));
        assertEquals(Disc.RED, bs.getBoard().getDisc(5, 0));
        assertEquals(0, before.count(Disc.RED));
    }

    @Test
    @DisplayName("Nem a soron következő szín nem léphet")
    void wrongColorCannotMove() {
        BoardService bs = new BoardService(Board.standard());
        assertThrows(IllegalStateException.class, () -> bs.doMove(new Move(Disc.YELLOW, 0)));
        assertFalse(bs.isLegal(new Move(Disc.YELLOW, 0)));
        assertTrue(bs.isLegal(new Move(Disc.RED, 0)));
    }

    @Test
    @DisplayName("Teli oszlop nem szabályos lépés")
    void fullColumnIsNotLegal() {
        BoardService bs = new BoardService(TestBoards.of(
                "R...",
                "Y...",
                "R...",
                "Y..."));
        assertEquals(List.of(1, 2, 3), bs.legalColumns());
        assertThrows(IllegalArgumentException.class, () -> bs.doMove(new Move(Disc.RED, 0)));
    }

    @Test
    @DisplayName("A korongok aránya: egyenlő vagy eggyel több piros a helyes")
    void discCountConsistency() {
        assertTrue(new BoardService(TestBoards.of("....", "....", "....", "RY..")).isDiscCountConsistent());
        assertTrue(new BoardService(TestBoards.of("....", "....", "....", "RYR.")).isDiscCountConsistent());
        assertFalse(new BoardService(TestBoards.of("....", "....", "....", "RRR.")).isDiscCountConsistent());
        assertFalse(new BoardService(TestBoards.of("....", "....", "....", "Y...")).isDiscCountConsistent());
    }

    // Győzelem és döntetlen

    @Test
    @DisplayName("Vízszintes négyes = győzelem, és pontosan az a négy mező a nyerő")
    void horizontalWin() {
        BoardService bs = new BoardService(TestBoards.of(
                ".......",
                ".......",
                "YYY....",
                "YRRRRYY"));
        assertEquals(GameResult.RED_WON, bs.evaluate());
        assertEquals(Set.of(new Position(3, 1), new Position(3, 2), new Position(3, 3), new Position(3, 4)),
                bs.winningPositions());
    }

    @Test
    @DisplayName("Függőleges négyes = győzelem")
    void verticalWin() {
        BoardService bs = new BoardService(TestBoards.of(
                "Y...",
                "Y.R.",
                "Y.R.",
                "Y.R."));
        assertEquals(GameResult.YELLOW_WON, bs.evaluate());
    }

    @Test
    @DisplayName("Átlós (jobbra lefelé \\) négyes = győzelem")
    void diagonalDownWin() {
        BoardService bs = new BoardService(TestBoards.of(
                "R...",
                "YR..",
                "YYR.",
                "YRYR"));
        assertEquals(GameResult.RED_WON, bs.evaluate());
    }

    @Test
    @DisplayName("Átlós (jobbra felfelé /) négyes = győzelem")
    void diagonalUpWin() {
        BoardService bs = new BoardService(TestBoards.of(
                "...Y",
                "..YR",
                ".YRR",
                "YRRY"));
        assertEquals(GameResult.YELLOW_WON, bs.evaluate());
    }

    @Test
    @DisplayName("Három egy sorban még nem győzelem")
    void threeInARowIsNotAWin() {
        BoardService bs = new BoardService(TestBoards.of(
                "....",
                "....",
                "YY..",
                "RRR."));
        assertEquals(GameResult.IN_PROGRESS, bs.evaluate());
        assertTrue(bs.winningPositions().isEmpty());
    }

    @Test
    @DisplayName("Ötös sornál mind az öt mező kiemelődik")
    void fiveInARowHighlightsAllFive() {
        BoardService bs = new BoardService(TestBoards.of(
                "......",
                "......",
                "YYY.Y.",
                "RRRRRY"));
        assertEquals(GameResult.RED_WON, bs.evaluate());
        assertEquals(5, bs.winningPositions().size());
    }

    @Test
    @DisplayName("Teli tábla négyes nélkül = döntetlen")
    void fullBoardWithoutWinnerIsDraw() {
        BoardService bs = new BoardService(TestBoards.of(
                "RYRY",
                "RYRY",
                "YRYR",
                "YRYR"));
        assertEquals(GameResult.DRAW, bs.evaluate());
        assertTrue(bs.legalColumns().isEmpty());
    }

    @Test
    @DisplayName("Győzelem után már nem lehet lépni")
    void noMovesAfterWin() {
        BoardService bs = new BoardService(TestBoards.of(
                "....",
                "Y...",
                "YY..",
                "RRRR"));
        assertTrue(bs.legalColumns().isEmpty());
        assertThrows(IllegalStateException.class, () -> bs.doMove(new Move(Disc.YELLOW, 3)));
    }

    // Győzteslépés

    @Test
    @DisplayName("isWinningMove felismeri a nyerő lépést, és nem módosítja a táblát")
    void isWinningMoveDetectsWin() {
        Board board = TestBoards.of(
                ".......",
                ".......",
                "YYY....",
                "RRR....");
        assertTrue(BoardService.isWinningMove(board, 3, Disc.RED));
        assertFalse(BoardService.isWinningMove(board, 3, Disc.YELLOW));
        assertFalse(BoardService.isWinningMove(board, 6, Disc.RED));
        assertEquals(3, board.count(Disc.RED), "a tábla nem változhatott");
    }

    @Test
    @DisplayName("isWinningMove: a résbe dobott korong is kiadhatja a négyest (R R _ R)")
    void isWinningMoveFillsGap() {
        Board board = TestBoards.of(
                ".......",
                ".......",
                ".......",
                "RR.R.YY");
        assertTrue(BoardService.isWinningMove(board, 2, Disc.RED));
    }

    @Test
    @DisplayName("isWinningMove teli vagy nem létező oszlopra hamis")
    void isWinningMoveOnFullOrMissingColumn() {
        Board board = TestBoards.of(
                "R...",
                "R...",
                "R...",
                "Y...");
        assertFalse(BoardService.isWinningMove(board, 0, Disc.RED));
        assertFalse(BoardService.isWinningMove(board, 9, Disc.RED));
    }

    // Indíthatóság

    @Test
    @DisplayName("Rendben lévő állásból indítható a játék")
    void playableBoardHasNoProblem() {
        assertTrue(new BoardService(Board.standard()).problemBeforePlay().isEmpty());
    }

    @Test
    @DisplayName("Rossz korongarányú állásból nem indítható a játék")
    void inconsistentBoardIsNotPlayable() {
        Optional<String> problem = new BoardService(TestBoards.of("....", "....", "....", "YY.."))
                .problemBeforePlay();
        assertTrue(problem.isPresent());
        assertTrue(problem.get().contains("korongok száma"));
    }

    @Test
    @DisplayName("Kész négyest vagy teli táblát tartalmazó állásból nem indítható a játék")
    void finishedBoardIsNotPlayable() {
        Optional<String> won = new BoardService(TestBoards.of(
                "....",
                "Y...",
                "YY..",
                "RRRR")).problemBeforePlay();
        assertTrue(won.isPresent());
        assertTrue(won.get().contains("négyes"));

        Optional<String> full = new BoardService(TestBoards.of(
                "RYRY",
                "RYRY",
                "YRYR",
                "YRYR")).problemBeforePlay();
        assertTrue(full.isPresent());
        assertTrue(full.get().contains("tele"));
    }
}
