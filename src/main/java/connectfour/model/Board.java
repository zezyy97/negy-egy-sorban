package connectfour.model;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

/**
 * A játéktábla. <b>Megváltoztathatatlan (immutable)</b> osztály: létrehozás után a tartalma
 * soha nem változik. Minden "módosító" metódus ({@link #drop}, {@link #removeTop})
 * egy ÚJ táblát ad vissza, a régit érintetlenül hagyja.
 * <p>
 * Miért jó ez?
 * <ul>
 *   <li>A visszavonás (UNDO) triviális: elég eltenni a régi táblákat egy verembe.</li>
 *   <li>A gépi játékos nyugodtan "kipróbálhat" lépéseket, nem rontja el az igazi állást.</li>
 *   <li>Nem fordulhat elő, hogy valaki kívülről, véletlenül átírja a tábla egy mezőjét.</li>
 * </ul>
 * Ezért a belső tömböt mindig lemásoljuk, ha kívülről jön, és kifelé sem adjuk ki.
 * <p>
 * Koordináták: a 0. sor a tábla teteje, a {@code rows-1}. sor az alja.
 */
public final class Board {

    /** Ennyi egyforma korong kell egy sorban a győzelemhez. */
    public static final int WIN_LENGTH = 4;
    public static final int MIN_SIZE = 4;
    public static final int MAX_SIZE = 10;
    public static final int STANDARD_ROWS = 6;
    public static final int STANDARD_COLS = 7;

    private static final char EMPTY_SYMBOL = '.';

    private final int rows;
    private final int cols;
    private final Disc[][] cells;

    /** Üres tábla a megadott méretben. */
    public Board(int rows, int cols) {
        validateSize(rows, cols);
        this.rows = rows;
        this.cols = cols;
        this.cells = new Disc[rows][cols];
    }

    /** Belső konstruktor */
    private Board(Disc[][] ownedCells) {
        this.rows = ownedCells.length;
        this.cols = ownedCells[0].length;
        this.cells = ownedCells;
    }

    /** A szabványos 6 sor × 7 oszlopos tábla. */
    public static Board standard() {
        return new Board(STANDARD_ROWS, STANDARD_COLS);
    }

