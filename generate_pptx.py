import os
import sys
from pptx import Presentation
from pptx.util import Inches, Pt
from pptx.dml.color import RGBColor
from pptx.enum.text import PP_ALIGN, MSO_ANCHOR
from pptx.enum.shapes import MSO_SHAPE

def create_deck():
    prs = Presentation()
    # 16:9 Widescreen layout
    prs.slide_width = Inches(13.333)
    prs.slide_height = Inches(7.5)
    blank_layout = prs.slide_layouts[6]

    # Color Palette
    NAVY = RGBColor(27, 42, 74)       # #1B2A4A
    NAVY_LIGHT = RGBColor(45, 67, 115) # #2D4373
    TEAL = RGBColor(0, 137, 123)      # #00897B
    TEAL_LIGHT = RGBColor(77, 182, 172) # #4DB6AC
    WHITE = RGBColor(255, 255, 255)
    BG_LIGHT = RGBColor(245, 247, 250)
    TEXT_DARK = RGBColor(25, 35, 55)
    TEXT_MUTED = RGBColor(100, 110, 125)
    CARD_BG = RGBColor(255, 255, 255)
    CARD_BORDER = RGBColor(220, 226, 235)
    GREEN = RGBColor(46, 125, 50)
    RED = RGBColor(198, 40, 40)

    def add_header(slide, title_text, category_text="BLUEPROOF ATTENDANCE PLATFORM"):
        # Header banner
        header_box = slide.shapes.add_textbox(Inches(0.8), Inches(0.4), Inches(11.7), Inches(1.1))
        tf = header_box.text_frame
        tf.word_wrap = True
        tf.margin_left = tf.margin_top = tf.margin_right = tf.margin_bottom = 0
        
        p_cat = tf.paragraphs[0]
        p_cat.text = category_text.upper()
        p_cat.font.size = Pt(11)
        p_cat.font.bold = True
        p_cat.font.color.rgb = TEAL

        p_title = tf.add_paragraph()
        p_title.text = title_text
        p_title.font.size = Pt(24)
        p_title.font.bold = True
        p_title.font.color.rgb = NAVY

    def set_slide_background(slide, color):
        background = slide.background
        fill = background.fill
        fill.solid()
        fill.fore_color.rgb = color

    # =========================================================================
    # SLIDE 1: Title Slide (Dark Theme)
    # =========================================================================
    s1 = prs.slides.add_slide(blank_layout)
    set_slide_background(s1, NAVY)

    # Accent decorative bar
    bar = s1.shapes.add_shape(MSO_SHAPE.RECTANGLE, Inches(0.8), Inches(1.2), Inches(0.15), Inches(4.8))
    bar.fill.solid()
    bar.fill.fore_color.rgb = TEAL
    bar.line.fill.background()

    # Title box
    tbox = s1.shapes.add_textbox(Inches(1.2), Inches(1.2), Inches(11.0), Inches(5.0))
    tf1 = tbox.text_frame
    tf1.word_wrap = True

    p = tf1.paragraphs[0]
    p.text = "BLUEPROOF"
    p.font.size = Pt(46)
    p.font.bold = True
    p.font.color.rgb = WHITE

    p2 = tf1.add_paragraph()
    p2.text = "Decentralized, Offline-First BLE Attendance System"
    p2.font.size = Pt(26)
    p2.font.color.rgb = TEAL_LIGHT
    p2.space_before = Pt(8)

    p3 = tf1.add_paragraph()
    p3.text = "Military-Grade Anti-Proxy Safeguards • Hardware Device Exclusivity • Zero Pairing Required"
    p3.font.size = Pt(16)
    p3.font.color.rgb = RGBColor(190, 205, 230)
    p3.space_before = Pt(16)

    p4 = tf1.add_paragraph()
    p4.text = "Platform: Native Android (Kotlin + Jetpack Compose) | Architecture: Room SQLite + BLE GATT\nGitHub: https://github.com/keshav2608kumar/BlueProof\nStatus: 55/55 Automated Tests Passing | Direct APK Compiled"
    p4.font.size = Pt(13)
    p4.font.color.rgb = RGBColor(160, 180, 210)
    p4.space_before = Pt(36)

    # =========================================================================
    # SLIDE 2: Problem Statement
    # =========================================================================
    s2 = prs.slides.add_slide(blank_layout)
    set_slide_background(s2, BG_LIGHT)
    add_header(s2, "The Critical Flaws in Existing Attendance Systems", "THE PROBLEM")

    cards_data = [
        ("Paper Roster", "Proxy Signatures", "Students sign for absent peers. Sheets are easily lost, damaged, or manipulated. Manual tallying wastes 10-15 minutes of every single lecture.", RED),
        ("QR Code Scanners", "Instant WhatsApp Proxy", "A single student takes a photo of the QR code and texts it into a group chat. Absent students scan it from their dorm beds across campus.", RED),
        ("Biometric Terminals", "Expensive & Slow", "Causes 15-minute queues at classroom doors. Fingerprint sensors suffer hygiene issues and hardware breakdowns. Costs $500–$2,000 per room.", RED),
        ("GPS Geofencing", "Spoofed by Apps", "GPS spoofing apps easily mock coordinates. Furthermore, GPS cannot determine altitude or vertical indoor floors in multi-story academic buildings.", RED),
    ]

    for i, (title, subtitle, desc, col) in enumerate(cards_data):
        x = Inches(0.8 + i * 2.98)
        y = Inches(1.8)
        w = Inches(2.8)
        h = Inches(5.0)

        card = s2.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, x, y, w, h)
        card.fill.solid()
        card.fill.fore_color.rgb = CARD_BG
        card.line.color.rgb = CARD_BORDER
        card.line.width = Pt(1)

        ctf = card.text_frame
        ctf.word_wrap = True
        ctf.margin_left = ctf.margin_right = Inches(0.25)
        ctf.margin_top = Inches(0.3)

        cp1 = ctf.paragraphs[0]
        cp1.text = title
        cp1.font.size = Pt(18)
        cp1.font.bold = True
        cp1.font.color.rgb = NAVY

        cp2 = ctf.add_paragraph()
        cp2.text = f"FAIL: {subtitle}"
        cp2.font.size = Pt(12)
        cp2.font.bold = True
        cp2.font.color.rgb = col
        cp2.space_before = Pt(6)

        cp3 = ctf.add_paragraph()
        cp3.text = desc
        cp3.font.size = Pt(13)
        cp3.font.color.rgb = TEXT_MUTED
        cp3.space_before = Pt(14)

    # =========================================================================
    # SLIDE 3: The BlueProof Solution
    # =========================================================================
    s3 = prs.slides.add_slide(blank_layout)
    set_slide_background(s3, BG_LIGHT)
    add_header(s3, "The BlueProof Breakthrough: Offline-First BLE Direct Radio", "THE SOLUTION")

    sol_cards = [
        ("100% Offline-First", "Direct Phone-to-Phone", "No Wi-Fi, no mobile data, and no cloud dependency required. Attendance operates over direct 2.4 GHz Bluetooth radio frequencies between teacher and student phones."),
        ("Sub-2-Second Verification", "Zero Bluetooth Pairing", "Students do not need to manually pair via Bluetooth settings. BlueProof utilizes transient BLE GATT read/write operations to log check-ins in under two seconds."),
        ("Hardened Anti-Proxy", "Physical Hardware Binding", "Enforces Hardware Device Exclusivity. One physical device can only punch attendance for one student per session, completely eliminating buddy punching."),
        ("Zero Extra Cost", "Runs on Existing Phones", "Requires zero expensive biometric terminals or RFID scanners. Works natively on standard Android smartphones using their onboard BLE antennas."),
    ]

    for i, (title, tag, desc) in enumerate(sol_cards):
        col_idx = i % 2
        row_idx = i // 2
        x = Inches(0.8 + col_idx * 5.95)
        y = Inches(1.8 + row_idx * 2.55)
        w = Inches(5.75)
        h = Inches(2.3)

        card = s3.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, x, y, w, h)
        card.fill.solid()
        card.fill.fore_color.rgb = CARD_BG
        card.line.color.rgb = CARD_BORDER

        ctf = card.text_frame
        ctf.word_wrap = True
        ctf.margin_left = ctf.margin_right = Inches(0.3)
        ctf.margin_top = Inches(0.25)

        cp1 = ctf.paragraphs[0]
        cp1.text = title
        cp1.font.size = Pt(19)
        cp1.font.bold = True
        cp1.font.color.rgb = NAVY

        cp2 = ctf.add_paragraph()
        cp2.text = tag
        cp2.font.size = Pt(12)
        cp2.font.bold = True
        cp2.font.color.rgb = TEAL
        cp2.space_before = Pt(4)

        cp3 = ctf.add_paragraph()
        cp3.text = desc
        cp3.font.size = Pt(13)
        cp3.font.color.rgb = TEXT_MUTED
        cp3.space_before = Pt(8)

    # =========================================================================
    # SLIDE 4: Architecture
    # =========================================================================
    s4 = prs.slides.add_slide(blank_layout)
    set_slide_background(s4, BG_LIGHT)
    add_header(s4, "System Architecture: Direct Radio Mesh Without Cloud", "SYSTEM DESIGN")

    # Teacher Box (Left)
    t_card = s4.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, Inches(0.8), Inches(1.8), Inches(5.6), Inches(5.0))
    t_card.fill.solid()
    t_card.fill.fore_color.rgb = CARD_BG
    t_card.line.color.rgb = NAVY
    t_card.line.width = Pt(2)
    ttf = t_card.text_frame
    ttf.word_wrap = True
    ttf.margin_left = ttf.margin_right = Inches(0.3)
    ttf.margin_top = Inches(0.3)

    tp1 = ttf.paragraphs[0]
    tp1.text = "TEACHER PHONE (Host & Verifier)"
    tp1.font.size = Pt(18)
    tp1.font.bold = True
    tp1.font.color.rgb = NAVY

    tp2 = ttf.add_paragraph()
    tp2.text = "• BleAdvertiser: Broadcasts BLE Service UUID (0000b100-...)\n• GattServer: Hosts 3 Characteristics:\n   1. SESSION_INFO (Read session ID, teacher profile, challenge)\n   2. CHECKIN (Write student verification payload)\n   3. RESPONSE (Notify immediate ACCEPTED/REJECTED confirmation)\n• SessionManager: Background timer rotating challenge every 30s\n• Mutex Concurrency: Handles 50+ students without race conditions\n• Room SQLite DB: Persists sessions, records, and audit logs"
    tp2.font.size = Pt(13)
    tp2.font.color.rgb = TEXT_DARK
    tp2.space_before = Pt(12)

    # Student Box (Right)
    s_card = s4.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, Inches(6.9), Inches(1.8), Inches(5.6), Inches(5.0))
    s_card.fill.solid()
    s_card.fill.fore_color.rgb = CARD_BG
    s_card.line.color.rgb = TEAL
    s_card.line.width = Pt(2)
    stf = s_card.text_frame
    stf.word_wrap = True
    stf.margin_left = stf.margin_right = Inches(0.3)
    stf.margin_top = Inches(0.3)

    sp1 = stf.paragraphs[0]
    sp1.text = "STUDENT PHONES (Scanner & Client)"
    sp1.font.size = Pt(18)
    sp1.font.bold = True
    sp1.font.color.rgb = TEAL

    sp2 = stf.add_paragraph()
    sp2.text = "• BleScanner: Filters for classroom Service UUID\n• Multi-Sample RSSI Buffer: Collects 5–20 signal samples\n• Proximity Analyzer: Computes median signal strength (-dBm)\n• GattClient: Connects directly to teacher without pairing\n• Challenge-Response Crypto: Calculates HMAC-SHA256 signature\n• Auto-Login Persistence: Auto-restores enrollment profile\n• Room SQLite DB: Retains personal attendance history offline"
    sp2.font.size = Pt(13)
    sp2.font.color.rgb = TEXT_DARK
    sp2.space_before = Pt(12)

    # =========================================================================
    # SLIDE 5: 5-Pillar Anti-Proxy Matrix
    # =========================================================================
    s5 = prs.slides.add_slide(blank_layout)
    set_slide_background(s5, BG_LIGHT)
    add_header(s5, "The 5-Pillar Anti-Proxy Security Matrix", "SECURITY INNOVATION")

    pillars = [
        ("1. Hardware Exclusivity", "Blocks Buddy Punching", "Bound to Settings.Secure.ANDROID_ID. One physical phone can only punch for ONE student per session. Switching student IDs on the same phone is instantly rejected.", GREEN),
        ("2. Enrolled Device Lock", "Blocks Stolen ID Abuse", "A student ID is cryptographically locked to the student's personal phone. A friend cannot borrow a roll number and punch from their own device.", GREEN),
        ("3. Multi-Sample RSSI", "Blocks Outside Punching", "Single Bluetooth signals fluctuate wildly. Taking the mathematical median of 5+ samples filters noise and blocks students outside the classroom walls.", GREEN),
        ("4. 30s Rotating Challenge", "Blocks Replay Attacks", "Teacher generates a new 32-byte SecureRandom token every 30 seconds. Sniffed BLE packets expire and become worthless within 30 seconds.", GREEN),
        ("5. HMAC-SHA256 Auth", "Zero Secret Exposure", "The student's 256-bit secret key is never sent over the air. The student sends a digital signature: HMAC(challenge + studentId + timestamp).", GREEN),
    ]

    for i, (title, sub, desc, col) in enumerate(pillars):
        x = Inches(0.8 + i * 2.38)
        y = Inches(1.8)
        w = Inches(2.25)
        h = Inches(5.0)

        card = s5.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, x, y, w, h)
        card.fill.solid()
        card.fill.fore_color.rgb = CARD_BG
        card.line.color.rgb = CARD_BORDER

        ctf = card.text_frame
        ctf.word_wrap = True
        ctf.margin_left = ctf.margin_right = Inches(0.2)
        ctf.margin_top = Inches(0.25)

        cp1 = ctf.paragraphs[0]
        cp1.text = title
        cp1.font.size = Pt(15)
        cp1.font.bold = True
        cp1.font.color.rgb = NAVY

        cp2 = ctf.add_paragraph()
        cp2.text = sub
        cp2.font.size = Pt(11)
        cp2.font.bold = True
        cp2.font.color.rgb = col
        cp2.space_before = Pt(4)

        cp3 = ctf.add_paragraph()
        cp3.text = desc
        cp3.font.size = Pt(12)
        cp3.font.color.rgb = TEXT_MUTED
        cp3.space_before = Pt(10)

    # =========================================================================
    # SLIDE 6: Lost / Replaced Phone Handling
    # =========================================================================
    s6 = prs.slides.add_slide(blank_layout)
    set_slide_background(s6, BG_LIGHT)
    add_header(s6, "Edge Case Solution: Lost & Replaced Student Phones", "PRACTICAL WORKFLOW")

    steps_data = [
        ("Step 1: Student Gets New Phone", "Student installs BlueProof on their new phone, registers their existing Student ID and Name. A new hardware ID is generated locally."),
        ("Step 2: Initial Check-In Blocked", "Upon check-in, teacher's phone detects hardware ID mismatch against the previous device. System protects account by initially rejecting."),
        ("Step 3: Teacher Taps 'Reset Lock'", "Teacher opens Teacher Dashboard -> Taps 'Enrolled (X)' card -> Finds student -> Taps 'Reset Lock'. The old hardware lock is cleared in 2 taps."),
        ("Step 4: Seamless Re-Binding", "Student retries check-in. SessionManager recognizes the cleared lock, binds the new phone's hardware ID, and marks student ACCEPTED!"),
    ]

    for i, (title, desc) in enumerate(steps_data):
        y = Inches(1.8 + i * 1.25)
        box = s6.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, Inches(0.8), y, Inches(11.7), Inches(1.05))
        box.fill.solid()
        box.fill.fore_color.rgb = CARD_BG
        box.line.color.rgb = CARD_BORDER

        btf = box.text_frame
        btf.word_wrap = True
        btf.margin_left = Inches(0.3)
        btf.margin_top = Inches(0.15)

        bp1 = btf.paragraphs[0]
        bp1.text = title
        bp1.font.size = Pt(16)
        bp1.font.bold = True
        bp1.font.color.rgb = NAVY

        bp2 = btf.add_paragraph()
        bp2.text = desc
        bp2.font.size = Pt(13)
        bp2.font.color.rgb = TEXT_MUTED
        bp2.space_before = Pt(2)

    # =========================================================================
    # SLIDE 7: Tech Stack
    # =========================================================================
    s7 = prs.slides.add_slide(blank_layout)
    set_slide_background(s7, BG_LIGHT)
    add_header(s7, "Comprehensive Technology Stack", "DEVELOPMENT STACK")

    tech_categories = [
        ("UI & Rendering", ["Kotlin 1.9.22 (100% Native)", "Jetpack Compose BOM 2024.02", "Material Design 3 Components", "Material Icons Extended"]),
        ("Architecture & State", ["MVVM Architecture Pattern", "Kotlin Coroutines 1.7.3", "StateFlow Reactive Streams", "Lifecycle Runtime Compose"]),
        ("Database & Persistence", ["Room Database 2.6.1 (SQLite)", "Kotlin Symbol Processing (KSP)", "Composite Unique Indices", "SharedPrefs for Profiles"]),
        ("Hardware & Security", ["Android BLE APIs (Adv/Scan)", "GATT Server & Client Handshake", "HMAC-SHA256 Cryptography", "SecureRandom 32-Byte Entropy"]),
        ("Reporting & Optional Cloud", ["Custom Compose Canvas Charts", "RFC 4180 CSV Export Engine", "FastAPI + Uvicorn Cloud Server", "SQLAlchemy + Pydantic v2"]),
    ]

    for i, (cat, items) in enumerate(tech_categories):
        x = Inches(0.8 + i * 2.38)
        y = Inches(1.8)
        w = Inches(2.25)
        h = Inches(5.0)

        card = s7.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, x, y, w, h)
        card.fill.solid()
        card.fill.fore_color.rgb = CARD_BG
        card.line.color.rgb = CARD_BORDER

        ctf = card.text_frame
        ctf.word_wrap = True
        ctf.margin_left = ctf.margin_right = Inches(0.2)
        ctf.margin_top = Inches(0.25)

        cp1 = ctf.paragraphs[0]
        cp1.text = cat
        cp1.font.size = Pt(16)
        cp1.font.bold = True
        cp1.font.color.rgb = NAVY

        for item in items:
            p = ctf.add_paragraph()
            p.text = f"• {item}"
            p.font.size = Pt(12)
            p.font.color.rgb = TEXT_DARK
            p.space_before = Pt(8)

    # =========================================================================
    # SLIDE 8: Handshake Sequence
    # =========================================================================
    s8 = prs.slides.add_slide(blank_layout)
    set_slide_background(s8, BG_LIGHT)
    add_header(s8, "Cryptographic Handshake Sequence Protocol", "NETWORK PROTOCOL")

    seq_steps = [
        ("1. Broadcast Beacon", "Teacher phone broadcasts BLE packet containing Service UUID and hashed Session ID."),
        ("2. Proximity Scan", "Student BleScanner discovers beacon, buffers 5+ RSSI samples, and verifies median >= -75 dBm."),
        ("3. Transient GATT Link", "Student phone initiates direct radio connection to Teacher GATT Server (zero pairing needed)."),
        ("4. Read Session Info", "Student reads SESSION_INFO characteristic: receives session ID, rotating challenge, teacher profile."),
        ("5. HMAC Signature", "Student computes HMAC-SHA256(challenge + studentId + timestamp, secretKey) on-device."),
        ("6. Submit Check-in", "Student writes JSON payload to CHECKIN characteristic with signature, deviceId, and RSSI samples."),
        ("7. 5-Tier Verification", "Teacher SessionManager verifies duplicate, phone reuse, device lock, 30s expiry, and signature."),
        ("8. Instant Notification", "Teacher writes record to SQLite, updates live UI feed, and notifies student screen: ACCEPTED!"),
    ]

    for i, (title, desc) in enumerate(seq_steps):
        col_idx = i % 2
        row_idx = i // 2
        x = Inches(0.8 + col_idx * 5.95)
        y = Inches(1.8 + row_idx * 1.25)
        w = Inches(5.75)
        h = Inches(1.05)

        box = s8.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, x, y, w, h)
        box.fill.solid()
        box.fill.fore_color.rgb = CARD_BG
        box.line.color.rgb = CARD_BORDER

        btf = box.text_frame
        btf.word_wrap = True
        btf.margin_left = Inches(0.25)
        btf.margin_top = Inches(0.12)

        bp1 = btf.paragraphs[0]
        bp1.text = title
        bp1.font.size = Pt(14)
        bp1.font.bold = True
        bp1.font.color.rgb = NAVY

        bp2 = btf.add_paragraph()
        bp2.text = desc
        bp2.font.size = Pt(11)
        bp2.font.color.rgb = TEXT_MUTED
        bp2.space_before = Pt(2)

    # =========================================================================
    # SLIDE 9: Database Schema
    # =========================================================================
    s9 = prs.slides.add_slide(blank_layout)
    set_slide_background(s9, BG_LIGHT)
    add_header(s9, "Database Schema & Entity Relationship Architecture", "LOCAL PERSISTENCE")

    schema_tables = [
        ("students", "Enrolled Students Table", ["id: Long (PK Autoincrement)", "studentId: String (UNIQUE Index)", "name: String (Full Student Name)", "deviceId: String (Hardware Lock)", "enrollmentKey: String (256-bit Secret)", "enrolledAt: Long (Timestamp)", "isActive: Boolean (Account Status)"]),
        ("sessions", "Class Sessions Table", ["id: Long (PK Autoincrement)", "sessionId: String (UUID Unique)", "name: String (e.g. CS101 Lecture 5)", "teacherName: String", "teacherId: String (e.g. TCH-1004)", "startTime / endTime: Long", "rssiThreshold: Int (-75 dBm)", "status: ACTIVE | PAUSED | ENDED"]),
        ("attendance_records", "Attendance Entries Table", ["id: Long (PK Autoincrement)", "studentId / sessionId: String (Indices)", "studentName: String", "checkInTime: Long", "rssiMedian: Int & rssiSamples: JSON", "deviceId: String (Phone Exclusivity)", "status: ACCEPTED | REJECTED", "UNIQUE: (studentId, sessionId)"]),
        ("audit_log", "Tamper-Evident Security Log", ["id: Long (PK Autoincrement)", "timestamp: Long", "eventType: String (CHECKIN_ACCEPTED, etc)", "sessionId: String? & studentId: String?", "details: String (JSON security metadata)", "proxyAlert: Boolean flag"]),
    ]

    for i, (title, sub, cols) in enumerate(schema_tables):
        x = Inches(0.8 + i * 2.98)
        y = Inches(1.8)
        w = Inches(2.8)
        h = Inches(5.0)

        card = s9.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, x, y, w, h)
        card.fill.solid()
        card.fill.fore_color.rgb = CARD_BG
        card.line.color.rgb = CARD_BORDER

        ctf = card.text_frame
        ctf.word_wrap = True
        ctf.margin_left = ctf.margin_right = Inches(0.2)
        ctf.margin_top = Inches(0.2)

        cp1 = ctf.paragraphs[0]
        cp1.text = title
        cp1.font.size = Pt(17)
        cp1.font.bold = True
        cp1.font.color.rgb = NAVY

        cp2 = ctf.add_paragraph()
        cp2.text = sub
        cp2.font.size = Pt(11)
        cp2.font.bold = True
        cp2.font.color.rgb = TEAL
        cp2.space_before = Pt(3)

        for col in cols:
            p = ctf.add_paragraph()
            p.text = f"• {col}"
            p.font.size = Pt(11)
            p.font.color.rgb = TEXT_DARK
            p.space_before = Pt(6)

    # =========================================================================
    # SLIDE 10: Complete UI/UX Suite
    # =========================================================================
    s10 = prs.slides.add_slide(blank_layout)
    set_slide_background(s10, BG_LIGHT)
    add_header(s10, "Complete UI/UX Suite: 11 Production-Ready Screens", "USER EXPERIENCE")

    screens_list = [
        ("WelcomeScreen", "Both", "Role selection: 'I'm a Teacher' vs 'I'm a Student' with branding."),
        ("StudentRegistrationScreen", "Student", "Roll number & Name registration with automatic local login persistence."),
        ("TeacherDashboardScreen", "Teacher", "Faculty Profile Card, Enrolled Students Manager, Quick Start button."),
        ("StartSessionScreen", "Teacher", "Duration selector, RSSI proximity slider, pre-filled editable Faculty ID."),
        ("BleScanScreen", "Student", "Radar animation, live signal strength bars, verified instructor check-in."),
        ("LiveSessionScreen", "Teacher", "Live student feed, connected device counter, countdown timer, pause/resume."),
        ("StudentDashboardScreen", "Student", "Personal greeting, today's attendance badge, history launcher."),
        ("AttendanceHistoryScreen", "Both", "Chronological session archives and historical check-in logs."),
        ("AnalyticsScreen", "Both", "Canvas bar charts, donut charts, and overall attendance rate trends."),
        ("SessionDetailsScreen", "Teacher", "Granular audit trail with one-tap CSV spreadsheet export."),
        ("SettingsScreen", "Both", "Hardware BLE diagnostics, device ID display, and database wipe."),
    ]

    for i, (title, role, desc) in enumerate(screens_list):
        col_idx = i // 6
        row_idx = i % 6
        x = Inches(0.8 + col_idx * 5.95)
        y = Inches(1.8 + row_idx * 0.85)
        w = Inches(5.75)
        h = Inches(0.75)

        box = s10.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, x, y, w, h)
        box.fill.solid()
        box.fill.fore_color.rgb = CARD_BG
        box.line.color.rgb = CARD_BORDER

        btf = box.text_frame
        btf.word_wrap = True
        btf.margin_left = Inches(0.2)
        btf.margin_top = Inches(0.1)

        bp1 = btf.paragraphs[0]
        bp1.text = f"{title} ({role})"
        bp1.font.size = Pt(13)
        bp1.font.bold = True
        bp1.font.color.rgb = NAVY

        bp2 = btf.add_paragraph()
        bp2.text = desc
        bp2.font.size = Pt(10.5)
        bp2.font.color.rgb = TEXT_MUTED
        bp2.space_before = Pt(1)

    # =========================================================================
    # SLIDE 11: Analytics & Export
    # =========================================================================
    s11 = prs.slides.add_slide(blank_layout)
    set_slide_background(s11, BG_LIGHT)
    add_header(s11, "Built-in Analytics & RFC 4180 CSV Report Export", "REPORTING ENGINE")

    rep_cards = [
        ("Zero-Dependency Canvas Charts", "High Performance & Lightweight", "• Custom BarChart: Renders Present vs. Absent counts side-by-side.\n• Custom DonutChart: Visualizes Accepted, Rejected, and Pending distributions.\n• Custom LineChart: Plots attendance trends across recent sessions.\n• Built directly on Jetpack Compose Canvas, avoiding heavy third-party chart libraries and keeping binary size under 17 MB."),
        ("Instant RFC 4180 CSV Export", "Direct Spreadsheet Download", "• One-tap export directly on the teacher's phone.\n• Generates standardized .csv files stored in Android Downloads/Documents folder.\n• Exported Columns: Student ID, Student Name, Check-in Time, Median RSSI, Validation Status, Rejection Reason, Device ID.\n• Ready to open in Microsoft Excel, Google Sheets, or import into University ERP systems."),
    ]

    for i, (title, sub, desc) in enumerate(rep_cards):
        x = Inches(0.8 + i * 5.95)
        y = Inches(1.8)
        w = Inches(5.75)
        h = Inches(5.0)

        card = s11.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, x, y, w, h)
        card.fill.solid()
        card.fill.fore_color.rgb = CARD_BG
        card.line.color.rgb = CARD_BORDER

        ctf = card.text_frame
        ctf.word_wrap = True
        ctf.margin_left = ctf.margin_right = Inches(0.3)
        ctf.margin_top = Inches(0.3)

        cp1 = ctf.paragraphs[0]
        cp1.text = title
        cp1.font.size = Pt(18)
        cp1.font.bold = True
        cp1.font.color.rgb = NAVY

        cp2 = ctf.add_paragraph()
        cp2.text = sub
        cp2.font.size = Pt(12)
        cp2.font.bold = True
        cp2.font.color.rgb = TEAL
        cp2.space_before = Pt(4)

        cp3 = ctf.add_paragraph()
        cp3.text = desc
        cp3.font.size = Pt(13)
        cp3.font.color.rgb = TEXT_DARK
        cp3.space_before = Pt(14)

    # =========================================================================
    # SLIDE 12: QA Verification Report (55/55 Passing)
    # =========================================================================
    s12 = prs.slides.add_slide(blank_layout)
    set_slide_background(s12, BG_LIGHT)
    add_header(s12, "Automated Verification: 55 Tests Passing (100% Success)", "QUALITY ASSURANCE")

    tests_summary = [
        ("AntiProxyEndToEndTest", "11 Tests", "Buddy-punching, same-phone ID switching, stolen ID, device lock reset, session lifecycle."),
        ("SessionValidatorTest", "11 Tests", "Expired challenge, stale tokens, weak RSSI rejection, insufficient sample counts, HMAC forgery."),
        ("ChallengeGeneratorTest", "10 Tests", "32-byte cryptographic uniqueness, HMAC-SHA256 consistency, token expiration calculations."),
        ("RssiAnalyzerTest", "7 Tests", "Median calculation on odd/even samples, boundary threshold detection, signal categorization."),
        ("AttendanceCalculatorTest", "6 Tests", "Percentage calculations, duration formatters, zero-student edge cases, rate evaluations."),
        ("FastAPI Backend Test Suite", "10 Tests", "Full REST API integration, cloud anti-proxy validation, duplicate rejection, CSV streaming."),
    ]

    for i, (title, count, desc) in enumerate(tests_summary):
        col_idx = i % 2
        row_idx = i // 2
        x = Inches(0.8 + col_idx * 5.95)
        y = Inches(1.8 + row_idx * 1.65)
        w = Inches(5.75)
        h = Inches(1.45)

        card = s12.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, x, y, w, h)
        card.fill.solid()
        card.fill.fore_color.rgb = CARD_BG
        card.line.color.rgb = GREEN if "100%" in "100%" else CARD_BORDER
        card.line.width = Pt(1.5)

        ctf = card.text_frame
        ctf.word_wrap = True
        ctf.margin_left = ctf.margin_right = Inches(0.25)
        ctf.margin_top = Inches(0.18)

        cp1 = ctf.paragraphs[0]
        cp1.text = f"{title} — {count} (PASSED)"
        cp1.font.size = Pt(15)
        cp1.font.bold = True
        cp1.font.color.rgb = GREEN

        cp2 = ctf.add_paragraph()
        cp2.text = desc
        cp2.font.size = Pt(12)
        cp2.font.color.rgb = TEXT_MUTED
        cp2.space_before = Pt(4)

    # =========================================================================
    # SLIDE 13: Competitive Matrix
    # =========================================================================
    s13 = prs.slides.add_slide(blank_layout)
    set_slide_background(s13, BG_LIGHT)
    add_header(s13, "Competitive Feature & Security Matrix", "BENCHMARK COMPARISON")

    # Table
    rows = 7
    cols = 6
    left = Inches(0.8)
    top = Inches(1.8)
    width = Inches(11.7)
    height = Inches(4.9)

    table_shape = s13.shapes.add_table(rows, cols, left, top, width, height)
    tbl = table_shape.table

    tbl_data = [
        ["Feature / Metric", "Paper", "QR Code", "Biometric", "GPS App", "BlueProof (Ours)"],
        ["Infrastructure Cost", "$0", "$0", "$500 - $2,000", "$0", "$0 (No Extra HW)"],
        ["Offline Resilience", "Yes", "No", "Partial", "No", "100% Offline (BLE)"],
        ["Check-in Speed", "10-15 mins", "2-3 mins", "10-15 mins", "2-5 mins", "< 2 Seconds"],
        ["Photo-Share Proxy", "N/A", "VULNERABLE", "Protected", "Protected", "IMMUNE (Radio Link)"],
        ["Buddy-Punching", "FAILS", "FAILS", "Protected", "FAILS", "PROTECTED (Hardware ID)"],
        ["Outside Room Punch", "FAILS", "FAILS", "Protected", "FAILS", "PROTECTED (RSSI Filter)"],
    ]

    for r_idx, row in enumerate(tbl_data):
        for c_idx, cell_value in enumerate(row):
            cell = tbl.cell(r_idx, c_idx)
            cell.text = cell_value
            p = cell.text_frame.paragraphs[0]
            p.font.size = Pt(11.5)
            p.alignment = PP_ALIGN.CENTER if c_idx > 0 else PP_ALIGN.LEFT
            if r_idx == 0:
                p.font.bold = True
                p.font.color.rgb = WHITE
                cell.fill.solid()
                cell.fill.fore_color.rgb = NAVY
            elif c_idx == 5:
                p.font.bold = True
                p.font.color.rgb = GREEN
                cell.fill.solid()
                cell.fill.fore_color.rgb = RGBColor(235, 248, 238)
            else:
                p.font.color.rgb = TEXT_DARK
                cell.fill.solid()
                cell.fill.fore_color.rgb = WHITE if r_idx % 2 == 1 else BG_LIGHT

    # =========================================================================
    # SLIDE 14: Future Roadmap
    # =========================================================================
    s14 = prs.slides.add_slide(blank_layout)
    set_slide_background(s14, BG_LIGHT)
    add_header(s14, "Strategic Roadmap: From Classroom to Campus Scale", "FUTURE SCOPE")

    phases = [
        ("Phase 1: Production Core", "Current Milestone", [
            "Complete BLE direct radio handshake",
            "Hardware Device Exclusivity anti-proxy",
            "Teacher-controlled Device Lock reset",
            "Local Room SQLite persistence",
            "One-tap CSV spreadsheet export",
            "55 automated unit & integration tests"
        ]),
        ("Phase 2: Mesh & Scale", "Next Quarter", [
            "BLE Mesh Relay protocol for 500+ seat auditoriums",
            "Background attendance beacon mode",
            "Automated multi-section course grouping",
            "Low-power Bluetooth battery optimization",
            "Encrypted classroom timetable sync"
        ]),
        ("Phase 3: Campus Enterprise", "Future Scale", [
            "On-device Face/Biometric confirmation via Android BiometricPrompt",
            "Official LMS connectors (Moodle, Blackboard, Canvas)",
            "Automated attendance shortage warning emails",
            "Dean & Department Chair analytics portal"
        ]),
    ]

    for i, (title, sub, items) in enumerate(phases):
        x = Inches(0.8 + i * 3.98)
        y = Inches(1.8)
        w = Inches(3.75)
        h = Inches(5.0)

        card = s14.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, x, y, w, h)
        card.fill.solid()
        card.fill.fore_color.rgb = CARD_BG
        card.line.color.rgb = CARD_BORDER

        ctf = card.text_frame
        ctf.word_wrap = True
        ctf.margin_left = ctf.margin_right = Inches(0.25)
        ctf.margin_top = Inches(0.25)

        cp1 = ctf.paragraphs[0]
        cp1.text = title
        cp1.font.size = Pt(17)
        cp1.font.bold = True
        cp1.font.color.rgb = NAVY

        cp2 = ctf.add_paragraph()
        cp2.text = sub
        cp2.font.size = Pt(11.5)
        cp2.font.bold = True
        cp2.font.color.rgb = TEAL
        cp2.space_before = Pt(3)

        for item in items:
            p = ctf.add_paragraph()
            p.text = f"• {item}"
            p.font.size = Pt(12)
            p.font.color.rgb = TEXT_DARK
            p.space_before = Pt(8)

    # =========================================================================
    # SLIDE 15: Conclusion & Q&A (Dark Theme)
    # =========================================================================
    s15 = prs.slides.add_slide(blank_layout)
    set_slide_background(s15, NAVY)

    # Accent decorative bar
    bar15 = s15.shapes.add_shape(MSO_SHAPE.RECTANGLE, Inches(0.8), Inches(1.2), Inches(0.15), Inches(4.8))
    bar15.fill.solid()
    bar15.fill.fore_color.rgb = TEAL
    bar15.line.fill.background()

    # Text frame
    tbox15 = s15.shapes.add_textbox(Inches(1.2), Inches(1.2), Inches(11.0), Inches(5.0))
    tf15 = tbox15.text_frame
    tf15.word_wrap = True

    p = tf15.paragraphs[0]
    p.text = "Thank You!"
    p.font.size = Pt(44)
    p.font.bold = True
    p.font.color.rgb = WHITE

    p2 = tf15.add_paragraph()
    p2.text = "BlueProof — Zero Hardware. Zero Internet. Zero Proxies."
    p2.font.size = Pt(22)
    p2.font.color.rgb = TEAL_LIGHT
    p2.space_before = Pt(6)

    p3 = tf15.add_paragraph()
    p3.text = "Summary of Deliverables:\n• 100% Native Kotlin Android application with 11 polished Material 3 screens\n• Production-grade BLE Advertiser, Scanner, and GATT Server engine\n• First-in-class Hardware Device Exclusivity anti-proxy architecture\n• 55/55 Passing Automated Unit and Integration Tests\n• Live GitHub repository with ready-to-install Android APK"
    p3.font.size = Pt(14)
    p3.font.color.rgb = RGBColor(190, 205, 230)
    p3.space_before = Pt(16)

    p4 = tf15.add_paragraph()
    p4.text = "GitHub: https://github.com/keshav2608kumar/BlueProof\nDirect APK: https://github.com/keshav2608kumar/BlueProof/raw/main/dist/BlueProof-debug.apk\n\nWe are now ready for Questions & Answers."
    p4.font.size = Pt(13.5)
    p4.font.bold = True
    p4.font.color.rgb = WHITE
    p4.space_before = Pt(20)

    # Output paths
    out_dir = r"c:\Users\kesha\Desktop\HACKATHON\BlueProof"
    out_file1 = os.path.join(out_dir, "BlueProof_Presentation.pptx")
    prs.save(out_file1)
    print(f"SAVED: {out_file1}")

    # Also save directly on Desktop for immediate 1-click access
    desktop_file = r"c:\Users\kesha\Desktop\BlueProof_Presentation.pptx"
    try:
        prs.save(desktop_file)
        print(f"SAVED ON DESKTOP: {desktop_file}")
    except Exception as e:
        print(f"Could not save to desktop directly: {e}")

if __name__ == "__main__":
    create_deck()
