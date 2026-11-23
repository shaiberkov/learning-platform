import { useState } from "react";
import { Link } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { FaCamera, FaClone, FaPlus, FaShareAlt, FaTrash, FaUsers } from "react-icons/fa";
import { errorMessage } from "../../lib/api.js";
import { resizeImage } from "../../components/ai/aiApi.js";
import EmptyState from "../../components/ui/EmptyState.jsx";
import { SkeletonList } from "../../components/ui/Skeleton.jsx";
import NoteButton from "../../features/studyTools/NoteButton.jsx";
import {
    addCardToDeck, fetchCardSummary, fetchClassCards, fetchMyDeck, generateCards, removeCard, saveCards, shareCard,
} from "../../features/studyTools/studyToolsApi.js";
import { SOURCE_LABELS, cleanDrafts, dueLabel } from "../../features/studyTools/studyToolsUtils.js";

// [s04] Flashcards: the student's deck, creating cards (AI from a photo of a book page or a pasted
// summary, or by hand), and cards classmates shared. Due cards are reviewed in the mistakes notebook,
// on the same spaced-repetition schedule.

const TABS = { deck: "החפיסה שלי", create: "יצירת כרטיסיות", class: "של הכיתה" };
const MODES = { photo: "צילום עמוד", summary: "הדבקת סיכום", manual: "כתיבה ידנית" };
const EMPTY_DRAFT = { front: "", back: "" };

function CardFace({ card }) {
    return (
        <div className="space-y-2">
            <p className="text-ink font-medium whitespace-pre-line break-words">{card.front}</p>
            <p className="muted text-sm whitespace-pre-line break-words border-t border-dashed border-line pt-2">{card.back}</p>
        </div>
    );
}

function Deck() {
    const queryClient = useQueryClient();
    const deck = useQuery({ queryKey: ["flashcards", "deck"], queryFn: fetchMyDeck });
    const [message, setMessage] = useState(null);
    const refresh = () => queryClient.invalidateQueries({ queryKey: ["flashcards"] });
    const onError = (err) => setMessage({ ok: false, text: errorMessage(err, "הפעולה נכשלה. נסו שוב.") });

    const share = useMutation({
        mutationFn: ({ id, shared }) => shareCard(id, shared),
        onSuccess: (res) => { setMessage({ ok: true, text: res.errorCode }); refresh(); },
        onError,
    });
    const remove = useMutation({
        mutationFn: (id) => removeCard(id),
        onSuccess: (res) => { setMessage({ ok: true, text: res.errorCode }); refresh(); },
        onError,
    });

    if (deck.isLoading) return <SkeletonList rows={3} />;
    if (deck.isError) return <p className="notice notice--danger">{errorMessage(deck.error, "לא הצלחנו לטעון את החפיסה")}</p>;
    if (!deck.data?.length) {
        return (
            <div className="glass">
                <EmptyState icon={<FaClone />} title="החפיסה ריקה"
                            body="צלמו עמוד מהספר, הדביקו סיכום או כתבו כרטיסייה בעצמכם, והיא תיכנס לחזרה המרווחת." />
            </div>
        );
    }

    return (
        <div className="space-y-3">
            {message && <p role="status" className={`notice ${message.ok ? "notice--mint" : "notice--danger"}`}>{message.text}</p>}
            <ul className="grid grid-cols-1 md:grid-cols-2 gap-3">
                {deck.data.map((card, i) => (
                    <li key={card.id} className="glass tile p-5 space-y-3 reveal" style={{ "--i": Math.min(i, 8) }}>
                        <div className="flex flex-wrap gap-2">
                            {card.topic && <span className="chip">{card.topic}</span>}
                            <span className={`chip ${card.dueDate && dueLabel(card.dueDate) === "לחזרה היום" ? "chip--amber" : ""}`}>
                                {dueLabel(card.dueDate)}
                            </span>
                            <span className="chip">{card.mine ? SOURCE_LABELS[card.source] || card.source : "מהכיתה"}</span>
                            {card.shared && card.mine && <span className="chip chip--mint">משותפת עם הכיתה</span>}
                        </div>
                        <CardFace card={card} />
                        <div className="flex flex-wrap gap-2 pt-1">
                            {card.mine && (
                                <button type="button" className="btn btn-ghost btn-sm" disabled={share.isPending}
                                        onClick={() => share.mutate({ id: card.id, shared: !card.shared })}>
                                    <FaShareAlt aria-hidden="true" /> {card.shared ? "ביטול שיתוף" : "שיתוף עם הכיתה"}
                                </button>
                            )}
                            <NoteButton target={{
                                targetType: "FLASHCARD", targetId: card.id, targetTitle: card.front.slice(0, 120),
                                subject: card.subject, topic: card.topic,
                            }} />
                            <button type="button" className="btn btn-danger btn-sm" disabled={remove.isPending}
                                    aria-label={card.mine ? `מחיקת הכרטיסייה ${card.front}` : `הסרה מהחפיסה: ${card.front}`}
                                    onClick={() => {
                                        const text = card.mine && card.shared
                                            ? "למחוק את הכרטיסייה? היא תימחק גם מהחפיסות של חברי הכיתה שהוסיפו אותה."
                                            : "להסיר את הכרטיסייה מהחפיסה?";
                                        if (window.confirm(text)) remove.mutate(card.id);
                                    }}>
                                <FaTrash aria-hidden="true" /> {card.mine ? "מחיקה" : "הסרה"}
                            </button>
                        </div>
                    </li>
                ))}
            </ul>
        </div>
    );
}

