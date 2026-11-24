package connectfour.command;

import connectfour.businesslogic.strategy.MoveStrategy;
import connectfour.businesslogic.strategy.RandomMoveStrategy;
import connectfour.businesslogic.strategy.SmartMoveStrategy;
import connectfour.model.GamePhase;
import connectfour.model.GameState;

import java.io.PrintStream;
import java.util.Set;
import java.util.regex.Matcher;

/**
 * LEVEL EASY|HARD – a gép nehézségi szintjének beállítása.
 * Ez cseréli ki a {@link GameState}-ben a {@link MoveStrategy} objektumot (Strategy minta).
 */
public class LevelCommand extends Command {

    public LevelCommand() {
        super("LEVEL", "LEVEL <EASY|HARD>",
                "A gép nehézsége: EASY = véletlenszerű, HARD = okos (ez az alapértelmezett).",
                "LEVEL\\s+(EASY|HARD|KONNYU|NEHEZ)",
                Set.of(GamePhase.EDIT, GamePhase.HAVE_BOARD, GamePhase.PLAYING));
    }

    @Override
    public void execute(GameState state, Matcher args, PrintStream out) {
        String level = args.group(1).toUpperCase();
        MoveStrategy strategy = switch (level) {
            case "EASY", "KONNYU" -> new RandomMoveStrategy();
            default -> new SmartMoveStrategy();
        };
        state.setMachineStrategy(strategy);
        out.println("A gép nehézsége mostantól: " + strategy.getName() + ".");
    }
}
