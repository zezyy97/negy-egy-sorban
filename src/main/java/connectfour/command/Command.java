package connectfour.command;

import connectfour.model.Board;
import connectfour.model.GamePhase;
import connectfour.model.GameState;

import java.io.PrintStream;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Minden felhasználói parancs közös ősosztálya – a <b>Command (parancs) tervezési minta</b> lényege.
 * <p>
 * Egy parancs egy önálló objektum, ami három dolgot tud magáról:
 * <ol>
 *   <li><b>hogyan néz ki</b> – egy reguláris kifejezés, ami a begépelt szövegre illeszkedik,
 *       és a csoportjaiban ({@code (...)}) kiemeli a paramétereket;</li>
 *   <li><b>mikor szabad használni</b> – azoknak a {@link GamePhase}-eknek a halmaza,
 *       amelyekben értelmes;</li>
 *   <li><b>mit csinál</b> – az {@link #execute} metódus, ami a {@link GameState}-et módosítja.</li>
 * </ol>
 * A {@code GameEngine} (a minta "Invoker"-e) így semmit sem tud az egyes parancsokról:
 * csak megkeresi azt, amelyik illeszkedik, és meghívja az {@code execute}-ot.
 * Új parancshoz nem kell a motorhoz nyúlni, elég egy új alosztály és egy sor a
 * {@link CommandRegistry}-ben (nyitott–zárt elv, Open/Closed Principle).
 * <p>
 * Különbség a példaprojekthez képest: ott minden alosztály újra lefordította a saját
 * mintáját és felülírta a {@code match} metódust – ez fölösleges ismétlés volt,
 * itt az ősosztály egyszer elintézi.
 */
public abstract class Command {

    private final String name;
    private final String usage;
    private final String description;
    private final Pattern pattern;
    private final Set<GamePhase> allowedPhases;

    /**
     * @param name          a parancs kulcsszava (pl. "DROP") – ezt listázzuk
     * @param usage         a használat módja (pl. "DROP &lt;oszlop&gt;") – a HELP írja ki
     * @param description   rövid magyar leírás – a HELP írja ki
     * @param regex         a teljes bemenetre illeszkedő minta (kis- és nagybetű mindegy)
     * @param allowedPhases a fázisok, amelyekben a parancs használható
     */
    protected Command(String name, String usage, String description, String regex,
                      Set<GamePhase> allowedPhases) {
        this.name = name;
        this.usage = usage;
        this.description = description;
        this.pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        this.allowedPhases = EnumSet.copyOf(allowedPhases);
    }

    /**
     * Illeszkedik-e a begépelt szöveg erre a parancsra.
     *
     * @return a paramétereket tartalmazó {@link Matcher}, vagy üres, ha nem illeszkedik
     */
    public Optional<Matcher> match(String input) {
        Matcher m = pattern.matcher(input.trim());
        return m.matches() ? Optional.of(m) : Optional.empty();
    }

    /** Használható-e a parancs a megadott fázisban. */
    public boolean isApplicableInPhase(GamePhase phase) {
        return allowedPhases.contains(phase);
    }

    /**
     * A parancs végrehajtása.
     *
     * @param state a játék állapota, amit a parancs módosíthat
     * @param args  a regex illeszkedése; {@code args.group(1)} az első paraméter stb.
     * @param out   ide írjuk a felhasználónak szóló üzeneteket (tesztben ez nem a konzol)
     */
    public abstract void execute(GameState state, Matcher args, PrintStream out);

    public String getName() {
        return name;
    }

    public String getUsage() {
        return usage;
    }

    public String getDescription() {
        return description;
    }

    public Set<GamePhase> getAllowedPhases() {
        return EnumSet.copyOf(allowedPhases);
    }

    // ===================== Közös segédmetódusok az alosztályoknak =====================

    /**
     * A felhasználó 1-től számozott oszlopát 0-tól számozott indexszé alakítja, és ellenőrzi.
     *
     * @return az oszlop indexe, vagy {@code null}, ha hibás (ilyenkor a hibaüzenetet már kiírta)
     */
    protected static Integer parseColumn(String text, Board board, PrintStream out) {
        int userColumn = Integer.parseInt(text);
        if (userColumn < 1 || userColumn > board.getCols()) {
            out.println("Nincs " + userColumn + ". oszlop – 1 és " + board.getCols() + " között válassz.");
            return null;
        }
        return userColumn - 1;
    }

    /** Kirajzolja az aktuális táblát; ha van kész négyes, azt zárójelekkel kiemeli. */
    protected static void printBoard(GameState state, PrintStream out) {
        if (!state.hasBoard()) {
            out.println("(Még nincs tábla.)");
            return;
        }
        out.print(state.getBoard().render(state.getBoardService().winningPositions()));
    }

    @Override
    public String toString() {
        return name;
    }
}
