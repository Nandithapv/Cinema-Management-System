"""
Generate clean, modern, professional diagram PNGs for Cinema Management System:
- docs/ER_Diagram.png
- docs/Class_Diagram.png
"""
from PIL import Image, ImageDraw, ImageFont
import os

os.makedirs("docs", exist_ok=True)

def get_font(size, bold=False):
    # Try common Windows TrueType fonts
    font_names = ["segoeuib.ttf" if bold else "segoeui.ttf",
                  "arialbd.ttf" if bold else "arial.ttf",
                  "calibrib.ttf" if bold else "calibri.ttf"]
    for fn in font_names:
        try:
            return ImageFont.truetype(fn, size)
        except Exception:
            continue
    return ImageFont.load_default()

# -------------------------------------------------------------
# 1. ER DIAGRAM GENERATION
# -------------------------------------------------------------
def generate_er_diagram():
    width, height = 1600, 1050
    img = Image.new("RGB", (width, height), color=(18, 20, 28))
    draw = ImageDraw.Draw(img)

    title_font = get_font(32, bold=True)
    sub_font = get_font(16, bold=False)
    hdr_font = get_font(18, bold=True)
    txt_font = get_font(14, bold=False)
    pk_font = get_font(14, bold=True)

    # Title Banner
    draw.text((50, 30), "ENTITY-RELATIONSHIP (ER) DIAGRAM", fill=(229, 9, 20), font=title_font)
    draw.text((50, 72), "Cinema Management System — Database Architecture (8 Normalized Relational Tables)", fill=(160, 163, 177), font=sub_font)
    draw.line([(50, 100), (width - 50, 100)], fill=(45, 48, 66), width=2)

    # Define tables: (x, y, w, h, name, fields[(name, type, key)])
    tables = [
        # Col 1
        (60, 140, 320, 260, "users", [
            ("user_id", "INT", "PK"),
            ("name", "VARCHAR(100)", ""),
            ("email", "VARCHAR(100)", "UK"),
            ("password", "VARCHAR(255)", ""),
            ("phone", "VARCHAR(15)", ""),
            ("role", "VARCHAR(20)", "CHK"),
            ("created_at", "DATETIME", "")
        ]),
        (60, 460, 320, 260, "movies", [
            ("movie_id", "INT", "PK"),
            ("title", "VARCHAR(150)", ""),
            ("genre", "VARCHAR(50)", ""),
            ("duration", "INT", ""),
            ("language", "VARCHAR(50)", ""),
            ("poster_path", "VARCHAR(255)", ""),
            ("is_active", "BOOLEAN", "")
        ]),
        (60, 780, 320, 190, "screens", [
            ("screen_id", "INT", "PK"),
            ("screen_name", "VARCHAR(50)", "UK"),
            ("capacity", "INT", "")
        ]),

        # Col 2
        (460, 460, 330, 240, "shows", [
            ("show_id", "INT", "PK"),
            ("movie_id", "INT", "FK"),
            ("screen_id", "INT", "FK"),
            ("show_date", "DATE", ""),
            ("show_time", "TIME", ""),
            ("ticket_price", "DECIMAL(10,2)", "")
        ]),
        (460, 780, 330, 190, "seats", [
            ("seat_id", "INT", "PK"),
            ("screen_id", "INT", "FK"),
            ("seat_number", "VARCHAR(10)", "UK (scr,num)")
        ]),

        # Col 3
        (870, 250, 330, 240, "bookings", [
            ("booking_id", "INT", "PK"),
            ("user_id", "INT", "FK"),
            ("show_id", "INT", "FK"),
            ("booking_date", "DATETIME", ""),
            ("total_amount", "DECIMAL(10,2)", ""),
            ("status", "VARCHAR(20)", "CHK")
        ]),
        (870, 580, 330, 260, "booking_details", [
            ("booking_detail_id", "INT", "PK"),
            ("booking_id", "INT", "FK"),
            ("show_id", "INT", "FK"),
            ("seat_id", "INT", "FK"),
            ("price", "DECIMAL(10,2)", ""),
            ("status", "VARCHAR(20)", "CHK")
        ]),

        # Col 4
        (1260, 250, 280, 240, "payments", [
            ("payment_id", "INT", "PK"),
            ("booking_id", "INT", "FK"),
            ("method", "VARCHAR(20)", "CHK"),
            ("amount", "DECIMAL(10,2)", ""),
            ("transaction_ref", "VARCHAR(100)", "UK"),
            ("payment_date", "DATETIME", "")
        ])
    ]

    # Draw Tables
    for x, y, w, h, name, fields in tables:
        # Table Outer Box
        draw.rounded_rectangle([x, y, x + w, y + h], radius=8, fill=(28, 30, 42), outline=(55, 60, 82), width=2)
        # Header Box
        draw.rounded_rectangle([x, y, x + w, y + 40], radius=8, fill=(38, 42, 60))
        draw.rectangle([x, y + 25, x + w, y + 40], fill=(38, 42, 60)) # flatten bottom corners of header
        draw.text((x + 14, y + 10), name.upper(), fill=(245, 197, 24), font=hdr_font)

        # Fields
        cur_y = y + 50
        for fname, ftype, key in fields:
            # Key tag
            if key == "PK":
                draw.text((x + 14, cur_y), "[PK]", fill=(245, 197, 24), font=pk_font)
            elif key == "FK":
                draw.text((x + 14, cur_y), "[FK]", fill=(52, 152, 219), font=pk_font)
            elif key.startswith("UK"):
                draw.text((x + 14, cur_y), "[UK]", fill=(46, 204, 113), font=pk_font)
            else:
                draw.text((x + 14, cur_y), "    ", fill=(160, 163, 177), font=txt_font)

            draw.text((x + 60, cur_y), fname, fill=(240, 240, 240), font=txt_font)
            # align type right
            draw.text((x + w - 110, cur_y), ftype, fill=(150, 155, 175), font=txt_font)
            cur_y += 26

    # Draw Relationship Connectors
    arrows = [
        # (x1, y1, x2, y2, label)
        # users -> bookings
        (380, 260, 870, 310, "1 : M  (places)"),
        # movies -> shows
        (380, 560, 460, 560, "1 : M  (features)"),
        # screens -> shows
        (380, 840, 460, 600, "1 : M  (hosts)"),
        # screens -> seats
        (380, 880, 460, 880, "1 : M  (contains 60)"),
        # shows -> bookings
        (790, 560, 870, 400, "1 : M  (reserves)"),
        # shows -> booking_details
        (790, 620, 870, 660, "1 : M  (targets)"),
        # bookings -> booking_details
        (1035, 490, 1035, 580, "1 : M  (items)"),
        # seats -> booking_details
        (790, 850, 870, 750, "1 : M  (reserves)"),
        # bookings -> payments
        (1200, 350, 1260, 350, "1 : 1  (settles)")
    ]

    for x1, y1, x2, y2, lbl in arrows:
        draw.line([(x1, y1), (x2, y2)], fill=(229, 9, 20), width=2)
        mx, my = (x1 + x2) // 2, (y1 + y2) // 2
        draw.rectangle([mx - 40, my - 10, mx + 40, my + 10], fill=(24, 25, 36))
        draw.text((mx - 36, my - 8), lbl, fill=(245, 197, 24), font=get_font(11, bold=True))

    img.save("docs/ER_Diagram.png", "PNG")
    print("Generated docs/ER_Diagram.png successfully.")

