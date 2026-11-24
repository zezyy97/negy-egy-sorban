package connectfour.command;

import connectfour.TestBoards;
import connectfour.db.GameRepository;
import connectfour.model.GamePhase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandRegistryTest {

    private CommandRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new CommandRegistry(new GameRepository(TestBoards.freshDatabase()));
    }

    private String matchedName(String input, GamePhase phase) {
        return registry.findMatchingCommand(input, phase)
                .map(m -> m.command().getName())
                .orElse(null);
    }

    @Test
    @DisplayName("A DROP parancs a puszta számra is illeszkedik, és a paramétert kiemeli")
    void dropMatchesPlainNumber() {
        Optional<CommandRegistry.MatchResult> m = registry.findMatchingCommand("4", GamePhase.PLAYING);
        assertTrue(m.isPresent());
        assertEquals("DROP", m.get().command().getName());
        assertEquals("4", m.get().matcher().group(1));
    }

    @Test
    @DisplayName("A kis- és nagybetű, valamint a fölösleges szóközök nem számítanak")
    void caseAndSpacesDoNotMatter() {
        assertEquals("DROP", matchedName("  drop   3 ", GamePhase.PLAYING));
        assertEquals("PUT", matchedName("put 2 y", GamePhase.HAVE_BOARD));
        assertEquals("LEVEL", matchedName("Level Easy", GamePhase.EDIT));
    }

    @Test
    @DisplayName("Rossz fázisban nem található a parancs, de a findInAnyPhase felismeri")
    void wrongPhaseIsRecognised() {
        assertNull(matchedName("DROP 3", GamePhase.EDIT));
        assertEquals("DROP", registry.findInAnyPhase("DROP 3").map(Command::getName).orElse(null));
    }

    @Test
    @DisplayName("Ismeretlen szövegre nincs találat")
    void unknownInputHasNoMatch() {
        assertFalse(registry.findMatchingCommand("REPÜLJ", GamePhase.PLAYING).isPresent());
        assertFalse(registry.findInAnyPhase("REPÜLJ").isPresent());
        assertFalse(registry.findMatchingCommand("PUT 2 X", GamePhase.HAVE_BOARD).isPresent());
    }

    @Test
    @DisplayName("HELP, PRINT és EXIT minden fázisban elérhető")
    void globalCommandsInEveryPhase() {
        for (GamePhase phase : GamePhase.values()) {
            List<String> names = registry.getCommandsForPhase(phase).stream().map(Command::getName).toList();
            assertTrue(names.containsAll(List.of("HELP", "PRINT", "EXIT")), "hiányzik itt: " + phase);
        }
    }

    @Test
    @DisplayName("Minden parancsnév egyedi, és van használati leírása")
    void namesAreUniqueAndDocumented() {
        Set<String> seen = new HashSet<>();
        for (Command c : registry.getAllCommands()) {
            assertTrue(seen.add(c.getName()), "kétszer szerepel: " + c.getName());
            assertFalse(c.getUsage().isBlank());
            assertFalse(c.getDescription().isBlank());
            assertFalse(c.getAllowedPhases().isEmpty());
        }
    }

    @Test
    @DisplayName("Egy bemenetre egy fázison belül legfeljebb egy parancs illeszkedik (nincs kétértelműség)")
    void noAmbiguousInputs() {
        Map<String, GamePhase> samples = Map.ofEntries(
                Map.entry("NEW", GamePhase.EDIT),
                Map.entry("NEW 5 6", GamePhase.HAVE_BOARD),
                Map.entry("PUT 1 R", GamePhase.HAVE_BOARD),
                Map.entry("TAKE 1", GamePhase.HAVE_BOARD),
                Map.entry("PLAY Y", GamePhase.HAVE_BOARD),
                Map.entry("LOAD 2", GamePhase.EDIT),
                Map.entry("4", GamePhase.PLAYING),
                Map.entry("M", GamePhase.PLAYING),
                Map.entry("HINT", GamePhase.PLAYING),
                Map.entry("UNDO", GamePhase.GAME_ENDED),
                Map.entry("SAVE abc", GamePhase.GAME_ENDED),
                Map.entry("RESTART", GamePhase.SAVED_GAME),
                Map.entry("?", GamePhase.SAVED_GAME));
        samples.forEach((input, phase) -> {
            long count = registry.getCommandsForPhase(phase).stream()
                    .filter(c -> c.match(input).isPresent())
                    .count();
            assertEquals(1L, count, "\"" + input + "\" ennyi parancsra illeszkedik: " + count);
        });
    }
}
