package connectfour.command;

import connectfour.model.GamePhase;
import connectfour.model.GameState;

import java.io.PrintStream;
import java.util.EnumSet;
import java.util.regex.Matcher;

/**
 * EXIT – kilépés a programból. Minden fázisban használható.
 * Csak jelzi a motornak, hogy álljon le; a ciklusból a motor lép ki.
 */
public class ExitCommand extends Command {

    public ExitCommand() {
        super("EXIT", "EXIT", "Kilépés a programból.",
                "EXIT", EnumSet.allOf(GamePhase.class));
    }

    @Override
    public void execute(GameState state, Matcher args, PrintStream out) {
        state.setExitRequested(true);
        out.println("Kilépés… Szia!");
    }
}