# -------------------------------------------------------------
# 2. CLASS DIAGRAM GENERATION
# -------------------------------------------------------------
def generate_class_diagram():
    width, height = 1600, 1150
    img = Image.new("RGB", (width, height), color=(18, 20, 28))
    draw = ImageDraw.Draw(img)

    title_font = get_font(32, bold=True)
    sub_font = get_font(16, bold=False)
    layer_font = get_font(20, bold=True)
    hdr_font = get_font(16, bold=True)
    code_font = get_font(13, bold=False)

    # Title Banner
    draw.text((50, 30), "UML CLASS DIAGRAM (MVC & DESIGN PATTERNS)", fill=(229, 9, 20), font=title_font)
    draw.text((50, 72), "Cinema Management System — 3-Tier Layered Architecture with Design Patterns", fill=(160, 163, 177), font=sub_font)
    draw.line([(50, 100), (width - 50, 100)], fill=(45, 48, 66), width=2)

    # Layer Headers
    layers = [
        (60, 120, "VIEW LAYER (Presentation & UI)", (229, 9, 20)),
        (60, 480, "SERVICE & CONTROLLER LAYER (Business Logic & Session)", (52, 152, 219)),
        (60, 780, "MODEL & DATA ACCESS LAYER (Domain Entities & JDBC)", (46, 204, 113))
    ]
    for lx, ly, ltitle, col in layers:
        draw.text((lx, ly), ltitle, fill=col, font=layer_font)
        draw.line([(lx, ly + 28), (lx + 600, ly + 28)], fill=col, width=1)

    # Class Boxes: (x, y, w, h, title, stereotype, methods)
    classes = [
        # View Layer
        (60, 170, 270, 260, "LoginFrame", "<<JFrame>>", [
            "- emailField : JTextField",
            "- passwordField : JPasswordField",
            "- userDAO : UserDAO",
            "+ handleLogin() : void",
            "+ routeUser(User) : void",
            "+ openRegisterFrame() : void"
        ]),
        (360, 170, 310, 260, "CustomerHomeFrame", "<<JFrame>>", [
            "- currentUserId : int",
            "- currentUserName : String",
            "- allMovies : List<Movie>",
            "+ loadMovies() : void",
            "+ applyFilter() : void",
            "+ onMovieBookClicked(...) : void",
            "+ openBookingHistory() : void",
            "+ handleLogout() : void"
        ]),
        (700, 170, 270, 260, "ShowSelectionDialog", "<<JDialog>>", [
            "- movieId : int",
            "- currentUser : User",
            "- showsList : List<Show>",
            "+ loadShows() : void",
            "+ handleProceed() : void",
            "--> opens SeatSelectionFrame"
        ]),
        (1000, 170, 270, 260, "SeatSelectionFrame", "<<JFrame>>", [
            "- show : Show",
            "- user : User",
            "- selectedSeats : List<Seat>",
            "+ loadSeatsFromDatabase()",
            "+ handleBookingProcess()",
            "--> opens PaymentDialog"
        ]),
        (1300, 170, 240, 260, "PaymentDialog", "<<JDialog>>", [
            "- bookingId : int",
            "- amount : double",
            "- isPaid : boolean",
            "+ onPay() : void",
            "--> uses PaymentStrategy",
            "--> opens TicketReceiptDialog"
        ]),

        # Services Layer
        (60, 530, 270, 200, "SessionManager", "<<Singleton / Static>>", [
            "- currentUser : User",
            "+ loginUser(User) : void",
            "+ logoutUser() : void",
            "+ getCurrentUser() : User",
            "+ isLoggedIn() : boolean"
        ]),
        (360, 530, 310, 200, "Theme", "<<Utility / Styling>>", [
            "+ PRIMARY : Color",
            "+ BG_DARK : Color",
            "+ ACCENT_GOLD : Color",
            "+ stylePrimaryButton(btn)",
            "+ styleSecondaryButton(btn)",
            "+ applyDarkTheme(frame)"
        ]),
        (700, 530, 270, 200, "MovieCardPanel", "<<JPanel / Component>>", [
            "- movieId : int",
            "- title : String",
            "- genre : String",
            "+ initUI() : void",
            "+ createPosterComponent()"
        ]),
        (1000, 530, 270, 200, "PaymentStrategy", "<<Interface>>", [
            "+ pay(double) : boolean",
            "+ getMethodName() : String",
            "",
            "Implementations:",
            "  * CardPayment",
            "  * UPIPayment"
        ]),
        (1300, 530, 240, 200, "TicketReceiptDialog", "<<JDialog>>", [
            "- bookingId : int",
            "- summary : Map",
            "+ printTicket()",
            "+ saveAsTextFile()"
        ]),

        # Model & DAO Layer
        (60, 830, 270, 270, "User / Customer / Admin", "<<Abstract Model>>", [
            "# userId : int",
            "# name : String",
            "# email : String",
            "# role : String",
            "+ Customer extends User",
            "+ Admin extends User",
            "(Inheritance & Encapsulation)"
        ]),
        (360, 830, 310, 270, "Movie & Show", "<<Domain Models>>", [
            "Movie (id, title, genre, dur, poster)",
            "Show (id, movie, screen, date, time, price)",
            "",
            "* Aggregation: Show aggregates",
            "  Movie and Screen objects"
        ]),
        (700, 830, 270, 270, "Booking & Seat", "<<Domain Models>>", [
            "Seat (id, screenId, seatNumber)",
            "Booking (id, userId, showId, status)",
            "BookingDetail (detailId, seatId, price)",
            "",
            "* Atomic JDBC Transactions"
        ]),
        (1000, 830, 270, 270, "DAOs (JDBC)", "<<Data Access Objects>>", [
            "BaseDAO<T> (Generic Interface)",
            "- UserDAO",
            "- MovieDAO (Overloaded getMovies)",
            "- ShowDAO",
            "- BookingDAO",
            "- PaymentDAO"
        ]),
        (1300, 830, 240, 270, "DatabaseConnection", "<<Singleton Pattern>>", [
            "- connection : Connection",
            "- DatabaseConnection()",
            "+ getConnection() : Connection",
            "",
            "* Shared Single Connection",
            "* Reads db.properties"
        ])
    ]

    for x, y, w, h, name, stype, methods in classes:
        draw.rounded_rectangle([x, y, x + w, y + h], radius=6, fill=(28, 30, 42), outline=(55, 60, 82), width=1)
        draw.rounded_rectangle([x, y, x + w, y + 42], radius=6, fill=(38, 42, 60))
        draw.rectangle([x, y + 25, x + w, y + 42], fill=(38, 42, 60))

        draw.text((x + 10, y + 6), name, fill=(245, 197, 24), font=hdr_font)
        draw.text((x + 10, y + 24), stype, fill=(160, 163, 177), font=get_font(11, bold=False))

        cur_y = y + 50
        for m in methods:
            if m.startswith("-->"):
                col = (245, 197, 24)
            elif m.startswith("*"):
                col = (46, 204, 113)
            elif m.startswith("+"):
                col = (235, 235, 235)
            elif m.startswith("-") or m.startswith("#"):
                col = (180, 185, 205)
            else:
                col = (160, 163, 177)
            draw.text((x + 10, cur_y), m, fill=col, font=code_font)
            cur_y += 20

    img.save("docs/Class_Diagram.png", "PNG")
    print("Generated docs/Class_Diagram.png successfully.")

if __name__ == "__main__":
    generate_er_diagram()
    generate_class_diagram()
