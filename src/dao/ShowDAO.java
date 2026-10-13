package dao;

import config.DatabaseConnection;
import models.Show;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ShowDAO implements BaseDAO<Show> {

    private static final String SELECT_JOIN =
        "SELECT s.*, m.title, sc.screen_name FROM shows s " +
        "JOIN movies m ON s.movie_id = m.movie_id " +
        "JOIN screens sc ON s.screen_id = sc.screen_id ";

    // True if the screen already has a show at that date and time.
    // excludeShowId = 0 when adding; the show's own id when editing.
    public boolean isScreenBusy(int screenId, Date date, Time time, int excludeShowId) {
        String sql = "SELECT COUNT(*) FROM shows WHERE screen_id=? AND show_date=? " +
                     "AND show_time=? AND show_id<>?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, screenId);
            ps.setDate(2, date);
            ps.setTime(3, time);
            ps.setInt(4, excludeShowId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return true; // be safe: treat errors as busy
        }
    }

    @Override
    public boolean insert(Show s) {
        if (isScreenBusy(s.getScreenId(), s.getShowDate(), s.getShowTime(), 0)) {
            System.out.println("Cannot add: screen already has a show at that time.");
            return false;
        }
        String sql = "INSERT INTO shows (movie_id, screen_id, show_date, show_time, ticket_price) " +
                     "VALUES (?,?,?,?,?)";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, s.getMovieId());
            ps.setInt(2, s.getScreenId());
            ps.setDate(3, s.getShowDate());
            ps.setTime(4, s.getShowTime());
            ps.setBigDecimal(5, s.getTicketPrice());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean update(Show s) {
        if (isScreenBusy(s.getScreenId(), s.getShowDate(), s.getShowTime(), s.getShowId())) {
            System.out.println("Cannot update: screen already has a show at that time.");
            return false;
        }
        String sql = "UPDATE shows SET movie_id=?, screen_id=?, show_date=?, show_time=?, " +
                     "ticket_price=? WHERE show_id=?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, s.getMovieId());
            ps.setInt(2, s.getScreenId());
            ps.setDate(3, s.getShowDate());
            ps.setTime(4, s.getShowTime());
            ps.setBigDecimal(5, s.getTicketPrice());
            ps.setInt(6, s.getShowId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Used by Member 4 for seat selection
    @Override
    public Show getById(int id) {
        String sql = SELECT_JOIN + "WHERE s.show_id=?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Show getShowById(int id) {
        return getById(id);
    }

    @Override
    public List<Show> getAll() {
        return query(SELECT_JOIN + "ORDER BY s.show_date, s.show_time", null);
    }

    // Used by Member 4 and Member 6: upcoming shows for one movie
    public List<Show> getShowsByMovie(int movieId) {
        return query(SELECT_JOIN +
            "WHERE s.movie_id=? AND (s.show_date > CURDATE() OR " +
            "(s.show_date = CURDATE() AND s.show_time > CURTIME())) " +
            "ORDER BY s.show_date, s.show_time", movieId);
    }

    // Returns false if bookings exist for this show (foreign key blocks it)
    @Override
    public boolean delete(int id) {
        String sql = "DELETE FROM shows WHERE show_id=?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLIntegrityConstraintViolationException e) {
            System.out.println("Cannot delete: this show has bookings.");
            return false;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private List<Show> query(String sql, Integer param) {
        List<Show> list = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            if (param != null) ps.setInt(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    private Show map(ResultSet rs) throws SQLException {
        Show s = new Show(
            rs.getInt("show_id"),
            rs.getInt("movie_id"),
            rs.getInt("screen_id"),
            rs.getDate("show_date"),
            rs.getTime("show_time"),
            rs.getBigDecimal("ticket_price"));
        s.setMovieTitle(rs.getString("title"));
        s.setScreenName(rs.getString("screen_name"));
        return s;
    }
}