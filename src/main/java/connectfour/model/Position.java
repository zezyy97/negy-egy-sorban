package connectfour.model;

/**
 * Egy mező helye a táblán (0-tól számozva). A 0. sor a tábla TETEJE,
 * a legnagyobb sorindex az alja – ide esnek le először a korongok.
 * <p>
 * Java {@code record}: a fordító automatikusan megírja a konstruktort, a gettereket
 * ({@code row()}, {@code col()}), valamint az {@code equals}, {@code hashCode} és
 * {@code toString} metódust. A mezői {@code final}-ok, tehát az objektum megváltoztathatatlan.
 * (A példaprojektben ugyanezt egy hosszabb, kézzel írt osztály csinálta.)
 */
public record Position(int row, int col) {

    /** Az adott irányba egy lépéssel arrébb lévő mező. */
    public Position step(LineDirection dir) {
        return new Position(row + dir.getRowStep(), col + dir.getColStep());
    }

    /** Rajta van-e a mező egy {@code rows} × {@code cols} méretű táblán. */
    public boolean isInside(int rows, int cols) {
        return row >= 0 && col >= 0 && row < rows && col < cols;
    }

    @Override
    public String toString() {
        return "(" + row + "," + col + ")";
    }
}
