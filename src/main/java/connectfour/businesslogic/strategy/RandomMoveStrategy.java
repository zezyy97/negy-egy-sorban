package connectfour.businesslogic.strategy;

import connectfour.model.Board;
import connectfour.model.Disc;

import java.util.List;
import java.util.Random;

/**
 * Könnyű szint: véletlenszerűen választ a nem teli oszlopok közül
 * (ugyanúgy, ahogy a példaprojekt gépi játékosa lépett).
 */
public class RandomMoveStrategy implements MoveStrategy {

    private final Random random;

    public RandomMoveStrategy() {
        this(new Random());
    }

    /** Tesztekhez: rögzített seed-del a "véletlen" is megismételhető. */
    public RandomMoveStrategy(Random random) {
        this.random = random;
    }

    @Override
    public String getName() {
        return "könnyű (véletlenszerű)";
    }

    @Override
    public int chooseColumn(Board board, Disc me) {
        List<Integer> free = StrategySupport.freeColumns(board);
        return free.get(random.nextInt(free.size()));
    }
}
