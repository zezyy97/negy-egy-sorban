package connectfour.model;

import connectfour.businesslogic.BoardService;
import connectfour.businesslogic.strategy.MoveStrategy;
import connectfour.businesslogic.strategy.SmartMoveStrategy;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * A futó alkalmazás teljes, változó állapota. Ezt kapja meg minden parancs, és ezt módosítja.
 * (A Command minta szóhasználatával ez – a {@link BoardService}-szel együtt – a "Receiver",
 * vagyis az az objektum, amin a parancsok dolgoznak.)
 * <p>
 * A példaprojekthez képest:
 * <ul>
 *   <li>nincs külön "ki lép" mező – ezt a {@link BoardService#nextToMove()} számolja ki;</li>
 *   <li>van egy <b>előzményverem</b> ({@link #history}) a visszavonáshoz (UNDO);</li>
 *   <li>a gépi játékos stratégiája cserélhető ({@link #machineStrategy}).</li>
 * </ul>
 */
public class GameState {

    /** A játék aktuális fázisa. */
    private GamePhase phase = GamePhase.EDIT;

    /** A szabályokat ismerő szolgáltatás az aktuális táblával; {@code null}, amíg nincs tábla. */
    private BoardService boardService;

    /** A játékos (ember) színe; {@code null}, amíg nem választott. A gép a másik színnel játszik. */
    private Disc humanDisc;

    /** A korábbi táblák, a legutóbbi van felül. Minden lépés előtt ide tesszük az előző állást. */
    private final Deque<Board> history = new ArrayDeque<>();

    /** A gép "gondolkodásmódja" (Strategy minta). Alapból a nehéz szint. */
    private MoveStrategy machineStrategy = new SmartMoveStrategy();

    /** Igaz, ha a felhasználó kilépést kért (EXIT). */
    private boolean exitRequested;

    // Fázis

    public GamePhase getPhase() {
        return phase;
    }

    public void setPhase(GamePhase phase) {
        this.phase = phase;
    }

    // Tábla

    public boolean hasBoard() {
        return boardService != null;
    }

    public BoardService getBoardService() {
        return boardService;
    }

    /** Kényelmi metódus: az aktuális tábla, vagy {@code null}, ha még nincs. */
    public Board getBoard() {
        return boardService == null ? null : boardService.getBoard();
    }

    /**
     * Új táblát állít be (NEW, LOAD, szerkesztés). Az előzményeket törli, mert egy
     * szerkesztés után a régi állásokra visszalépni nem lenne értelmes.
     */
    public void setBoard(Board board) {
        this.boardService = new BoardService(board);
        history.clear();
    }

    // Lépés és visszavonás

    /**
     * Végrehajt egy játékbeli lépést, és előtte elmenti az előző állást a verembe.
     * Ha a lépés szabálytalan, a {@link BoardService} kivételt dob, és a verembe sem kerül semmi.
     */
    public void playMove(Move move) {
        Board before = boardService.getBoard();
        boardService.doMove(move);
        history.push(before);
    }

    /** Van-e visszavonható lépés. */
    public boolean canUndo() {
        return !history.isEmpty();
    }

    /** Egy lépés visszavonása: a verem tetején lévő táblát állítja vissza. */
    public void undoOneMove() {
        if (history.isEmpty()) {
            throw new IllegalStateException("Nincs visszavonható lépés.");
        }
        boardService = new BoardService(history.pop());
    }

    public int getHistorySize() {
        return history.size();
    }

    // Oldalak

    public Disc getHumanDisc() {
        return humanDisc;
    }

    public void setHumanDisc(Disc humanDisc) {
        this.humanDisc = humanDisc;
    }

    /** A gép színe, vagy {@code null}, ha a játékos még nem választott. */
    public Disc getMachineDisc() {
        return humanDisc == null ? null : humanDisc.opponent();
    }

    /** Most a játékos (ember) következik-e. */
    public boolean isHumanOnMove() {
        return humanDisc != null && hasBoard() && boardService.nextToMove() == humanDisc;
    }

    // Gépi stratégia

    public MoveStrategy getMachineStrategy() {
        return machineStrategy;
    }

    public void setMachineStrategy(MoveStrategy machineStrategy) {
        this.machineStrategy = machineStrategy;
    }

    // Kilépés / újrakezdés

    public boolean isExitRequested() {
        return exitRequested;
    }

    public void setExitRequested(boolean exitRequested) {
        this.exitRequested = exitRequested;
    }

    /** Mindent alaphelyzetbe tesz (RESTART), kivéve a választott nehézségi szintet. */
    public void reset() {
        boardService = null;
        humanDisc = null;
        history.clear();
        phase = GamePhase.EDIT;
    }
}
