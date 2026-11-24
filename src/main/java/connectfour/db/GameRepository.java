package connectfour.db;

import connectfour.model.Board;
import connectfour.model.Disc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Az adatbázis-műveletek egy helyen – ez a <b>Repository / DAO</b> (Data Access Object) minta.
 * <p>
 * A példaprojektben az SQL a parancsokban (LIST, LOAD, SAVE) szét volt szórva, ráadásul a
 * LIST és a LOAD ugyanazt a betöltő kódot kétszer is tartalmazta. Itt a parancsok csak ezt
 * az osztályt hívják, SQL-t nem látnak. Előnyök: egy helyen kell javítani, a parancsok
 * rövidebbek, és ha egyszer fájlba vagy más adatbázisba mentenénk, csak ezt kellene cserélni.
 * <p>
 * Minden lekérdezés {@link PreparedStatement}-tel megy (paraméterezve), így a felhasználó
 * által megadott név nem tud SQL-befecskendezést (SQL injection) okozni.
 */
public class GameRepository {

    private static final Logger LOG = Logger.getLogger(GameRepository.class.getName());
    private static final String TABLE = "SAVED_GAME_CONNECT_FOUR";

    private final String jdbcUrl;

    public GameRepository(String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
    }

    /** Az összes mentés azonosító szerint sorban. */
    public List<SavedGame> findAll() {
        String sql = "SELECT * FROM " + TABLE + " ORDER BY ID";
        List<SavedGame> result = new ArrayList<>();
        try (Connection conn = connect();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(map(rs));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Nem sikerült lekérdezni a mentéseket.", e);
        }
        return result;
    }

    /** Egy mentés azonosító alapján; üres, ha nincs ilyen. */
    public Optional<SavedGame> findById(int id) {
        String sql = "SELECT * FROM " + TABLE + " WHERE ID = ?";
        try (Connection conn = connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Nem sikerült betölteni az " + id + " azonosítójú mentést.", e);
        }
    }

    /** Van-e már ilyen nevű mentés (a név egyedi az adatbázisban). */
    public boolean existsByName(String name) {
        String sql = "SELECT COUNT(*) FROM " + TABLE + " WHERE NAME = ?";
        try (Connection conn = connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Nem sikerült ellenőrizni a mentés nevét.", e);
        }
    }

    /**
     * Elment egy állást.
     *
     * @return az új mentés azonosítója
     */
    public int save(String name, Board board, Disc humanDisc) {
        String sql = "INSERT INTO " + TABLE
                + " (NAME, ROW_COUNT, COL_COUNT, BOARD_DESCRIPTION, HUMAN_DISC) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = connect();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setInt(2, board.getRows());
            ps.setInt(3, board.getCols());
            ps.setString(4, board.toDescription());
            ps.setString(5, humanDisc == null ? null : String.valueOf(humanDisc.getSymbol()));
            ps.executeUpdate();
            LOG.fine("INSERT a mentés táblába: " + name);
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Nem sikerült elmenteni a játszmát.", e);
        }
    }

    // ===================== Segédmetódusok =====================

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(jdbcUrl, DbInit.USER, DbInit.PASSWORD);
    }

    /** Egy eredménysor átalakítása {@link SavedGame} objektummá. */
    private SavedGame map(ResultSet rs) throws SQLException {
        int rows = rs.getInt("ROW_COUNT");
        int cols = rs.getInt("COL_COUNT");
        Board board = Board.fromDescription(rows, cols, rs.getString("BOARD_DESCRIPTION"));
        String human = rs.getString("HUMAN_DISC");
        Disc humanDisc = (human == null || human.isBlank()) ? null : Disc.fromLetter(human.charAt(0));
        Timestamp ts = rs.getTimestamp("SAVED_AT");
        LocalDateTime savedAt = ts == null ? null : ts.toLocalDateTime();
        return new SavedGame(rs.getInt("ID"), rs.getString("NAME"), board, humanDisc, savedAt);
    }
}
