import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { apiGet, apiPost, errorMessage } from "../../lib/api.js";
import { PageSkeleton } from "../../components/ui/Skeleton.jsx";
import EmptyState from "../../components/ui/EmptyState.jsx";
import {
    PORTFOLIO_BASE_PATH,
    MY_PORTFOLIO,
    ADD_PORTFOLIO_PROJECT,
    DELETE_PORTFOLIO_PROJECT,
    CREATE_PORTFOLIO_SHARE,
    REVOKE_PORTFOLIO_SHARE,
} from "../../constants/pages.constants.js";
import { formatDate, PORTFOLIO_PRINT_CSS, shareUrl } from "./questPortfolio.helpers.js";
import YearsTimeline from "./YearsTimeline.jsx";

// [s09] Personal portfolio: badges, projects, finished quests and grades from year to year.
// "הדפסה / PDF" uses the browser's print dialog (Save as PDF); the share link opens a read-only public page.

const PORTFOLIO_KEY = ["portfolio"];

function ProjectForm({ onAdded }) {
    const [title, setTitle] = useState("");
    const [description, setDescription] = useState("");
    const [error, setError] = useState("");
    const add = useMutation({
        mutationFn: () => apiPost(`${PORTFOLIO_BASE_PATH}${ADD_PORTFOLIO_PROJECT}`, undefined, { title, description }),
        onSuccess: (res) => {
            setTitle("");
            setDescription("");
            setError("");
            onAdded(res.errorCode);
        },
        onError: (e) => setError(errorMessage(e, e?.userMessage || "לא הצלחנו להוסיף את הפרויקט")),
    });
    const submit = (e) => {
        e.preventDefault();
        if (!title.trim()) {
            setError("נא לכתוב שם לפרויקט");
            return;
        }
        add.mutate();
    };
    return (
        <form onSubmit={submit} className="row-card space-y-3 no-print" aria-label="הוספת פרויקט">
            <div className="field-wrap">
                <label htmlFor="project-title" className="label">שם הפרויקט</label>
                <input id="project-title" className="field" maxLength={120} value={title}
                       onChange={(e) => setTitle(e.target.value)} aria-invalid={!!error} />
            </div>
            <div className="field-wrap">
                <label htmlFor="project-description" className="label">תיאור קצר (לא חובה)</label>
                <textarea id="project-description" className="field" rows={3} maxLength={1000} value={description}
                          onChange={(e) => setDescription(e.target.value)} />
            </div>
            {error && <p className="field-error" role="alert">{error}</p>}
            <button type="submit" className="btn btn-primary btn-sm" disabled={add.isPending}>הוספה לפורטפוליו</button>
        </form>
    );
}

