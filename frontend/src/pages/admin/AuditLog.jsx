import {useContext, useEffect, useState} from "react";
import axios from "axios";
import {FaClipboardList, FaFilter} from "react-icons/fa";
import {UserContext} from "../../context/UserContext.jsx";
import {AUDIT_ACTIONS, AUDIT_LOG_BASE_PATH, LIST_AUDIT_LOG} from "../../constants/pages.constants.js";

const PAGE_SIZE = 25;
const EMPTY_FILTERS = {action: "", actorUserId: "", entityId: "", schoolCode: "", from: "", to: ""};

// Audit log viewer: a system admin sees every entry, a school manager only their own school's.
export default function AuditLog() {
    const {user} = useContext(UserContext);
    const isAdmin = user?.role === "SYSTEM_ADMIN";
    const [actions, setActions] = useState([]);
    const [filters, setFilters] = useState(EMPTY_FILTERS);
    const [applied, setApplied] = useState(EMPTY_FILTERS);
    const [page, setPage] = useState(0);
    const [result, setResult] = useState({items: [], total: 0});
    const [message, setMessage] = useState("");
    const [loading, setLoading] = useState(false);

    useEffect(() => {
        axios.get(`${AUDIT_LOG_BASE_PATH}${AUDIT_ACTIONS}`)
            .then((response) => response.data.success && setActions(response.data.data || []))
            .catch(() => setActions([]));
    }, []);

    useEffect(() => {
        const load = async () => {
            setLoading(true);
            setMessage("");
            try {
                const params = {page, size: PAGE_SIZE};
                Object.entries(applied).forEach(([key, value]) => {
                    if (value) params[key] = value;
                });
                const response = await axios.get(`${AUDIT_LOG_BASE_PATH}${LIST_AUDIT_LOG}`, {params});
                if (response.data.success) {
                    setResult(response.data.data);
                } else {
                    setResult({items: [], total: 0});
                    setMessage(response.data.errorCode);
                }
            } catch (e) {
                setMessage(e.response?.data?.errorCode || "אירעה שגיאה בטעינת יומן הפעולות");
            } finally {
                setLoading(false);
            }
        };
        if (user) load();
    }, [applied, page, user?.userId]);

    const update = (key) => (e) => setFilters({...filters, [key]: e.target.value});

    const applyFilters = (e) => {
        e.preventDefault();
        setPage(0);
        setApplied(filters);
    };

    const clearFilters = () => {
        setFilters(EMPTY_FILTERS);
        setPage(0);
        setApplied(EMPTY_FILTERS);
    };

    const totalPages = Math.max(1, Math.ceil((result.total || 0) / PAGE_SIZE));

    return (
        <div className="page page--wide" dir="rtl">
            <div className="flex items-center gap-3 mb-5 reveal">
                <span className="icon-badge"><FaClipboardList/></span>
                <div>
                    <span className="eyebrow">Audit · {isAdmin ? "כל המערכת" : "בית הספר שלך"}</span>
                    <h2 className="display text-3xl mt-1">יומן פעולות</h2>
                </div>
            </div>

            <form onSubmit={applyFilters} className="glass p-5 mb-5 grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4 reveal" style={{"--i": 1}}>
                <div>
                    <label className="label">פעולה</label>
                    <select className="field" value={filters.action} onChange={update("action")}>
                        <option value="">הכל</option>
                        {actions.map((a) => <option key={a.value} value={a.value}>{a.label}</option>)}
                    </select>
                </div>
                <div>
                    <label className="label">ת"ז של המבצע</label>
                    <input className="field num" value={filters.actorUserId} onChange={update("actorUserId")}/>
                </div>
                <div>
                    <label className="label">מזהה הרשומה (ת"ז / מזהה)</label>
                    <input className="field num" value={filters.entityId} onChange={update("entityId")}/>
                </div>
                {isAdmin && (
                    <div>
                        <label className="label">קוד בית ספר</label>
                        <input className="field num" value={filters.schoolCode} onChange={update("schoolCode")}/>
                    </div>
                )}
                <div>
                    <label className="label">מתאריך</label>
                    <input type="date" className="field num" value={filters.from} onChange={update("from")}/>
                </div>
                <div>
                    <label className="label">עד תאריך</label>
                    <input type="date" className="field num" value={filters.to} onChange={update("to")}/>
                </div>
                <div className="flex gap-3 items-end sm:col-span-2 lg:col-span-3">
                    <button type="submit" className="btn btn-primary"><FaFilter/> סינון</button>
                    <button type="button" onClick={clearFilters} className="btn btn-ghost">ניקוי</button>
                </div>
            </form>

            {message && <p className="notice mb-4">{message}</p>}

            <div className="overflow-x-auto reveal" style={{"--i": 2}}>
                <table className="data-table">
                    <thead>
                    <tr>
                        <th>זמן</th>
                        <th>מבצע</th>
                        <th>פעולה</th>
                        <th>רשומה</th>
                        <th>לפני</th>
                        <th>אחרי</th>
                        {isAdmin && <th>בית ספר</th>}
                        <th>IP</th>
                    </tr>
                    </thead>
                    <tbody>
                    {result.items.map((entry) => (
                        <tr key={entry.id}>
                            <td className="num whitespace-nowrap">{new Date(entry.createdAt).toLocaleString("he-IL")}</td>
                            <td>
                                <div className="text-ink">{entry.actorUsername || "—"}</div>
                                <div className="num text-xs">{entry.actorUserId}</div>
                                {entry.impersonatorId && (
                                    <span className="chip chip--amber mt-1">בהתחזות ע"י {entry.impersonatorId}</span>
                                )}
                            </td>
                            <td>{entry.actionLabel}</td>
                            <td className="num">{entry.entityType} {entry.entityId}</td>
                            <td className="text-xs break-words max-w-[14rem]">{entry.beforeValue || "—"}</td>
                            <td className="text-xs break-words max-w-[14rem]">{entry.afterValue || "—"}</td>
                            {isAdmin && <td className="num">{entry.schoolCode || "—"}</td>}
                            <td className="num text-xs">{entry.ip || "—"}</td>
                        </tr>
                    ))}
                    {!loading && result.items.length === 0 && (
                        <tr>
                            <td colSpan={isAdmin ? 8 : 7}>אין רשומות להצגה</td>
                        </tr>
                    )}
                    </tbody>
                </table>
            </div>

            <div className="flex items-center justify-center gap-3 mt-4">
                <button className="btn btn-ghost" disabled={page === 0 || loading} onClick={() => setPage(page - 1)}>הקודם</button>
                <span className="num text-ink-2">{page + 1} / {totalPages}</span>
                <button className="btn btn-ghost" disabled={page + 1 >= totalPages || loading} onClick={() => setPage(page + 1)}>הבא</button>
            </div>
        </div>
    );
}
