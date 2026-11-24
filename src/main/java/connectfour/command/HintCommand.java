package connectfour.command;

import connectfour.businesslogic.strategy.SmartMoveStrategy;
import connectfour.model.GamePhase;
import connectfour.model.HungarianText;
import connectfour.model.GameState;

import java.io.PrintStream;
import java.util.Set;
import java.util.regex.Matcher;

/**
 * HINT – tipp a játékosnak: melyik oszlopot választaná az okos stratégia az ő színével.
 * Szép példa az újrahasznosításra: ugyanaz a {@link SmartMoveStrategy}, amivel a gép játszik.
 */
public class HintCommand extends Command {

    private final SmartMoveStrategy advisor = new SmartMoveStrategy();

    public HintCommand() {
        super("HINT", "HINT", "Tipp: melyik oszlopba érdemes dobnod.",
                "HINT|TIPP", Set.of(GamePhase.PLAYING));
    }

    @Override
    public void execute(GameState state, Matcher args, PrintStream out) {
        if (!state.isHumanOnMove()) {
            out.println("Most a gép jön, nem kell tipp – írd be: MACHINE");
            return;
        }
        int col = advisor.chooseColumn(state.getBoard(), state.getHumanDisc());
        out.println("Tipp: próbáld " + HungarianText.column(col + 1) + "ot.");
    }
}
