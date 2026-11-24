package connectfour.model;

/**
 * Egy állás kiértékelése: tart-e még a játszma, és ha nem, hogyan ért véget.
 */
public enum GameResult {

    IN_PROGRESS,   // még lehet lépni, senki sem nyert
    RED_WON,       // a pirosnak kirakott egy négyest
    YELLOW_WON,    // a sárgának kirakott egy négyest
    DRAW;          // megtelt a tábla, és senki sem nyert

    public boolean isOver() {
        return this != IN_PROGRESS;
    }

    /** A győztes színe, vagy {@code null}, ha döntetlen / még tart a játék. */
    public Disc winner() {
        return switch (this) {
            case RED_WON -> Disc.RED;
            case YELLOW_WON -> Disc.YELLOW;
            default -> null;
        };
    }

    public static GameResult wonBy(Disc disc) {
        return disc == Disc.RED ? RED_WON : YELLOW_WON;
    }
}
