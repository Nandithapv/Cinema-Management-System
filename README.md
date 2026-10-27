# 🎬 Cinema Management System (OOP Microproject)

A desktop Cinema Management System built in **Java Swing** with **MySQL Database** via JDBC, applying Object-Oriented Programming (OOP) design patterns and clean architectural separation.

---

## 👥 Team Role Distribution & Architecture

| Member | Module / Responsibility | Core OOP Concept |
|---|---|---|
| **Nandana S S** | Database Schema, Singleton `DatabaseConnection`, Base DAO | Abstraction & Singleton Pattern |
| **Rithuparna P S** | User Auth (`User`, `Customer`, `Admin`), `UserDAO`, `LoginFrame` | Inheritance & Encapsulation |
| **Namitha Krishna A S** | Movie & Show Management, Screen Allocation, Admin Views | Aggregation & Overloading |
| **Shada** | Seat Matrix Grid, Real-time Booking Logic, `BookingDAO` | Encapsulation & State Management |
| **Renjitha Raju** | Payment Strategy (`CardPayment`, `UPIPayment`), Booking Cancellation | Polymorphism (Strategy Pattern) |
| **Nanditha P V** *(Integration Lead)* | Customer Portal, `Theme.java`, `MovieCardPanel`, `Main.java`, Documentation | Loose Coupling & High Cohesion |

---

## 🏗️ Package Structure

All project source files reside under `src/`:

```
src/
├── Main.java                        # App Entry Point & Look-and-Feel Setup
├── config/                          # Database connection singleton & props
├── dao/                             # Data Access Objects (JDBC queries)
├── models/                          # Data models and entities
├── services/                        # Business logic, sessions, strategies
└── views/                           # Swing UI frames and dialogs
    ├── Theme.java                   # Shared styling, colors, fonts, buttons
    ├── CustomerHomeFrame.java       # Customer movie catalog & booking hub
    ├── ShowSelectionDialog.java     # Movie showtime picker
    └── components/
        └── MovieCardPanel.java      # Reusable movie display card
```

---

## 🚀 Setup & Execution Guide

### 1. Prerequisites
- **JDK 17+** (JDK 21 LTS or JDK 25 recommended)
- **MySQL Server 8.0+** & MySQL Workbench
- **MySQL Connector/J JAR** (e.g. `mysql-connector-j-8.x.jar`) added to project build path

### 2. Database Setup
1. Open MySQL Workbench.
2. Run `schema.sql` to initialize the `CinemaManagement` database with all required tables and default admin account:
   * **Admin Email:** `admin@cinema.com`
   * **Admin Password:** `admin123`

### 3. Local Configuration
Create a local `db.properties` file in the project root (this file is ignored by git):
```properties
db.url=jdbc:mysql://localhost:3306/CinemaManagement?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
db.user=root
db.password=YOUR_MYSQL_PASSWORD
```

### 4. Running the Application
* **In Eclipse:** Right-click `src/Main.java` $\rightarrow$ **Run As** $\rightarrow$ **Java Application**.
* **Direct UI Testing:** You can also run `src/views/CustomerHomeFrame.java` or `src/views/Theme.java` standalone to test views independently.

---

## 🎨 Shared UI Theme (`views.Theme`)
All team members should style their Swing components using `views.Theme` for visual consistency:
* Primary Action Buttons: `Theme.stylePrimaryButton(btn)`
* Secondary Buttons: `Theme.styleSecondaryButton(btn)`
* Danger / Cancel Buttons: `Theme.styleDangerButton(btn)`
* Backgrounds: `Theme.applyDarkTheme(frame)`

## UI libraries (not stored in Git)
The redesigned screens use FlatLaf. Download `flatlaf-3.7.2.jar` from Maven Central
(com/formdev/flatlaf/3.7.2/), put it in the project's `lib` folder and add it to the build path
(right-click the jar > Build Path > Add to Build Path).
Posters live in `resources/posters` (any image whose file name matches the movie title is found).
