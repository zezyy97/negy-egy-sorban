package connectfour.engine;

import connectfour.businesslogic.BoardService;
import connectfour.command.Command;
import connectfour.command.CommandRegistry;
import connectfour.command.CommandRegistry.MatchResult;
import connectfour.model.Disc;
import connectfour.model.GamePhase;
import connectfour.model.GameResult;
import connectfour.model.GameState;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * A játék motorja – a Command minta <b>"Invoker"</b> szerepe: beolvas egy sort, megkeresi
 * a hozzá illő parancsot, és végrehajtja. Azt, hogy a parancs MIT csinál, nem tudja.
 * <p>
 * A fő ciklus lépései:
 * <ol>
 *   <li>kiírja a fázist és az itt használható parancsokat (és hogy ki jön),</li>
 *   <li>beolvas egy sort,</li>
 *   <li>a {@link CommandRegistry} megkeresi a parancsot, a motor végrehajtja,</li>
 *   <li>a parancs után ellenőrzi, vége lett-e a játszmának (→ GAME_ENDED).</li>
 * </ol>
 * A bemenet és a kimenet konstruktorban jön, nem fixen a {@code System.in}/{@code System.out}:
 * így a teljes játékmenet tesztelhető egy előre megírt "forgatókönyvvel".
 */
public class GameEngine implements Runnable {

    private static final Logger LOG = Logger.getLogger(GameEngine.class.getName());

    private final GameState state;
    private final CommandRegistry registry;
    private final Scanner in;
    private final PrintStream out;

    public GameEngine(GameState state, CommandRegistry registry, InputStream input, PrintStream out) {
        this.state = state;
        this.registry = registry;
        this.in = new Scanner(input);
        this.out = out;
    }

    @Override
    public void run() {
        out.println("=== NÉGY A SORBAN ===");
        out.println("Dobj be felváltva korongokat; aki előbb kirak négyet egy sorban");
        out.println("(vízszintesen, függőlegesen vagy átlósan), az nyer. Súgó: HELP");

        while (!state.isExitRequested()) {
            showPrompt();
            if (!in.hasNextLine()) {          // a bemenet véget ért (pl. Ctrl+D vagy tesztben elfogyott)
                out.println();
                out.println("A bemenet véget ért, kilépek.");
                break;
            }
            String input = in.nextLine().trim();
            if (input.isEmpty()) continue;

            Optional<MatchResult> match = registry.findMatchingCommand(input, state.getPhase());
            if (match.isEmpty()) {
                explainWhyNotMatched(input);
                continue;
            }

            try {
                LOG.info("Parancs: " + input + " (fázis: " + state.getPhase() + ")");
                match.get().command().execute(state, match.get().matcher(), out);
            } catch (RuntimeException e) {
                // Egy váratlan hiba ne állítsa le az egész programot.
                LOG.log(Level.SEVERE, "Váratlan hiba a parancs végrehajtásakor: " + input, e);
                out.println("Váratlan hiba: " + e.getMessage());
            }

            autoDetectEndOfGame();
        }
        out.println("=== Vége ===");
    }

    /**
     * Minden parancs után: ha játszma közben négyes jött ki vagy megtelt a tábla,
     * kihirdetjük az eredményt, és GAME_ENDED fázisba lépünk.
     */
    void autoDetectEndOfGame() {
        if (state.getPhase() != GamePhase.PLAYING || !state.hasBoard()) return;

        BoardService bs = state.getBoardService();
        GameResult result = bs.evaluate();
        if (!result.isOver()) return;

        out.println();
        if (result == GameResult.DRAW) {
            out.println("*** Döntetlen – megtelt a tábla. ***");
        } else {
            Disc winner = result.winner();
            boolean humanWon = winner == state.getHumanDisc();
            out.println("*** " + winner.getDisplayName() + " nyert! "
                    + (humanWon ? "Gratulálok, legyőzted a gépet! ***" : "Most a gép győzött. ***"));
            out.println("(A nyerő négyest a fenti táblán zárójelek jelölik.)");
        }
        LOG.info("A játszma véget ért: " + result);
        state.setPhase(GamePhase.GAME_ENDED);
        out.println("Elmentheted (SAVE <név>), visszaléphetsz (UNDO), vagy kezdhetsz újat (RESTART).");
    }

    private void showPrompt() {
        out.println();
        List<Command> available = registry.getCommandsForPhase(state.getPhase());
        out.println("[" + state.getPhase() + "] Parancsok: "
                + available.stream().map(Command::getName).collect(Collectors.joining(", ")));
        if (state.getPhase() == GamePhase.PLAYING) {
            out.println(state.isHumanOnMove()
                    ? "Te jössz (" + state.getHumanDisc().getDisplayName() + ") – írd be az oszlop számát, pl. 4"
                    : "A gép jön (" + state.getMachineDisc().getDisplayName() + ") – írd be: MACHINE");
        }
        out.print("> ");
        out.flush();
    }

    /** Elgépelés, vagy létező parancs rossz fázisban? A kettőre más üzenet a segítőkész. */
    private void explainWhyNotMatched(String input) {
        Optional<Command> elsewhere = registry.findInAnyPhase(input);
        if (elsewhere.isPresent()) {
            out.println("A " + elsewhere.get().getName() + " parancsot most (" + state.getPhase()
                    + " fázisban) nem lehet használni. Súgó: HELP");
        } else {
            out.println("Ismeretlen parancs vagy hibás formátum: \"" + input + "\". Súgó: HELP");
        }
    }
}
