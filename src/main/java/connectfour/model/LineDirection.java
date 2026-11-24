package connectfour.model;

/**
 * A négy irány, amerre egy nyerő "négyes" állhat.
 * <p>
 * Elég négy irány (nem kell nyolc), mert minden vonalat a bal vagy a felső végéről
 * indulva vizsgálunk: a "balra" irány ugyanazt a vonalat adná, mint a "jobbra".
 * A számok azt adják meg, mennyit lépünk sorban és oszlopban egy lépésnél
 * (ugyanaz az ötlet, mint a példaprojekt {@code Direction} enumjában).
 */
public enum LineDirection {

    HORIZONTAL(0, 1),       // vízszintesen jobbra   —
    VERTICAL(1, 0),         // függőlegesen lefelé   |
    DIAGONAL_DOWN(1, 1),    // átlósan jobbra le     \
    DIAGONAL_UP(-1, 1);     // átlósan jobbra fel    /

    private final int rowStep;
    private final int colStep;

    LineDirection(int rowStep, int colStep) {
        this.rowStep = rowStep;
        this.colStep = colStep;
    }

    public int getRowStep() {
        return rowStep;
    }

    public int getColStep() {
        return colStep;
    }
}
