package models;

/**
 * Admin class representing cinema administrators who manage movies and shows.
 * Inherits common fields and validation from User.
 */
public class Admin extends User {

    public Admin(int userId, String name, String email) {
        super(userId, name, email, "ADMIN");
    }

    public Admin(String name, String email) {
        super(0, name, email, "ADMIN");
    }

    @Override
    public boolean isAdmin() {
        return true;
    }
}
