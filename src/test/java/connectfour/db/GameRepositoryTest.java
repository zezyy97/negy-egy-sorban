package connectfour.db;

import connectfour.TestBoards;
import connectfour.businesslogic.BoardService;
import connectfour.model.Board;
import connectfour.model.Disc;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameRepositoryTest {

    private GameRepository repository;

    @BeforeEach
    void setUp() {
        repository = new GameRepository(TestBoards.freshDatabase());
    }

    @Test
    @DisplayName("A létrehozó szkript 4 mintajátszmát tölt be")
    void seededGamesArePresent() {
        List<SavedGame> games = repository.findAll();
        assertEquals(4, games.size());
        assertEquals(List.of("ures-6x7", "piros-nyerhet", "blokkolj", "kicsi-5x6"),
                games.stream().map(SavedGame::name).toList());
    }

    @Test
    @DisplayName("Minden mintajátszma szabályos, folytatható állás")
    void seededGamesArePlayable() {
        for (SavedGame g : repository.findAll()) {
            assertTrue(new BoardService(g.board()).problemBeforePlay().isEmpty(), g.name());
            assertNotNull(g.savedAt());
        }
    }

    @Test
    @DisplayName("Mentés, majd visszaolvasás ugyanazt az állást adja")
    void saveAndFindRoundTrip() {
        Board board = TestBoards.of(
                ".....",
                ".....",
                ".....",
                "..Y..",
                "..RR.");
        int id = repository.save("proba", board, Disc.YELLOW);

        Optional<SavedGame> loaded = repository.findById(id);
        assertTrue(loaded.isPresent());
        assertEquals("proba", loaded.get().name());
        assertEquals(board, loaded.get().board());
        assertEquals(Disc.YELLOW, loaded.get().humanDisc());
    }

    @Test
    @DisplayName("Szín nélkül is lehet menteni (NULL kerül az adatbázisba)")
    void saveWithoutHumanDisc() {
        int id = repository.save("szin-nelkul", Board.standard(), null);
        assertNull(repository.findById(id).orElseThrow().humanDisc());
    }

    @Test
    @DisplayName("existsByName és nem létező azonosító")
    void existsAndMissing() {
        assertTrue(repository.existsByName("blokkolj"));
        assertFalse(repository.existsByName("nincs-ilyen"));
        assertTrue(repository.findById(999).isEmpty());
    }

    @Test
    @DisplayName("Azonos névvel másodszor nem lehet menteni (UNIQUE megszorítás)")
    void duplicateNameIsRejectedByDatabase() {
        repository.save("dupla", Board.standard(), Disc.RED);
        assertThrows(DataAccessException.class, () -> repository.save("dupla", Board.standard(), Disc.RED));
    }

    @Test
    @DisplayName("SQL-befecskendezés ellen védett: a név csak adat, nem SQL")
    void nameIsNotInterpretedAsSql() {
        String evil = "x'); DROP TABLE--";
        repository.save(evil, Board.standard(), null);
        assertTrue(repository.existsByName(evil));
        assertEquals(5, repository.findAll().size(), "a tábla megmaradt, és benne van az új sor");
    }
}