function DraftEditor({ drafts, setDrafts }) {
    const update = (i, field, value) => setDrafts(drafts.map((d, j) => (j === i ? { ...d, [field]: value } : d)));
    return (
        <ol className="space-y-3">
            {drafts.map((d, i) => (
                <li key={i} className="row-card space-y-2">
                    <div className="flex items-center justify-between gap-2">
                        <span className="num text-xs text-ink-3">#{i + 1}</span>
                        <button type="button" className="btn btn-ghost btn-sm" aria-label={`הסרת כרטיסייה ${i + 1}`}
                                onClick={() => setDrafts(drafts.filter((_, j) => j !== i))}>
                            <FaTrash aria-hidden="true" />
                        </button>
                    </div>
                    <label className="label" htmlFor={`draft-front-${i}`}>שאלה</label>
                    <textarea id={`draft-front-${i}`} className="field !min-h-[3.5rem]" maxLength={500} value={d.front}
                              onChange={(e) => update(i, "front", e.target.value)} />
                    <label className="label" htmlFor={`draft-back-${i}`}>תשובה</label>
                    <textarea id={`draft-back-${i}`} className="field !min-h-[3.5rem]" maxLength={1000} value={d.back}
                              onChange={(e) => update(i, "back", e.target.value)} />
                </li>
            ))}
        </ol>
    );
}

