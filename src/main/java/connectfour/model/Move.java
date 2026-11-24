package connectfour.model;

/**
 * Egy lépés (operátor): melyik színű korongot melyik oszlopba dobjuk.
 * <p>
 * A példaprojektben a lépés "bábu + irány" volt. Itt a gravitáció miatt elég az
 * oszlop: a korong magától leesik a legalsó szabad mezőre, a sort a tábla számolja ki.
 *
 * @param disc   a lépő játékos színe
 * @param column az oszlop indexe 0-tól (a felhasználó 1-től számoz, ezt a parancs alakítja át)
 */
public record Move(Disc disc, int column) {

    public Move {
        if (disc == null) {
            throw new IllegalArgumentException("A lépéshez meg kell adni a korong színét.");
        }
    }

    @Override
    public String toString() {
        return disc.getDisplayName() + " → " + (column + 1) + ". oszlop";
    }
}
