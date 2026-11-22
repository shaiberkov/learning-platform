import {useContext, useEffect, useState} from "react";
import {UserContext} from "../../context/UserContext.jsx";
import {FaArchive, FaGraduationCap, FaPlus, FaTrash} from "react-icons/fa";
import {downloadCsv, schoolGet, schoolPost} from "../../features/school/schoolApi.js";
import {
    ADD_SEMESTER,
    CLOSE_SCHOOL_YEAR,
    CREATE_SCHOOL_YEAR,
    PROMOTION_PLAN,
    REMOVE_SEMESTER,
    SCHOOL_YEAR_ARCHIVE,
    SCHOOL_YEARS,
} from "../../constants/pages.constants.js";

const OUTCOME_LABELS = {PROMOTED: "עלה כיתה", GRADUATED: "סיים", STAYED: "נשאר"};
const GRADUATE = "__graduate__";

function SchoolYears() {
    const {user} = useContext(UserContext);
    const schoolCode = user?.schoolCode;
    const [years, setYears] = useState([]);
    const [message, setMessage] = useState("");
    const [newYear, setNewYear] = useState({name: "", startDate: "", endDate: ""});
    const [semesterForms, setSemesterForms] = useState({});
    const [closing, setClosing] = useState(null); // year being closed
    const [plan, setPlan] = useState([]);
    const [nextYearName, setNextYearName] = useState("");
    const [archive, setArchive] = useState(null); // {year, records}

    const load = () =>
        schoolGet(SCHOOL_YEARS, {schoolCode}).then((res) => {
            if (res.success) setYears(res.data);
            else setMessage(res.errorCode);
        });

    useEffect(() => {
        if (schoolCode) load();
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [schoolCode]);

    const createYear = async () => {
        const res = await schoolPost(CREATE_SCHOOL_YEAR, {schoolCode, ...newYear});
        setMessage(res.errorCode);
        if (res.success) {
            setNewYear({name: "", startDate: "", endDate: ""});
            load();
        }
    };

    const addSemester = async (yearId) => {
        const form = semesterForms[yearId] || {};
        const res = await schoolPost(ADD_SEMESTER, {schoolCode, yearId, ...form});
        setMessage(res.errorCode);
        if (res.success) {
            setSemesterForms({...semesterForms, [yearId]: {}});
            load();
        }
    };

    const removeSemester = async (yearId, semesterId) => {
        const res = await schoolPost(REMOVE_SEMESTER, {schoolCode, yearId, semesterId});
        setMessage(res.errorCode);
        if (res.success) load();
    };

    const openClose = async (year) => {
        const res = await schoolGet(PROMOTION_PLAN, {schoolCode});
        if (!res.success) {
            setMessage(res.errorCode);
            return;
        }
        setPlan(res.data);
        setNextYearName("");
        setClosing(year);
    };

    const confirmClose = async () => {
        const body = plan.map((p) => ({fromGrade: p.fromGrade, toGrade: p.toGrade}));
        const res = await schoolPost(CLOSE_SCHOOL_YEAR, {schoolCode, yearId: closing.id, nextYearName}, body);
        setMessage(res.errorCode);
        if (res.success) {
            setClosing(null);
            load();
        }
    };

    const openArchive = async (year) => {
        const res = await schoolGet(SCHOOL_YEAR_ARCHIVE, {schoolCode, yearId: year.id});
        if (res.success) setArchive({year, records: res.data});
        else setMessage(res.errorCode);
    };

    const exportArchive = () =>
        downloadCsv(`archive-${archive.year.name}.csv`, [
            ["ת.ז", "שם", "שכבה", "כיתה", "מבחנים", "ממוצע", "תוצאה", "שכבה חדשה", "כיתה חדשה"],
            ...archive.records.map((r) => [r.studentUserId, r.studentName, r.gradeName, r.className, r.testsCount,
                r.averageScore ?? "", OUTCOME_LABELS[r.outcome], r.nextGradeName ?? "", r.nextClassName ?? ""]),
        ]);

    const gradeNames = plan.map((p) => p.fromGrade);
    const totalStudents = plan.reduce((sum, p) => sum + p.studentCount, 0);

    return (
        <div className="page page--wide space-y-5" dir="rtl">
            <div className="glass p-6 sm:p-8 reveal">
                <div className="flex items-start gap-4 pb-5 mb-5 border-b border-dashed border-line">
                    <span className="icon-badge"><FaGraduationCap/></span>
                    <div>
                        <span className="eyebrow">School years · שנות לימודים</span>
                        <h2 className="title text-2xl sm:text-3xl mt-1">ניהול שנות לימודים</h2>
                        <p className="muted text-sm mt-2">
                            מחציות לכל שנה, וסגירת שנה בלחיצה אחת: כל התלמידים עולים לשכבה הבאה, השכבה הבוגרת מסיימת,
                            ותמונת מצב של כל תלמיד נשמרת בארכיון.
                        </p>
                    </div>
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-4 gap-3 items-end">
                    <div>
                        <label className="label">שם השנה</label>
                        <input className="field" placeholder='למשל תשפ"ז' value={newYear.name}
                               onChange={(e) => setNewYear({...newYear, name: e.target.value})}/>
                    </div>
                    <div>
                        <label className="label">תאריך התחלה</label>
                        <input type="date" className="field" value={newYear.startDate}
                               onChange={(e) => setNewYear({...newYear, startDate: e.target.value})}/>
                    </div>
                    <div>
                        <label className="label">תאריך סיום</label>
                        <input type="date" className="field" value={newYear.endDate}
                               onChange={(e) => setNewYear({...newYear, endDate: e.target.value})}/>
                    </div>
                    <button className="btn btn-primary" onClick={createYear}><FaPlus/> שנה חדשה</button>
                </div>
                {message && <p className="notice mt-4">{message}</p>}
            </div>

            {years.length === 0 && <p className="faint text-sm">עדיין לא הוגדרו שנות לימודים.</p>}

            {years.map((y, i) => (
                <div key={y.id} className="glass p-6 reveal space-y-4" style={{"--i": i + 1}}>
                    <div className="flex flex-wrap items-center gap-3">
                        <h3 className="title text-xl">{y.name}</h3>
                        <span className="faint text-sm num">{y.startDate} – {y.endDate}</span>
                        {y.active && <span className="chip chip--mint">שנה פעילה</span>}
                        {y.closed && <span className="chip">נסגרה {y.closedAt}</span>}
                        <div className="mr-auto flex gap-2">
                            {y.closed
                                ? <button className="btn btn-ghost btn-sm" onClick={() => openArchive(y)}><FaArchive/> ארכיון ({y.archivedStudents})</button>
                                : <button className="btn btn-danger btn-sm" onClick={() => openClose(y)}><FaGraduationCap/> סגירת שנה והעלאת כיתה</button>}
                        </div>
                    </div>

                    <div className="space-y-2">
                        <span className="eyebrow">מחציות</span>
                        {y.semesters.length === 0 && <p className="faint text-sm">אין מחציות.</p>}
                        {y.semesters.map((s) => (
                            <div key={s.id} className="row-card flex items-center gap-3">
                                <span className="font-semibold">{s.name}</span>
                                <span className="faint text-sm num">{s.startDate} – {s.endDate}</span>
                                {!y.closed && (
                                    <button className="mr-auto text-ink-3 hover:text-danger" aria-label="הסרת מחצית"
                                            onClick={() => removeSemester(y.id, s.id)}><FaTrash/></button>
                                )}
                            </div>
                        ))}
                        {!y.closed && (
                            <div className="grid grid-cols-1 sm:grid-cols-4 gap-2 items-end pt-2">
                                <input className="field" placeholder="שם המחצית (למשל מחצית א')"
                                       value={semesterForms[y.id]?.name || ""}
                                       onChange={(e) => setSemesterForms({...semesterForms, [y.id]: {...semesterForms[y.id], name: e.target.value}})}/>
                                <input type="date" className="field" value={semesterForms[y.id]?.startDate || ""}
                                       onChange={(e) => setSemesterForms({...semesterForms, [y.id]: {...semesterForms[y.id], startDate: e.target.value}})}/>
                                <input type="date" className="field" value={semesterForms[y.id]?.endDate || ""}
                                       onChange={(e) => setSemesterForms({...semesterForms, [y.id]: {...semesterForms[y.id], endDate: e.target.value}})}/>
                                <button className="btn btn-ghost" onClick={() => addSemester(y.id)}><FaPlus/> הוספת מחצית</button>
                            </div>
                        )}
                    </div>
                </div>
            ))}

            {closing && (
                <div className="modal-backdrop" onClick={() => setClosing(null)}>
                    <div className="modal !max-w-xl space-y-4" dir="rtl" onClick={(e) => e.stopPropagation()}>
                        <h3 className="title text-xl">סגירת שנת {closing.name}</h3>
                        <p className="muted text-sm">
                            {totalStudents} תלמידים יועברו לפי התוכנית. כל תלמיד עובר לכיתה המקבילה בשכבת היעד (ז2 ← ח2).
                            הפעולה אינה הפיכה, ותמונת המצב נשמרת בארכיון.
                        </p>
                        <table className="data-table">
                            <thead><tr><th>משכבה</th><th>תלמידים</th><th>לשכבה</th></tr></thead>
                            <tbody>
                            {plan.map((p, idx) => (
                                <tr key={p.fromGrade}>
                                    <td>{p.fromGrade}</td>
                                    <td className="num">{p.studentCount}</td>
                                    <td>
                                        <select className="field" value={p.toGrade === "" ? GRADUATE : p.toGrade}
                                                onChange={(e) => {
                                                    const next = [...plan];
                                                    next[idx] = {...p, toGrade: e.target.value === GRADUATE ? "" : e.target.value};
                                                    setPlan(next);
                                                }}>
                                            {gradeNames.map((g) => <option key={g} value={g}>{g === p.fromGrade ? `${g} (נשארים)` : g}</option>)}
                                            <option value={GRADUATE}>מסיימים את בית הספר</option>
                                        </select>
                                    </td>
                                </tr>
                            ))}
                            </tbody>
                        </table>
                        <div>
                            <label className="label">פתיחת שנה חדשה (רשות)</label>
                            <input className="field" placeholder="שם השנה הבאה" value={nextYearName}
                                   onChange={(e) => setNextYearName(e.target.value)}/>
                        </div>
                        <div className="flex gap-2">
                            <button className="btn btn-danger" onClick={confirmClose}>סגירת השנה והעלאת כל התלמידים</button>
                            <button className="btn btn-ghost" onClick={() => setClosing(null)}>ביטול</button>
                        </div>
                    </div>
                </div>
            )}

            {archive && (
                <div className="glass p-6 reveal space-y-3">
                    <div className="flex flex-wrap items-center gap-3">
                        <h3 className="title text-xl">ארכיון {archive.year.name}</h3>
                        <button className="btn btn-ghost btn-sm mr-auto" onClick={exportArchive}>ייצוא ל-CSV</button>
                        <button className="btn btn-ghost btn-sm" onClick={() => setArchive(null)}>סגירה</button>
                    </div>
                    <div className="overflow-x-auto">
                        <table className="data-table">
                            <thead><tr><th>תלמיד</th><th>שכבה</th><th>כיתה</th><th>מבחנים</th><th>ממוצע</th><th>תוצאה</th><th>יעד</th></tr></thead>
                            <tbody>
                            {archive.records.map((r) => (
                                <tr key={r.studentUserId}>
                                    <td>{r.studentName}</td>
                                    <td>{r.gradeName}</td>
                                    <td>{r.className}</td>
                                    <td className="num">{r.testsCount}</td>
                                    <td className="num">{r.averageScore ?? "—"}</td>
                                    <td>{OUTCOME_LABELS[r.outcome]}</td>
                                    <td>{r.nextClassName || r.nextGradeName || "—"}</td>
                                </tr>
                            ))}
                            </tbody>
                        </table>
                    </div>
                </div>
            )}
        </div>
    );
}

export default SchoolYears;
