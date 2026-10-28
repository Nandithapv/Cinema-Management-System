# 🧪 Quality Assurance & Master Integration Report
**Project:** Cinema Management System (OOP Microproject)  
**Lead QA Engineer & Master Integrator:** Nanditha P V (Member 6)  
**Test Date:** October 2026  
**Status:** ✅ ALL TESTS PASSED (100% Codebase Compilation & Functional Integrity)  

---

## 1. Scope of Integration & Testing
As Master Integration & QA Lead, Member 6 was tasked with:
1. Merging modules submitted by 5 individual team members from their independent Git branches.
2. Resolving all package discrepancies, missing constructors, and connection leaks.
3. Conducting rigorous end-to-end integration tests across all workflows.
4. Verifying thread-safety, transaction atomicity, and UI resilience during connection drops.

---

## 2. Master Test Suite Matrix

| Test ID | Module Under Test | Test Scenario | Expected Outcome | Actual Result | Status |
|---|---|---|---|---|---|
| **TC-01** | `Main.java` | App launch & UI initialization | Sets system Look-and-Feel, anti-aliased fonts, launches `LoginFrame` on EDT | Smooth launch with anti-aliasing | ✅ PASS |
| **TC-02** | `UserDAO` / `LoginFrame` | Admin login with valid credentials (`admin@cinema.com` / `admin123`) | Authenticates successfully, stores admin in `SessionManager`, opens `AdminDashboardFrame` | Opened Admin Dashboard | ✅ PASS |
| **TC-03** | `UserDAO` / `LoginFrame` | Customer login with valid credentials | Authenticates successfully, stores customer in `SessionManager`, opens `CustomerHomeFrame` | Opened Customer Home | ✅ PASS |
| **TC-04** | `UserDAO` / `LoginFrame` | Login attempt with invalid password | Rejects login with generic error message: *"Invalid email or password."* | Proper warning shown, password cleared | ✅ PASS |
| **TC-05** | `RegisterFrame` | Customer self-registration with duplicate email | Traps database unique constraint and displays friendly notice | Displayed duplicate email notice | ✅ PASS |
| **TC-06** | `CustomerHomeFrame` | Live search by movie title (e.g. "Interstellar") | Instantly filters catalog cards to matching titles | Matched movies displayed | ✅ PASS |
| **TC-07** | `CustomerHomeFrame` | Genre filtering (e.g. "Sci-Fi", "Romance") | Filters cards to selected genre; Reset button restores full catalog | Filtered & reset accurately | ✅ PASS |
| **TC-08** | `ShowSelectionDialog` | Show selection for chosen movie | Queries `ShowDAO.getShowsByMovie()`, renders dates/screens in styled table | Showtimes loaded cleanly | ✅ PASS |
| **TC-09** | `ShowSelectionDialog` | Seat Selection Forward Routing | Clicking *"Select Seats ->"* opens `SeatSelectionFrame` with selected `Show` and logged-in `User` | Opened SeatSelectionFrame | ✅ PASS |
| **TC-10** | `SeatSelectionFrame` | Interactive seat toggling | 🟢 Green toggles to 🟡 Yellow; updates live price label `(count * ticketPrice)` | Price calculated dynamically | ✅ PASS |
| **TC-11** | `SeatSelectionFrame` | Locked seat rendering | Seats booked in `booking_details` are rendered 🔴 Red and `setEnabled(false)` | Booked seats cannot be clicked | ✅ PASS |
| **TC-12** | `BookingDAO` | Atomic reservation transaction | Inserts header in `bookings` and line items in `booking_details` in one transaction | Fully committed atomically | ✅ PASS |
| **TC-13** | `BookingDAO` | Seat collision (double-booking race condition) | Second user trying to book taken seat triggers rollback & error message | Rollback executed; seat kept safe | ✅ PASS |
| **TC-14** | `PaymentDialog` | Strategy Pattern switching | Allows choosing between Card and UPI; validates credentials and saves payment | Delegated to selected strategy | ✅ PASS |
| **TC-15** | `PaymentDialog` | Cancel payment without paying | Closing payment window triggers `PaymentDAO.releaseBooking()`, freeing seats | Seats freed in database | ✅ PASS |
| **TC-16** | `TicketReceiptDialog` | Ticket generation & text export | Displays stylized digital ticket with booking reference; exports to text file | Generated ticket & exported text | ✅ PASS |
| **TC-17** | `BookingHistoryFrame` | Booking cancellation | Calls `PaymentDAO.cancelBooking()`, updates status to `CANCELLED`, deletes details | Booking cancelled and seats freed | ✅ PASS |
| **TC-18** | `AdminDashboardFrame` | Session logout | Clicking *"Logout"* clears `SessionManager`, disposes admin frame, returns to `LoginFrame` | Returned to LoginFrame safely | ✅ PASS |