    /**
     * Tábla visszaállítása szöveges leírásból (ezt tároljuk az adatbázisban).
     * A leírás soronként, felülről lefelé, balról jobbra sorolja a mezőket:
     * 'R' = piros, 'Y' = sárga, '.' = üres.
     *
     * @throws IllegalArgumentException ha a leírás hossza vagy egy karaktere hibás,
     *                                  vagy egy korong "lebeg" (alatta üres mező van)
     */
    public static Board fromDescription(int rows, int cols, String description) {
        validateSize(rows, cols);
        if (description == null || description.length() != rows * cols) {
            throw new IllegalArgumentException(
                    "A tábla leírásának pontosan " + rows * cols + " karakter hosszúnak kell lennie.");
        }
        Disc[][] cells = new Disc[rows][cols];
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                char ch = description.charAt(r * cols + c);
                if (ch != EMPTY_SYMBOL) {
                    Disc disc = Disc.fromLetter(ch);
                    if (disc == null) {
                        throw new IllegalArgumentException("Ismeretlen karakter a tábla leírásában: '" + ch + "'");
                    }
                    cells[r][c] = disc;
                }
            }
        }
        checkGravity(cells);
        return new Board(cells);
    }

    // Lekérdezések

    public int getRows() {
        return rows;
    }

    public int getCols() {
        return cols;
    }

    /** A mezőn lévő korong, vagy {@code null}, ha üres. */
    public Disc getDisc(int row, int col) {
        return cells[row][col];
    }

    public Disc getDisc(Position p) {
        return cells[p.row()][p.col()];
    }

    public boolean isEmptyAt(Position p) {
        return getDisc(p) == null;
    }

    public boolean isInside(Position p) {
        return p.isInside(rows, cols);
    }

    /** Létező oszlop-e (0-tól számozva). */
    public boolean isValidColumn(int col) {
        return col >= 0 && col < cols;
    }

    /** Tele van-e az oszlop (a legfelső mező foglalt). */
    public boolean isColumnFull(int col) {
        return cells[0][col] != null;
    }

    /** Tele van-e az egész tábla. */
    public boolean isFull() {
        for (int c = 0; c < cols; c++) {
            if (!isColumnFull(c)) return false;
        }
        return true;
    }

    /**
     * Melyik sorba esne a korong, ha ebbe az oszlopba dobnánk: a legalsó üres mező.
     *
     * @return a sor indexe, vagy -1, ha az oszlop tele van
     */
    public int landingRow(int col) {
        for (int r = rows - 1; r >= 0; r--) {
            if (cells[r][col] == null) return r;
        }
        return -1;
    }

    /**
     * Az oszlop legfelső korongjának sora.
     *
     * @return a sor indexe, vagy -1, ha az oszlop üres
     */
    public int topDiscRow(int col) {
        for (int r = 0; r < rows; r++) {
            if (cells[r][col] != null) return r;
        }
        return -1;
    }

    /** Hány darab ilyen színű korong van a táblán. */
    public int count(Disc disc) {
        int n = 0;
        for (Disc[] row : cells) {
            for (Disc d : row) {
                if (d == disc) n++;
            }
        }
        return n;
    }

    // "Módosítások" – mindig új táblát adnak

    /**
     * Bedob egy korongot az oszlopba. A korong a legalsó szabad mezőre esik.
     *
     * @return az új tábla (ez a tábla nem változik)
     * @throws IllegalArgumentException ha nincs ilyen oszlop, vagy az oszlop tele van
     */
    public Board drop(int col, Disc disc) {
        if (disc == null) throw new IllegalArgumentException("Nincs megadva a korong színe.");
        if (!isValidColumn(col)) {
            throw new IllegalArgumentException("Nincs " + (col + 1) + ". oszlop (1–" + cols + " választható).");
        }
        int row = landingRow(col);
        if (row < 0) {
            throw new IllegalArgumentException(HungarianText.columnCapitalized(col + 1) + " már tele van.");
        }
        Disc[][] copy = copyCells();
        copy[row][col] = disc;
        return new Board(copy);
    }

    /**
     * Kiveszi az oszlop legfelső korongját (szerkesztéshez).
     *
     * @return az új tábla
     * @throws IllegalArgumentException ha nincs ilyen oszlop, vagy az oszlop üres
     */
    public Board removeTop(int col) {
        if (!isValidColumn(col)) {
            throw new IllegalArgumentException("Nincs " + (col + 1) + ". oszlop (1–" + cols + " választható).");
        }
        int row = topDiscRow(col);
        if (row < 0) {
            throw new IllegalArgumentException(HungarianText.columnCapitalized(col + 1) + " üres, nincs mit kivenni.");
        }
        Disc[][] copy = copyCells();
        copy[row][col] = null;
        return new Board(copy);
    }

    // Szöveges alakok

    /** Egysoros leírás az adatbázisba, pl. 6×7-es táblánál 42 karakter. */
    public String toDescription() {
        StringBuilder sb = new StringBuilder(rows * cols);
        for (Disc[] row : cells) {
            for (Disc d : row) {
                sb.append(d == null ? EMPTY_SYMBOL : d.getSymbol());
            }
        }
        return sb.toString();
    }

    /**
     * Kirajzolja a táblát. A kiemelt mezők (pl. a nyerő négyes) zárójelben jelennek meg.
     * <pre>
     *   1  2  3  4  5  6  7
     * | .  .  .  .  .  .  . |
     * | .  . (R) Y  .  .  . |
     * +---------------------+
     * </pre>
     */
    public String render(Collection<Position> highlighted) {
        StringBuilder sb = new StringBuilder();
        sb.append(' ');
        for (int c = 0; c < cols; c++) {
            sb.append(c + 1 < 10 ? " " + (c + 1) + " " : (c + 1) + " ");
        }
        sb.append('\n');
        for (int r = 0; r < rows; r++) {
            sb.append('|');
            for (int c = 0; c < cols; c++) {
                Disc d = cells[r][c];
                char symbol = d == null ? EMPTY_SYMBOL : d.getSymbol();
                boolean mark = highlighted.contains(new Position(r, c));
                sb.append(mark ? '(' : ' ').append(symbol).append(mark ? ')' : ' ');
            }
            sb.append("|\n");
        }
        sb.append('+').append("-".repeat(cols * 3)).append("+\n");
        return sb.toString();
    }

    @Override
    public String toString() {
        return render(Set.of());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Board other)) return false;
        return Arrays.deepEquals(cells, other.cells);
    }

    @Override
    public int hashCode() {
        return Arrays.deepHashCode(cells);
    }

    // Segédmetódusok

    private Disc[][] copyCells() {
        Disc[][] copy = new Disc[rows][];
        for (int r = 0; r < rows; r++) {
            copy[r] = cells[r].clone();
        }
        return copy;
    }

    private static void validateSize(int rows, int cols) {
        if (rows < MIN_SIZE || rows > MAX_SIZE || cols < MIN_SIZE || cols > MAX_SIZE) {
            throw new IllegalArgumentException(
                    "A tábla mérete " + MIN_SIZE + "–" + MAX_SIZE + " sor és oszlop lehet (most: "
                            + rows + "×" + cols + ").");
        }
    }

    /** Ellenőrzi, hogy nincs-e "lebegő" korong (olyan, ami alatt üres mező van). */
    private static void checkGravity(Disc[][] cells) {
        int rows = cells.length;
        int cols = cells[0].length;
        for (int c = 0; c < cols; c++) {
            boolean seenEmptyBelow = false;
            for (int r = rows - 1; r >= 0; r--) {
                if (cells[r][c] == null) {
                    seenEmptyBelow = true;
                } else if (seenEmptyBelow) {
                    throw new IllegalArgumentException(
                            "Lebegő korong " + HungarianText.column(c + 1) + "ban: alatta üres mező van.");
                }
            }
        }
    }
}
