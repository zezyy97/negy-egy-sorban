package connectfour.command;

import connectfour.model.Board;
import connectfour.model.Disc;
import connectfour.model.GamePhase;
import connectfour.model.HungarianText;
import connectfour.model.GameState;

import java.io.PrintStream;
import java.util.Set;
import java.util.regex.Matcher;

/**
 * PUT &lt;oszlop&gt; &lt;R|Y&gt; – szerkesztés: egy korong bedobása, a lépéssorrendtől függetlenül.
 * A gravitáció itt is érvényes, így szerkesztéssel sem lehet "lebegő" korongot létrehozni.
 * Hogy a kész állásban stimmel-e a korongok aránya, azt a PLAY ellenőrzi.
 */
public class PutCommand extends Command {

    public PutCommand() {
        super("PUT", "PUT <oszlop> <R|Y>",
                "Szerkesztés: korong bedobása (R vagy P = piros, Y vagy S = sárga).",
                "PUT\\s+(\\d{1,2})\\s+([RYPS])",
                Set.of(GamePhase.HAVE_BOARD));
    }

    @Override
    public void execute(GameState state, Matcher args, PrintStream out) {
        Board board = state.getBoard();
        Integer col = parseColumn(args.group(1), board, out);
        if (col == null) return;
        Disc disc = Disc.fromLetter(args.group(2).charAt(0));

        if (board.isColumnFull(col)) {
            out.println(HungarianText.columnCapitalized(col + 1) + " tele van.");
            return;
        }
        state.setBoard(board.drop(col, disc));
        out.println(disc.getDisplayName() + " korong " + HungarianText.column(col + 1) + "ba.");
        printBoard(state, out);
    }
}
