import {useContext, useEffect, useMemo, useState} from "react";
import {UserContext} from "../../context/UserContext.jsx";
import {FaCalendarAlt, FaTrash, FaGripVertical} from "react-icons/fa";
import {DAYS, schoolGet, schoolPost} from "../../features/school/schoolApi.js";
import {
    GET_ALL_CLASSES_BY_SCHOOL_CODE,
    TIMETABLE_CHECK,
    TIMETABLE_DELETE,
    TIMETABLE_LESSONS,
    TIMETABLE_SAVE,
    TIMETABLE_TEACHERS,
} from "../../constants/pages.constants.js";

const DAY_START = 8 * 60;
const DAY_END = 17 * 60;
const SLOT = 30;
const SLOT_H = 34; // px per half hour
const SLOTS = Array.from({length: (DAY_END - DAY_START) / SLOT}, (_, i) => DAY_START + i * SLOT);

const toMin = (hhmm) => {
    const [h, m] = hhmm.split(":").map(Number);
    return h * 60 + m;
};
const toHHMM = (min) => `${String(Math.floor(min / 60)).padStart(2, "0")}:${String(min % 60).padStart(2, "0")}`;

const CONFLICT_LABELS = {TEACHER: "מורה תפוס/ה", CLASS: "כיתה תפוסה", ROOM: "חדר תפוס"};

