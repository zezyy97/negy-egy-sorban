package connectfour.command;

import connectfour.db.DataAccessException;
import connectfour.db.GameRepository;
import connectfour.model.GamePhase;
import connectfour.model.GameState;

import java.io.PrintStream;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * SAVE &lt;név&gt; – az aktuális állás mentése az adatbázisba.
 * <ul>
 *   <li>HAVE_BOARD és PLAYING fázisban a fázis nem változik (mentés közben is lehet folytatni).</li>
 *   <li>GAME_ENDED fázisban a mentés után SAVED_GAME fázisba lépünk (mint a példában).</li>
 * </ul>
 * A regex szándékosan megenged bármilyen nevet ({@code \S+}), hogy hibás névnél
 * érthető üzenetet adhassunk, ne csak annyit, hogy "ismeretlen parancs".
 */
public class SaveCommand extends Command {

    private static final Logger LOG = Logger.getLogger(SaveCommand.class.getName());

    private static final String VALID_NAME = "[A-Za-z0-9_-]{1,20}";

    private final GameRepository repository;

    public SaveCommand(GameRepository repository) {
        super("SAVE", "SAVE <név>",
                "Az állás mentése (név: legfeljebb 20 betű, szám, - vagy _).",
                "SAVE(?:\\s+(\\S+))?",
                Set.of(GamePhase.HAVE_BOARD, GamePhase.PLAYING, GamePhase.GAME_ENDED));
        this.repository = repository;
    }

    @Override
    public void execute(GameState state, Matcher args, PrintStream out) {
        String name = args.group(1);
        if (name == null) {
            out.println("Adj nevet a mentésnek, pl. SAVE elso-jatszmam");
            return;
        }
        if (!name.matches(VALID_NAME)) {
            out.println("A név legfeljebb 20 karakter lehet, és csak ékezet nélküli betűt, számot, "
                    + "kötőjelet vagy aláhúzást tartalmazhat.");
            return;
        }
        try {
            if (repository.existsByName(name)) {
                out.println("Már van \"" + name + "\" nevű mentés – válassz másik nevet.");
                return;
            }
            int id = repository.save(name, state.getBoard(), state.getHumanDisc());
            LOG.info("Játék elmentve: " + name + " (id: " + id + ")");
            out.println("Elmentve \"" + name + "\" néven (azonosító: " + id + ").");
        } catch (DataAccessException e) {
            LOG.log(Level.SEVERE, "Mentés sikertelen: " + name, e);
            out.println("Hiba: " + e.getMessage());
            return;
        }

        if (state.getPhase() == GamePhase.GAME_ENDED) {
            state.setPhase(GamePhase.SAVED_GAME);
        }
    }
}
