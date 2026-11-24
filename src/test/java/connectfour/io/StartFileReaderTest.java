package connectfour.io;

import connectfour.model.Disc;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StartFileReaderTest {

    private static StartPosition parse(String text) throws IOException {
        return StartFileReader.read(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void readsBoardAndHumanDisc() throws IOException {
        StartPosition p = parse("# komment\nHUMAN=Y\n....\n....\n....\nRY..\n");
        assertEquals(4, p.board().getRows());
        assertEquals(4, p.board().getCols());
        assertEquals(Disc.YELLOW, p.humanDisc());
        assertEquals(Disc.RED, p.board().getDisc(3, 0));
    }

    @Test
    void humanDiscIsOptional() throws IOException {
        assertNull(parse("....\n....\n....\n....\n").humanDisc());
    }

    @Test
    void bundledDefaultFileIsValid() throws IOException {
        try (var in = StartFileReader.class.getResourceAsStream("/start.txt")) {
            StartPosition p = StartFileReader.read(in);
            assertEquals(6, p.board().getRows());
            assertEquals(7, p.board().getCols());
            assertEquals(Disc.RED, p.humanDisc());
        }
    }

    @Test
    void rejectsEmptyFile() {
        assertThrows(IllegalArgumentException.class, () -> parse("# csak komment\n"));
    }

    @Test
    void rejectsUnevenRows() {
        assertThrows(IllegalArgumentException.class, () -> parse("....\n...\n....\n....\n"));
    }

    @Test
    void rejectsBadHumanDisc() {
        assertThrows(IllegalArgumentException.class, () -> parse("HUMAN=X\n....\n....\n....\n....\n"));
    }

    @Test
    void rejectsFloatingDisc() {
        assertThrows(IllegalArgumentException.class, () -> parse("R...\n....\n....\n....\n"));
    }
}
