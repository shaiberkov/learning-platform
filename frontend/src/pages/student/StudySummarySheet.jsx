// [s05] Personal summary sheet before a test: topics, formulas and the student's own mistakes
// with a short explanation each. Saved sheets can be reopened and printed / saved as PDF.
import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { FaFileAlt, FaPrint, FaTrash } from "react-icons/fa";
import EmptyState from "../../components/ui/EmptyState.jsx";
import { Skeleton, SkeletonList } from "../../components/ui/Skeleton.jsx";
import { errorMessage } from "../../lib/api.js";
import SummaryPicker from "../../features/studySummary/SummaryPicker.jsx";
import {
    createSheet,
    deleteSheet,
    fetchSheet,
    fetchSheets,
    fetchStudyOptions,
} from "../../features/studySummary/studySummaryApi.js";
import { buildGenerateRequest, formatCreatedAt } from "../../features/studySummary/studySummaryUtils.js";

function SheetView({ sheet, onDelete, deleting }) {
    const content = sheet.content || {};
    const topics = content.topics || [];
    const formulas = content.formulas || [];
    const mistakes = content.mistakes || [];
    const tips = content.tips || [];

    return (
        <article className="glass p-6 sm:p-8 space-y-6 print-sheet" aria-labelledby="sheet-title">
            <header className="flex flex-wrap items-start justify-between gap-3">
                <div>
                    <span className="eyebrow">דף סיכום אישי · {formatCreatedAt(sheet.createdAt)}</span>
                    <h2 id="sheet-title" className="display text-2xl sm:text-3xl mt-1">{sheet.title}</h2>
                    {!sheet.aiGenerated && <span className="chip chip--amber mt-2">דף בסיסי ללא AI</span>}
                </div>
                <div className="flex gap-2 no-print">
                    <button type="button" className="btn btn-primary btn-sm" onClick={() => window.print()}>
                        <FaPrint aria-hidden="true" /> הדפסה / PDF
                    </button>
                    <button type="button" className="btn btn-ghost btn-sm" onClick={onDelete} disabled={deleting}
                            aria-label="מחיקת דף הסיכום">
                        <FaTrash aria-hidden="true" />
                    </button>
                </div>
            </header>

            {content.intro && <p className="muted leading-relaxed">{content.intro}</p>}

            <section className="space-y-3" aria-labelledby="sheet-topics">
                <h3 id="sheet-topics" className="title text-lg">הנושאים למבחן</h3>
                <ul className="space-y-2">
                    {topics.map((t, i) => (
                        <li key={i} className="row-card">
                            <p className="font-semibold text-ink">{t.name}</p>
                            {t.summary && <p className="muted text-sm leading-relaxed mt-1">{t.summary}</p>}
                        </li>
                    ))}
                </ul>
            </section>

            <section className="space-y-3" aria-labelledby="sheet-formulas">
                <h3 id="sheet-formulas" className="title text-lg">נוסחאות וכללים</h3>
                {formulas.length === 0 ? (
                    <p className="faint text-sm">נוסחאות מופיעות בדף שנבנה עם עוזר ה-AI.</p>
                ) : (
                    <div className="overflow-x-auto">
                        <table className="data-table w-full">
                            <thead>
                                <tr><th scope="col">שם</th><th scope="col">נוסחה</th><th scope="col">מתי משתמשים</th></tr>
                            </thead>
                            <tbody>
                                {formulas.map((f, i) => (
                                    <tr key={i}>
                                        <td>{f.name}</td>
                                        <td><span className="num" dir="ltr">{f.expression}</span></td>
                                        <td className="muted text-sm">{f.note}</td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                )}
            </section>

            <section className="space-y-3" aria-labelledby="sheet-mistakes">
                <h3 id="sheet-mistakes" className="title text-lg">הטעויות שלך בתרגולים</h3>
                {mistakes.length === 0 ? (
                    <p className="faint text-sm">אין טעויות שמורות בנושאים האלה. כל הכבוד!</p>
                ) : (
                    <ol className="space-y-3">
                        {mistakes.map((m, i) => (
                            <li key={i} className="row-card space-y-2">
                                {m.topic && <span className="chip">{m.topic}</span>}
                                <p className="num text-ink break-words" dir="ltr">{m.question}</p>
                                <p className="text-sm">
                                    התשובה הנכונה: <span className="num font-semibold text-mint" dir="ltr">{m.correctAnswer}</span>
                                </p>
                                {m.explanation && <p className="muted text-sm leading-relaxed">{m.explanation}</p>}
                            </li>
                        ))}
                    </ol>
                )}
            </section>

            {tips.length > 0 && (
                <section className="space-y-2" aria-labelledby="sheet-tips">
                    <h3 id="sheet-tips" className="title text-lg">טיפים לקראת המבחן</h3>
                    <ul className="list-disc pr-5 space-y-1 muted text-sm">
                        {tips.map((tip, i) => <li key={i}>{tip}</li>)}
                    </ul>
                </section>
            )}
        </article>
    );
}

export default function StudySummarySheet() {
    const queryClient = useQueryClient();
    const [choice, setChoice] = useState({ testId: "", topics: [] });
    const [selectedId, setSelectedId] = useState(null);
    const [failed, setFailed] = useState(null);

    const options = useQuery({ queryKey: ["studySummary", "options"], queryFn: fetchStudyOptions });
    const sheets = useQuery({ queryKey: ["studySummary", "sheets"], queryFn: fetchSheets });
    const sheet = useQuery({
        queryKey: ["studySummary", "sheet", selectedId],
        queryFn: () => fetchSheet(selectedId),
        enabled: selectedId != null,
    });

    const create = useMutation({
        mutationFn: (basic) => createSheet(buildGenerateRequest(choice, basic)),
        onMutate: () => setFailed(null),
        onSuccess: (response) => {
            queryClient.setQueryData(["studySummary", "sheet", response.data.id], response.data);
            setSelectedId(response.data.id);
            queryClient.invalidateQueries({ queryKey: ["studySummary", "sheets"] });
        },
        onError: (error) => setFailed(errorMessage(error, "לא הצלחנו לבנות את הדף. נסו שוב.")),
    });

    const remove = useMutation({
        mutationFn: deleteSheet,
        onSuccess: () => {
            setSelectedId(null);
            queryClient.invalidateQueries({ queryKey: ["studySummary", "sheets"] });
        },
        onError: (error) => setFailed(errorMessage(error, "המחיקה נכשלה")),
    });

    const aiAvailable = options.data?.aiAvailable;

    return (
        <div className="page page--wide space-y-4 sm:space-y-5" dir="rtl">
            <div className="glass p-6 sm:p-8 reveal no-print">
                <div className="flex items-center gap-3">
                    <span className="icon-badge"><FaFileAlt aria-hidden="true" /></span>
                    <div>
                        <span className="eyebrow">Summary sheet · דף סיכום</span>
                        <h1 className="display text-3xl sm:text-4xl mt-1">דף סיכום אישי לפני מבחן</h1>
                    </div>
                </div>
                <p className="muted mt-4 leading-relaxed">
                    בחרו מבחן קרוב או נושאים, ונבנה לכם דף אחד: הנושאים, הנוסחאות הרלוונטיות והטעויות שלכם מהתרגולים
                    עם הסבר קצר לכל אחת. אפשר להדפיס או לשמור כ-PDF.
                </p>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-12 gap-4 sm:gap-5">
                <div className="md:col-span-5 space-y-4 no-print">
                    <section className="glass p-5 sm:p-6 space-y-4 reveal" style={{ "--i": 1 }} aria-labelledby="sheet-new">
                        <h2 id="sheet-new" className="title text-lg">דף חדש</h2>
                        {options.isLoading ? (
                            <div role="status" aria-live="polite" className="space-y-3">
                                <span className="sr-only">טוען...</span>
                                <Skeleton className="h-10 w-full" />
                                <Skeleton className="h-8 w-2/3" />
                            </div>
                        ) : (
                            <SummaryPicker idPrefix="sheet" options={options.data} value={choice} onChange={setChoice} />
                        )}
                        {aiAvailable === false && (
                            <p className="notice">עוזר ה-AI עדיין לא הופעל בשרת. אפשר לבנות דף בסיסי עם הנושאים והטעויות, בלי הסברים ונוסחאות.</p>
                        )}
                        {failed && <p className="notice notice--danger" role="alert">{failed}</p>}
                        <div className="flex flex-wrap gap-2">
                            {aiAvailable !== false && (
                                <button type="button" className="btn btn-primary" disabled={create.isPending}
                                        onClick={() => create.mutate(false)}>
                                    {create.isPending ? "בונים את הדף..." : "בנו לי דף סיכום"}
                                </button>
                            )}
                            {(aiAvailable === false || failed) && (
                                <button type="button" className={`btn ${aiAvailable === false ? "btn-primary" : "btn-ghost"}`}
                                        disabled={create.isPending} onClick={() => create.mutate(true)}>
                                    דף בסיסי בלי AI
                                </button>
                            )}
                        </div>
                    </section>

                    <section className="glass p-5 sm:p-6 space-y-3 reveal" style={{ "--i": 2 }} aria-labelledby="sheet-saved">
                        <h2 id="sheet-saved" className="title text-lg">הדפים שלי</h2>
                        {sheets.isLoading ? (
                            <SkeletonList rows={3} />
                        ) : !sheets.data?.length ? (
                            <EmptyState title="עדיין אין דפי סיכום" body="דף שתבנו יישמר כאן ותוכלו לפתוח אותו שוב." />
                        ) : (
                            <ul className="space-y-2">
                                {sheets.data.map((s) => (
                                    <li key={s.id}>
                                        <button type="button" onClick={() => setSelectedId(s.id)}
                                                className={`choice ${selectedId === s.id ? "is-active" : ""}`}
                                                aria-current={selectedId === s.id ? "true" : undefined}>
                                            <span className="flex-1">
                                                <span className="block">{s.title}</span>
                                                <span className="faint text-xs num">{formatCreatedAt(s.createdAt)}</span>
                                            </span>
                                        </button>
                                    </li>
                                ))}
                            </ul>
                        )}
                    </section>
                </div>

                <div className="md:col-span-7">
                    {selectedId == null ? (
                        <div className="glass p-6 reveal no-print" style={{ "--i": 3 }}>
                            <EmptyState title="בחרו או בנו דף סיכום" body="הדף יופיע כאן, מוכן להדפסה." />
                        </div>
                    ) : sheet.isLoading ? (
                        <div className="glass p-6"><SkeletonList rows={4} /></div>
                    ) : sheet.isError ? (
                        <p className="notice notice--danger" role="alert">{errorMessage(sheet.error, "לא הצלחנו לפתוח את הדף")}</p>
                    ) : sheet.data ? (
                        <SheetView sheet={sheet.data} deleting={remove.isPending}
                                   onDelete={() => window.confirm("למחוק את דף הסיכום?") && remove.mutate(sheet.data.id)} />
                    ) : null}
                </div>
            </div>
        </div>
    );
}
