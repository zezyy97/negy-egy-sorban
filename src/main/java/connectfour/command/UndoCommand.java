package connectfour.command;

import connectfour.model.GamePhase;
import connectfour.model.GameState;

import java.io.PrintStream;
import java.util.Set;
import java.util.regex.Matcher;

/**
 * UNDO – visszavonás. Addig lépünk vissza, amíg újra a játékos jön: ha a gép már
 * válaszolt, akkor az ő lépését ÉS a játékosét is visszavonjuk (különben a játékos
 * hiába vonna vissza, a gép jönne).
 * <p>
 * Működik a játszma vége után is (GAME_ENDED → PLAYING), így egy elvesztett
 * játszma utolsó lépései újrajátszhatók.
 * <p>
 * A megvalósítás azért ilyen egyszerű, mert a {@code Board} megváltoztathatatlan:
 * a {@code GameState} minden lépés előtt egy verembe teszi az előző táblát,
 * a visszavonás pedig csak leveszi a verem tetejét.
 */
public class UndoCommand extends Command {

    public UndoCommand() {
        super("UNDO", "UNDO", "Az utolsó lépésed (és a gép válaszának) visszavonása.",
                "UNDO", Set.of(GamePhase.PLAYING, GamePhase.GAME_ENDED));
    }

    @Override
    public void execute(GameState state, Matcher args, PrintStream out) {
        if (!state.canUndo()) {
            out.println("Nincs mit visszavonni.");
            return;
        }
        int undone = 0;
        do {
            state.undoOneMove();
            undone++;
        } while (!state.isHumanOnMove() && state.canUndo());

        state.setPhase(GamePhase.PLAYING);
        out.println(undone == 1 ? "Visszavontam 1 lépést." : "Visszavontam " + undone + " lépést.");
        printBoard(state, out);
    }
}
