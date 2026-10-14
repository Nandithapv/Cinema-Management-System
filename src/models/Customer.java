package models;

/**
 * Customer class representing cinema ticket booking users.
 * Inherits common fields and validation from User.
 */
public class Customer extends User {

    public Customer(int userId, String name, String email) {
        super(userId, name, email, "CUSTOMER");
    }

    public Customer(String name, String email) {
        super(0, name, email, "CUSTOMER");
    }

    @Override
    public boolean isAdmin() {
        return false;
    }
}
