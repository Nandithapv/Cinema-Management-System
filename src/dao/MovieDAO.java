package dao;

import config.DatabaseConnection;
import models.Movie;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Movie Data Access Object.
 * Handles CRUD operations for movies in the database.
 * Author: Member 3 (Namitha) & Member 6 (Nanditha - bug fixes & active/genre filtering).
 * 
 * OOP Viva Concepts:
 * - Method Overloading: getMovies() and getMovies(String genre)
 * - Safe Singleton connection usage (NEVER closes connection).
 */
public class MovieDAO implements BaseDAO<Movie> {

    @Override
    public boolean insert(Movie m) {
        String sql = "INSERT INTO movies (title, genre, duration, language, poster_path, is_active) VALUES (?,?,?,?,?,?)";
        Connection c = DatabaseConnection.getConnection();
        if (c == null) return false;

        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, m.getTitle());
            ps.setString(2, m.getGenre());
            ps.setInt(3, m.getDuration());
            ps.setString(4, m.getLanguage());
            ps.setString(5, m.getPosterPath() != null ? m.getPosterPath() : "default_poster.png");
            ps.setBoolean(6, m.isActive());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            // Fallback for schemas without poster_path/is_active
            String fallbackSql = "INSERT INTO movies (title, genre, duration, language) VALUES (?,?,?,?)";
            try (PreparedStatement ps2 = c.prepareStatement(fallbackSql)) {
                ps2.setString(1, m.getTitle());
                ps2.setString(2, m.getGenre());
                ps2.setInt(3, m.getDuration());
                ps2.setString(4, m.getLanguage());
                return ps2.executeUpdate() > 0;
            } catch (SQLException ex) {
                ex.printStackTrace();
                return false;
            }
        }
    }

    @Override
    public boolean update(Movie m) {
        String sql = "UPDATE movies SET title=?, genre=?, duration=?, language=?, poster_path=?, is_active=? WHERE movie_id=?";
        Connection c = DatabaseConnection.getConnection();
        if (c == null) return false;

        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, m.getTitle());
            ps.setString(2, m.getGenre());
            ps.setInt(3, m.getDuration());
            ps.setString(4, m.getLanguage());
            ps.setString(5, m.getPosterPath());
            ps.setBoolean(6, m.isActive());
            ps.setInt(7, m.getMovieId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            String fallbackSql = "UPDATE movies SET title=?, genre=?, duration=?, language=? WHERE movie_id=?";
            try (PreparedStatement ps2 = c.prepareStatement(fallbackSql)) {
                ps2.setString(1, m.getTitle());
                ps2.setString(2, m.getGenre());
                ps2.setInt(3, m.getDuration());
                ps2.setString(4, m.getLanguage());
                ps2.setInt(5, m.getMovieId());
                return ps2.executeUpdate() > 0;
            } catch (SQLException ex) {
                ex.printStackTrace();
                return false;
            }
        }
    }

    @Override
    public Movie getById(int id) {
        String sql = "SELECT * FROM movies WHERE movie_id=?";
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
    public List<Movie> getAll() {
        List<Movie> list = new ArrayList<>();
        String sql = "SELECT * FROM movies ORDER BY title";
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

    // Used by Member 6 for the customer home screen
    public List<Movie> getAllMovies() {
        return getAll();
    }

    // Customer catalog filter: returns only active movies
    public List<Movie> getActiveMovies() {
        List<Movie> list = new ArrayList<>();
        String sql = "SELECT * FROM movies WHERE is_active=TRUE ORDER BY title";
        Connection c = DatabaseConnection.getConnection();
        if (c == null) return list;

        try (Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            // If is_active column doesn't exist, fall back to getAll()
            return getAll();
        }
        return list;
    }

    // OOP Method Overloading for Viva: getMovies() vs getMovies(String genre)
    public List<Movie> getMovies() {
        return getActiveMovies();
    }

    public List<Movie> getMovies(String genre) {
        if (genre == null || genre.trim().isEmpty() || "All Genres".equalsIgnoreCase(genre)) {
            return getActiveMovies();
        }
        List<Movie> list = new ArrayList<>();
        String sql = "SELECT * FROM movies WHERE is_active=TRUE AND LOWER(genre) LIKE ? ORDER BY title";
        Connection c = DatabaseConnection.getConnection();
        if (c == null) return list;

        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, "%" + genre.trim().toLowerCase() + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            // Fallback without is_active
            String fallbackSql = "SELECT * FROM movies WHERE LOWER(genre) LIKE ? ORDER BY title";
            try (PreparedStatement ps2 = c.prepareStatement(fallbackSql)) {
                ps2.setString(1, "%" + genre.trim().toLowerCase() + "%");
                try (ResultSet rs2 = ps2.executeQuery()) {
                    while (rs2.next()) list.add(map(rs2));
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
        return list;
    }

    // Returns false if the movie still has shows (foreign key blocks it)
    @Override
    public boolean delete(int id) {
        String sql = "DELETE FROM movies WHERE movie_id=?";
        Connection c = DatabaseConnection.getConnection();
        if (c == null) return false;

        try (PreparedStatement ps = c.prepareStatement(sql)) {
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
        Movie m = new Movie(
            rs.getInt("movie_id"),
            rs.getString("title"),
            rs.getString("genre"),
            rs.getInt("duration"),
            rs.getString("language"));

        try {
            m.setPosterPath(rs.getString("poster_path"));
        } catch (SQLException ignored) {}

        try {
            m.setActive(rs.getBoolean("is_active"));
        } catch (SQLException ignored) {}

        return m;
    }
}