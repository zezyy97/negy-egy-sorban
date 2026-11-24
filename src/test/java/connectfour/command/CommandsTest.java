package connectfour.command;

import connectfour.TestBoards;
import connectfour.businesslogic.strategy.RandomMoveStrategy;
import connectfour.businesslogic.strategy.SmartMoveStrategy;
import connectfour.db.GameRepository;
import connectfour.model.Board;
import connectfour.model.Disc;
import connectfour.model.GamePhase;
import connectfour.model.GameState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Az egyes parancsok viselkedése: fázisátmenetek, ellenőrzések, hibaüzenetek.
 * A parancsokat ugyanúgy hívjuk, ahogy a motor: a registry megkeresi, mi végrehajtjuk.
 */
class CommandsTest {

    private GameState state;
    private CommandRegistry registry;
    private GameRepository repository;

    @BeforeEach
    void setUp() {
        repository = new GameRepository(TestBoards.freshDatabase());
        registry = new CommandRegistry(repository);
        state = new GameState();
    }

    /** Végrehajt egy parancsot az aktuális fázisban, és visszaadja, amit kiírt. */
    private String run(String input) {
        CommandRegistry.MatchResult m = registry.findMatchingCommand(input, state.getPhase())
                .orElseGet(() -> fail("Nem található parancs erre: \"" + input + "\" (" + state.getPhase() + ")"));
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        m.command().execute(state, m.matcher(), new PrintStream(buffer, true, StandardCharsets.UTF_8));
        return buffer.toString(StandardCharsets.UTF_8);
    }

    // NEW, PUT, TAKE parancsok

    @Test
    @DisplayName("NEW paraméter nélkül 6×7-es táblát ad, és HAVE_BOARD fázisba lép")
    void newCreatesStandardBoard() {
        run("NEW");
        assertEquals(GamePhase.HAVE_BOARD, state.getPhase());
        assertEquals(Board.standard(), state.getBoard());
    }

    @Test
    @DisplayName("NEW érvénytelen mérettel: hibaüzenet, a fázis nem változik")
    void newRejectsInvalidSize() {
        String out = run("NEW 3 20");
        assertEquals(GamePhase.EDIT, state.getPhase());
        assertFalse(state.hasBoard());
        assertTrue(out.contains("4–10"));
    }

    @Test
    @DisplayName("PUT és TAKE szerkeszti a táblát")
    void putAndTakeEditBoard() {
        run("NEW 4 4");
        run("PUT 2 R");
        run("PUT 2 S");          // magyar betűjel: S = sárga
        assertEquals(Disc.RED, state.getBoard().getDisc(3, 1));
        assertEquals(Disc.YELLOW, state.getBoard().getDisc(2, 1));
        run("TAKE 2");
        assertNull(state.getBoard().getDisc(2, 1));
        assertTrue(run("TAKE 3").contains("üres"));
        assertTrue(run("PUT 9 R").contains("Nincs 9. oszlop"));
    }

    // PLAY

    @Test
    @DisplayName("PLAY nem indul rossz korongarányú állásból")
    void playRejectsInconsistentBoard() {
        run("NEW");
        run("PUT 1 Y");
        String out = run("PLAY R");
        assertEquals(GamePhase.HAVE_BOARD, state.getPhase());
        assertTrue(out.contains("korongok száma"));
    }

    @Test
    @DisplayName("PLAY R: a játékos piros, és ő jön")
    void playRedLetsHumanStart() {
        run("NEW");
        run("PLAY R");
        assertEquals(GamePhase.PLAYING, state.getPhase());
        assertEquals(Disc.RED, state.getHumanDisc());
        assertTrue(state.isHumanOnMove());
    }

    @Test
    @DisplayName("PLAY Y: a gép (piros) kezd")
    void playYellowLetsMachineStart() {
        run("NEW");
        run("PLAY Y");
        assertFalse(state.isHumanOnMove());
        assertEquals(Disc.RED, state.getMachineDisc());
    }

    // DROP, MACHINE, HINT

    @Test
    @DisplayName("DROP és MACHINE csak a saját körben működik")
    void turnsAreEnforced() {
        run("NEW");
        run("PLAY R");
        assertTrue(run("MACHINE").contains("Most te jössz"));
        run("4");
        assertEquals(Disc.RED, state.getBoard().getDisc(5, 3));
        assertTrue(run("DROP 1").contains("Most nem te jössz"));
        assertEquals(1, state.getBoard().count(Disc.RED), "a második piros lépés nem történhetett meg");
        run("MACHINE");
        assertEquals(1, state.getBoard().count(Disc.YELLOW));
        assertTrue(state.isHumanOnMove());
    }

    @Test
    @DisplayName("DROP teli oszlopba: hibaüzenet, nincs lépés")
    void dropIntoFullColumnIsRejected() {
        run("NEW 4 4");
        run("PUT 1 R");
        run("PUT 1 Y");
        run("PUT 1 R");
        run("PUT 1 Y");
        run("PLAY R");
        String out = run("1");
        assertTrue(out.contains("tele van"));
        assertEquals(0, state.getHistorySize());
    }