function SharePanel({ share, onChanged }) {
    const [displayName, setDisplayName] = useState(share?.displayName || "");
    const [copied, setCopied] = useState(false);
    const [error, setError] = useState("");
    const link = shareUrl(window.location.origin, share?.token);

    const create = useMutation({
        mutationFn: () => apiPost(`${PORTFOLIO_BASE_PATH}${CREATE_PORTFOLIO_SHARE}`, { displayName }),
        onSuccess: (res) => { setCopied(false); setError(""); onChanged(res.errorCode); },
        onError: (e) => setError(errorMessage(e, "לא הצלחנו ליצור קישור")),
    });
    const revoke = useMutation({
        mutationFn: () => apiPost(`${PORTFOLIO_BASE_PATH}${REVOKE_PORTFOLIO_SHARE}`),
        onSuccess: (res) => { setCopied(false); onChanged(res.errorCode); },
        onError: (e) => setError(errorMessage(e, "לא הצלחנו לבטל את השיתוף")),
    });

    const copy = async () => {
        try {
            await navigator.clipboard.writeText(link);
            setCopied(true);
        } catch {
            setError("לא הצלחנו להעתיק. אפשר לסמן את הקישור ולהעתיק ידנית");
        }
    };

    return (
        <section className="glass p-6 sm:p-8 space-y-4 no-print" aria-labelledby="share-title">
            <h2 id="share-title" className="title text-2xl">שיתוף</h2>
            <p className="muted text-sm">
                קישור לקריאה בלבד: מי שמקבל/ת אותו רואה את התגים, הפרויקטים והממוצעים, בלי הכיתה, בית הספר או פרטים אישיים.
                אפשר לבטל אותו בכל רגע.
            </p>
            <div className="field-wrap">
                <label htmlFor="share-name" className="label">השם שיופיע בדף המשותף</label>
                <input id="share-name" className="field" maxLength={60} value={displayName}
                       placeholder="למשל: נועה" onChange={(e) => setDisplayName(e.target.value)} />
            </div>
            <div className="flex flex-wrap gap-2">
                <button type="button" className="btn btn-primary btn-sm" disabled={create.isPending} onClick={() => create.mutate()}>
                    {share ? "יצירת קישור חדש (הישן יבוטל)" : "יצירת קישור שיתוף"}
                </button>
                {share && (
                    <button type="button" className="btn btn-danger btn-sm" disabled={revoke.isPending} onClick={() => revoke.mutate()}>
                        ביטול השיתוף
                    </button>
                )}
            </div>
            {share && (
                <div className="space-y-2">
                    <label htmlFor="share-link" className="label">הקישור שלך</label>
                    <div className="flex flex-wrap gap-2">
                        <input id="share-link" className="field num flex-1 min-w-0" dir="ltr" readOnly value={link}
                               onFocus={(e) => e.target.select()} />
                        <button type="button" className="btn btn-ghost btn-sm" onClick={copy}>{copied ? "הועתק!" : "העתקה"}</button>
                    </div>
                    <p className="faint text-xs">נוצר ב-{formatDate(share.createdAt)}</p>
                </div>
            )}
            {error && <p className="field-error" role="alert">{error}</p>}
        </section>
    );
}

