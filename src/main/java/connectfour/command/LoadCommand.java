package connectfour.command;

import connectfour.db.DataAccessException;
import connectfour.db.GameRepository;
import connectfour.db.SavedGame;
import connectfour.model.GamePhase;
import connectfour.model.GameState;

import java.io.PrintStream;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * LOAD &lt;id&gt; – egy mentés betöltése.
 * <ul>
 *   <li>Ha a mentésben van játékosszín, és az állás folytatható, egyből PLAYING fázisba lépünk
 *       (folytatjuk a félbehagyott játszmát).</li>
 *   <li>Különben HAVE_BOARD fázisba kerülünk: lehet szerkeszteni és színt választani.</li>
 * </ul>
 */
public class LoadCommand extends Command {

    private static final Logger LOG = Logger.getLogger(LoadCommand.class.getName());

    private final GameRepository repository;

    public LoadCommand(GameRepository repository) {
        super("LOAD", "LOAD <id>", "Mentett játszma betöltése (az azonosítót a LIST mutatja).",
                "LOAD\\s+(\\d{1,9})", Set.of(GamePhase.EDIT, GamePhase.HAVE_BOARD, GamePhase.SAVED_GAME));
        this.repository = repository;
    }

    @Override
    public void execute(GameState state, Matcher args, PrintStream out) {
        int id = Integer.parseInt(args.group(1));

        Optional<SavedGame> found;
        try {
            found = repository.findById(id);
        } catch (DataAccessException e) {
            LOG.log(Level.SEVERE, "Betöltés sikertelen: " + id, e);
            out.println("Hiba: " + e.getMessage());
            return;
        }
        if (found.isEmpty()) {
            out.println("Nincs " + id + " azonosítójú mentés. A LIST megmutatja, melyek vannak.");
            return;
        }

        SavedGame game = found.get();
        LOG.info("Játék betöltve: " + game.name() + " (id: " + id + ")");
        state.setBoard(game.board());
        state.setHumanDisc(game.humanDisc());
        out.println("Betöltve: \"" + game.name() + "\" (" + game.board().getRows() + "×"
                + game.board().getCols() + ")");
        printBoard(state, out);

        boolean playable = state.getBoardService().problemBeforePlay().isEmpty();
        if (game.humanDisc() != null && playable) {
            state.setPhase(GamePhase.PLAYING);
            out.println("Folytatjuk a játszmát – te vagy a " + game.humanDisc().getDisplayName().toLowerCase() + ".");
        } else {
            state.setHumanDisc(null);
            state.setPhase(GamePhase.HAVE_BOARD);
            out.println("Szerkesztheted, vagy válassz színt: PLAY R / PLAY Y.");
        }
    }
}