    @Test
    @DisplayName("HINT a nyerő oszlopot javasolja")
    void hintSuggestsWinningColumn() {
        state.setBoard(TestBoards.of(
                ".......",
                ".......",
                ".......",
                ".......",
                "YYY....",
                "RRR...."));
        state.setPhase(GamePhase.PLAYING);
        state.setHumanDisc(Disc.RED);
        assertTrue(run("HINT").contains("a 4. oszlop"));
    }

    // UNDO

    @Test
    @DisplayName("UNDO a játékos és a gép lépését is visszavonja, hogy újra a játékos jöjjön")
    void undoRevertsHumanAndMachineMove() {
        run("NEW");
        run("PLAY R");
        Board start = state.getBoard();
        run("4");
        run("MACHINE");
        String out = run("UNDO");
        assertTrue(out.contains("2 lépést"));
        assertEquals(start, state.getBoard());
        assertTrue(state.isHumanOnMove());
        assertTrue(run("UNDO").contains("Nincs mit"));
    }

    @Test
    @DisplayName("UNDO a játszma vége után visszavisz PLAYING fázisba")
    void undoAfterGameEndReturnsToPlaying() {
        state.setBoard(TestBoards.of(
                ".......",
                ".......",
                ".......",
                ".......",
                "YYY....",
                "RRR...."));
        state.setPhase(GamePhase.PLAYING);
        state.setHumanDisc(Disc.RED);
        run("4");                                   // piros nyer
        state.setPhase(GamePhase.GAME_ENDED);       // ezt a motor tenné meg
        run("UNDO");
        assertEquals(GamePhase.PLAYING, state.getPhase());
        assertEquals(3, state.getBoard().count(Disc.RED));
    }

    // LEVEL, RESTART

    @Test
    @DisplayName("LEVEL lecseréli a gép stratégiáját")
    void levelSwitchesStrategy() {
        assertTrue(state.getMachineStrategy() instanceof SmartMoveStrategy);
        run("LEVEL easy");
        assertTrue(state.getMachineStrategy() instanceof RandomMoveStrategy);
        run("LEVEL NEHEZ");
        assertTrue(state.getMachineStrategy() instanceof SmartMoveStrategy);
    }

    @Test
    @DisplayName("RESTART mindent alaphelyzetbe tesz")
    void restartResetsState() {
        run("NEW");
        run("PLAY R");
        run("4");
        run("RESTART");
        assertEquals(GamePhase.EDIT, state.getPhase());
        assertFalse(state.hasBoard());
        assertNull(state.getHumanDisc());
        assertFalse(state.canUndo());
    }

    // SAVE, LIST, LOAD

    @Test
    @DisplayName("SAVE: hiányzó, hibás és már foglalt név esetén nem ment")
    void saveValidatesName() {
        run("NEW");
        assertTrue(run("SAVE").contains("Adj nevet"));
        assertTrue(run("SAVE ékezetes").contains("ékezet nélküli"));
        assertTrue(run("SAVE ures-6x7").contains("Már van"));   // a szkript már tartalmazza
    }

    @Test
    @DisplayName("SAVE befejezett játszma után SAVED_GAME fázisba lép, és LOAD-dal visszatölthető")
    void saveAfterGameEndAndLoadBack() {
        state.setBoard(TestBoards.of("....", "Y...", "YY..", "RRRR"));
        state.setHumanDisc(Disc.RED);
        state.setPhase(GamePhase.GAME_ENDED);
        String out = run("SAVE gyozelem");
        assertTrue(out.contains("Elmentve"));
        assertEquals(GamePhase.SAVED_GAME, state.getPhase());
        assertTrue(run("LIST").contains("gyozelem"));

        run("LOAD 5");
        // befejezett állás: nem folytatható, ezért szerkesztő fázisba kerülünk
        assertEquals(GamePhase.HAVE_BOARD, state.getPhase());
        assertEquals(4, state.getBoard().count(Disc.RED));
    }

    @Test
    @DisplayName("LOAD színnel mentett, folytatható játszmánál egyből PLAYING fázisba lép")
    void loadContinuesSavedGame() {
        String out = run("LOAD 2");     // "piros-nyerhet"
        assertTrue(out.contains("piros-nyerhet"));
        assertEquals(GamePhase.PLAYING, state.getPhase());
        assertEquals(Disc.RED, state.getHumanDisc());
    }

    @Test
    @DisplayName("LOAD szín nélküli mentésnél HAVE_BOARD fázisba lép; nem létező azonosítónál hibát ír")
    void loadWithoutSideAndMissingId() {
        run("LOAD 1");
        assertEquals(GamePhase.HAVE_BOARD, state.getPhase());
        assertNull(state.getHumanDisc());
        assertTrue(run("LOAD 99").contains("Nincs 99"));
    }

    // PRINT, HELP

    @Test
    @DisplayName("PRINT és HELP tábla nélkül is működik")
    void printAndHelpWithoutBoard() {
        assertTrue(run("PRINT").contains("(még nincs)"));
        String help = run("HELP");
        assertTrue(help.contains("NEW [sorok oszlopok]"));
        assertFalse(help.contains("DROP"), "a HELP csak az adott fázis parancsait mutatja");
    }
}
