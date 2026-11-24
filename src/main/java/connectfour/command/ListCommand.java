package connectfour.command;

import connectfour.db.DataAccessException;
import connectfour.db.GameRepository;
import connectfour.db.SavedGame;
import connectfour.model.GamePhase;
import connectfour.model.GameState;

import java.io.PrintStream;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;

/**
 * LIST – az adatbázisban lévő mentések listázása. Az állapotot nem változtatja.
 */
public class ListCommand extends Command {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy.MM.dd. HH:mm");

    private final GameRepository repository;

    public ListCommand(GameRepository repository) {
        super("LIST", "LIST", "A mentett játszmák listája.",
                "LIST", Set.of(GamePhase.EDIT, GamePhase.HAVE_BOARD, GamePhase.SAVED_GAME));
        this.repository = repository;
    }

    @Override
    public void execute(GameState state, Matcher args, PrintStream out) {
        List<SavedGame> games;
        try {
            games = repository.findAll();
        } catch (DataAccessException e) {
            out.println("Hiba: " + e.getMessage());
            return;
        }
        if (games.isEmpty()) {
            out.println("Még nincs mentett játszma.");
            return;
        }
        out.println(" ID  Név                   Méret  Te      Mentve");
        out.println("---------------------------------------------------------");
        for (SavedGame g : games) {
            out.printf("%3d  %-20s  %2d×%-2d  %-6s  %s%n",
                    g.id(), g.name(), g.board().getRows(), g.board().getCols(),
                    g.humanDisc() == null ? "–" : g.humanDisc().getDisplayName(),
                    g.savedAt() == null ? "" : g.savedAt().format(TIME));
        }
        out.println("Betöltés: LOAD <id>");
    }
}
