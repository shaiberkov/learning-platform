"""Demo data for a LOCAL database: wipes every table of the `learning` schema (except Flyway's history and the
past exams that PastExamSeeder loads) and fills it with two schools, staff, students, parents and their activity.

    python scripts/demo-seed/seed_demo_data.py --yes

Run the backend once before (so Flyway has applied every migration), stop it, run this, start it again
(the server caches would otherwise show the old data). Reads DB_PASSWORD from backend/.env and calls the
`mysql` client. Refuses any host other than localhost. Every user's password is Demo1234!; see README.md.
"""
import argparse, glob, hashlib, random, shutil, subprocess, uuid, datetime as dt, json, os, sys

random.seed(2026)
HERE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.dirname(os.path.dirname(HERE))
PASSWORD = "Demo1234!"
TODAY = dt.date.today()
NOW = dt.datetime.combine(TODAY, dt.time(8, 0))
MATH = "מתמטיקה"

sql = []
creds = []

def q(v):
    if v is None: return "NULL"
    if isinstance(v, bool): return "1" if v else "0"
    if isinstance(v, (int, float)): return str(v)
    if isinstance(v, dt.datetime): return "'" + v.strftime("%Y-%m-%d %H:%M:%S") + "'"
    if isinstance(v, dt.date): return "'" + v.isoformat() + "'"
    if isinstance(v, dt.time): return "'" + v.strftime("%H:%M:%S") + "'"
    return "'" + str(v).replace("\\", "\\\\").replace("'", "''") + "'"

rows_by_table = {}
def ins(table, **cols):
    rows_by_table.setdefault(table, []).append(cols)

def flush():
    for table, rows in rows_by_table.items():
        keys = list(rows[0].keys())
        for i in range(0, len(rows), 200):
            chunk = rows[i:i+200]
            vals = ",\n".join("(" + ",".join(q(r[k]) for k in keys) + ")" for r in chunk)
            sql.append(f"INSERT INTO `{table}` ({','.join('`'+k+'`' for k in keys)}) VALUES\n{vals};")

def il_id(prefix8):
    """Valid Israeli ID: 8 digits + check digit."""
    s = 0
    for i, ch in enumerate(prefix8):
        d = int(ch) * (1 if i % 2 == 0 else 2)
        s += d if d < 10 else d - 9
    return prefix8 + str((10 - s % 10) % 10)

# ---------------- people ----------------
FIRST_M = ["יונתן","איתי","נועם","אורי","דניאל","עומר","אריאל","יואב","איתמר","רועי","עידו","אלון","תומר","גיא","אביב","מתן","נדב","עמית","יהונתן","אלעד"]
FIRST_F = ["נועה","מאיה","תמר","שירה","יעל","אביגיל","הילה","רוני","ליה","אלה","מיכל","עדי","שני","טליה","אגם","הדר","ענבר","רותם","גלי","אורי-אל"]
LAST = ["כהן","לוי","מזרחי","פרץ","ביטון","דהן","אברהם","פרידמן","אזולאי","מלכה","חדד","גבאי","שפירא","רוזן","בן דוד","קפלן","אוחיון","ויסמן","גולן","שמעוני","טל","ברק","נחום","יוסף"]

user_seq = [100]
phone_seq = [0]
used_names = set()
def next_phone():
    phone_seq[0] += 1
    return "05" + random.choice("02348") + str(1000000 + phone_seq[0] * 7919 % 9000000).rjust(7, "0")

def make_user(role, user_id, name, email, extra_note=""):
    user_seq[0] += 1
    pk = user_seq[0]
    salt = str(uuid.UUID(int=random.getrandbits(128)))
    h = hashlib.sha256((PASSWORD + salt).encode("utf-8")).hexdigest().upper()
    ins("user_entity", id=pk, email=email, otp=None, otp_timestamp=None, password_hash=h,
        phone_number=next_phone(), profile_picture=None, role=role, salt=salt, user_id=user_id, username=name)
    creds.append(dict(role=role, name=name, user_id=user_id, email=email, note=extra_note))
    return pk

def rand_name(gender=None):
    while True:
        g = gender or random.choice("MF")
        f = random.choice(FIRST_M if g == "M" else FIRST_F)
        n = f"{f} {random.choice(LAST)}"
        if n not in used_names:
            used_names.add(n); return n

# ---------------- schools ----------------
schools = [
    dict(id=101, code="5001", name="תיכון אורט הדמו", grades={"ז": 2, "ח": 2, "ט": 2}),
    dict(id=102, code="5002", name="חטיבת ביניים אופק", grades={"ז": 1, "ח": 1}),
]
admin_pk = make_user("SYSTEM_ADMIN", il_id("10000000"), "מנהל מערכת – שי", "admin@example.com", "גישה לכל בתי הספר")
ins("system_admin_entity", id=admin_pk)

classes = []   # dict(id, name, grade, school)
grade_id = 200; class_id = 300
for s in schools:
    ins("school_entity", id=s["id"], school_code=s["code"], school_name=s["name"])
    s["grade_ids"] = {}
    for g, n in s["grades"].items():
        grade_id += 1
        s["grade_ids"][g] = grade_id
        ins("school_grade_entity", id=grade_id, grade_name=g, school_id=s["id"])
        for i in range(1, n + 1):
            class_id += 1
            c = dict(id=class_id, name=f"{g}{i}", grade=g, school=s, teachers=[], students=[])
            classes.append(c)
            ins("class_room_entity", id=class_id, name=c["name"], grade_id=grade_id, school_id=s["id"])
    for flag in ["AI_CHAT", "MESSAGES", "PRACTICE_TESTS", "TEACHER_TESTS"]:
        ins("school_feature_flag", enabled=True, feature=flag, school_code=s["code"], updated_at=NOW, updated_by=creds[0]["user_id"])
    ins("school_year", id=s["id"], active=True, closed=False, closed_at=None, name="תשפ\"ז 2026-2027",
        start_date=dt.date(2026, 9, 1), end_date=dt.date(2027, 6, 20), school_id=s["id"])
    ins("semester", name="מחצית א'", start_date=dt.date(2026, 9, 1), end_date=dt.date(2027, 1, 31), school_year_id=s["id"])
    ins("semester", name="מחצית ב'", start_date=dt.date(2027, 2, 1), end_date=dt.date(2027, 6, 20), school_year_id=s["id"])

