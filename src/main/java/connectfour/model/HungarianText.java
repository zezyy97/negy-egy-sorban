package connectfour.model;

/**
 * Apró segéd a természetes magyar üzenetekhez: a névelő a szám kiejtésétől függ
 * ("az 1. oszlop", "az 5. oszlop", de "a 4. oszlop"), így nem kell "a(z)"-t írni.
 */
public final class HungarianText {

    private HungarianText() {
    }

    /**
     * Pl. 4 → "a 4. oszlop", 5 → "az 5. oszlop".
     *
     * @param userColumn az oszlop 1-től számozva
     */
    public static String column(int userColumn) {
        return article(userColumn) + " " + userColumn + ". oszlop";
    }

    /** Ugyanaz nagy kezdőbetűvel, mondat elejére: "Az 5. oszlop". */
    public static String columnCapitalized(int userColumn) {
        String s = column(userColumn);
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    /** "az", ha a szám kiejtése magánhangzóval kezdődik (1 = egy, 5 = öt, 50 = ötven…), különben "a". */
    static String article(int number) {
        String digits = String.valueOf(number);
        char first = digits.charAt(0);
        boolean vowel = (first == '1' && digits.length() == 1) || first == '5';
        return vowel ? "az" : "a";
    }
}