function Create() {
    const queryClient = useQueryClient();
    const [mode, setMode] = useState("photo");
    const [subject, setSubject] = useState("");
    const [topic, setTopic] = useState("");
    const [summary, setSummary] = useState("");
    const [image, setImage] = useState(null);
    const [drafts, setDrafts] = useState([]);
    const [source, setSource] = useState("MANUAL");
    const [share, setShare] = useState(false);
    const [message, setMessage] = useState(null);

    const switchMode = (next) => {
        setMode(next);
        setMessage(null);
        setDrafts(next === "manual" ? [{ ...EMPTY_DRAFT }] : []);
        setSource("MANUAL");
    };

    const generate = useMutation({
        mutationFn: () => generateCards(mode === "photo"
            ? { imageDataUrl: image, subject, topic }
            : { text: summary, subject, topic }),
        onSuccess: (res) => {
            setDrafts(res.data.cards);
            setSource(res.data.aiGenerated ? (mode === "photo" ? "AI_IMAGE" : "AI_TEXT") : "MANUAL");
            setMessage({ ok: true, text: res.data.notice || res.errorCode });
        },
        onError: (err) => setMessage({ ok: false, text: errorMessage(err, "לא הצלחנו ליצור כרטיסיות. נסו שוב.") }),
    });

    const save = useMutation({
        mutationFn: () => saveCards({ cards: cleanDrafts(drafts), subject, topic, source, share }),
        onSuccess: (res) => {
            setMessage({ ok: true, text: res.errorCode });
            setDrafts(mode === "manual" ? [{ ...EMPTY_DRAFT }] : []);
            setSummary("");
            setImage(null);
            queryClient.invalidateQueries({ queryKey: ["flashcards"] });
        },
        onError: (err) => setMessage({ ok: false, text: errorMessage(err, "השמירה נכשלה. נסו שוב.") }),
    });

    const onPhoto = async (e) => {
        const file = e.target.files?.[0];
        if (!file) return;
        try {
            setImage(await resizeImage(file));
            setMessage(null);
        } catch {
            setMessage({ ok: false, text: "לא הצלחנו לקרוא את התמונה. נסו קובץ JPG או PNG." });
        }
    };

    const ready = cleanDrafts(drafts).length;

    return (
        <div className="glass p-6 sm:p-8 space-y-5">
            <div className="grid grid-cols-3 gap-2" role="tablist" aria-label="דרך היצירה">
                {Object.entries(MODES).map(([value, label]) => (
                    <button key={value} type="button" role="tab" aria-selected={mode === value}
                            className={`choice ${mode === value ? "is-active" : ""}`} onClick={() => switchMode(value)}>
                        {label}
                    </button>
                ))}
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div className="field-wrap">
                    <label className="label" htmlFor="fc-subject">מקצוע</label>
                    <input id="fc-subject" className="field" maxLength={255} value={subject}
                           onChange={(e) => setSubject(e.target.value)} placeholder="למשל: מתמטיקה" />
                </div>
                <div className="field-wrap">
                    <label className="label" htmlFor="fc-topic">נושא</label>
                    <input id="fc-topic" className="field" maxLength={255} value={topic}
                           onChange={(e) => setTopic(e.target.value)} placeholder="למשל: משוואות" />
                </div>
            </div>

            {mode === "photo" && (
                <div className="space-y-3">
                    <label className="btn btn-ghost cursor-pointer" htmlFor="fc-photo">
                        <FaCamera aria-hidden="true" /> {image ? "החלפת התמונה" : "צילום או בחירת תמונה של עמוד"}
                    </label>
                    <input id="fc-photo" type="file" accept="image/*" capture="environment" className="sr-only" onChange={onPhoto} />
                    {image && <img src={image} alt="העמוד שצולם" className="max-h-64 rounded-2xl border border-line" />}
                    <button type="button" className="btn btn-primary" disabled={!image || generate.isPending}
                            onClick={() => generate.mutate()}>
                        {generate.isPending ? "ה-AI קורא את העמוד..." : "צרו לי כרטיסיות"}
                    </button>
                </div>
            )}

            {mode === "summary" && (
                <div className="space-y-3">
                    <div className="field-wrap">
                        <label className="label" htmlFor="fc-summary">הסיכום שלי</label>
                        <textarea id="fc-summary" className="field" maxLength={4000} value={summary}
                                  onChange={(e) => setSummary(e.target.value)}
                                  placeholder={"הדביקו כאן סיכום. טיפ: שורות בצורה \"מושג: הסבר\" הופכות לכרטיסיות גם בלי AI."} />
                        <span className="faint text-xs num">{summary.length}/4000</span>
                    </div>
                    <button type="button" className="btn btn-primary" disabled={!summary.trim() || generate.isPending}
                            onClick={() => generate.mutate()}>
                        {generate.isPending ? "יוצרים כרטיסיות..." : "צרו לי כרטיסיות"}
                    </button>
                </div>
            )}

            {message && <p role="status" className={`notice ${message.ok ? "notice--mint" : "notice--danger"}`}>{message.text}</p>}

            {drafts.length > 0 && (
                <div className="space-y-4">
                    {mode !== "manual" && <h2 className="title text-lg">עברו על הכרטיסיות ותקנו לפני השמירה</h2>}
                    <DraftEditor drafts={drafts} setDrafts={setDrafts} />
                    <div className="flex flex-wrap items-center gap-3">
                        <button type="button" className="btn btn-ghost btn-sm" onClick={() => setDrafts([...drafts, { ...EMPTY_DRAFT }])}>
                            <FaPlus aria-hidden="true" /> כרטיסייה נוספת
                        </button>
                        <label className="inline-flex items-center gap-2 text-ink-2">
                            <input type="checkbox" checked={share} onChange={(e) => setShare(e.target.checked)} />
                            שיתוף עם הכיתה
                        </label>
                    </div>
                    <button type="button" className="btn btn-primary btn-block" disabled={!ready || save.isPending}
                            onClick={() => save.mutate()}>
                        שמירת <span className="num">{ready}</span> כרטיסיות לחפיסה
                    </button>
                </div>
            )}
        </div>
    );
}

