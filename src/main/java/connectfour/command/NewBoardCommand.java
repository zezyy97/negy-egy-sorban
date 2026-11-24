package connectfour.command;

import connectfour.model.Board;
import connectfour.model.GamePhase;
import connectfour.model.GameState;

import java.io.PrintStream;
import java.util.Set;
import java.util.regex.Matcher;

/**
 * NEW [sorok oszlopok] – új, üres tábla.
 * Paraméter nélkül a szabványos 6×7-es táblát adja, egyébként 4–10 sor és oszlop lehet.
 * Átmenet: EDIT / HAVE_BOARD → HAVE_BOARD.
 * <p>
 * A regexben a {@code (?: ... )?} egy NEM rögzítő, elhagyható csoport: így ugyanaz a parancs
 * kezeli a "NEW" és a "NEW 5 6" alakot (a példaprojektben ehhez két külön parancs kellett).
 */
public class NewBoardCommand extends Command {

    public NewBoardCommand() {
        super("NEW", "NEW [sorok oszlopok]",
                "Új üres tábla. Paraméter nélkül 6×7-es; pl. NEW 5 6.",
                "NEW(?:\\s+(\\d{1,2})\\s+(\\d{1,2}))?",
                Set.of(GamePhase.EDIT, GamePhase.HAVE_BOARD));
    }

    @Override
    public void execute(GameState state, Matcher args, PrintStream out) {
        Board board;
        if (args.group(1) == null) {
            board = Board.standard();
        } else {
            int rows = Integer.parseInt(args.group(1));
            int cols = Integer.parseInt(args.group(2));
            try {
                board = new Board(rows, cols);
            } catch (IllegalArgumentException e) {
                out.println(e.getMessage());
                return;
            }
        }
        state.setBoard(board);
        state.setHumanDisc(null);
        state.setPhase(GamePhase.HAVE_BOARD);

        out.println("Új " + board.getRows() + "×" + board.getCols() + "-es tábla kész.");
        printBoard(state, out);
        out.println("Szerkesztheted (PUT / TAKE), vagy válassz színt: PLAY R (te kezdesz) / PLAY Y (a gép kezd).");
    }
}
