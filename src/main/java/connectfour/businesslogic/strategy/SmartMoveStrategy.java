package connectfour.businesslogic.strategy;

import connectfour.businesslogic.BoardService;
import connectfour.model.Board;
import connectfour.model.Disc;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * Nehéz szint: egyszerű, de meglepően erős szabályrendszer. Sorban megnézi:
 * <ol>
 *   <li><b>Nyerhetek most?</b> Ha valamelyik oszloppal kirakom a négyest, azt választom.</li>
 *   <li><b>Nyerhet az ellenfél?</b> Ha ő a következő lépésével nyerne, oda dobok (blokkolok).</li>
 *   <li><b>Ne adjak ajándékot!</b> Kiszűröm azokat az oszlopokat, ahol a korongom után
 *       az ellenfél azonnal nyerni tudna (pl. pont az ő nyerő mezője alá dobnék).</li>
 *   <li><b>Középre!</b> A maradékból a középhez legközelebbi oszlopot választom, mert onnan
 *       indul a legtöbb lehetséges négyes. Egyenlőség esetén véletlenszerűen.</li>
 * </ol>
 * A TIPP (HINT) parancs is ezt használja, a játékos színével.
 */
public class SmartMoveStrategy implements MoveStrategy {

    private final Random random;

    public SmartMoveStrategy() {
        this(new Random());
    }

    public SmartMoveStrategy(Random random) {
        this.random = random;
    }

    @Override
    public String getName() {
        return "nehéz (okos)";
    }

    @Override
    public int chooseColumn(Board board, Disc me) {
        List<Integer> free = StrategySupport.freeColumns(board);
        Disc opponent = me.opponent();

        // 1. Ha nyerni tudok, nyerek.
        for (int col : free) {
            if (BoardService.isWinningMove(board, col, me)) return col;
        }

        // 2. Ha az ellenfél nyerne, blokkolok.
        for (int col : free) {
            if (BoardService.isWinningMove(board, col, opponent)) return col;
        }

        // 3. Csak olyan oszlop, ami után az ellenfél nem nyer azonnal.
        List<Integer> safe = new ArrayList<>();
        for (int col : free) {
            Board after = board.drop(col, me);
            boolean opponentWinsNext = StrategySupport.freeColumns(after).stream()
                    .anyMatch(c -> BoardService.isWinningMove(after, c, opponent));
            if (!opponentWinsNext) safe.add(col);
        }
        List<Integer> candidates = safe.isEmpty() ? free : safe;   // ha nincs biztonságos, mindegy

        // 4. A középhez legközelebbiek közül véletlenszerűen.
        double center = (board.getCols() - 1) / 2.0;
        Comparator<Integer> byDistance = Comparator.comparingDouble(c -> Math.abs(c - center));
        int best = candidates.stream().min(byDistance).orElseThrow();
        double bestDistance = Math.abs(best - center);
        List<Integer> bestOnes = candidates.stream()
                .filter(c -> Math.abs(c - center) == bestDistance)
                .toList();
        return bestOnes.get(random.nextInt(bestOnes.size()));
    }
}
