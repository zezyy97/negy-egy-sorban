package connectfour.command;

import connectfour.model.Board;
import connectfour.model.GamePhase;
import connectfour.model.HungarianText;
import connectfour.model.GameState;

import java.io.PrintStream;
import java.util.Set;
import java.util.regex.Matcher;

/**
 * TAKE &lt;oszlop&gt; – szerkesztés: az oszlop legfelső korongjának kivétele.
 */
public class TakeCommand extends Command {

    public TakeCommand() {
        super("TAKE", "TAKE <oszlop>",
                "Szerkesztés: az oszlop legfelső korongjának kivétele.",
                "TAKE\\s+(\\d{1,2})",
                Set.of(GamePhase.HAVE_BOARD));
    }

    @Override
    public void execute(GameState state, Matcher args, PrintStream out) {
        Board board = state.getBoard();
        Integer col = parseColumn(args.group(1), board, out);
        if (col == null) return;

        if (board.topDiscRow(col) < 0) {
            out.println(HungarianText.columnCapitalized(col + 1) + " üres, nincs mit kivenni.");
            return;
        }
        state.setBoard(board.removeTop(col));
        out.println("Kivettem " + HungarianText.column(col + 1) + " felső korongját.");
        printBoard(state, out);
    }
}
