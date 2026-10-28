# 📐 Cinema Management System — Architecture & Diagrams
*Prepared by Nanditha P V (Master Integration & QA Lead)*

---

## 1. 🗄️ Entity-Relationship (ER) Diagram

```mermaid
erDiagram
    USERS ||--o{ BOOKINGS : places
    SCREENS ||--|{ SEATS : contains
    SCREENS ||--o{ SHOWS : hosts
    MOVIES ||--o{ SHOWS : features
    SHOWS ||--o{ BOOKINGS : reserves
    SHOWS ||--o{ BOOKING_DETAILS : includes
    BOOKINGS ||--|{ BOOKING_DETAILS : contains
    SEATS ||--o{ BOOKING_DETAILS : reserved_in
    BOOKINGS ||--o| PAYMENTS : settled_by

    USERS {
        int user_id PK
        string name
        string email UK
        string password
        string role "ADMIN | CUSTOMER"
    }

    MOVIES {
        int movie_id PK
        string title
        string genre
        int duration
        string language
        string poster_path
        boolean is_active
    }

    SCREENS {
        int screen_id PK
        string screen_name
        int capacity
    }

    SEATS {
        int seat_id PK
        int screen_id FK
        string seat_number "A1-F10"
    }

    SHOWS {
        int show_id PK
        int movie_id FK
        int screen_id FK
        date show_date
        time show_time
        decimal ticket_price
    }

    BOOKINGS {
        int booking_id PK
        int user_id FK
        int show_id FK
        datetime booking_date
        string status "CONFIRMED | CANCELLED"
    }

    BOOKING_DETAILS {
        int booking_detail_id PK
        int booking_id FK
        int show_id FK
        int seat_id FK
        decimal price
    }

    PAYMENTS {
        int payment_id PK
        int booking_id FK
        string method "CARD | UPI"
        decimal amount
        datetime payment_date
    }
```

---

## 2. 🧩 High-Level Class Diagram (MVC Structure)

```mermaid
classDiagram
    %% Model Layer
    class User {
        <<Abstract>>
        #int userId
        #String name
        #String email
        #String role
        +getters()
    }
    class Customer {
        +Customer(...)
    }
    class Admin {
        +Admin(...)
    }
    User <|-- Customer
    User <|-- Admin

    class Movie {
        -int movieId
        -String title
        -String genre
        -int duration
        -String language
        +getters()
    }

    class Show {
        -int showId
        -int movieId
        -int screenId
        -Date showDate
        -Time showTime
        -BigDecimal ticketPrice
        +getters()
    }

    Show o-- Movie : aggregates
    Show o-- Screen : aggregates

    %% View Layer (Member 6 Deliverables)
    class Theme {
        +Color BG_DARK
        +Color PRIMARY
        +Color ACCENT_GOLD
        +Font FONT_TITLE
        +stylePrimaryButton(JButton)
        +styleSecondaryButton(JButton)
        +styleDangerButton(JButton)
    }

    class MovieCardPanel {
        -int movieId
        -String title
        -String genre
        -OnBookListener listener
        +initUI()
    }

    class ShowSelectionDialog {
        -int movieId
        -OnShowSelectedListener listener
        +loadShows()
    }

    class CustomerHomeFrame {
        -int currentUserId
        -String currentUserName
        -List~MovieItem~ allMovies
        +loadMovies()
        +applyFilter()
        +openBookingHistory()
        +handleLogout()
    }

    CustomerHomeFrame *-- MovieCardPanel : displays grid of
    CustomerHomeFrame ..> ShowSelectionDialog : opens on book
    MovieCardPanel ..> Theme : styled with
    CustomerHomeFrame ..> Theme : styled with
    ShowSelectionDialog ..> Theme : styled with

    %% Services & DAOs
    class SessionManager {
        <<Singleton / Static>>
        -User currentLoggedInUser
        +loginUser(User)
        +logoutUser()
        +getCurrentUser()
        +isLoggedIn()
    }

    CustomerHomeFrame ..> SessionManager : reads user from
```

---

## 3. 🎓 Viva Questions & Answers for Nanditha (Member 6)

### Q1: What was your specific architectural role in this project?
> **Answer:** "I served as the **Customer Portal, Master Integration & QA Lead**. My role was twofold:
> 1. **User Experience:** Building the entire customer storefront (`CustomerHomeFrame`), reusable components (`MovieCardPanel`), and the show selector modal (`ShowSelectionDialog`).
> 2. **System Integration & Architecture:** Establishing the shared UI design language (`Theme.java`), enforcing clean MVC boundaries, preventing tight coupling between the views and database queries, and leading end-to-end integration across all 6 modules."

### Q2: How does your module demonstrate "Loose Coupling and High Cohesion"?
> **Answer:**
> * **High Cohesion:** Every class has a single, sharply focused responsibility:
>   * `Theme`: Responsible solely for visual design tokens and component styling.
>   * `MovieCardPanel`: Encapsulates purely the visual representation of a single movie card and fires an event callback when clicked.
>   * `ShowSelectionDialog`: Manages only the workflow of presenting showtimes and capturing customer selection.
>   * `CustomerHomeFrame`: Manages the overall customer dashboard layout, filtering, and navigation.
> * **Loose Coupling:** The UI components do not execute SQL queries or hold database connection handles. Communication occurs via data transfer objects (`MovieItem` / `Movie`, `Show`) and event listener interfaces (`OnBookListener`, `OnShowSelectedListener`). This guarantees that modifications to the database layer or backend business logic do not break the presentation layer.

### Q3: What is the benefit of the ShowSelectionDialog you built?
> **Answer:** "In cinema booking workflows, customers don't book a 'movie' directly—they book a **specific show** at a specific theater screen, date, and time. `ShowSelectionDialog` acts as the vital bridge between browsing the movie catalog and selecting physical seats in Shada's seat matrix."
