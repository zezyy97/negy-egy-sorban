package connectfour.command;

import connectfour.model.GamePhase;
import connectfour.model.GameState;

import java.io.PrintStream;
import java.util.Set;
import java.util.regex.Matcher;

/**
 * RESTART – vissza az elejére (EDIT fázis). A tábla, a színválasztás és az előzmények
 * törlődnek, az adatbázis és a beállított nehézség megmarad.
 */
public class RestartCommand extends Command {

    public RestartCommand() {
        super("RESTART", "RESTART", "Újrakezdés: vissza a kezdőmenübe.",
                "RESTART",
                Set.of(GamePhase.HAVE_BOARD, GamePhase.PLAYING, GamePhase.GAME_ENDED, GamePhase.SAVED_GAME));
    }

    @Override
    public void execute(GameState state, Matcher args, PrintStream out) {
        state.reset();
        out.println("Újrakezdés. Kérj új táblát (NEW) vagy tölts be egy mentést (LIST, LOAD).");
    }
}
