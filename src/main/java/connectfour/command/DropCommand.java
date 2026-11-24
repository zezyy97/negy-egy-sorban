package connectfour.command;

import connectfour.model.Board;
import connectfour.model.GamePhase;
import connectfour.model.HungarianText;
import connectfour.model.GameState;
import connectfour.model.Move;

import java.io.PrintStream;
import java.util.Set;
import java.util.regex.Matcher;

/**
 * DROP &lt;oszlop&gt; – a játékos lépése: korong bedobása az oszlopba.
 * Kényelmi okból elég csak a számot beírni (pl. "4"), ezt a regexben az elhagyható
 * {@code (?:DROP\s+)?} rész teszi lehetővé.
 * <p>
 * Csak akkor szabad, ha tényleg a játékos jön (a példaprojektben a soron következőt
 * nem ellenőrizte a lépés parancs, így egymás után többször is lehetett lépni).
 * A játszma végét nem itt figyeljük, hanem a {@code GameEngine} minden parancs után.
 */
public class DropCommand extends Command {

    public DropCommand() {
        super("DROP", "DROP <oszlop>  (vagy csak a szám)",
                "Korong bedobása a megadott oszlopba.",
                "(?:DROP\\s+)?(\\d{1,2})",
                Set.of(GamePhase.PLAYING));
    }

    @Override
    public void execute(GameState state, Matcher args, PrintStream out) {
        if (!state.isHumanOnMove()) {
            out.println("Most nem te jössz, hanem a gép – írd be: MACHINE");
            return;
        }
        Board board = state.getBoard();
        Integer col = parseColumn(args.group(1), board, out);
        if (col == null) return;
        if (board.isColumnFull(col)) {
            out.println(HungarianText.columnCapitalized(col + 1) + " tele van, válassz másikat.");
            return;
        }

        state.playMove(new Move(state.getHumanDisc(), col));
        out.println("Bedobtad a korongot " + HungarianText.column(col + 1) + "ba.");
        printBoard(state, out);
    }
}
