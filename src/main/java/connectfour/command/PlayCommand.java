package connectfour.command;

import connectfour.model.Disc;
import connectfour.model.GamePhase;
import connectfour.model.GameState;

import java.io.PrintStream;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;

/**
 * PLAY R|Y – színválasztás és a játszma indítása. Átmenet: HAVE_BOARD → PLAYING.
 * <p>
 * Indítás előtt ellenőrzi, hogy az (esetleg szerkesztett) állásból szabályosan lehet-e
 * játszani: stimmel-e a korongok aránya, és nincs-e már kész négyes vagy tele tábla.
 * (A példaprojekt ezt nem ellenőrizte, ott el lehetett indítani egy értelmetlen állást is.)
 * <p>
 * Egy paraméteres parancs váltja ki a példa két parancsát (PLAY_FOX, PLAY_HOUND).
 */
public class PlayCommand extends Command {

    public PlayCommand() {
        super("PLAY", "PLAY <R|Y>",
                "Színválasztás és indulás. A piros kezd: PLAY R = te kezdesz, PLAY Y = a gép kezd.",
                "PLAY\\s+([RYPS])",
                Set.of(GamePhase.HAVE_BOARD));
    }

    @Override
    public void execute(GameState state, Matcher args, PrintStream out) {
        Optional<String> problem = state.getBoardService().problemBeforePlay();
        if (problem.isPresent()) {
            out.println("Ebből az állásból nem lehet játszani: " + problem.get());
            out.println("Javítsd ki szerkesztéssel (PUT / TAKE), vagy kérj új táblát (NEW).");
            return;
        }

        Disc human = Disc.fromLetter(args.group(1).charAt(0));
        state.setHumanDisc(human);
        state.setPhase(GamePhase.PLAYING);

        out.println("Te vagy a " + human.getDisplayName().toLowerCase() + ", a gép a "
                + human.opponent().getDisplayName().toLowerCase() + ". Nehézség: " + state.getMachineStrategy().getName() + ".");
        printBoard(state, out);
    }
}
