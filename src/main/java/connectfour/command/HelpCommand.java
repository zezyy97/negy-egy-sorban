package connectfour.command;

import connectfour.model.GamePhase;
import connectfour.model.GameState;

import java.io.PrintStream;
import java.util.EnumSet;
import java.util.List;
import java.util.regex.Matcher;

/**
 * HELP – kiírja az aktuális fázisban használható parancsokat, a használatukkal és a leírásukkal.
 * <p>
 * Jól mutatja a Command minta előnyét: a súgót nem kell kézzel karbantartani,
 * mert minden parancs maga tudja a saját leírását – a HELP csak végigmegy rajtuk.
 */
public class HelpCommand extends Command {

    private final CommandRegistry registry;

    public HelpCommand(CommandRegistry registry) {
        super("HELP", "HELP", "Az itt használható parancsok részletes leírása.",
                "HELP|\\?", EnumSet.allOf(GamePhase.class));
        this.registry = registry;
    }

    @Override
    public void execute(GameState state, Matcher args, PrintStream out) {
        List<Command> available = registry.getCommandsForPhase(state.getPhase());
        int width = available.stream().mapToInt(c -> c.getUsage().length()).max().orElse(10);
        out.println("Parancsok ebben a fázisban (" + state.getPhase() + "):");
        for (Command c : available) {
            out.printf("  %-" + width + "s  %s%n", c.getUsage(), c.getDescription());
        }
        out.println("A kis- és nagybetű mindegy. Az oszlopokat 1-től számozzuk.");
    }
}
