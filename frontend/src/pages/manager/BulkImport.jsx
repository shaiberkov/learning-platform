import {useContext, useState} from "react";
import {UserContext} from "../../context/UserContext.jsx";
import {FaFileCsv, FaDownload, FaUpload, FaCheckCircle} from "react-icons/fa";
import {schoolPost, downloadCsv} from "../../features/school/schoolApi.js";
import {BULK_IMPORT_COMMIT, BULK_IMPORT_PREVIEW} from "../../constants/pages.constants.js";

const ROLE_LABELS = {STUDENT: "תלמיד", TEACHER: "מורה", PARENT: "הורה"};

const TEMPLATE = [
    ["role", "username", "userId", "email", "phone", "className", "subjects", "children"],
    ["תלמיד", "דנה כהן", "123456789", "dana@example.com", "0501234567", "ז1", "", ""],
    ["מורה", "רון לוי", "234567891", "ron@example.com", "0502345678", "ז1;ז2", "מתמטיקה;פיזיקה", ""],
    ["הורה", "מיכל כהן", "345678912", "michal@example.com", "0503456789", "", "", "123456789"],
];

function BulkImport() {
    const {user} = useContext(UserContext);
    const [csv, setCsv] = useState("");
    const [fileName, setFileName] = useState("");
    const [preview, setPreview] = useState(null);
    const [created, setCreated] = useState(null);
    const [message, setMessage] = useState("");
    const [busy, setBusy] = useState(false);

    const onFile = async (e) => {
        const file = e.target.files?.[0];
        if (!file) return;
        setFileName(file.name);
        setCreated(null);
        setMessage("");
        const text = await file.text();
        setCsv(text);
        setBusy(true);
        const res = await schoolPost(BULK_IMPORT_PREVIEW, {schoolCode: user.schoolCode}, text, "text/plain");
        setBusy(false);
        setPreview(res.data || null);
        setMessage(res.success ? "" : res.errorCode);
    };

    const commit = async () => {
        setBusy(true);
        const res = await schoolPost(BULK_IMPORT_COMMIT, {schoolCode: user.schoolCode}, csv, "text/plain");
        setBusy(false);
        setMessage(res.errorCode || "");
        if (res.success) {
            setCreated(res.data);
            setPreview(null);
        }
    };

    const downloadCredentials = () =>
        downloadCsv("initial-passwords.csv", [
            ["שורה", "תפקיד", "שם", "תעודת זהות", "סיסמה ראשונית"],
            ...created.map((u) => [u.rowNumber, ROLE_LABELS[u.role], u.username, u.userId, u.initialPassword]),
        ]);

    return (
        <div className="page page--wide" dir="rtl">
            <div className="glass p-6 sm:p-8 reveal space-y-5">
                <div className="flex items-start gap-4 pb-5 border-b border-dashed border-line">
                    <span className="icon-badge"><FaFileCsv/></span>
                    <div>
                        <span className="eyebrow">Bulk import · ייבוא משתמשים</span>
                        <h2 className="title text-2xl sm:text-3xl mt-1">ייבוא תלמידים, מורים והורים מקובץ CSV</h2>
                        <p className="muted text-sm mt-2">
                            עמודות: תפקיד (תלמיד / מורה / הורה), שם, תעודת זהות, אימייל, טלפון, כיתה (חובה לתלמיד; למורה אפשר כמה כיתות מופרדות ב-;),
                            מקצועות (למורה, מופרדים ב-;), ילדים (להורה: ת.ז הילדים מופרדות ב-;). לכל משתמש חדש נוצרת סיסמה ראשונית.
                        </p>
                    </div>
                </div>

                <div className="flex flex-wrap gap-3 items-center">
                    <label className="btn btn-primary cursor-pointer">
                        <FaUpload/> בחירת קובץ
                        <input type="file" accept=".csv,text/csv" className="hidden" onChange={onFile}/>
                    </label>
                    <button className="btn btn-ghost" onClick={() => downloadCsv("import-template.csv", TEMPLATE)}>
                        <FaDownload/> הורדת קובץ לדוגמה
                    </button>
                    {fileName && <span className="chip">{fileName}</span>}
                    {busy && <span className="faint text-sm">מעבד...</span>}
                </div>

                {message && <p className="notice">{message}</p>}

                {preview && preview.rows.length > 0 && (
                    <>
                        <div className="flex flex-wrap items-center gap-3">
                            <span className="chip chip--mint num">{preview.validCount} תקינות</span>
                            <span className={`chip num ${preview.errorCount ? "chip--danger" : ""}`}>{preview.errorCount} עם שגיאות</span>
                            <button className="btn btn-mint mr-auto" disabled={busy || preview.validCount === 0} onClick={commit}>
                                <FaCheckCircle/> ייבוא {preview.validCount} שורות תקינות
                            </button>
                        </div>
                        {preview.errorCount > 0 && (
                            <p className="faint text-sm">שורות עם שגיאות לא ייובאו. אפשר לתקן את הקובץ ולהעלות אותו שוב.</p>
                        )}
                        <div className="overflow-x-auto">
                            <table className="data-table">
                                <thead>
                                <tr><th>שורה</th><th>תפקיד</th><th>שם</th><th>ת.ז</th><th>כיתה</th><th>אימייל</th><th>טלפון</th><th>סטטוס</th></tr>
                                </thead>
                                <tbody>
                                {preview.rows.map((r) => (
                                    <tr key={r.rowNumber}>
                                        <td className="num">{r.rowNumber}</td>
                                        <td>{ROLE_LABELS[r.role] || "?"}</td>
                                        <td>{r.username}</td>
                                        <td className="num">{r.userId}</td>
                                        <td>{r.role === "PARENT" ? r.children.join(", ") : r.className}</td>
                                        <td>{r.email}</td>
                                        <td className="num">{r.phone}</td>
                                        <td className="text-right">
                                            {r.errors.length === 0
                                                ? <span className="text-mint">תקין</span>
                                                : r.errors.map((e) => <div key={e} className="text-danger text-xs">{e}</div>)}
                                            {r.warnings.map((w) => <div key={w} className="text-amber text-xs">{w}</div>)}
                                        </td>
                                    </tr>
                                ))}
                                </tbody>
                            </table>
                        </div>
                    </>
                )}

                {created && (
                    <div className="space-y-3">
                        <div className="flex flex-wrap items-center gap-3">
                            <span className="title text-lg">משתמשים שנוצרו</span>
                            <button className="btn btn-primary btn-sm mr-auto" onClick={downloadCredentials}>
                                <FaDownload/> הורדת סיסמאות ראשוניות (CSV)
                            </button>
                        </div>
                        <p className="faint text-sm">הסיסמאות מוצגות פעם אחת בלבד. יש להוריד אותן ולמסור לכל משתמש בנפרד.</p>
                        <div className="overflow-x-auto">
                            <table className="data-table">
                                <thead><tr><th>שורה</th><th>תפקיד</th><th>שם</th><th>ת.ז</th><th>סיסמה ראשונית</th></tr></thead>
                                <tbody>
                                {created.map((u) => (
                                    <tr key={u.userId}>
                                        <td className="num">{u.rowNumber}</td>
                                        <td>{ROLE_LABELS[u.role]}</td>
                                        <td>{u.username}</td>
                                        <td className="num">{u.userId}</td>
                                        <td className="num" dir="ltr">{u.initialPassword}</td>
                                    </tr>
                                ))}
                                </tbody>
                            </table>
                        </div>
                    </div>
                )}
            </div>
        </div>
    );
}

export default BulkImport;
