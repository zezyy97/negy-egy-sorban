package connectfour.model;

/**
 * Egy korong színe. A táblán kétféle korong lehet: piros és sárga.
 * <p>
 * Az üres mezőt NEM jelöljük külön konstanssal: a {@link Board} az üres mezőt
 * {@code null}-ként tárolja, kifelé pedig {@link Board#isEmptyAt(Position)} mondja meg.
 * Így az {@link #opponent()} mindig értelmes (az "üres" ellenfelét nem kell kitalálni).
 */
public enum Disc {

    RED('R', "Piros"),
    YELLOW('Y', "Sárga");

    /** Egybetűs jel: ezt látjuk a táblán, és ezt mentjük az adatbázisba. */
    private final char symbol;

    /** Magyar név a játékosnak szóló üzenetekhez. */
    private final String displayName;

    Disc(char symbol, String displayName) {
        this.symbol = symbol;
        this.displayName = displayName;
    }

    public char getSymbol() {
        return symbol;
    }

    public String getDisplayName() {
        return displayName;
    }

    /** A másik szín (piros ↔ sárga). */
    public Disc opponent() {
        return this == RED ? YELLOW : RED;
    }

    /**
     * Betűből színt csinál. Elfogadja az angol (R / Y) és a magyar (P = piros, S = sárga)
     * kezdőbetűt is, kis- és nagybetűvel egyaránt.
     *
     * @return a szín, vagy {@code null}, ha a betű egyiknek sem felel meg (pl. '.')
     */
    public static Disc fromLetter(char letter) {
        return switch (Character.toUpperCase(letter)) {
            case 'R', 'P' -> RED;
            case 'Y', 'S' -> YELLOW;
            default -> null;
        };
    }
}