---

## 3. Defects Identified and Corrected by Member 6

### Bug #1: Singleton Connection Auto-Close Defect
- **Severity:** Critical (Blocker)
- **Affected Files:** `src/dao/MovieDAO.java`, `src/dao/ShowDAO.java`, `src/dao/ScreenDAO.java`
- **Root Cause:** DAOs placed the singleton connection in try-with-resources:
  ```java
  // BROKEN: Closes the singleton connection!
  try (Connection c = DatabaseConnection.getConnection();
       PreparedStatement ps = c.prepareStatement(sql)) { ... }
  ```
- **Correction Applied:**
  ```java
  // FIXED: Connection is kept open; only PreparedStatement & ResultSet are auto-closed
  Connection c = DatabaseConnection.getConnection();
  try (PreparedStatement ps = c.prepareStatement(sql)) { ... }
  ```

### Bug #2: AutoCommit State Leak in Transactions
- **Severity:** High
- **Affected Files:** `src/dao/ScreenDAO.java`
- **Root Cause:** When `insert()` or `delete()` called `c.setAutoCommit(false)`, if an exception occurred or after committing, `c.setAutoCommit(true)` was never restored, corrupting subsequent non-transactional queries.
- **Correction Applied:** Wrapped all transaction logic in `try { ... } finally { c.setAutoCommit(oldAutoCommit); }`.

### Bug #3: Stale Database Socket Reconnect Failure
- **Severity:** Medium
- **Affected Files:** `src/config/DatabaseConnection.java`
- **Root Cause:** `getConnection()` only verified `if (connection == null)`. If a database connection closed or timed out, subsequent requests received a dead connection.
- **Correction Applied:** Updated check to `if (connection == null || connection.isClosed())` and added automated property file resolution for `db.properties`.

### Bug #4: Hardcoded User Reflection in UI Frames
- **Severity:** Medium
- **Affected Files:** `src/views/CustomerHomeFrame.java`, `src/views/ShowSelectionDialog.java`, `src/Main.java`
- **Root Cause:** Earlier drafts used Java Reflection (`Class.forName(...)`) to prevent build breaks while awaiting other branches.
- **Correction Applied:** Upgraded all views to direct, strongly typed Java references with full compile-time validation.

---

## 4. Live Verification Checklist (Manual Sign-Off)

Conducted and verified on local environment with active MySQL database instance:

| # | Test Scenario / Check | Result | Verification Notes |
|:---:|---|:---:|---|
| 1 | **Card payment + receipt** | ✅ Pass | Card payment processed, transaction saved, digital receipt displayed & exportable |
| 2 | **Seat selection (2 seats = Rs. 300)** | ✅ Pass | Live seat toggling correctly calculated total dynamic ticket price |
| 3 | **Payment validation popup** | ✅ Pass | Form field validation alerts triggered on invalid or missing payment inputs |
| 4 | **My Bookings** | ✅ Pass | Historical customer bookings rendered accurately with show and seat details |
| 5 | **UPI payment** | ✅ Pass | UPI strategy executed and verified with reference ID generation |
| 6 | **Inception (no shows)** | ✅ Pass | Handled movies with zero active showtimes cleanly without runtime exceptions |
| 7 | **Double-booking blocked** | ✅ Pass | Concurrent / duplicate seat selection rejected and integrity maintained |
| 8 | **Bookings isolated per user account** | ✅ Pass | Session isolation verified; users only see their own respective booking history |
| 9 | **Data in MySQL** | ✅ Pass | All records confirmed persisted into relational tables (`bookings`, `booking_details`, `payments`) |

---

## 5. Final QA Conclusion
All 6 modules now operate in complete harmony. Compilation was verified with OpenJDK with **zero warnings and zero errors**. All manual and integration test scenarios have passed sign-off. The application is verified ready for deployment, grading, and presentation.
