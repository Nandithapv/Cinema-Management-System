package dao;

import config.DatabaseConnection;
import models.Screen;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Screen Data Access Object.
 * Handles screens and auto-creation of 60 seats (A1-F10) per screen.
 * Author: Member 3 (Namitha) & Member 6 (Nanditha - connection safety & transaction integrity).
 */
public class ScreenDAO implements BaseDAO<Screen> {

    private static final int ROWS = 6;   // A to F
    private static final int COLS = 10;  // 1 to 10

    // Inserts the screen AND its 60 seats in one atomic transaction
    @Override
    public boolean insert(Screen s) {
        Connection c = DatabaseConnection.getConnection();
        if (c == null) return false;

        boolean oldAutoCommit = true;
        try {
            oldAutoCommit = c.getAutoCommit();
            c.setAutoCommit(false);

            int screenId;
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO screens (screen_name, capacity) VALUES (?,?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, s.getScreenName());
                ps.setInt(2, ROWS * COLS);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    keys.next();
                    screenId = keys.getInt(1);
                }
            }

            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO seats (screen_id, seat_number) VALUES (?,?)")) {
                for (int r = 0; r < ROWS; r++) {
                    for (int n = 1; n <= COLS; n++) {
                        ps.setInt(1, screenId);
                        ps.setString(2, "" + (char) ('A' + r) + n);
                        ps.addBatch();
                    }
                }
                ps.executeBatch();
            }

            c.commit();
            s.setScreenId(screenId);
            s.setCapacity(ROWS * COLS);
            return true;

        } catch (SQLException e) {
            try {
                c.rollback();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            e.printStackTrace();
            return false;
        } finally {
            try {
                c.setAutoCommit(oldAutoCommit);
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    }

    // Only the name can change; capacity follows the seats
    @Override
    public boolean update(Screen s) {
        String sql = "UPDATE screens SET screen_name=? WHERE screen_id=?";
        Connection c = DatabaseConnection.getConnection();
        if (c == null) return false;

        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, s.getScreenName());
            ps.setInt(2, s.getScreenId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public Screen getById(int id) {
        String sql = "SELECT * FROM screens WHERE screen_id=?";
        Connection c = DatabaseConnection.getConnection();
        if (c == null) return null;

        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<Screen> getAll() {
        List<Screen> list = new ArrayList<>();
        String sql = "SELECT * FROM screens ORDER BY screen_id";
        Connection c = DatabaseConnection.getConnection();
        if (c == null) return list;

        try (Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // Deletes seats first, then the screen. Fails (false) if shows use it.
    @Override
    public boolean delete(int id) {
        Connection c = DatabaseConnection.getConnection();
        if (c == null) return false;

        boolean oldAutoCommit = true;
        try {
            oldAutoCommit = c.getAutoCommit();
            c.setAutoCommit(false);

            try (PreparedStatement ps = c.prepareStatement(
                    "DELETE FROM seats WHERE screen_id=?")) {
                ps.setInt(1, id);
                ps.executeUpdate();
            }

            int rows;
            try (PreparedStatement ps = c.prepareStatement(
                    "DELETE FROM screens WHERE screen_id=?")) {
                ps.setInt(1, id);
                rows = ps.executeUpdate();
            }

            c.commit();
            return rows > 0;

        } catch (SQLIntegrityConstraintViolationException e) {
            try {
                c.rollback();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            System.out.println("Cannot delete: this screen has shows or bookings.");
            return false;
        } catch (SQLException e) {
            try {
                c.rollback();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            e.printStackTrace();
            return false;
        } finally {
            try {
                c.setAutoCommit(oldAutoCommit);
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    }

    private Screen map(ResultSet rs) throws SQLException {
        return new Screen(
            rs.getInt("screen_id"),
            rs.getString("screen_name"),
            rs.getInt("capacity"));
    }
}