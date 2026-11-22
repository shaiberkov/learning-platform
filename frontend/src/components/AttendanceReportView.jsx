import { useCallback, useEffect, useState } from "react";
import { apiGet } from "../Utils/authRequest.js";
import { ATTENDANCE_BASE_PATH, GET_ATTENDANCE_REPORT } from "../constants/pages.constants.js";

const isoDaysAgo = (days) => {
    const d = new Date();
    d.setDate(d.getDate() - days);
    return d.toISOString().slice(0, 10);
};

// Absence report for a teacher (own lessons) or a school manager (whole school): the server decides by role.
export default function AttendanceReportView({ userId }) {
    const [from, setFrom] = useState(isoDaysAgo(29));
    const [to, setTo] = useState(isoDaysAgo(0));
    const [className, setClassName] = useState("");
    const [onlyUnusual, setOnlyUnusual] = useState(false);
    const [report, setReport] = useState(null);
    const [message, setMessage] = useState("");

    const load = useCallback(async (params) => {
        if (!userId) return;
        const res = await apiGet(`${ATTENDANCE_BASE_PATH}${GET_ATTENDANCE_REPORT}`, { userId, ...params });
        setMessage(res.errorCode || "");
        if (res.success) setReport(res.data);
    }, [userId]);

    useEffect(() => { load({ from: isoDaysAgo(29), to: isoDaysAgo(0) }); }, [load]);

    const submit = (e) => {
        e.preventDefault();
        const params = { from, to };
        if (className) params.className = className;
        load(params);
    };

    const rows = (report?.students || []).filter((r) => !onlyUnusual || r.unusual);

    return (
        <section className="space-y-4">
            <form className="flex flex-wrap items-end gap-3" onSubmit={submit}>
                <div>
                    <label className="label">מתאריך</label>
                    <input type="date" className="field" value={from} onChange={(e) => setFrom(e.target.value)} />
                </div>
                <div>
                    <label className="label">עד תאריך</label>
                    <input type="date" className="field" value={to} onChange={(e) => setTo(e.target.value)} />
                </div>
                <div>
                    <label className="label">כיתה</label>
                    <select className="field" value={className} onChange={(e) => setClassName(e.target.value)}>
                        <option value="">כל הכיתות</option>
                        {(report?.classNames || []).map((c) => <option key={c} value={c}>{c}</option>)}
                    </select>
                </div>
                <button className="btn btn-primary" type="submit">הפקת דוח</button>
                <label className="flex items-center gap-2 text-sm">
                    <input type="checkbox" checked={onlyUnusual} onChange={(e) => setOnlyUnusual(e.target.checked)} />
                    רק היעדרויות חריגות
                </label>
            </form>

            {message && <div className="notice">{message}</div>}

            {report && report.students.length > 0 && (
                <>
                    <div className="flex flex-wrap gap-2 text-sm">
                        <span className="chip chip--mint">נוכחות <span className="num">{report.totals.PRESENT}</span></span>
                        <span className="chip chip--danger">היעדרויות <span className="num">{report.totals.ABSENT}</span></span>
                        <span className="chip chip--amber">איחורים <span className="num">{report.totals.LATE}</span></span>
                        <span className="chip">מוצדקות <span className="num">{report.totals.EXCUSED}</span></span>
                    </div>
                    <div className="overflow-x-auto">
                        <table className="data-table text-right min-w-[640px]">
                            <thead>
                            <tr>
                                <th>תלמיד/ה</th>
                                <th>כיתה</th>
                                <th>נוכח/ת</th>
                                <th>נעדר/ת</th>
                                <th>איחור</th>
                                <th>מוצדקת</th>
                                <th>אחוז היעדרות</th>
                            </tr>
                            </thead>
                            <tbody>
                            {rows.map((r) => (
                                <tr key={`${r.studentId}-${r.className}`}>
                                    <td className="font-medium">
                                        {r.name} {r.unusual && <span className="chip chip--danger mr-1">חריג</span>}
                                    </td>
                                    <td>{r.className}</td>
                                    <td className="num">{r.present}</td>
                                    <td className="num font-bold">{r.absent}</td>
                                    <td className="num">{r.late}</td>
                                    <td className="num">{r.excused}</td>
                                    <td className="num">{r.absenceRate}%</td>
                                </tr>
                            ))}
                            </tbody>
                        </table>
                    </div>
                    <p className="muted text-xs">
                        "חריג": 3 היעדרויות ומעלה בטווח, או היעדרות מחמישית מהשיעורים שסומנו. המנהל/ת מקבל/ת הודעה אוטומטית
                        בכל היעדרות שלישית של תלמיד בתוך 30 יום, וכשחצי כיתה או יותר נעדרים משיעור.
                    </p>
                </>
            )}
        </section>
    );
}
