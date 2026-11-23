import { useState } from "react";
import { Link } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { FaCheck, FaClone, FaTimes } from "react-icons/fa";
import { errorMessage } from "../../lib/api.js";
import { SkeletonList } from "../../components/ui/Skeleton.jsx";
import NoteButton from "./NoteButton.jsx";
import { fetchDueCards, reviewCard } from "./studyToolsApi.js";

// [s04] Today's flashcards inside the mistakes notebook: same SM-2 schedule as the notebook's
// questions, but self-graded. The student flips the card and says whether they knew it.
export default function FlashcardReviewPanel() {
    const queryClient = useQueryClient();
    const due = useQuery({ queryKey: ["flashcards", "due"], queryFn: fetchDueCards });
    const [index, setIndex] = useState(0);
    const [flipped, setFlipped] = useState(false);
    const [result, setResult] = useState(null);

    const grade = useMutation({
        mutationFn: ({ id, knew }) => reviewCard(id, knew),
        onSuccess: (res) => setResult({ ok: true, ...res.data }),
        onError: (err) => setResult({ ok: false, text: errorMessage(err, "לא הצלחנו לשמור את החזרה") }),
    });

    const next = () => {
        setResult(null);
        setFlipped(false);
        setIndex((i) => i + 1);
        queryClient.invalidateQueries({ queryKey: ["flashcards", "summary"] });
    };

    if (due.isLoading) return <SkeletonList rows={1} />;
    const cards = due.data || [];
    if (due.isError || cards.length === 0) {
        return (
            <div className="glass p-5 flex flex-wrap items-center justify-between gap-3">
                <span className="muted">
                    {due.isError ? errorMessage(due.error, "לא הצלחנו לטעון כרטיסיות") : "אין כרטיסיות לחזרה היום."}
                </span>
                <Link to="/flashcards" className="btn btn-ghost btn-sm"><FaClone aria-hidden="true" /> לכרטיסיות</Link>
            </div>
        );
    }

    const card = cards[index];
    if (!card) {
        return (
            <div className="glass p-6 text-center space-y-3">
                <span className="icon-badge icon-badge--mint mx-auto"><FaCheck aria-hidden="true" /></span>
                <p className="title text-lg">סיימת את הכרטיסיות של היום</p>
                <button type="button" className="btn btn-ghost btn-sm" onClick={() => { setIndex(0); due.refetch(); }}>רענון</button>
            </div>
        );
    }

    return (
        <section className="glass p-6 sm:p-8 space-y-4" aria-label="כרטיסיות לחזרה היום">
            <div className="flex flex-wrap items-center justify-between gap-2">
                <h2 className="title text-xl">כרטיסיות לחזרה</h2>
                <div className="flex flex-wrap gap-2">
                    <span className="num text-xs text-ink-3">{index + 1} / {cards.length}</span>
                    {card.topic && <span className="chip">{card.topic}</span>}
                    {card.lapses > 0 && <span className="chip">נשכחה: <span className="num">{card.lapses}</span></span>}
                </div>
            </div>

            <div className="glass--sunk p-5 sm:p-6 space-y-3">
                <p className="text-ink text-lg font-medium whitespace-pre-line break-words">{card.front}</p>
                {flipped && (
                    <p className="text-ink-2 whitespace-pre-line break-words border-t border-dashed border-line pt-3" aria-live="polite">
                        {card.back}
                    </p>
                )}
            </div>

            {!flipped ? (
                <button type="button" className="btn btn-primary btn-block" onClick={() => setFlipped(true)}>הצגת התשובה</button>
            ) : !result ? (
                <div className="grid grid-cols-2 gap-2">
                    <button type="button" className="btn btn-mint" disabled={grade.isPending}
                            onClick={() => grade.mutate({ id: card.id, knew: true })}>
                        <FaCheck aria-hidden="true" /> ידעתי
                    </button>
                    <button type="button" className="btn btn-danger" disabled={grade.isPending}
                            onClick={() => grade.mutate({ id: card.id, knew: false })}>
                        <FaTimes aria-hidden="true" /> לא ידעתי
                    </button>
                </div>
            ) : (
                <div className="space-y-3">
                    <p role="status" className={`notice ${result.ok && result.knew ? "notice--mint" : result.ok ? "" : "notice--danger"}`}>
                        {result.ok
                            ? result.knew
                                ? `מצוין! הכרטיסייה תחזור בעוד ${result.intervalDays} ימים.`
                                : "הכרטיסייה תחזור מחר."
                            : result.text}
                    </p>
                    <button type="button" className="btn btn-primary btn-block" onClick={next}>
                        {index + 1 < cards.length ? "לכרטיסייה הבאה" : "סיום"}
                    </button>
                </div>
            )}

            <NoteButton target={{
                targetType: "FLASHCARD", targetId: card.id, targetTitle: card.front.slice(0, 120),
                subject: card.subject, topic: card.topic,
            }} />
        </section>
    );
}
