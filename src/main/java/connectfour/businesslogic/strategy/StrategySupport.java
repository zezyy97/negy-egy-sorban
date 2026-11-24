package connectfour.businesslogic.strategy;

import connectfour.model.Board;

import java.util.ArrayList;
import java.util.List;

/** Közös segédmetódusok a stratégiáknak. */
final class StrategySupport {

    private StrategySupport() {
        // csak statikus metódusok – nem példányosítjuk
    }

    /**
     * A nem teli oszlopok listája.
     *
     * @throws IllegalStateException ha minden oszlop tele van
     */
    static List<Integer> freeColumns(Board board) {
        List<Integer> free = new ArrayList<>();
        for (int c = 0; c < board.getCols(); c++) {
            if (!board.isColumnFull(c)) free.add(c);
        }
        if (free.isEmpty()) {
            throw new IllegalStateException("A tábla tele van, nincs hova lépni.");
        }
        return free;
    }
}
