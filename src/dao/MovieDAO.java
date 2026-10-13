package dao;

import config.DatabaseConnection;
import models.Movie;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MovieDAO implements BaseDAO<Movie> {

    @Override
    public boolean insert(Movie m) {
        String sql = "INSERT INTO movies (title, genre, duration, language) VALUES (?,?,?,?)";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, m.getTitle());
            ps.setString(2, m.getGenre());
            ps.setInt(3, m.getDuration());
            ps.setString(4, m.getLanguage());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean update(Movie m) {
        String sql = "UPDATE movies SET title=?, genre=?, duration=?, language=? WHERE movie_id=?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, m.getTitle());
            ps.setString(2, m.getGenre());
            ps.setInt(3, m.getDuration());
            ps.setString(4, m.getLanguage());
            ps.setInt(5, m.getMovieId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public Movie getById(int id) {
        String sql = "SELECT * FROM movies WHERE movie_id=?";
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

    @Override
    public List<Movie> getAll() {
        List<Movie> list = new ArrayList<>();
        String sql = "SELECT * FROM movies ORDER BY title";
        try (Connection c = DatabaseConnection.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // Used by Member 6 for the customer home screen
    public List<Movie> getAllMovies() {
        return getAll();
    }

    // Returns false if the movie still has shows (foreign key blocks it)
    @Override
    public boolean delete(int id) {
        String sql = "DELETE FROM movies WHERE movie_id=?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLIntegrityConstraintViolationException e) {
            System.out.println("Cannot delete: this movie still has shows.");
            return false;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private Movie map(ResultSet rs) throws SQLException {
        return new Movie(
            rs.getInt("movie_id"),
            rs.getString("title"),
            rs.getString("genre"),
            rs.getInt("duration"),
            rs.getString("language"));
    }
}