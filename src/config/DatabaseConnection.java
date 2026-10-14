package config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {

    private static Connection connection;

    private DatabaseConnection() {
    }

    public static Connection getConnection() {

        if (connection == null) {
            try {
                connection = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/CinemaManagement",
                    "root",
                    "nandana@2007"
                );

                System.out.println("Database connected successfully!");

            } catch (SQLException e) {
                System.out.println("Database connection failed!");
                e.printStackTrace();
            }
        }

        return connection;
    }
}
