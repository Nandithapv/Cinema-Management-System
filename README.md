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

Movie posters live in `resources/posters` (any image whose file name matches the movie title is found).

---

## 🚀 Setup & Execution Guide

### 1. Prerequisites
- **JDK 17+** (JDK 21 LTS or JDK 25 recommended)
- **MySQL Server 8.0+** & MySQL Workbench
- **Libraries:** FlatLaf and MySQL Connector/J (see step 3 below)

### 2. Database Setup
1. Open MySQL Workbench.
2. Run `schema.sql` to initialize the `CinemaManagement` database with all required tables and default admin account:
   * **Admin Email:** `admin@cinema.com`
   * **Admin Password:** `admin123`

### 3. Add Required Libraries
The jars are not stored in Git. Create a `lib` folder in the project root and download both jars into it:

- [flatlaf-3.7.2.jar](https://repo1.maven.org/maven2/com/formdev/flatlaf/3.7.2/flatlaf-3.7.2.jar)
- [mysql-connector-j-8.4.0.jar](https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/8.4.0/mysql-connector-j-8.4.0.jar)

Then in Eclipse: right-click the project → **Build Path → Configure Build Path → Libraries → Add JARs...**, select both jars from `lib`, and click **Apply and Close**.

### 4. Local Configuration
Copy `db.properties.example` to `db.properties` in the project root (this file is ignored by git) and fill in your MySQL password:
```properties
db.url=jdbc:mysql://localhost:3306/CinemaManagement?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
db.user=root
db.password=YOUR_MYSQL_PASSWORD
```

### 5. Running the Application
* **In Eclipse:** Right-click `src/Main.java` → **Run As** → **Java Application**.
* **Direct UI Testing:** You can also run `src/views/CustomerHomeFrame.java` or `src/views/Theme.java` standalone to test views independently.

---

## 🎨 Shared UI Theme (`views.Theme`)
All team members should style their Swing components using `views.Theme` for visual consistency:
* Primary Action Buttons: `Theme.stylePrimaryButton(btn)`
* Secondary Buttons: `Theme.styleSecondaryButton(btn)`
* Danger / Cancel Buttons: `Theme.styleDangerButton(btn)`
* Backgrounds: `Theme.applyDarkTheme(frame)`