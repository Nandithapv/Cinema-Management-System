
CREATE DATABASE IF NOT EXISTS CinemaManagement;
USE CinemaManagement;

-- 1. USERS
CREATE TABLE IF NOT EXISTS users (
    user_id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    phone VARCHAR(15),
    role VARCHAR(20) NOT NULL DEFAULT 'CUSTOMER',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (role IN ('ADMIN', 'CUSTOMER'))
);

-- 2. MOVIES
CREATE TABLE IF NOT EXISTS movies (
    movie_id INT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(150) NOT NULL,
    genre VARCHAR(50) NOT NULL,
    duration INT NOT NULL,
    language VARCHAR(50) NOT NULL,
    poster_path VARCHAR(255) DEFAULT 'default_poster.png',
    description TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    CHECK (duration > 0)
);

-- 3. SCREENS
CREATE TABLE IF NOT EXISTS screens (
    screen_id INT PRIMARY KEY AUTO_INCREMENT,
    screen_name VARCHAR(50) NOT NULL UNIQUE,
    capacity INT NOT NULL,
    CHECK (capacity > 0)
);

-- 4. SHOWS
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

-- 5. SEATS
CREATE TABLE IF NOT EXISTS seats (
    seat_id INT PRIMARY KEY AUTO_INCREMENT,
    screen_id INT NOT NULL,
    seat_number VARCHAR(10) NOT NULL,

    FOREIGN KEY (screen_id)
        REFERENCES screens(screen_id) ON DELETE CASCADE,

    UNIQUE (screen_id, seat_number)
);

-- 6. BOOKINGS
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

-- 7. BOOKING DETAILS
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

-- 8. PAYMENTS (NO STATUS COLUMN)
CREATE TABLE IF NOT EXISTS payments (
    payment_id INT PRIMARY KEY AUTO_INCREMENT,
    booking_id INT NOT NULL,
    method VARCHAR(20) NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    transaction_ref VARCHAR(100) UNIQUE,
    payment_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (booking_id)
        REFERENCES bookings(booking_id) ON DELETE RESTRICT,

    CHECK (method IN ('CARD', 'UPI', 'CASH')),
    CHECK (amount >= 0)
);

-- SAMPLE ADMIN ACCOUNT
INSERT INTO users (name, email, password, role)
VALUES ('Admin', 'admin@cinema.com', SHA2('admin123', 256), 'ADMIN')
ON DUPLICATE KEY UPDATE email = email;

-- SAMPLE SCREEN
INSERT INTO screens (screen_name, capacity)
VALUES ('Screen 1', 60)
ON DUPLICATE KEY UPDATE screen_name = screen_name;

-- SAMPLE MOVIES
INSERT INTO movies
(title, genre, duration, language, poster_path, description, is_active)
SELECT 'Interstellar', 'Sci-Fi', 169, 'English',
       'default_poster.png', 'A journey through space.', TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM movies WHERE title = 'Interstellar'
);

INSERT INTO movies
(title, genre, duration, language, poster_path, description, is_active)
SELECT 'Inception', 'Sci-Fi', 148, 'English',
       'default_poster.png', 'A dream within a dream.', TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM movies WHERE title = 'Inception'
);

INSERT INTO movies
(title, genre, duration, language, poster_path, description, is_active)
SELECT 'Drishyam', 'Thriller', 160, 'Malayalam',
       'default_poster.png', 'A family thriller.', TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM movies WHERE title = 'Drishyam'
);

INSERT INTO movies
(title, genre, duration, language, poster_path, description, is_active)
SELECT 'Premam', 'Romance', 156, 'Malayalam',
       'default_poster.png', 'A story of love and growing up.', TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM movies WHERE title = 'Premam'
);

-- CREATE 60 SEATS FOR SCREEN 1
INSERT INTO seats (screen_id, seat_number)
SELECT sc.screen_id, CONCAT(r.row_letter, n.seat_no)
FROM screens sc
CROSS JOIN (
    SELECT 'A' AS row_letter UNION ALL
    SELECT 'B' UNION ALL SELECT 'C' UNION ALL
    SELECT 'D' UNION ALL SELECT 'E' UNION ALL
    SELECT 'F'
) r
CROSS JOIN (
    SELECT 1 AS seat_no UNION ALL SELECT 2 UNION ALL
    SELECT 3 UNION ALL SELECT 4 UNION ALL
    SELECT 5 UNION ALL SELECT 6 UNION ALL
    SELECT 7 UNION ALL SELECT 8 UNION ALL
    SELECT 9 UNION ALL SELECT 10
) n
WHERE sc.screen_name = 'Screen 1'
AND NOT EXISTS (
    SELECT 1 FROM seats existing
    WHERE existing.screen_id = sc.screen_id
      AND existing.seat_number = CONCAT(r.row_letter, n.seat_no)
);

-- SAMPLE FUTURE SHOWS
INSERT INTO shows
(movie_id, screen_id, show_date, show_time, ticket_price)
SELECT m.movie_id, sc.screen_id,
       DATE_ADD(CURDATE(), INTERVAL 1 DAY), '10:00:00', 150.00
FROM movies m
JOIN screens sc ON sc.screen_name = 'Screen 1'
WHERE m.title = 'Interstellar'
AND NOT EXISTS (
    SELECT 1 FROM shows sh
    WHERE sh.screen_id = sc.screen_id
      AND sh.show_date = DATE_ADD(CURDATE(), INTERVAL 1 DAY)
      AND sh.show_time = '10:00:00'
);

INSERT INTO shows
(movie_id, screen_id, show_date, show_time, ticket_price)
SELECT m.movie_id, sc.screen_id,
       DATE_ADD(CURDATE(), INTERVAL 1 DAY), '14:00:00', 150.00
FROM movies m
JOIN screens sc ON sc.screen_name = 'Screen 1'
WHERE m.title = 'Inception'
AND NOT EXISTS (
    SELECT 1 FROM shows sh
    WHERE sh.screen_id = sc.screen_id
      AND sh.show_date = DATE_ADD(CURDATE(), INTERVAL 1 DAY)
      AND sh.show_time = '14:00:00'
);

INSERT INTO shows
(movie_id, screen_id, show_date, show_time, ticket_price)
SELECT m.movie_id, sc.screen_id,
       DATE_ADD(CURDATE(), INTERVAL 2 DAY), '10:00:00', 150.00
FROM movies m
JOIN screens sc ON sc.screen_name = 'Screen 1'
WHERE m.title = 'Drishyam'
AND NOT EXISTS (
    SELECT 1 FROM shows sh
    WHERE sh.screen_id = sc.screen_id
      AND sh.show_date = DATE_ADD(CURDATE(), INTERVAL 2 DAY)
      AND sh.show_time = '10:00:00'
);

INSERT INTO shows
(movie_id, screen_id, show_date, show_time, ticket_price)
SELECT m.movie_id, sc.screen_id,
       DATE_ADD(CURDATE(), INTERVAL 2 DAY), '14:00:00', 150.00
FROM movies m
JOIN screens sc ON sc.screen_name = 'Screen 1'
WHERE m.title = 'Premam'
AND NOT EXISTS (
    SELECT 1 FROM shows sh
    WHERE sh.screen_id = sc.screen_id
      AND sh.show_date = DATE_ADD(CURDATE(), INTERVAL 2 DAY)
      AND sh.show_time = '14:00:00'
);