managers = {}
for idx, (s, nm) in enumerate([(schools[0], "רחל אברמוביץ"), (schools[1], "דוד שטרן")]):
    pk = make_user("SCHOOLMANAGER", il_id(f"2000000{idx+1}"), nm, f"manager{idx+1}@example.com", f"מנהל/ת {s['name']}")
    used_names.add(nm)
    ins("school_manager_entity", id=pk, school_id=s["id"])
    managers[s["id"]] = dict(pk=pk, user_id=creds[-1]["user_id"], name=nm)

teacher_specs = [  # (school index, name, subjects, class names)
    (0, "מיכל לוינסון", ["מתמטיקה"], ["ז1", "ז2"]),
    (0, "אבי גרינברג", ["מתמטיקה"], ["ח1", "ח2"]),
    (0, "סיגל חדד", ["מתמטיקה"], ["ט1", "ט2"]),
    (0, "יוסי מרקוביץ", ["מדעים"], ["ז1", "ח1", "ט1"]),
    (0, "ענת בירנבאום", ["אנגלית"], ["ז2", "ח2", "ט2"]),
    (0, "רונית שלום", ["היסטוריה", "תנ\"ך"], ["ז1", "ז2", "ח1"]),
    (1, "אלי בן חיים", ["מתמטיקה"], ["ז1", "ח1"]),
    (1, "דנה פלג", ["מדעים", "אנגלית"], ["ז1", "ח1"]),
]
teachers = []
for i, (si, nm, subjects, cls) in enumerate(teacher_specs):
    s = schools[si]
    used_names.add(nm)
    uid = il_id(f"3000000{i+1}")
    pk = make_user("TEACHER", uid, nm, f"teacher{i+1}@example.com", f"{', '.join(subjects)} · {s['name']} · כיתות {', '.join(cls)}")
    ins("teacher_entity", id=pk, school_id=s["id"])
    t = dict(pk=pk, user_id=uid, name=nm, subjects=subjects, school=s, classes=[])
    for sub in subjects:
        ins("teacher_subject", teacher_id=pk, subject_name=sub)
    for cn in cls:
        c = next(c for c in classes if c["school"] is s and c["name"] == cn)
        c["teachers"].append(t); t["classes"].append(c)
        ins("teacher_classroom", teacher_id=pk, classroom_id=c["id"])
    teachers.append(t)

students = []
sidx = 0
for c in classes:
    for k in range(5):
        sidx += 1
        g = "M" if k % 2 == 0 else "F"
        nm = rand_name(g)
        uid = il_id(f"4000{sidx:04d}")
        pk = make_user("STUDENT", uid, nm, f"student{sidx}@example.com", f"{c['school']['name']} · כיתה {c['name']}")
        ins("student_entity", id=pk, class_room_id=c["id"], school_id=c["school"]["id"])
        st = dict(pk=pk, user_id=uid, name=nm, cls=c, skill=random.uniform(0.45, 0.95))
        c["students"].append(st); students.append(st)
        for t in c["teachers"]:
            ins("teacher_student", teacher_id=t["pk"], student_id=pk)

parents = []
for i in range(6):
    st = students[i * 6]
    last = st["name"].split(" ", 1)[1]
    nm = f"{random.choice(FIRST_F if i % 2 else FIRST_M)} {last}"
    while nm in used_names: nm = f"{random.choice(FIRST_F + FIRST_M)} {last}"
    used_names.add(nm)
    uid = il_id(f"5000{i+1:04d}")
    kids = [st] + ([students[i * 6 + 1]] if i == 0 else [])
    pk = make_user("PARENT", uid, nm, f"parent{i+1}@example.com", "הורה של " + ", ".join(k["name"] for k in kids))
    ins("parent_entity", id=pk)
    for k in kids:
        ins("parent_student", parent_id=pk, student_id=k["pk"])
    parents.append(dict(pk=pk, user_id=uid, name=nm, kids=kids))

