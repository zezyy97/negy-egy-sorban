package connectfour.command;

import connectfour.db.GameRepository;
import connectfour.model.GamePhase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;

/**
 * Az összes parancs nyilvántartása. Itt jönnek létre a parancsobjektumok
 * (a Command minta "Client" szerepe), és itt lehet megkeresni, melyik illeszkedik
 * a begépelt szövegre az aktuális fázisban.
 * <p>
 * Ha új parancsot akarsz, írd meg az osztályát, és vedd fel ide egy sorral – mást nem kell
 * módosítani.
 */
public final class CommandRegistry {

    private final List<Command> commands = new ArrayList<>();

    /**
     * @param repository az adatbázis-réteg; a LIST, LOAD és SAVE parancs kapja meg
     *                   (függőséginjektálás konstruktoron át – így tesztben más adatbázis adható)
     */
    public CommandRegistry(GameRepository repository) {

        // ===== Mindig használható parancsok =====
        commands.add(new HelpCommand(this));
        commands.add(new PrintCommand());
        commands.add(new ExitCommand());

        // ===== Tábla létrehozása, betöltése =====
        commands.add(new NewBoardCommand());
        commands.add(new ListCommand(repository));
        commands.add(new LoadCommand(repository));

        // ===== Szerkesztés =====
        commands.add(new PutCommand());
        commands.add(new TakeCommand());

        // ===== Beállítások, színválasztás =====
        commands.add(new LevelCommand());
        commands.add(new PlayCommand());

        // ===== Játék közben =====
        commands.add(new DropCommand());
        commands.add(new MachineCommand());
        commands.add(new HintCommand());
        commands.add(new UndoCommand());

        // ===== Mentés, újrakezdés =====
        commands.add(new SaveCommand(repository));
        commands.add(new RestartCommand());
    }

    /** Az összes parancs (csak olvasható lista). */
    public List<Command> getAllCommands() {
        return Collections.unmodifiableList(commands);
    }

    /** Az adott fázisban használható parancsok, a regisztráció sorrendjében. */
    public List<Command> getCommandsForPhase(GamePhase phase) {
        return commands.stream()
                .filter(c -> c.isApplicableInPhase(phase))
                .toList();
    }

    /**
     * Megkeresi az első parancsot, ami (1) használható ebben a fázisban és (2) illeszkedik a szövegre.
     * A parancsot és az illeszkedést együtt adja vissza, így nem kell kétszer illeszteni
     * (a példaprojekt motorja kétszer futtatta le a regexet).
     */
    public Optional<MatchResult> findMatchingCommand(String input, GamePhase phase) {
        for (Command cmd : commands) {
            if (!cmd.isApplicableInPhase(phase)) continue;
            Optional<Matcher> m = cmd.match(input);
            if (m.isPresent()) {
                return Optional.of(new MatchResult(cmd, m.get()));
            }
        }
        return Optional.empty();
    }

    /**
     * Létező parancs-e a szöveg valamelyik (akár más) fázisban.
     * Ebből tudja a motor megkülönböztetni az elgépelést a "most nem használható" esettől.
     */
    public Optional<Command> findInAnyPhase(String input) {
        return commands.stream()
                .filter(c -> c.match(input).isPresent())
                .findFirst();
    }

    /** Egy megtalált parancs és a hozzá tartozó illeszkedés (benne a paraméterekkel). */
    public record MatchResult(Command command, Matcher matcher) {
    }
}
