package connectfour;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * A naplózás beállítása (java.util.logging). A naplót a {@code connectfour.log} fájlba írjuk,
 * hogy ne keveredjen a játék konzolos kimenetével.
 */
public final class LoggingSetup {

    public static final String LOG_FILE = "connectfour.log";

    private LoggingSetup() {
    }

    public static void init() {
        LogManager.getLogManager().reset();
        Logger root = Logger.getLogger("");
        root.setLevel(Level.INFO);
        try {
            FileHandler handler = new FileHandler(LOG_FILE, true);
            handler.setEncoding("UTF-8");
            handler.setFormatter(new SimpleFormatter());
            root.addHandler(handler);
        } catch (IOException | SecurityException e) {
            System.out.println("Figyelem: a naplófájl nem hozható létre (" + e.getMessage() + ").");
        }
    }
}