# ---------------- timetable ----------------
DAYS = ["SUNDAY", "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY"]
PY_DAY = {"SUNDAY": 6, "MONDAY": 0, "TUESDAY": 1, "WEDNESDAY": 2, "THURSDAY": 3}
# The UI timetable grid only places lessons that start on :00 or :30.
SLOTS = [(dt.time(h, 0), dt.time(h, 45)) for h in range(8, 14)]
lessons = []  # dict(id, cls, teacher, subject, day)
teacher_busy = set()
lesson_id = 400
for c in classes:
    ins("schedule_entity", id=c["id"], class_room_id=c["id"])
    used = set()
    day_cursor = random.randrange(5)
    for t in c["teachers"]:
        per_week = 5 if MATH in t["subjects"] else 3
        placed = 0
        tries = 0
        while placed < per_week and tries < 200:
            tries += 1
            d = DAYS[(day_cursor + tries // 6) % 5]; si = random.randrange(len(SLOTS))
            if (d, si) in used or (t["pk"], d, si) in teacher_busy: continue
            used.add((d, si)); teacher_busy.add((t["pk"], d, si))
            lesson_id += 1
            subj = t["subjects"][placed % len(t["subjects"])]
            ins("lesson_entity", id=lesson_id, day_of_week=d, start_time=SLOTS[si][0], end_time=SLOTS[si][1],
                subject=subj, schedule_id=c["id"], teacher_id=t["pk"], room=f"חדר {100 + c['id'] % 100}")
            lessons.append(dict(id=lesson_id, cls=c, teacher=t, subject=subj, day=d))
            placed += 1; day_cursor += 1 + tries // 6; tries = 0

# ---------------- question generator ----------------
def gen_question(sub):
    r = random.randint
    if sub == "חיבור מספרים שלמים":
        a, b = r(-60, 120), r(-60, 120); return f"{a} + {b} = ?", a + b, "מספרים שלמים"
    if sub == "חיסור מספרים שלמים":
        a, b = r(-50, 150), r(-50, 100); return f"{a} - {b} = ?", a - b, "מספרים שלמים"
    if sub == "כפל מספרים שלמים":
        a, b = r(-12, 15), r(2, 15); return f"{a} × {b} = ?", a * b, "מספרים שלמים"
    if sub == "חילוק מספרים שלמים":
        b, a = r(2, 12), r(-12, 15); return f"{a*b} ÷ {b} = ?", a, "מספרים שלמים"
    if sub == "חזקות":
        a, b = r(2, 6), r(2, 4); return f"{a}^{b} = ?", a ** b, "מספרים שלמים"
    if sub == "שורש ריבועי":
        a = r(2, 20); return f"√{a*a} = ?", a, "מספרים שלמים"
    if sub == "מישוואה עם נעלם אחד":
        x, a, b = r(-10, 12), r(2, 9), r(-20, 20); return f"{a}x + ({b}) = {a*x+b}. x = ?", x, "משוואות"
    if sub == "חיבור שברים":
        d = r(3, 9); a, b = r(1, d - 1), r(1, d - 1)
        return f"{a}/{d} + {b}/{d} = ? (במונה של {d})", a + b, "שברים"
    if sub == "חישוב שיפוע":
        x1, y1, m = r(-5, 5), r(-5, 5), r(-4, 4); x2 = x1 + r(1, 4)
        return f"מהו השיפוע של הישר העובר דרך ({x1},{y1}) ו-({x2},{y1 + m*(x2-x1)})?", m, "הנדסה אנליטית"
    if sub == "חישוב הנחות":
        p, pct = r(4, 30) * 10, random.choice([10, 20, 25, 50])
        return f"מחיר חולצה {p} ש\"ח. מה המחיר אחרי הנחה של {pct}%?", p * (100 - pct) // 100, "שאלות מילוליות"
    raise ValueError(sub)

SUBS_BY_GRADE = {
    "ז": ["חיבור מספרים שלמים", "חיסור מספרים שלמים", "כפל מספרים שלמים", "חילוק מספרים שלמים", "חיבור שברים"],
    "ח": ["חזקות", "שורש ריבועי", "מישוואה עם נעלם אחד", "חישוב הנחות", "כפל מספרים שלמים"],
    "ט": ["מישוואה עם נעלם אחד", "חישוב שיפוע", "חזקות", "שורש ריבועי", "חישוב הנחות"],
}
TOPIC_OF = {s: gen_question(s)[2] for subs in SUBS_BY_GRADE.values() for s in subs}

qid = [1000]; tid = [600]; rid = [800]
history_ids = {}
for st in students:
    ins("student_progress_entity", id=st["pk"], student_id=st["pk"])
    ins("student_question_history_entity", id=st["pk"], student_id=st["pk"])
    history_ids[st["pk"]] = st["pk"]

def fmt(d): return d.strftime("%d/%m/%Y %H:%M")
def rand_wrong(ans): return ans + random.choice([-3, -2, -1, 1, 2, 5, 10])

answer_records = []
mastery = {}  # (uid, sub) -> [attempts, correct]
def record_answers(st, test_id, questions, when, source, teacher=None):
    correct_ids, wrong_ids = [], []
    for (qpk, text, ans, sub) in questions:
        ok = random.random() < st["skill"]
        (correct_ids if ok else wrong_ids).append(qpk)
        given = ans if ok else rand_wrong(ans)
        ins("test_answer_record", class_room_id=st["cls"]["id"], correct=ok, correct_answer=str(ans), created_at=when,
            given_answer=str(given), question_id=qpk, question_text=text, seconds_spent=random.randint(15, 140),
            source=source, student_user_id=st["user_id"], sub_topic=sub, subject=MATH,
            teacher_user_id=teacher["user_id"] if teacher else None, test_id=test_id, topic=TOPIC_OF[sub])
        m = mastery.setdefault((st["user_id"], sub), [0, 0]); m[0] += 1; m[1] += ok
        if not ok and random.random() < 0.5:
            ins("review_item", answer=str(ans), created_at=when, due_date=TODAY + dt.timedelta(days=random.randint(-2, 5)),
                easiness=2.5, interval_days=1, lapses=1, last_reviewed_at=None, question_id=qpk, question_text=text,
                repetitions=0, review_count=0, source=source, student_user_id=st["user_id"], sub_topic=sub, subject=MATH,
                topic=TOPIC_OF[sub], given_answer=str(given),
                error_type=random.choice(["CALCULATION", "CONCEPTUAL", "MISREAD", "UNCLASSIFIED"]), error_type_source="AI", error_note=None)
    return correct_ids, wrong_ids

def store_result(kind, test_id, st, correct_ids, wrong_ids, finish, teacher=None):
    rid[0] += 1
    n = len(correct_ids) + len(wrong_ids)
    ins("test_result_entity", test_result_type=kind, id=rid[0], score=round(100 * len(correct_ids) / n), test_id=test_id,
        finish_time=finish.strftime("%H:%M"), correct_answers_count=len(correct_ids), incorrect_answers_count=len(wrong_ids))
    if kind == "TEACHER_TEST_RESULT":
        ins("teacher_test_result_entity", id=rid[0], student_id=st["pk"], teacher_id=teacher["pk"])
    else:
        ins("practice_test_result_entity", id=rid[0], student_id=st["pk"])
    for x in correct_ids: ins("test_result_entity_correct_questions", test_result_entity_id=rid[0], correct_questions_id=x)
    for x in wrong_ids: ins("test_result_entity_incorrect_questions", test_result_entity_id=rid[0], incorrect_questions_id=x)

# ---------------- teacher tests ----------------
math_teachers = [t for t in teachers if MATH in t["subjects"]]
teacher_tests = []
for t in math_teachers:
    for c in t["classes"]:
        subs = SUBS_BY_GRADE[c["grade"]]
        plan = [(-35, "מבחן פתיחה", "קל"), (-14, "בוחן אמצע", "בינוני"), (-3, "בוחן שבועי", "בינוני"), (6, "מבחן מחצית", "קשה")]
        for (days, title, diff) in plan:
            tid[0] += 1
            start = dt.datetime.combine(TODAY + dt.timedelta(days=days), dt.time(10, 0))
            sub_pick = random.sample(subs, 3)
            n_q = 6
            ins("test_entity", test_type="TEACHER", id=tid[0], difficulty=diff, question_count=n_q, subject=MATH,
                time_limit_minutes=45, topic=TOPIC_OF[sub_pick[0]], start_time=fmt(start))
            ins("teacher_test_entity", id=tid[0], teacher_id=t["pk"], exam_group_id=None, publish_at=None, published=True,
                shuffle_options=False, shuffle_questions=False, source_test_id=None, template=False,
                title=f"{title} – {c['name']}", version_label=None)
            qs = []
            for p in range(n_q):
                text, ans, topic = gen_question(sub_pick[p % 3])
                qid[0] += 1
                ins("question_entity", question_type="TEST", id=qid[0], answer=str(ans), question_text=text,
                    sub_topic=sub_pick[p % 3], subject=MATH, topic=topic, difficulty_level={"קל": 1, "בינוני": 2, "קשה": 3}[diff])
                ins("test_question_entity", id=qid[0], test_id=tid[0], answer_key_json=None, kind=None, options_json=None,
                    points=None, position=None, prompt=None, tolerance=None)
                qs.append((qid[0], text, ans, sub_pick[p % 3]))
            for st in c["students"]:
                ins("teacher_test_student", test_id=tid[0], student_id=st["pk"])
            teacher_tests.append(dict(id=tid[0], teacher=t, cls=c, start=start, title=f"{title} – {c['name']}"))
            if days < 0:
                for st in c["students"]:
                    if random.random() < 0.08: continue  # missed the test
                    when = start + dt.timedelta(minutes=random.randint(20, 44))
                    ok, bad = record_answers(st, tid[0], qs, when, "TEACHER_TEST", t)
                    store_result("TEACHER_TEST_RESULT", tid[0], st, ok, bad, when, t)

# ---------------- practice tests + practice history ----------------
for st in students:
    subs = SUBS_BY_GRADE[st["cls"]["grade"]]
    for k in range(random.randint(3, 6)):
        tid[0] += 1
        start = dt.datetime.combine(TODAY - dt.timedelta(days=random.randint(1, 50)), dt.time(random.randint(15, 21), random.choice([0, 15, 30, 45])))
        sub = random.choice(subs); n_q = random.choice([4, 5, 6])
        ins("test_entity", test_type="PRACTICE", id=tid[0], difficulty=random.choice(["קל", "בינוני", "קשה"]), question_count=n_q,
            subject=MATH, time_limit_minutes=30, topic=TOPIC_OF[sub], start_time=fmt(start))
        ins("practice_test_entity", id=tid[0], student_id=st["pk"])
        qs = []
        for p in range(n_q):
            text, ans, topic = gen_question(sub)
            qid[0] += 1
            ins("question_entity", question_type="TEST", id=qid[0], answer=str(ans), question_text=text, sub_topic=sub,
                subject=MATH, topic=topic, difficulty_level=2)
            ins("test_question_entity", id=qid[0], test_id=tid[0], answer_key_json=None, kind=None, options_json=None,
                points=None, position=None, prompt=None, tolerance=None)
            qs.append((qid[0], text, ans, sub))
        when = start + dt.timedelta(minutes=random.randint(5, 25))
        ok, bad = record_answers(st, tid[0], qs, when, "PRACTICE_TEST")
        store_result("PRACTICE_TEST_RESULT", tid[0], st, ok, bad, when)
    # free practice questions + skill levels
    for sub in subs:
        att = random.randint(4, 12)
        for _ in range(att):
            text, ans, topic = gen_question(sub)
            qid[0] += 1
            okq = random.random() < st["skill"]
            ins("question_entity", question_type="PRACTICE", id=qid[0], answer=str(ans), question_text=text, sub_topic=sub,
                subject=MATH, topic=topic, difficulty_level=None)
            ins("practice_question_entity", id=qid[0], student_progress_id=st["pk"], question_history_id=st["pk"],
                hints_used=random.choice([0, 0, 0, 1, 2]), points_earned=10 if okq else 0)
            ins("answered_questions", history_id=st["pk"], is_answered_correct=okq, question_id=qid[0])
            m = mastery.setdefault((st["user_id"], sub), [0, 0]); m[0] += 1; m[1] += okq
        lvl = max(1, min(5, round(st["skill"] * 5 + random.uniform(-1, 1))))
        ins("skill_levels_by_sub_topic", progress_id=st["pk"], skill_level=lvl, skill_levels_by_sub_topic_key=sub)
        ins("sub_topic_success_streak", progress_id=st["pk"], success_streak=random.randint(0, 6), sub_topic_success_streak_key=sub)
        ins("sub_topic_incorrect_streak", progress_id=st["pk"], incorrect_streak=random.randint(0, 2), sub_topic_incorrect_streak_key=sub)

for (uid, sub), (a, c) in mastery.items():
    ins("topic_mastery", attempts=a, correct_count=c, rating=round(800 + 800 * c / a + random.uniform(-50, 50), 1),
        student_user_id=uid, sub_topic=sub, subject=MATH, topic=TOPIC_OF[sub], updated_at=NOW - dt.timedelta(days=random.randint(0, 10)))

# ---------------- gradebook ----------------
for t in teachers:
    for c in t["classes"]:
        for cat, w in [("EXAM", 50), ("QUIZ", 20), ("HOMEWORK", 20), ("PROJECT", 10)]:
            ins("gradebook_weight", category=cat, class_room_id=c["id"], teacher_id=t["user_id"], weight=w)
        subj = t["subjects"][0]
        for (title, cat, days, mx) in [(f"מבחן {subj} – אוקטובר", "EXAM", -40, 100), (f"בוחן {subj}", "QUIZ", -20, 100),
                                       ("שיעורי בית מצטברים", "HOMEWORK", -7, 100), ("פרויקט קבוצתי", "PROJECT", -10, 100)]:
            ins("gradebook_assignment", assigned_date=TODAY + dt.timedelta(days=days), category=cat, class_room_id=c["id"],
                max_score=mx, teacher_id=t["user_id"], title=title)
            aid = len(rows_by_table["gradebook_assignment"])
            rows_by_table["gradebook_assignment"][-1]["id"] = aid
            for st in c["students"]:
                sc = max(35, min(100, round(st["skill"] * 100 + random.uniform(-15, 12))))
                ins("gradebook_grade", score=sc, student_user_id=st["user_id"], assignment_id=aid)

# ---------------- homework ----------------
hw_titles = {
    MATH: [("דף עבודה – שברים", "לפתור תרגילים 1-15 בעמוד 42"), ("תרגול משוואות", "פתרו את כל המשוואות בדף המצורף ובדקו את התשובה"),
           ("הכנה למבחן", "לחזור על נושאי המחצית ולפתור את מבחן הדוגמה")],
    "מדעים": [("דו\"ח מעבדה", "לכתוב דו\"ח על ניסוי הצמחים"), ("קריאה – מערכת השמש", "לקרוא פרק 3 ולענות על השאלות")],
    "אנגלית": [("Vocabulary unit 4", "Learn the 20 new words and write a sentence for each"), ("Reading comprehension", "Read the story on page 31 and answer questions 1-6")],
    "היסטוריה": [("עבודה על המהפכה התעשייתית", "לכתוב עמוד אחד על השפעות המהפכה")],
    "תנ\"ך": [("סיכום פרק", "לסכם את ספר שמואל א' פרק ט\"ז")],
}
hw_id = 0
for t in teachers:
    for c in t["classes"]:
        subj = t["subjects"][0]
        for k, (title, desc) in enumerate(hw_titles[subj][:2]):
            hw_id += 1
            due = dt.datetime.combine(TODAY + dt.timedelta(days=[-5, 4][k]), dt.time(23, 59))
            ins("homework", id=hw_id, created_at=due - dt.timedelta(days=7), description=desc, due_date=due, reminder_sent=k == 0,
                subject=subj, teacher_user_id=t["user_id"], title=f"{title} ({c['name']})")
            for st in c["students"]:
                ins("homework_student", homework_id=hw_id, student_user_id=st["user_id"])
                if (k == 0 and random.random() < 0.8) or (k == 1 and random.random() < 0.3):
                    sub_at = due - dt.timedelta(hours=random.randint(-20, 70))
                    ins("homework_submission", content="מצורף הפתרון שלי. " + random.choice(["היה קשה בשאלה 4.", "הכל ברור.", "לא הבנתי את שאלה 2."]),
                        late=sub_at > due, student_user_id=st["user_id"], submitted_at=sub_at, homework_id=hw_id)

# ---------------- attendance (last 3 weeks) ----------------
for l in lessons:
    for w in range(3):
        d = TODAY - dt.timedelta(days=(TODAY.weekday() - PY_DAY[l["day"]]) % 7 + 7 * w)
        if d >= TODAY: continue
        for st in l["cls"]["students"]:
            r = random.random()
            status = "PRESENT" if r < 0.86 else "LATE" if r < 0.93 else "ABSENT" if r < 0.97 else "EXCUSED"
            ins("attendance_record", class_room_id=l["cls"]["id"], class_room_name=l["cls"]["name"], lesson_date=d, lesson_id=l["id"],
                marked_at=dt.datetime.combine(d, dt.time(8, 10)), marked_by_teacher_id=l["teacher"]["user_id"],
                school_code=l["cls"]["school"]["code"], status=status, student_name=st["name"], student_user_id=st["user_id"], subject=l["subject"])

# ---------------- messages ----------------
msg_id = 0
def message(sender, receiver, title, content, when, read=True, thread=None, parent=None):
    global msg_id
    msg_id += 1
    ins("message_entity", id=msg_id, content=content, sent_at=when, title=title, receiver_id=receiver, sender_id=sender,
        attachment_id=None, batch_id=None, parent_message_id=parent, pending=False,
        read_at=when + dt.timedelta(hours=2) if read else None, scheduled_for=None, thread_id=thread or str(uuid.UUID(int=random.getrandbits(128))))
    return msg_id

for s in schools:
    mg = managers[s["id"]]
    for t in [t for t in teachers if t["school"] is s]:
        message(mg["pk"], t["pk"], "ישיבת צוות ביום שלישי", "תזכורת: ישיבת צוות ביום שלישי ב-14:00 בחדר המורים.", NOW - dt.timedelta(days=3), read=random.random() < 0.6)
        th = str(uuid.UUID(int=random.getrandbits(128)))
        m1 = message(t["pk"], mg["pk"], "בקשה לחדר מחשבים", "אפשר לשריין את חדר המחשבים לשיעור ביום חמישי?", NOW - dt.timedelta(days=2), thread=th)
        message(mg["pk"], t["pk"], "Re: בקשה לחדר מחשבים", "בטח, שריינתי לך את השעה השלישית.", NOW - dt.timedelta(days=2, hours=-3), read=False, thread=th, parent=m1)
    for c in [c for c in classes if c["school"] is s]:
        for st in c["students"]:
            message(mg["pk"], st["pk"], "ברוכים הבאים לשנת הלימודים", "שנת לימודים מוצלחת! מערכת התרגול פתוחה לכם בכל שעה.", NOW - dt.timedelta(days=70), read=True)
for t in teachers:
    for c in t["classes"]:
        for st in c["students"]:
            message(t["pk"], st["pk"], "תזכורת לשיעורי בית", f"שלום {st['name'].split()[0]}, אל תשכח/י להגיש את שיעורי הבית עד יום חמישי.",
                    NOW - dt.timedelta(days=random.randint(1, 6)), read=random.random() < 0.5)
    st = t["classes"][0]["students"][0]
    th = str(uuid.UUID(int=random.getrandbits(128)))
    m1 = message(st["pk"], t["pk"], "שאלה על התרגיל", "לא הבנתי את שאלה 3 בדף העבודה, אפשר הסבר?", NOW - dt.timedelta(days=1), thread=th, read=False)
for p in parents:
    k = p["kids"][0]
    t = k["cls"]["teachers"][0]
    th = str(uuid.UUID(int=random.getrandbits(128)))
    m1 = message(p["pk"], t["pk"], f"לגבי {k['name']}", "שלום, רציתי לשאול איך מתקדמים במתמטיקה?", NOW - dt.timedelta(days=4), thread=th)
    message(t["pk"], p["pk"], f"Re: לגבי {k['name']}", "שלום, מתקדמים יפה! ממליצה להמשיך לתרגל במערכת.", NOW - dt.timedelta(days=3), read=False, thread=th, parent=m1)
message(admin_pk, managers[101]["pk"], "עדכון מערכת", "נוספו פיצ'רים חדשים לתלמידים: דו-קרב, אתגר יומי וכרטיסיות.", NOW - dt.timedelta(days=5))
message(admin_pk, managers[102]["pk"], "עדכון מערכת", "נוספו פיצ'רים חדשים לתלמידים: דו-קרב, אתגר יומי וכרטיסיות.", NOW - dt.timedelta(days=5), read=False)

# ---------------- notifications ----------------
for tt in teacher_tests:
    if tt["start"] > NOW:
        for st in tt["cls"]["students"]:
            ins("notification_entity", body=f"נקבע מבחן ב-{fmt(tt['start'])}", created_at=NOW - dt.timedelta(days=1), link="/student/tests",
                read_at=None, title=f"מבחן חדש: {tt['title']}", type="NEW_TEST", user_id=st["user_id"])
for st in students:
    ins("notification_entity", body="יש לך שיעורי בית להגשה עד יום שישי", created_at=NOW - dt.timedelta(hours=10), link="/student/homework",
        read_at=None, title="תזכורת שיעורי בית", type="HOMEWORK_REMINDER", user_id=st["user_id"])

# ---------------- gamification / student features ----------------
BADGES = [("FIRST_TEST", "מבחן ראשון", "השלמת את המבחן הראשון"), ("STREAK_7", "רצף שבועי", "תרגלת 7 ימים ברצף"),
          ("PERFECT_SCORE", "ציון מושלם", "קיבלת 100 במבחן"), ("PRACTICE_100", "100 תרגילים", "פתרת 100 תרגילים")]
for st in students:
    total = 0
    active_days = sorted(random.sample(range(1, 45), random.randint(8, 30)))
    for d in active_days:
        xp = random.randint(20, 160); total += xp
        ins("student_daily_activity", activity_date=TODAY - dt.timedelta(days=d), student_user_id=st["user_id"], xp=xp)
    streak = random.randint(0, 9)
    ins("student_engagement", current_streak=streak, last_active_date=TODAY - dt.timedelta(days=1 if streak else 4),
        longest_streak=max(streak, random.randint(3, 14)), student_user_id=st["user_id"], total_xp=total)
    for code, title, desc in random.sample(BADGES, random.randint(1, 3)):
        ins("student_badge", code=code, description=desc, earned_at=NOW - dt.timedelta(days=random.randint(1, 40)), student_user_id=st["user_id"], title=title)
    ins("notification_preference_entity", email=True, in_app=True, push=False, user_id=st["user_id"])
    ins("student_accommodation", dyslexia_font=False, extra_time_percent=25 if random.random() < 0.1 else 0, read_aloud=False, student_user_id=st["user_id"])
    for d in range(random.randint(1, 4)):
        mins = random.choice([15, 25, 25, 45])
        start = dt.datetime.combine(TODAY - dt.timedelta(days=random.randint(1, 14)), dt.time(17, 0))
        ins("focus_session", actual_minutes=mins, completed=True, ended_at=start + dt.timedelta(minutes=mins), planned_minutes=mins,
            started_at=start, student_user_id=st["user_id"], task=random.choice(["שיעורי בית במתמטיקה", "חזרה למבחן", "תרגול שברים"]))
    ws = TODAY - dt.timedelta(days=(TODAY.weekday() + 1) % 7 + 7)
    ins("student_reflection", achieved=True, created_at=dt.datetime.combine(ws + dt.timedelta(days=6), dt.time(19, 0)), goal_progress=5,
        goal_target=5, goal_type="PRACTICE_DAYS", next_goal="לתרגל משוואות 4 פעמים השבוע", stuck_on="שאלות מילוליות",
        student_user_id=st["user_id"], updated_at=None, week_start=ws, went_well="השתפרתי בכפל מספרים שלמים")

# daily challenges + attempts
for s in schools:
    for g in s["grades"]:
        for d in range(0, 7):
            sub = random.choice(SUBS_BY_GRADE[g]); text, ans, topic = gen_question(sub)
            ins("daily_challenge", answer=str(ans), challenge_date=TODAY - dt.timedelta(days=d), created_at=NOW - dt.timedelta(days=d),
                grade_name=g, level=2, question_text=text, school_code=s["code"], sub_topic=sub, subject=MATH, topic=topic)
            chid = len(rows_by_table["daily_challenge"]); rows_by_table["daily_challenge"][-1]["id"] = chid
            if d == 0: continue
            for c in [c for c in classes if c["school"] is s and c["grade"] == g]:
                for st in c["students"]:
                    if random.random() < 0.5:
                        ok = random.random() < st["skill"]
                        t0 = dt.datetime.combine(TODAY - dt.timedelta(days=d), dt.time(16, random.randint(0, 59)))
                        ins("daily_challenge_attempt", answer=str(ans if ok else rand_wrong(ans)), answered_at=t0 + dt.timedelta(seconds=50),
                            challenge_date=TODAY - dt.timedelta(days=d), challenge_id=chid, correct=ok, started_at=t0, student_user_id=st["user_id"])

# duels inside classes
for c in classes:
    ss = c["students"]
    for k in range(3):
        a, b = random.sample(ss, 2)
        sub = random.choice(SUBS_BY_GRADE[c["grade"]]); created = NOW - dt.timedelta(days=random.randint(1, 20))
        ac, bc = random.randint(1, 5), random.randint(1, 5)
        ins("duel", challenger_answered=5, challenger_correct=ac, challenger_finished_at=created + dt.timedelta(minutes=4), challenger_id=a["user_id"],
            challenger_name=a["name"], created_at=created, finished_at=created + dt.timedelta(minutes=5), level=2, mode="COMPETITIVE",
            opponent_answered=5, opponent_correct=bc, opponent_finished_at=created + dt.timedelta(minutes=5), opponent_id=b["user_id"],
            opponent_name=b["name"], outcome="CHALLENGER" if ac > bc else "OPPONENT" if bc > ac else "DRAW", school_code=c["school"]["code"],
            started_at=created + dt.timedelta(minutes=1), status="FINISHED", sub_topic=sub, subject=MATH, system_score=None, topic=TOPIC_OF[sub])

# flashcards + forum + lesson content + bank + peer questions
fc_id = 0
for t in math_teachers:
    for c in t["classes"]:
        for front, back in [("מה זה מספר ראשוני?", "מספר שמתחלק רק ב-1 ובעצמו"), ("a² - b² = ?", "(a-b)(a+b)"),
                            ("שיפוע ישר דרך שתי נקודות", "m = (y2-y1)/(x2-x1)"), ("מינוס כפול מינוס", "פלוס")]:
            fc_id += 1
            ins("flashcard", id=fc_id, back=back, class_room_id=c["id"], created_at=NOW - dt.timedelta(days=15), front=front,
                owner_user_id=t["user_id"], source="TEACHER", subject=MATH, topic=None)
            for st in c["students"]:
                if random.random() < 0.6:
                    ins("flashcard_review", created_at=NOW - dt.timedelta(days=10), due_date=TODAY + dt.timedelta(days=random.randint(-1, 6)),
                        easiness=round(random.uniform(1.8, 2.7), 2), flashcard_id=fc_id, interval_days=random.randint(1, 6), lapses=random.randint(0, 2),
                        last_reviewed_at=NOW - dt.timedelta(days=random.randint(1, 5)), repetitions=random.randint(1, 4), review_count=random.randint(1, 6), student_user_id=st["user_id"])
        # forum
        st = c["students"][1]
        ins("class_forum_thread", answer_count=2, author_name=st["name"], author_user_id=st["user_id"], body="איך פותרים משוואה כשיש x בשני הצדדים?",
            class_room_id=c["id"], created_at=NOW - dt.timedelta(days=3), exercise_text="3x + 4 = x + 10", last_activity_at=NOW - dt.timedelta(days=2),
            resolved=True, school_code=c["school"]["code"], title="משוואה עם x בשני הצדדים", topic="משוואות")
        thr = len(rows_by_table["class_forum_thread"]); rows_by_table["class_forum_thread"][-1]["id"] = thr
        st2 = c["students"][2]
        ins("class_forum_post", approved=True, approved_by_name=t["name"], author_name=st2["name"], author_role="STUDENT", author_user_id=st2["user_id"],
            body="מעבירים את ה-x לצד אחד: 2x = 6, ואז x = 3", created_at=NOW - dt.timedelta(days=2, hours=5), thread_id=thr)
        ins("class_forum_post", approved=True, approved_by_name=t["name"], author_name=t["name"], author_role="TEACHER", author_user_id=t["user_id"],
            body="נכון מאוד! תמיד לבדוק את הפתרון על ידי הצבה.", created_at=NOW - dt.timedelta(days=2), thread_id=thr)
        st3 = c["students"][3]
        text, ans, topic = gen_question(SUBS_BY_GRADE[c["grade"]][0])
        ins("peer_question", author_name=st3["name"], author_user_id=st3["user_id"], bank_question_id=None, class_room_id=c["id"], correct_answer=str(ans),
            created_at=NOW - dt.timedelta(days=1), distractors=json.dumps([str(ans + 1), str(ans - 2), str(ans + 10)]), explanation="חישוב ישיר",
            question_text=text, review_note=None, reviewed_at=None, reviewer_teacher_id=None, school_code=c["school"]["code"], status="PENDING", topic=topic)
    for title, topic, sub, body in [("מבוא לשברים", "שברים", "חיבור שברים", "שבר מורכב ממונה ומכנה. כדי לחבר שברים עם מכנה זהה – מחברים את המונים."),
                                    ("משוואה עם נעלם אחד", "משוואות", "מישוואה עם נעלם אחד", "המטרה: לבודד את x. מה שעושים בצד אחד – עושים גם בצד השני.")]:
        ins("lesson_content", body=body, created_at=NOW - dt.timedelta(days=30), sub_topic=sub, subject=MATH, teacher_user_id=t["user_id"],
            title=title, topic=topic, updated_at=None, video_embed_url=None)
        lcid = len(rows_by_table["lesson_content"]); rows_by_table["lesson_content"][-1]["id"] = lcid
        ins("lesson_content_example", lesson_content_id=lcid, example_text="דוגמה: 1/5 + 2/5 = 3/5" if topic == "שברים" else "דוגמה: 2x + 3 = 11 → x = 4", example_order=0)
        for c in t["classes"]:
            for st in c["students"]:
                if random.random() < 0.6:
                    ins("lesson_progress", learned=True, lesson_content_id=lcid, practiced=random.random() < 0.6, reviewed=random.random() < 0.3,
                        student_user_id=st["user_id"], updated_at=NOW - dt.timedelta(days=random.randint(1, 20)))
    for g in {c["grade"] for c in t["classes"]}:
        for sub in SUBS_BY_GRADE[g][:3]:
            for _ in range(3):
                text, ans, topic = gen_question(sub)
                att = random.randint(5, 60)
                ins("bank_question", answer=str(ans), attempts=att, correct_attempts=random.randint(0, att), created_at=NOW - dt.timedelta(days=20),
                    explanation=None, grade=g, level="בינוני", options=None, owner_name=t["name"], owner_teacher_id=t["user_id"], question_text=text,
                    school_code=t["school"]["code"], shared=True, source="TEACHER", sub_topic=sub, subject=MATH, times_asked=random.randint(1, 8), topic=topic)

# audit log
for s in schools:
    mg = managers[s["id"]]
    for t in [t for t in teachers if t["school"] is s]:
        ins("audit_log", action="USER_ASSIGNED_AS_TEACHER", actor_role="SCHOOLMANAGER", actor_user_id=mg["user_id"], actor_username=mg["name"],
            after_value="TEACHER", before_value="STUDENT", created_at=NOW - dt.timedelta(days=80), entity_id=t["user_id"], entity_type="USER",
            impersonator_id=None, ip="127.0.0.1", school_code=s["code"])
        ins("audit_log", action="TEACHER_ASSIGNED_TO_CLASSES", actor_role="SCHOOLMANAGER", actor_user_id=mg["user_id"], actor_username=mg["name"],
            after_value=", ".join(c["name"] for c in t["classes"]), before_value=None, created_at=NOW - dt.timedelta(days=79), entity_id=t["user_id"],
            entity_type="TEACHER", impersonator_id=None, ip="127.0.0.1", school_code=s["code"])
    ins("audit_log", action="MANAGER_ASSIGNED_TO_SCHOOL", actor_role="SYSTEM_ADMIN", actor_user_id=creds[0]["user_id"], actor_username=creds[0]["name"],
        after_value=s["name"], before_value=None, created_at=NOW - dt.timedelta(days=90), entity_id=mg["user_id"], entity_type="USER",
        impersonator_id=None, ip="127.0.0.1", school_code=s["code"])

# ---------------- emit ----------------
KEEP = ("flyway_schema_history", "past_exam", "past_exam_section", "past_exam_question")
LOCAL_HOSTS = ("localhost", "127.0.0.1", "::1")


def find_mysql():
    found = shutil.which("mysql")
    if found:
        return found
    candidates = sorted(glob.glob(r"C:\Program Files\MySQL\MySQL Server *\bin\mysql.exe"))
    if candidates:
        return candidates[-1]
    sys.exit("mysql client not found: put it on PATH or pass --mysql")


def read_env_password():
    env = os.path.join(REPO, "backend", ".env")
    if os.path.exists(env):
        for line in open(env, encoding="utf-8"):
            if line.startswith("DB_PASSWORD="):
                return line.split("=", 1)[1].strip()
    return os.environ.get("DB_PASSWORD", "")


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--host", default="localhost")
    ap.add_argument("--port", default="3306")
    ap.add_argument("--user", default="root")
    ap.add_argument("--database", default="learning")
    ap.add_argument("--mysql", help="path to the mysql client")
    ap.add_argument("--sql-only", metavar="FILE", help="write the SQL to FILE instead of running it")
    ap.add_argument("--yes", action="store_true", help="confirm wiping the database")
    args = ap.parse_args()

    flush()
    body = "\n".join(sql)
    if args.sql_only:
        with open(args.sql_only, "w", encoding="utf-8") as f:
            f.write("SET NAMES utf8mb4;\n" + body + "\n")
        print(f"wrote {args.sql_only}")
        return
    if args.host not in LOCAL_HOSTS:
        sys.exit(f"refusing to wipe {args.host}: this script is for a local database only")
    if not args.yes:
        sys.exit(f"this DELETES all data in {args.database}@{args.host}. Re-run with --yes to continue")

    mysql = args.mysql or find_mysql()
    env = dict(os.environ, MYSQL_PWD=read_env_password())
    base = [mysql, "-h", args.host, "-P", args.port, "-u", args.user, "--default-character-set=utf8mb4"]

    def query(q):
        r = subprocess.run(base + ["-N", "-e", q], env=env, capture_output=True, text=True, encoding="utf-8")
        if r.returncode:
            sys.exit(r.stderr)
        return r.stdout.split()

    applied = query(f"SELECT version FROM `{args.database}`.flyway_schema_history WHERE success = 1")
    if "13" not in applied:
        sys.exit("migration V13 has not run on this database: start the backend once, stop it, then re-run")
    tables = query("SELECT table_name FROM information_schema.tables WHERE table_schema = '" + args.database
                   + "' AND table_type = 'BASE TABLE' AND table_name NOT IN (" + ",".join(q(k) for k in KEEP) + ")")
    script = ("SET NAMES utf8mb4;\nSET FOREIGN_KEY_CHECKS=0;\n"
              + "".join(f"TRUNCATE TABLE `{t}`;\n" for t in tables)
              + body + "\nSET FOREIGN_KEY_CHECKS=1;\n")
    r = subprocess.run(base + [args.database], input=script, env=env, capture_output=True, text=True, encoding="utf-8")
    if r.returncode:
        sys.exit(r.stderr)
    print(f"loaded: {len(creds)} users, " + ", ".join(f"{t} {len(v)}" for t, v in rows_by_table.items() if len(v) >= 100))
    print(f"every user's password: {PASSWORD} (list in scripts/demo-seed/README.md)")


if __name__ == "__main__":
    main()
