package connectfour.businesslogic;

import connectfour.model.Board;
import connectfour.model.Disc;
import connectfour.model.GameResult;
import connectfour.model.LineDirection;
import connectfour.model.Move;
import connectfour.model.Position;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Az üzleti logika (játékszabályok) helye. A {@link Board} csak "tárol",
 * ez az osztály dönti el, mi szabályos, ki jön, és ki nyert.
 * <p>
 * Ugyanúgy működik, mint a példaprojekt {@code BoardService}-e: egy aktuális táblát tart,
 * és a {@link #doMove(Move)} egy ÚJ táblát állít be (mert a {@link Board} megváltoztathatatlan).
 * <p>
 * Fontos döntés: <b>azt, hogy ki következik, nem tároljuk, hanem kiszámoljuk</b> a korongok
 * számából. A piros kezd, ezért ha ugyanannyi piros és sárga korong van, a piros jön,
 * ha eggyel több a piros, a sárga. Így ez az adat sosem kerülhet ellentmondásba a táblával
 * (a példában a "ki lép" két helyen is tárolva volt: a Boardban és a GameState-ben).
 */
public class BoardService {

    private Board board;

    public BoardService(Board board) {
        this.board = Objects.requireNonNull(board, "A tábla nem lehet null.");
    }

    public Board getBoard() {
        return board;
    }

    // ===================== Ki jön? =====================

    /** A soron következő szín: a piros kezd, utána felváltva. */
    public Disc nextToMove() {
        return board.count(Disc.RED) > board.count(Disc.YELLOW) ? Disc.YELLOW : Disc.RED;
    }

    /**
     * Rendben van-e a korongok száma: vagy ugyanannyi piros és sárga van,
     * vagy eggyel több piros (mert a piros kezd). Szerkesztés után ez elromolhat.
     */
    public boolean isDiscCountConsistent() {
        int red = board.count(Disc.RED);
        int yellow = board.count(Disc.YELLOW);
        return red == yellow || red == yellow + 1;
    }

    // ===================== Lépések =====================

    /** A szabályos oszlopok (0-tól): amelyik nincs tele. Véget ért játszmában üres lista. */
    public List<Integer> legalColumns() {
        List<Integer> result = new ArrayList<>();
        if (evaluate().isOver()) return result;
        for (int c = 0; c < board.getCols(); c++) {
            if (!board.isColumnFull(c)) result.add(c);
        }
        return result;
    }

    /** Szabályos-e a lépés: a soron következő lép, létező és nem teli oszlopba, és tart még a játék. */
    public boolean isLegal(Move move) {
        return move.disc() == nextToMove() && legalColumns().contains(move.column());
    }

    /**
     * Végrehajt egy lépést: a táblát lecseréli az új állásra.
     *
     * @throws IllegalStateException    ha a játéknak már vége, vagy nem ez a szín jön
     * @throws IllegalArgumentException ha az oszlop nem létezik vagy tele van
     */
    public void doMove(Move move) {
        if (evaluate().isOver()) {
            throw new IllegalStateException("A játszmának már vége, nem lehet többet lépni.");
        }
        if (move.disc() != nextToMove()) {
            throw new IllegalStateException("Most nem " + move.disc().getDisplayName() + " jön, hanem "
                    + nextToMove().getDisplayName() + ".");
        }
        board = board.drop(move.column(), move.disc());   // új tábla; a drop ellenőrzi az oszlopot
    }

    // ===================== Kiértékelés =====================

    /** Hogy áll a játszma: tart még, valaki nyert, vagy döntetlen. */
    public GameResult evaluate() {
        Optional<Disc> winner = findWinner(board);
        if (winner.isPresent()) return GameResult.wonBy(winner.get());
        if (board.isFull()) return GameResult.DRAW;
        return GameResult.IN_PROGRESS;
    }

    /** Az összes mező, ami valamelyik nyerő négyes része (a kiemeléshez). */
    public Set<Position> winningPositions() {
        return findWinningPositions(board);
    }

    /**
     * Miért nem lehet ebből az állásból játszmát indítani?
     *
     * @return a hiba magyar nyelvű leírása, vagy üres, ha minden rendben
     */
    public Optional<String> problemBeforePlay() {
        if (!isDiscCountConsistent()) {
            return Optional.of("A korongok száma nem stimmel: " + board.count(Disc.RED) + " piros és "
                    + board.count(Disc.YELLOW) + " sárga van. Ugyanannyinak kell lennie, vagy eggyel több "
                    + "pirosnak (mert a piros kezd).");
        }
        GameResult result = evaluate();
        if (result.isOver()) {
            return Optional.of(result == GameResult.DRAW
                    ? "A tábla már tele van."
                    : "Ezen a táblán már van egy kész négyes.");
        }
        return Optional.empty();
    }

    // ===================== Statikus segédek (a gépi játékos is ezeket használja) =====================

    /** Van-e győztes az adott táblán. */
    public static Optional<Disc> findWinner(Board board) {
        Set<Position> line = findWinningPositions(board);
        if (line.isEmpty()) return Optional.empty();
        return Optional.of(board.getDisc(line.iterator().next()));
    }

    /**
     * Megkeresi az összes nyerő négyest. Minden korongtól elindulunk mind a 4 irányba,
     * és megnézzük, hogy a következő 3 mező is ugyanolyan színű-e.
     * Egy 6×7-es táblán ez legfeljebb 42 × 4 vizsgálat – bőven elég gyors.
     */
    public static Set<Position> findWinningPositions(Board board) {
        Set<Position> result = new LinkedHashSet<>();
        for (int r = 0; r < board.getRows(); r++) {
            for (int c = 0; c < board.getCols(); c++) {
                Disc disc = board.getDisc(r, c);
                if (disc == null) continue;
                for (LineDirection dir : LineDirection.values()) {
                    List<Position> line = lineFrom(board, new Position(r, c), dir, disc);
                    if (line.size() == Board.WIN_LENGTH) result.addAll(line);
                }
            }
        }
        return result;
    }

    /**
     * Nyerne-e ez a szín, ha most ebbe az oszlopba dobna?
     * A táblát nem változtatja meg (a {@link Board#drop} új táblát ad).
     */
    public static boolean isWinningMove(Board board, int col, Disc disc) {
        if (!board.isValidColumn(col) || board.isColumnFull(col)) return false;
        Board after = board.drop(col, disc);
        Position landed = new Position(board.landingRow(col), col);
        // Elég a most lerakott korongon átmenő vonalakat nézni.
        for (LineDirection dir : LineDirection.values()) {
            int total = 1
                    + countSame(after, landed, dir.getRowStep(), dir.getColStep(), disc)
                    + countSame(after, landed, -dir.getRowStep(), -dir.getColStep(), disc);
            if (total >= Board.WIN_LENGTH) return true;
        }
        return false;
    }

    /** A {@code start}-tól {@code dir} irányba legfeljebb WIN_LENGTH darab azonos színű mező. */
    private static List<Position> lineFrom(Board board, Position start, LineDirection dir, Disc disc) {
        List<Position> line = new ArrayList<>();
        Position p = start;
        while (line.size() < Board.WIN_LENGTH && board.isInside(p) && board.getDisc(p) == disc) {
            line.add(p);
            p = p.step(dir);
        }
        return line;
    }

    /** Hány azonos színű korong van egymás után a {@code from} mező mellett (önmagát nem számolva). */
    private static int countSame(Board board, Position from, int dRow, int dCol, Disc disc) {
        int n = 0;
        Position p = new Position(from.row() + dRow, from.col() + dCol);
        while (board.isInside(p) && board.getDisc(p) == disc) {
            n++;
            p = new Position(p.row() + dRow, p.col() + dCol);
        }
        return n;
    }
}
