package connectfour.command;

import connectfour.businesslogic.strategy.MoveStrategy;
import connectfour.model.Disc;
import connectfour.model.GamePhase;
import connectfour.model.HungarianText;
import connectfour.model.GameState;
import connectfour.model.Move;

import java.io.PrintStream;
import java.util.Set;
import java.util.regex.Matcher;

/**
 * MACHINE – a gép lép egyet. Hogy HOGYAN választ, azt a beállított {@link MoveStrategy}
 * dönti el; ez a parancs csak meghívja (Strategy minta).
 */
public class MachineCommand extends Command {

    public MachineCommand() {
        super("MACHINE", "MACHINE", "A gép lép.",
                "MACHINE|M", Set.of(GamePhase.PLAYING));
    }

    @Override
    public void execute(GameState state, Matcher args, PrintStream out) {
        if (state.isHumanOnMove()) {
            out.println("Most te jössz – dobj be egy korongot (pl. DROP 4).");
            return;
        }
        Disc machine = state.getMachineDisc();
        MoveStrategy strategy = state.getMachineStrategy();
        int col = strategy.chooseColumn(state.getBoard(), machine);

        state.playMove(new Move(machine, col));
        out.println("A gép (" + machine.getDisplayName() + ") " + HungarianText.column(col + 1) + "ba dobott.");
        printBoard(state, out);
    }
}
