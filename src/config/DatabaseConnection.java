package config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    private static final String URL = "jdbc:mysql://localhost:3306/CinemaManagement";
    private static final String USER = "root";
    private static final String PASSWORD = "namitha@2008";

    private DatabaseConnection() {}

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    // Temporary test
    public static void main(String[] args) {
        try (Connection c = getConnection()) {
            System.out.println("Connected");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}