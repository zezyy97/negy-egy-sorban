package connectfour.command;

import connectfour.businesslogic.BoardService;
import connectfour.model.Disc;
import connectfour.model.GamePhase;
import connectfour.model.GameResult;
import connectfour.model.GameState;

import java.io.PrintStream;
import java.util.EnumSet;
import java.util.regex.Matcher;

/**
 * PRINT – a teljes állapot kiírása: fázis, színek, nehézség, ki jön, és maga a tábla.
 * Ami még nincs beállítva, arról kiírja, hogy nincs.
 */
public class PrintCommand extends Command {

    public PrintCommand() {
        super("PRINT", "PRINT", "Az állás és a beállítások kiírása.",
                "PRINT", EnumSet.allOf(GamePhase.class));
    }

    @Override
    public void execute(GameState state, Matcher args, PrintStream out) {
        out.println("=== ÁLLAPOT ===");
        out.println("Fázis:      " + state.getPhase());

        Disc human = state.getHumanDisc();
        out.println("Te:         " + (human == null ? "(még nem választottál színt)" : human.getDisplayName()));
        out.println("Gép:        " + (human == null ? "–" : human.opponent().getDisplayName()));
        out.println("Nehézség:   " + state.getMachineStrategy().getName());

        if (!state.hasBoard()) {
            out.println("Tábla:      (még nincs)");
        } else {
            BoardService bs = state.getBoardService();
            GameResult result = bs.evaluate();
            if (result.isOver()) {
                out.println("Eredmény:   " + (result == GameResult.DRAW
                        ? "döntetlen"
                        : result.winner().getDisplayName() + " nyert"));
            } else if (bs.isDiscCountConsistent()) {
                out.println("Következik: " + bs.nextToMove().getDisplayName());
            } else {
                out.println("Következik: (nem eldönthető – rossz a korongok aránya)");
            }
            out.println("Visszavonható lépések: " + state.getHistorySize());
            printBoard(state, out);
        }
        out.println("===============");
    }
}
