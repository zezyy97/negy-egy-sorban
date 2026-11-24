package connectfour.io;

import connectfour.model.Board;
import connectfour.model.Disc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * A kezdőállapot beolvasása külső szöveges fájlból.
 * <p>
 * Fájlformátum:
 * <pre>
 * # megjegyzés
 * HUMAN=R          (elhagyható: a játékos színe)
 * .......          (a tábla soronként, felülről lefelé)
 * ...
 * </pre>
 */
public final class StartFileReader {

    private static final Logger LOG = Logger.getLogger(StartFileReader.class.getName());
    private static final String HUMAN_PREFIX = "HUMAN=";

    private StartFileReader() {
    }

    /** Beolvasás a fájlrendszerből. */
    public static StartPosition read(Path file) throws IOException {
        LOG.info("Kezdőállapot beolvasása fájlból: " + file);
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            return parse(reader);
        }
    }

    /** Beolvasás adatfolyamból (pl. a jar-ba csomagolt alapértelmezett fájlból). */
    public static StartPosition read(InputStream input) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            return parse(reader);
        }
    }

    private static StartPosition parse(BufferedReader reader) throws IOException {
        Disc human = null;
        List<String> rows = new ArrayList<>();
        String line;
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            if (line.toUpperCase().startsWith(HUMAN_PREFIX)) {
                String value = line.substring(HUMAN_PREFIX.length()).trim();
                human = value.length() == 1 ? Disc.fromLetter(value.charAt(0)) : null;
                if (human == null) {
                    throw new IllegalArgumentException("Hibás játékosszín a fájlban: \"" + value + "\"");
                }
                continue;
            }
            rows.add(line);
        }
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("A fájlban nincs tábla.");
        }
        int cols = rows.get(0).length();
        for (String row : rows) {
            if (row.length() != cols) {
                throw new IllegalArgumentException("A tábla sorai nem egyforma hosszúak.");
            }
        }
        Board board = Board.fromDescription(rows.size(), cols, String.join("", rows));
        return new StartPosition(board, human);
    }
}
