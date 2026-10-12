
CREATE DATABASE IF NOT EXISTS CinemaManagement;
USE CinemaManagement;

CREATE TABLE IF NOT EXISTS users (
    user_id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    phone VARCHAR(15),
    role VARCHAR(20) NOT NULL DEFAULT 'CUSTOMER',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS movies (
    movie_id INT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(150) NOT NULL,
    genre VARCHAR(50) NOT NULL,
    duration INT NOT NULL,
    language VARCHAR(50) NOT NULL,
    poster_url VARCHAR(255) DEFAULT 'default_poster.png',
    description TEXT
);

CREATE TABLE IF NOT EXISTS screens (
    screen_id INT PRIMARY KEY AUTO_INCREMENT,
    screen_name VARCHAR(50) NOT NULL UNIQUE,
    capacity INT NOT NULL,
    CHECK (capacity > 0)
);

CREATE TABLE IF NOT EXISTS shows (
    show_id INT PRIMARY KEY AUTO_INCREMENT,
    movie_id INT NOT NULL,
    screen_id INT NOT NULL,
    show_date DATE NOT NULL,
    show_time TIME NOT NULL,
    ticket_price DECIMAL(10,2) NOT NULL DEFAULT 150.00,

    FOREIGN KEY (movie_id)
        REFERENCES movies(movie_id) ON DELETE RESTRICT,

    FOREIGN KEY (screen_id)
        REFERENCES screens(screen_id) ON DELETE RESTRICT,

    UNIQUE (screen_id, show_date, show_time),
    CHECK (ticket_price >= 0)
);

CREATE TABLE IF NOT EXISTS seats (
    seat_id INT PRIMARY KEY AUTO_INCREMENT,
    screen_id INT NOT NULL,
    seat_number VARCHAR(10) NOT NULL,

    FOREIGN KEY (screen_id)
        REFERENCES screens(screen_id) ON DELETE CASCADE,

    UNIQUE (screen_id, seat_number)
);

CREATE TABLE IF NOT EXISTS bookings (
    booking_id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    show_id INT NOT NULL,
    booking_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    total_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    status VARCHAR(20) NOT NULL DEFAULT 'CONFIRMED',

    FOREIGN KEY (user_id)
        REFERENCES users(user_id) ON DELETE RESTRICT,

    FOREIGN KEY (show_id)
        REFERENCES shows(show_id) ON DELETE RESTRICT,

    CHECK (total_amount >= 0),
    CHECK (status IN ('CONFIRMED', 'CANCELLED'))
);

CREATE TABLE IF NOT EXISTS booking_details (
    booking_detail_id INT PRIMARY KEY AUTO_INCREMENT,
    booking_id INT NOT NULL,
    show_id INT NOT NULL,
    seat_id INT NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'CONFIRMED',

    active_seat_key VARCHAR(50)
        GENERATED ALWAYS AS (
            IF(
                status = 'CONFIRMED',
                CONCAT(show_id, '_', seat_id),
                NULL
            )
        ) STORED,

    UNIQUE KEY uq_active_seat (active_seat_key),

    FOREIGN KEY (booking_id)
        REFERENCES bookings(booking_id) ON DELETE RESTRICT,

    FOREIGN KEY (show_id)
        REFERENCES shows(show_id) ON DELETE RESTRICT,

    FOREIGN KEY (seat_id)
        REFERENCES seats(seat_id) ON DELETE RESTRICT,

    CHECK (price >= 0),
    CHECK (status IN ('CONFIRMED', 'CANCELLED'))
);

CREATE TABLE IF NOT EXISTS payments (
    payment_id INT PRIMARY KEY AUTO_INCREMENT,
    booking_id INT NOT NULL,
    method VARCHAR(20) NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'SUCCESS',
    transaction_ref VARCHAR(100) UNIQUE,
    payment_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (booking_id)
        REFERENCES bookings(booking_id) ON DELETE RESTRICT,

    CHECK (method IN ('CARD', 'UPI', 'CASH')),
    CHECK (status IN ('SUCCESS', 'REFUNDED', 'FAILED')),
    CHECK (amount >= 0)
);