function TimetableBuilder() {
    const {user} = useContext(UserContext);
    const schoolCode = user?.schoolCode;
    const [classes, setClasses] = useState([]);
    const [teachers, setTeachers] = useState([]);
    const [lessons, setLessons] = useState([]);
    const [selectedClass, setSelectedClass] = useState("");
    const [draft, setDraft] = useState({teacherId: "", subject: "", duration: 60, room: ""});
    const [conflicts, setConflicts] = useState([]);
    const [message, setMessage] = useState("");
    const [busy, setBusy] = useState(false);

    const reloadLessons = () =>
        schoolGet(TIMETABLE_LESSONS, {schoolCode}).then((res) => {
            if (res.success) setLessons(res.data);
            else setMessage(res.errorCode);
        });

    useEffect(() => {
        if (!schoolCode) return;
        schoolGet(GET_ALL_CLASSES_BY_SCHOOL_CODE, {schoolCode}).then((res) => {
            if (res.success && Array.isArray(res.data)) setClasses([...res.data].sort());
        });
        schoolGet(TIMETABLE_TEACHERS, {schoolCode}).then((res) => {
            if (res.success) setTeachers(res.data);
        });
        reloadLessons();
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [schoolCode]);

    const classTeachers = useMemo(
        () => teachers.filter((t) => t.classes.includes(selectedClass)),
        [teachers, selectedClass]
    );
    const draftTeacher = teachers.find((t) => t.userId === draft.teacherId);
    const draftReady = selectedClass && draft.teacherId && draft.subject;
    const classLessons = lessons.filter((l) => l.classRoomName === selectedClass);

    // Check first (dry run), then save; the server re-checks on save.
    const place = async (lesson) => {
        setBusy(true);
        setMessage("");
        const check = await schoolPost(TIMETABLE_CHECK, {schoolCode}, lesson);
        if (!check.success) {
            setConflicts(check.data || []);
            setMessage(check.errorCode);
            setBusy(false);
            return;
        }
        const res = await schoolPost(TIMETABLE_SAVE, {schoolCode}, lesson);
        setConflicts(res.data && Array.isArray(res.data) ? res.data : []);
        setMessage(res.errorCode || "");
        if (res.success) await reloadLessons();
        setBusy(false);
    };

    const placeNew = (day, start) => {
        if (!draftReady) {
            setMessage("יש לבחור מורה ומקצוע לפני השיבוץ");
            return;
        }
        place({
            subject: draft.subject,
            dayOfWeek: day,
            startTime: toHHMM(start),
            endTime: toHHMM(start + Number(draft.duration)),
            classRoomName: selectedClass,
            teacherId: draft.teacherId,
            room: draft.room,
        });
    };

    const moveLesson = (lesson, day, start) => {
        const duration = toMin(lesson.endTime) - toMin(lesson.startTime);
        place({...lesson, dayOfWeek: day, startTime: toHHMM(start), endTime: toHHMM(start + duration)});
    };

    const removeLesson = async (lesson) => {
        if (!window.confirm(`להסיר את שיעור ה${lesson.subject} של ${lesson.teacherName}?`)) return;
        const res = await schoolPost(TIMETABLE_DELETE, {schoolCode, lessonId: lesson.id});
        setMessage(res.errorCode || "");
        if (res.success) reloadLessons();
    };

    const onDrop = (e, day, start) => {
        e.preventDefault();
        const payload = e.dataTransfer.getData("text/plain");
        if (payload === "new") {
            placeNew(day, start);
        } else {
            const lesson = lessons.find((l) => String(l.id) === payload);
            if (lesson) moveLesson(lesson, day, start);
        }
    };

    return (
        <div className="page page--wide" dir="rtl">
            <div className="glass p-6 sm:p-8 reveal space-y-5">
                <div className="flex items-start gap-4 pb-5 border-b border-dashed border-line">
                    <span className="icon-badge"><FaCalendarAlt/></span>
                    <div>
                        <span className="eyebrow">Timetable builder · בניית מערכת שעות</span>
                        <h2 className="title text-2xl sm:text-3xl mt-1">בניית מערכת שעות</h2>
                        <p className="muted text-sm mt-2">
                            בחרו כיתה, הגדירו שיעור וגררו אותו למשבצת (או לחצו על משבצת ריקה). גררו שיעור קיים כדי להזיז אותו.
                            המערכת בודקת שהמורה, הכיתה והחדר פנויים.
                        </p>
                    </div>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-5 gap-3 items-end">
                    <div>
                        <label className="label">כיתה</label>
                        <select className="field" value={selectedClass} onChange={(e) => {
                            setSelectedClass(e.target.value);
                            setDraft({...draft, teacherId: "", subject: ""});
                            setConflicts([]);
                        }}>
                            <option value="">בחר כיתה</option>
                            {classes.map((c) => <option key={c} value={c}>{c}</option>)}
                        </select>
                    </div>
                    <div>
                        <label className="label">מורה</label>
                        <select className="field" value={draft.teacherId} disabled={!selectedClass}
                                onChange={(e) => setDraft({...draft, teacherId: e.target.value, subject: ""})}>
                            <option value="">{selectedClass && classTeachers.length === 0 ? "אין מורים משויכים לכיתה" : "בחר מורה"}</option>
                            {classTeachers.map((t) => <option key={t.userId} value={t.userId}>{t.username}</option>)}
                        </select>
                    </div>
                    <div>
                        <label className="label">מקצוע</label>
                        <select className="field" value={draft.subject} disabled={!draftTeacher}
                                onChange={(e) => setDraft({...draft, subject: e.target.value})}>
                            <option value="">בחר מקצוע</option>
                            {draftTeacher?.subjects.map((s) => <option key={s} value={s}>{s}</option>)}
                        </select>
                    </div>
                    <div>
                        <label className="label">משך</label>
                        <select className="field" value={draft.duration} onChange={(e) => setDraft({...draft, duration: e.target.value})}>
                            <option value={30}>30 דקות</option>
                            <option value={60}>שעה</option>
                            <option value={90}>שעה וחצי</option>
                            <option value={120}>שעתיים</option>
                        </select>
                    </div>
                    <div>
                        <label className="label">חדר (רשות)</label>
                        <input className="field" value={draft.room} placeholder="למשל 101"
                               onChange={(e) => setDraft({...draft, room: e.target.value})}/>
                    </div>
                </div>

                {draftReady && (
                    <div
                        draggable
                        onDragStart={(e) => e.dataTransfer.setData("text/plain", "new")}
                        className="row-card inline-flex items-center gap-3 cursor-grab active:cursor-grabbing"
                    >
                        <FaGripVertical className="text-accent"/>
                        <span className="title text-sm">{draft.subject}</span>
                        <span className="faint text-xs">{draftTeacher?.username} · {draft.duration} דק׳{draft.room ? ` · חדר ${draft.room}` : ""}</span>
                        <span className="chip">גררו למערכת</span>
                    </div>
                )}

                {message && <p className={`notice ${conflicts.length ? "notice--danger" : ""}`}>{message}</p>}
                {conflicts.length > 0 && (
                    <ul className="space-y-2">
                        {conflicts.map((c, i) => (
                            <li key={i} className="row-card flex items-center gap-3">
                                <span className="chip chip--danger">{CONFLICT_LABELS[c.type] || c.type}</span>
                                <span className="text-sm">{c.message}</span>
                            </li>
                        ))}
                    </ul>
                )}

                {selectedClass && (
                    <div className="overflow-x-auto">
                        <div className="grid min-w-[720px]" style={{gridTemplateColumns: `4rem repeat(${DAYS.length}, 1fr)`}}>
                            <div/>
                            {DAYS.map((d) => <div key={d.value} className="text-center eyebrow pb-2">{d.label}</div>)}

                            <div className="relative" style={{height: SLOTS.length * SLOT_H}}>
                                {SLOTS.filter((s) => s % 60 === 0).map((s) => (
                                    <span key={s} className="absolute right-1 faint text-xs num" style={{top: ((s - DAY_START) / SLOT) * SLOT_H - 6}}>
                                        {toHHMM(s)}
                                    </span>
                                ))}
                            </div>

                            {DAYS.map((d) => (
                                <div key={d.value} className="relative border-r border-line" style={{height: SLOTS.length * SLOT_H}}>
                                    {SLOTS.map((s) => (
                                        <div
                                            key={s}
                                            onDragOver={(e) => e.preventDefault()}
                                            onDrop={(e) => onDrop(e, d.value, s)}
                                            onClick={() => !busy && draftReady && placeNew(d.value, s)}
                                            title={`${d.label} ${toHHMM(s)}`}
                                            className={`absolute inset-x-0 border-t border-line hover:bg-accent-soft transition-colors ${draftReady ? "cursor-pointer" : ""}`}
                                            style={{top: ((s - DAY_START) / SLOT) * SLOT_H, height: SLOT_H}}
                                        />
                                    ))}
                                    {classLessons.filter((l) => l.dayOfWeek === d.value).map((l) => {
                                        const top = ((toMin(l.startTime) - DAY_START) / SLOT) * SLOT_H;
                                        const height = ((toMin(l.endTime) - toMin(l.startTime)) / SLOT) * SLOT_H;
                                        return (
                                            <div
                                                key={l.id}
                                                draggable
                                                onDragStart={(e) => e.dataTransfer.setData("text/plain", String(l.id))}
                                                className="absolute inset-x-1 rounded-xl bg-accent-soft border border-accent px-2 py-1 overflow-hidden cursor-grab active:cursor-grabbing"
                                                style={{top: top + 1, height: height - 2}}
                                                title={`${l.subject} · ${l.teacherName} · ${l.startTime}-${l.endTime}${l.room ? ` · חדר ${l.room}` : ""}`}
                                            >
                                                <div className="flex items-start justify-between gap-1">
                                                    <span className="text-xs font-semibold text-ink truncate">{l.subject}</span>
                                                    <button
                                                        type="button"
                                                        className="text-ink-3 hover:text-danger"
                                                        aria-label="הסרת שיעור"
                                                        onClick={(e) => { e.stopPropagation(); removeLesson(l); }}
                                                    >
                                                        <FaTrash size={10}/>
                                                    </button>
                                                </div>
                                                <div className="text-[0.68rem] text-ink-2 truncate">{l.teacherName}</div>
                                                <div className="text-[0.68rem] faint num">{l.startTime}-{l.endTime}{l.room ? ` · ${l.room}` : ""}</div>
                                            </div>
                                        );
                                    })}
                                </div>
                            ))}
                        </div>
                    </div>
                )}
            </div>
        </div>
    );
}

export default TimetableBuilder;
