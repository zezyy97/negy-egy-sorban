package connectfour.businesslogic.strategy;

import connectfour.model.Board;
import connectfour.model.Disc;

/**
 * A gépi játékos "gondolkodásmódja" – a <b>Strategy (stratégia) tervezési minta</b>.
 * <p>
 * A {@code MachineCommand} nem tudja és nem is kell tudnia, HOGYAN választ oszlopot a gép:
 * csak meghívja a {@link #chooseColumn} metódust. A konkrét algoritmus futás közben
 * cserélhető (LEVEL EASY / LEVEL HARD), anélkül, hogy a parancsot módosítani kellene.
 * Új nehézségi szinthez elég egy új osztály, ami megvalósítja ezt az interfészt.
 */
public interface MoveStrategy {

    /** A szint neve a kiíráshoz (pl. "könnyű"). */
    String getName();

    /**
     * Kiválaszt egy oszlopot.
     *
     * @param board az aktuális tábla (megváltoztathatatlan, nyugodtan lehet rajta "próbálgatni")
     * @param me    a gép színe
     * @return egy NEM teli oszlop indexe (0-tól)
     * @throws IllegalStateException ha egyik oszlopba sem lehet már dobni
     */
    int chooseColumn(Board board, Disc me);
}
