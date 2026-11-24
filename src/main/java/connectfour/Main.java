package connectfour;

import connectfour.command.CommandRegistry;
import connectfour.db.DbInit;
import connectfour.db.GameRepository;
import connectfour.engine.GameEngine;
import connectfour.io.StartFileReader;
import connectfour.io.StartPosition;
import connectfour.model.GamePhase;
import connectfour.model.GameState;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A program belépési pontja. Itt "drótozzuk össze" a részeket:
 * <pre>
 *   adatbázis (DbInit) → GameRepository → CommandRegistry (parancsok) → GameEngine (motor)
 *                                                      GameState (állapot) ──┘
 * </pre>
 * A kezdőállapot külső szövegfájlból jön: az első parancssori argumentum a fájl útvonala,
 * enélkül a jar-ba csomagolt alapértelmezett {@code start.txt} kerül beolvasásra.
 * Maga a Main semmilyen játéklogikát nem tartalmaz.
 */
public class Main {

    private static final Logger LOG = Logger.getLogger(Main.class.getName());
    private static final int H2_CONSOLE_PORT = 8082;
    private static final String DEFAULT_START_RESOURCE = "/start.txt";

    public static void main(String[] args) {
        LoggingSetup.init();
        LOG.info("A program elindult.");
        try {
            DbInit.initDb(DbInit.DEFAULT_URL);
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Az adatbázis létrehozása sikertelen.", e);
            System.out.println("Nem sikerült létrehozni az adatbázist: " + e.getMessage());
            System.out.println("A mentés és a betöltés nem fog működni, de játszani lehet.");
        }
        DbInit.startWebConsole(H2_CONSOLE_PORT);

        GameState state = new GameState();
        loadStartPosition(args, state);
        CommandRegistry registry = new CommandRegistry(new GameRepository(DbInit.DEFAULT_URL));
        GameEngine engine = new GameEngine(state, registry, System.in, System.out);

        engine.run();

        DbInit.stopWebConsole();
        LOG.info("A program leállt.");
    }

    /** Beolvassa a kezdőállapotot a külső fájlból; hiba esetén üres szerkesztő fázisból indulunk. */
    static void loadStartPosition(String[] args, GameState state) {
        try {
            StartPosition start;
            if (args.length > 0) {
                start = StartFileReader.read(Path.of(args[0]));
            } else {
                try (InputStream in = Main.class.getResourceAsStream(DEFAULT_START_RESOURCE)) {
                    if (in == null) {
                        return;
                    }
                    start = StartFileReader.read(in);
                }
            }
            applyStartPosition(start, state);
        } catch (IOException | IllegalArgumentException e) {
            LOG.log(Level.WARNING, "A kezdőállapot beolvasása sikertelen.", e);
            System.out.println("A kezdőállapot fájl nem olvasható (" + e.getMessage()
                    + "), üres szerkesztővel indulunk.");
        }
    }

    static void applyStartPosition(StartPosition start, GameState state) {
        state.setBoard(start.board());
        boolean playable = state.getBoardService().problemBeforePlay().isEmpty();
        if (start.humanDisc() != null && playable) {
            state.setHumanDisc(start.humanDisc());
            state.setPhase(GamePhase.PLAYING);
        } else {
            state.setPhase(GamePhase.HAVE_BOARD);
        }
        LOG.info("Kezdőállapot betöltve, fázis: " + state.getPhase());
    }
}
