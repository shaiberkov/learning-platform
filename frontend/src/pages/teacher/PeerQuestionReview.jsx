import { useId, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { FaCheck, FaClipboardCheck, FaUndo } from "react-icons/fa";
import EmptyState from "../../components/ui/EmptyState.jsx";
import { SkeletonList } from "../../components/ui/Skeleton.jsx";
import { errorMessage } from "../../lib/api.js";
import { forumApi, peerApi } from "../../features/classCommunity/classCommunityApi.js";
import { PEER_STATUS, formatWhen, successRate } from "../../features/classCommunity/classCommunityUtils.js";

// [s02] The teacher's review queue for questions students wrote. Approving adds the question to the
// teacher's question bank (source STUDENT) and to the class practice, credited to the student.

const STATUSES = [
    { id: "PENDING", label: "ממתינות" },
    { id: "APPROVED", label: "אושרו" },
    { id: "REJECTED", label: "הוחזרו" },
];

function ReviewCard({ question, className, index }) {
    const [note, setNote] = useState("");
    const ids = useId();
    const queryClient = useQueryClient();
    const done = () => queryClient.invalidateQueries({ queryKey: ["peer-review"] });
    const approve = useMutation({ mutationFn: () => peerApi.approve(question.id, note), onSuccess: done });
    const reject = useMutation({ mutationFn: () => peerApi.reject(question.id, note), onSuccess: done });
    const error = (approve.isError && approve.error) || (reject.isError && reject.error);
    const status = PEER_STATUS[question.status] ?? PEER_STATUS.PENDING;
    const rate = successRate(question.attempts, question.correctAttempts);

    return (
        <div className="glass p-5 sm:p-6 space-y-3 reveal" style={{ "--i": Math.min(index, 8) }}>
            <div className="flex flex-wrap items-center gap-2">
                <span className={`chip ${status.chip}`}>{status.label}</span>
                {className && <span className="chip">{className}</span>}
                {question.topic && <span className="chip">{question.topic}</span>}
                <span className="faint text-xs">{question.authorName} · {formatWhen(question.createdAt)}</span>
            </div>
            <p className="title text-lg whitespace-pre-line" dir="auto">{question.questionText}</p>
            <ul className="space-y-1" aria-label="אפשרויות">
                <li className="row-card choice is-active" dir="auto">✓ {question.correctAnswer}</li>
                {question.distractors.map((d) => <li key={d} className="row-card" dir="auto">✗ {d}</li>)}
            </ul>
            {question.explanation && <p className="muted text-sm whitespace-pre-line">הסבר: {question.explanation}</p>}
            {question.reviewNote && <div className="notice">הערה: {question.reviewNote}</div>}
            {question.status === "APPROVED" && (
                <p className="faint text-xs">
                    <span className="num">{question.attempts}</span> תשובות בתרגול הכיתה
                    {rate != null && <> · <span className="num">{rate}%</span> הצלחה</>}
                </p>
            )}
            {question.status === "PENDING" && (
                <div className="space-y-2">
                    <label className="label" htmlFor={`${ids}-note`}>הערה לתלמיד (חובה בהחזרה לתיקון)</label>
                    <input id={`${ids}-note`} className="field" maxLength={500} value={note} onChange={(e) => setNote(e.target.value)} />
                    <div className="flex flex-wrap gap-2">
                        <button type="button" className="btn btn-mint btn-sm" disabled={approve.isPending || reject.isPending}
                                onClick={() => approve.mutate()}>
                            <FaCheck aria-hidden="true" /> אישור והוספה לתרגול הכיתה
                        </button>
                        <button type="button" className="btn btn-ghost btn-sm" disabled={approve.isPending || reject.isPending || !note.trim()}
                                onClick={() => reject.mutate()}>
                            <FaUndo aria-hidden="true" /> החזרה לתיקון
                        </button>
                    </div>
                </div>
            )}
            {error && <div className="notice notice--danger">{errorMessage(error, "הפעולה נכשלה")}</div>}
        </div>
    );
}

export default function PeerQuestionReview() {
    const [classId, setClassId] = useState("");
    const [status, setStatus] = useState("PENDING");
    const ids = useId();
    const classes = useQuery({ queryKey: ["forum-classes"], queryFn: forumApi.classes });
    const queue = useQuery({
        queryKey: ["peer-review", classId, status],
        queryFn: () => peerApi.reviewQueue(classId ? Number(classId) : null, status),
        refetchInterval: 30000,
    });
    const classNames = Object.fromEntries((classes.data ?? []).map((c) => [c.id, c.name]));

    return (
        <div className="page page--medium space-y-5" dir="rtl">
            <div className="glass p-6 sm:p-8 space-y-4 reveal">
                <div className="flex items-center gap-3">
                    <span className="icon-badge icon-badge--mint" aria-hidden="true"><FaClipboardCheck /></span>
                    <div>
                        <span className="eyebrow">Peer questions · שאלות תלמידים</span>
                        <h2 className="display text-3xl sm:text-4xl">אישור שאלות שתלמידים כתבו</h2>
                    </div>
                </div>
                <div className="grid gap-3 sm:grid-cols-2">
                    <div className="field-wrap">
                        <label className="label" htmlFor={`${ids}-class`}>כיתה</label>
                        <select id={`${ids}-class`} className="field" value={classId} onChange={(e) => setClassId(e.target.value)}>
                            <option value="">כל הכיתות שלי</option>
                            {(classes.data ?? []).map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
                        </select>
                    </div>
                    <div className="flex flex-wrap gap-2 items-end" role="group" aria-label="סטטוס">
                        {STATUSES.map((s) => (
                            <button key={s.id} type="button" aria-pressed={status === s.id}
                                    className={`choice ${status === s.id ? "is-active" : ""}`} onClick={() => setStatus(s.id)}>
                                {s.label}
                            </button>
                        ))}
                    </div>
                </div>
            </div>
            {queue.isLoading && <SkeletonList rows={3} />}
            {queue.isError && <div className="notice notice--danger">{errorMessage(queue.error, "לא ניתן לטעון את השאלות")}</div>}
            {queue.data?.length === 0 && (
                <div className="glass">
                    <EmptyState title={status === "PENDING" ? "אין שאלות שממתינות לאישור" : "אין שאלות להצגה"}
                                body="כשתלמידים יכתבו שאלות לחברי הכיתה, הן יופיעו כאן לבדיקה." />
                </div>
            )}
            {queue.data?.map((q, i) => (
                <ReviewCard key={q.id} question={q} className={classNames[q.classRoomId]} index={i} />
            ))}
        </div>
    );
}