export default function Portfolio() {
    const queryClient = useQueryClient();
    const [status, setStatus] = useState("");
    const portfolio = useQuery({ queryKey: PORTFOLIO_KEY, queryFn: () => apiGet(`${PORTFOLIO_BASE_PATH}${MY_PORTFOLIO}`) });

    const removeProject = useMutation({
        mutationFn: (projectId) => apiPost(`${PORTFOLIO_BASE_PATH}${DELETE_PORTFOLIO_PROJECT}`, { projectId }),
        onSuccess: (res) => refresh(res.errorCode),
        onError: (e) => setStatus(errorMessage(e, "לא הצלחנו למחוק")),
    });

    function refresh(message) {
        setStatus(message || "");
        queryClient.invalidateQueries({ queryKey: PORTFOLIO_KEY });
    }

    if (portfolio.isLoading) return <PageSkeleton />;
    if (portfolio.isError) {
        return (
            <div className="page page--medium" dir="rtl">
                <div className="notice notice--danger">{errorMessage(portfolio.error, "לא הצלחנו לטעון את הפורטפוליו")}</div>
            </div>
        );
    }
    const p = portfolio.data;

    return (
        <div className="page page--wide space-y-5" dir="rtl">
            <style>{PORTFOLIO_PRINT_CSS}</style>

            <section className="glass p-6 sm:p-8 flex flex-wrap items-center gap-6 reveal">
                <span className="grid place-items-center w-24 h-24 rounded-full bg-accent-soft text-6xl" aria-hidden="true">{p.avatarEmoji}</span>
                <div className="space-y-1 flex-1 min-w-0">
                    <span className="eyebrow">Portfolio · הפורטפוליו שלי</span>
                    <h1 className="display text-3xl sm:text-4xl">{p.studentName}</h1>
                    <p className="muted">
                        {p.gradeName ? `כיתה ${p.gradeName}${p.className ? ` (${p.className})` : ""}` : "עוד לא שובצת לכיתה"}
                        {p.schoolName ? ` · ${p.schoolName}` : ""}
                    </p>
                    <p className="text-ink">
                        רמה <span className="num">{p.level}</span> · <span className="num">{p.totalXp}</span> נקודות
                        · רצף שיא <span className="num">{p.longestStreak}</span> ימים
                    </p>
                </div>
                <button type="button" className="btn btn-ghost no-print" onClick={() => window.print()}>הדפסה / שמירה כ-PDF</button>
            </section>

            <div aria-live="polite" className="no-print">{status && <div className="notice notice--mint">{status}</div>}</div>

            <section className="glass p-6 sm:p-8 space-y-4 reveal" style={{ "--i": 1 }} aria-labelledby="years-title">
                <h2 id="years-title" className="title text-2xl">ההתקדמות שלי לאורך השנים</h2>
                <YearsTimeline years={p.years} />
            </section>

            <div className="grid grid-cols-1 lg:grid-cols-2 gap-5">
                <section className="glass p-6 sm:p-8 space-y-4 reveal" style={{ "--i": 2 }} aria-labelledby="badges-title">
                    <h2 id="badges-title" className="title text-2xl">תגים <span className="chip num">{p.badges.length}</span></h2>
                    {p.badges.length === 0 ? (
                        <EmptyState title="עוד אין תגים" body="תגים ניתנים על הישגים: רצף ימים, שליטה בנושא, מבחן מושלם ומסעות." />
                    ) : (
                        <ul className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                            {p.badges.map((b) => (
                                <li key={b.code} className="row-card space-y-1">
                                    <div className="font-semibold text-ink">{b.title}</div>
                                    {b.description && <div className="muted text-sm">{b.description}</div>}
                                    {b.earnedAt && <div className="faint text-xs">{formatDate(b.earnedAt)}</div>}
                                </li>
                            ))}
                        </ul>
                    )}
                </section>

                <section className="glass p-6 sm:p-8 space-y-4 reveal" style={{ "--i": 3 }} aria-labelledby="quests-title">
                    <h2 id="quests-title" className="title text-2xl">מסעות שהושלמו</h2>
                    {p.quests.length === 0 ? (
                        <EmptyState title="עוד לא הושלם מסע" body="מסעות למידה הם סיפורים עם תרגילים, ובסופם פרס." actionLabel="למסעות" actionTo="/quests" />
                    ) : (
                        <ul className="space-y-2">
                            {p.quests.map((q) => (
                                <li key={q.code} className="row-card flex items-center gap-3">
                                    <span className="text-3xl" aria-hidden="true">{q.emoji}</span>
                                    <span className="font-semibold text-ink flex-1">{q.title}</span>
                                    <span className="faint text-xs">{formatDate(q.completedAt)}</span>
                                </li>
                            ))}
                        </ul>
                    )}
                </section>
            </div>

            <section className="glass p-6 sm:p-8 space-y-4 reveal" style={{ "--i": 4 }} aria-labelledby="projects-title">
                <h2 id="projects-title" className="title text-2xl">הפרויקטים שלי</h2>
                <ProjectForm onAdded={refresh} />
                {p.projects.length === 0 ? (
                    <EmptyState title="עוד אין פרויקטים" body="הוסיפו עבודה, מצגת או פרויקט שאתם גאים בו." />
                ) : (
                    <ul className="grid grid-cols-1 md:grid-cols-2 gap-3">
                        {p.projects.map((proj) => (
                            <li key={proj.id} className="row-card space-y-2">
                                <div className="flex items-start justify-between gap-2">
                                    <span className="font-semibold text-ink">{proj.title}</span>
                                    <button type="button" className="btn btn-ghost btn-sm no-print"
                                            aria-label={`מחיקת הפרויקט ${proj.title}`}
                                            disabled={removeProject.isPending}
                                            onClick={() => window.confirm(`למחוק את "${proj.title}"?`) && removeProject.mutate(proj.id)}>
                                        מחיקה
                                    </button>
                                </div>
                                {proj.description && <p className="muted text-sm whitespace-pre-line">{proj.description}</p>}
                                <p className="faint text-xs">
                                    {proj.gradeName ? `כיתה ${proj.gradeName} · ` : ""}{formatDate(proj.createdAt)}
                                </p>
                            </li>
                        ))}
                    </ul>
                )}
            </section>

            <SharePanel key={p.share?.token || "none"} share={p.share} onChanged={refresh} />
        </div>
    );
}