function ClassCards() {
    const queryClient = useQueryClient();
    const cards = useQuery({ queryKey: ["flashcards", "class"], queryFn: fetchClassCards });
    const [message, setMessage] = useState(null);
    const add = useMutation({
        mutationFn: (id) => addCardToDeck(id),
        onSuccess: (res) => { setMessage({ ok: true, text: res.errorCode }); queryClient.invalidateQueries({ queryKey: ["flashcards"] }); },
        onError: (err) => setMessage({ ok: false, text: errorMessage(err, "הפעולה נכשלה. נסו שוב.") }),
    });

    if (cards.isLoading) return <SkeletonList rows={3} />;
    if (cards.isError) return <p className="notice notice--danger">{errorMessage(cards.error, "לא הצלחנו לטעון את כרטיסיות הכיתה")}</p>;
    if (!cards.data?.length) {
        return (
            <div className="glass">
                <EmptyState icon={<FaUsers />} title="עוד אין כרטיסיות משותפות"
                            body="כשחברי הכיתה ישתפו כרטיסיות, הן יופיעו כאן ותוכלו להוסיף אותן לחפיסה שלכם." />
            </div>
        );
    }
    return (
        <div className="space-y-3">
            {message && <p role="status" className={`notice ${message.ok ? "notice--mint" : "notice--danger"}`}>{message.text}</p>}
            <ul className="grid grid-cols-1 md:grid-cols-2 gap-3">
                {cards.data.map((card) => (
                    <li key={card.id} className="glass p-5 space-y-3">
                        <div className="flex flex-wrap gap-2">
                            {card.subject && <span className="chip">{card.subject}</span>}
                            {card.topic && <span className="chip">{card.topic}</span>}
                        </div>
                        <CardFace card={card} />
                        {card.inDeck ? (
                            <span className="chip chip--mint">בחפיסה שלך</span>
                        ) : (
                            <button type="button" className="btn btn-mint btn-sm" disabled={add.isPending}
                                    onClick={() => add.mutate(card.id)}>
                                <FaPlus aria-hidden="true" /> הוספה לחפיסה שלי
                            </button>
                        )}
                    </li>
                ))}
            </ul>
        </div>
    );
}

export default function Flashcards() {
    const [tab, setTab] = useState("deck");
    const summary = useQuery({ queryKey: ["flashcards", "summary"], queryFn: fetchCardSummary });

    return (
        <div className="page page--wide space-y-4 sm:space-y-5" dir="rtl">
            <div className="grid grid-cols-1 md:grid-cols-12 gap-4 sm:gap-5">
                <div className="md:col-span-8 glass p-6 sm:p-8 reveal">
                    <div className="flex items-center gap-3">
                        <span className="icon-badge icon-badge--mint"><FaClone aria-hidden="true" /></span>
                        <div>
                            <span className="eyebrow">Flashcards · כרטיסיות</span>
                            <h1 className="display text-3xl sm:text-4xl mt-1">כרטיסיות לחזרה</h1>
                        </div>
                    </div>
                    <p className="muted mt-4 leading-relaxed">
                        צלמו עמוד מהספר או הדביקו סיכום, וה-AI יכין כרטיסיות שאלה-תשובה. כל כרטיסייה נכנסת לחזרה
                        המרווחת של מחברת הטעויות: מה שזוכרים חוזר בעוד יום, שבוע ויותר; מה ששוכחים חוזר מחר.
                    </p>
                </div>
                <div className="md:col-span-4 glass tile p-6 flex flex-col justify-between gap-3 reveal" style={{ "--i": 1 }}>
                    <span className="faint text-xs">כרטיסיות לחזרה היום</span>
                    <div className="num text-5xl font-semibold text-ink" aria-live="polite">
                        {summary.data?.dueToday ?? 0}
                        <span className="text-ink-3 text-lg"> / {summary.data?.total ?? 0}</span>
                    </div>
                    <Link to="/mistakes-notebook" className="btn btn-primary">לחזרה היומית</Link>
                </div>
            </div>

            <div className="grid grid-cols-3 gap-2" role="tablist" aria-label="כרטיסיות">
                {Object.entries(TABS).map(([value, label]) => (
                    <button key={value} type="button" role="tab" aria-selected={tab === value}
                            className={`choice ${tab === value ? "is-active" : ""}`} onClick={() => setTab(value)}>
                        {label}
                    </button>
                ))}
            </div>

            {tab === "deck" && <Deck />}
            {tab === "create" && <Create />}
            {tab === "class" && <ClassCards />}
        </div>
    );
}
