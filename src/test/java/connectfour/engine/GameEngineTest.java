package connectfour.engine;

import connectfour.TestBoards;
import connectfour.command.CommandRegistry;
import connectfour.db.GameRepository;
import connectfour.model.GamePhase;
import connectfour.model.GameState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Teljes játékmenetek "forgatókönyvből": a motor bemenetére előre megírt sorokat adunk,
 * és a kimenetet, valamint a végső állapotot ellenőrizzük.
 */
class GameEngineTest {

    private GameState state;
    private CommandRegistry registry;

    @BeforeEach
    void setUp() {
        state = new GameState();
        registry = new CommandRegistry(new GameRepository(TestBoards.freshDatabase()));
    }

    /** Lefuttatja a motort a megadott sorokkal, és visszaadja a teljes kimenetet. */
    private String play(String... lines) {
        String script = String.join("\n", lines) + "\n";
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        GameEngine engine = new GameEngine(state, registry,
                new ByteArrayInputStream(script.getBytes(StandardCharsets.UTF_8)),
                new PrintStream(buffer, true, StandardCharsets.UTF_8));
        engine.run();
        return buffer.toString(StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("A játékos nyer, a motor észreveszi, majd mentés után SAVED_GAME fázis")
    void humanWinsThenSaves() {
        String out = play(
                "NEW",
                "PUT 1 R", "PUT 1 Y",
                "PUT 2 R", "PUT 2 Y",
                "PUT 3 R", "PUT 3 Y",
                "PLAY R",
                "4",
                "SAVE nyertem",
                "EXIT");
        assertTrue(out.contains("Piros nyert!"));
        assertTrue(out.contains("legyőzted a gépet"));
        assertTrue(out.contains("(R)(R)(R)(R)"), "a nyerő négyes kiemelve");
        assertEquals(GamePhase.SAVED_GAME, state.getPhase());
        assertTrue(state.isExitRequested());
    }

    @Test
    @DisplayName("A gép nyer, ha a játékos nem blokkol")
    void machineWins() {
        String out = play(
                "NEW",
                "PUT 1 Y", "PUT 2 Y", "PUT 3 Y",
                "PUT 5 R", "PUT 6 R", "PUT 7 R", "PUT 5 R",
                "PLAY R",
                "MACHINE",
                "EXIT");
        assertTrue(out.contains("Sárga nyert!"));
        assertTrue(out.contains("Most a gép győzött"));
        assertEquals(GamePhase.GAME_ENDED, state.getPhase());
    }

    @Test
    @DisplayName("Ismeretlen parancsra és rossz fázisú parancsra érthető üzenet")
    void helpfulErrorMessages() {
        String out = play("valami", "DROP 3", "EXIT");
        assertTrue(out.contains("Ismeretlen parancs"));
        assertTrue(out.contains("A DROP parancsot most (EDIT fázisban) nem lehet használni"));
    }

    @Test
    @DisplayName("Ha elfogy a bemenet, a motor rendben leáll (nem dob kivételt)")
    void endOfInputStopsGracefully() {
        String out = play("NEW");
        assertTrue(out.contains("A bemenet véget ért"));
        assertEquals(GamePhase.HAVE_BOARD, state.getPhase());
    }

    @Test
    @DisplayName("Végigjátszott játszma: legfeljebb 42 lépés után mindig vége (győzelem vagy döntetlen)")
    void gameAlwaysTerminates() {
        // A gép piros, a "játékos" sárga, és mindig a legkisebb sorszámú nem teli oszlopba dob:
        // körönként beírjuk az 1..7 számokat, az első szabályos elfogadódik, a többit a motor
        // elutasítja (mert addigra már a gép jön). A végén elfogy a bemenet, a motor leáll.
        StringBuilder script = new StringBuilder("NEW\nPLAY Y\n");
        for (int round = 0; round < 21; round++) {
            script.append("MACHINE\n");
            for (int col = 1; col <= 7; col++) {
                script.append(col).append('\n');
            }
        }
        play(script.toString().split("\n"));
        assertEquals(GamePhase.GAME_ENDED, state.getPhase());
    }
}
