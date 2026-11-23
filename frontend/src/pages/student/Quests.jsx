import { useEffect, useRef, useState } from "react";
import { useSearchParams } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { apiGet, apiPost, errorMessage } from "../../lib/api.js";
import { SkeletonList } from "../../components/ui/Skeleton.jsx";
import EmptyState from "../../components/ui/EmptyState.jsx";
import {
    QUEST_BASE_PATH,
    LIST_QUESTS,
    QUEST_STATE,
    START_QUEST,
    ANSWER_QUEST,
    QUEST_COSMETICS,
    EQUIP_COSMETIC,
} from "../../constants/pages.constants.js";
import { questPercent, questStatusLabel, roomToneClass } from "./questPortfolio.helpers.js";

// [s09] Story quests: a topic becomes a journey of chapters, each passed by solving generated questions.
// Finishing a quest unlocks cosmetic rewards for the student's avatar and room.

const QUESTS_KEY = ["quests"];
const COSMETICS_KEY = ["quest-cosmetics"];

function MyRoom({ cosmetics }) {
    if (!cosmetics) return null;
    const { avatar, room } = cosmetics;
    return (
        <section
            className={`glass p-6 sm:p-8 reveal ${roomToneClass(room.tone)}`}
            aria-label={`החדר שלי: ${room.name}, הדמות: ${avatar.name}`}
        >
            <span className="eyebrow">Quests · מסעות למידה</span>
            <div className="flex flex-wrap items-center gap-6 mt-2">
                <span className="grid place-items-center w-24 h-24 rounded-full bg-surface border border-line text-6xl" aria-hidden="true">
                    {avatar.emoji}
                </span>
                <div className="space-y-1 min-w-0">
                    <h1 className="display text-3xl sm:text-4xl">המסעות שלי</h1>
                    <p className="muted">בוחרים מסע, פותרים תרגילים ומתקדמים בסיפור. כל מסע שמסיימים פותח פרס לחדר ולדמות.</p>
                    <p className="text-ink font-semibold">
                        <span aria-hidden="true">{room.emoji} </span>{room.name}
                        {room.decor && <span className="ms-2 text-xl" aria-hidden="true">{room.decor}</span>}
                    </p>
                </div>
            </div>
        </section>
    );
}

function Wardrobe({ cosmetics, onEquip, pending }) {
    if (!cosmetics) return null;
    const groups = [
        { kind: "AVATAR", title: "דמויות" },
        { kind: "ROOM", title: "עיצובי חדר" },
    ];
    return (
        <section className="glass p-6 sm:p-8 space-y-5 reveal" style={{ "--i": 3 }} aria-labelledby="wardrobe-title">
            <h2 id="wardrobe-title" className="title text-2xl">הארון שלי</h2>
            {groups.map((g) => (
                <div key={g.kind} className="space-y-3">
                    <h3 className="font-semibold text-ink">{g.title}</h3>
                    <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-5 gap-3">
                        {cosmetics.items.filter((i) => i.kind === g.kind).map((item) => (
                            <button
                                key={item.code}
                                type="button"
                                className={`choice flex flex-col items-center gap-1 p-3 ${item.equipped ? "is-active" : ""}`}
                                disabled={!item.unlocked || item.equipped || pending}
                                aria-pressed={item.equipped}
                                aria-label={item.unlocked
                                    ? `${item.name}${item.equipped ? " (בשימוש)" : ", לבחירה"}`
                                    : `${item.name}, נעול. נפתח במסע ${item.questTitle}`}
                                onClick={() => onEquip(item.code)}
                            >
                                <span className="text-4xl" aria-hidden="true">{item.unlocked ? item.emoji : "🔒"}</span>
                                <span className="text-sm text-ink">{item.name}</span>
                                {!item.unlocked && <span className="faint text-xs">במסע: {item.questTitle}</span>}
                            </button>
                        ))}
                    </div>
                </div>
            ))}
        </section>
    );
}

function QuestCard({ quest, index, onOpen }) {
    const percent = questPercent(quest.chaptersDone, quest.chapterCount);
    const action = quest.status === "NOT_STARTED" ? "להתחיל את המסע" : quest.status === "COMPLETED" ? "לצפות במסע" : "להמשיך";
    return (
        <article className="row-card tile space-y-3 reveal" style={{ "--i": index }}>
            <div className="flex items-start justify-between gap-2">
                <div className="flex items-center gap-3">
                    <span className="text-4xl" aria-hidden="true">{quest.emoji}</span>
                    <div>
                        <h3 className="title text-xl">{quest.title}</h3>
                        <p className="faint text-xs">{quest.topic} · כיתות {quest.gradeBand}</p>
                    </div>
                </div>
                <span className={`chip ${quest.status === "COMPLETED" ? "chip--mint" : quest.status === "IN_PROGRESS" ? "chip--amber" : ""}`}>
                    {questStatusLabel(quest.status)}
                </span>
            </div>
            <p className="muted text-sm">{quest.tagline}</p>
            <div className="track" role="progressbar" aria-valuemin={0} aria-valuemax={100} aria-valuenow={percent}
                 aria-label={`התקדמות במסע ${quest.title}`}>
                <span style={{ width: `${percent}%` }} />
            </div>
            <p className="faint text-xs">
                <span className="num">{quest.chaptersDone}</span> מתוך <span className="num">{quest.chapterCount}</span> פרקים
                · פרסים: {quest.rewards.map((r) => (
                    <span key={r.code} title={r.name} aria-label={r.name}>{r.unlocked ? r.emoji : "🔒"} </span>
                ))}
            </p>
            <button type="button" className="btn btn-primary btn-sm" onClick={() => onOpen(quest.code)}>{action}</button>
        </article>
    );
}

function QuestPlay({ questCode, onBack }) {
    const queryClient = useQueryClient();
    const [answer, setAnswer] = useState("");
    const [feedback, setFeedback] = useState(null);
    const inputRef = useRef(null);
    const stateKey = ["quest-state", questCode];

    const state = useQuery({
        queryKey: stateKey,
        queryFn: () => apiGet(`${QUEST_BASE_PATH}${QUEST_STATE}`, { questCode }),
    });

    const start = useMutation({
        mutationFn: () => apiPost(`${QUEST_BASE_PATH}${START_QUEST}`, { questCode }),
        onSuccess: (res) => {
            queryClient.setQueryData(stateKey, res.data);
            queryClient.invalidateQueries({ queryKey: QUESTS_KEY });
            setFeedback({ ok: true, text: res.errorCode });
        },
        onError: (e) => setFeedback({ ok: false, text: errorMessage(e, "לא הצלחנו להתחיל את המסע") }),
    });

    const submit = useMutation({
        mutationFn: (value) => apiPost(`${QUEST_BASE_PATH}${ANSWER_QUEST}`, { questCode, answer: value }),
        onSuccess: (res) => {
            const result = res.data;
            queryClient.setQueryData(stateKey, result.state);
            setFeedback({ ok: result.correct, text: result.message, unlocked: result.unlocked, done: result.questCompleted });
            setAnswer("");
            if (result.chapterCompleted || result.questCompleted) {
                queryClient.invalidateQueries({ queryKey: QUESTS_KEY });
            }
            if (result.questCompleted) {
                queryClient.invalidateQueries({ queryKey: COSMETICS_KEY });
            }
        },
        onError: (e) => setFeedback({ ok: false, text: errorMessage(e, "לא הצלחנו לבדוק את התשובה") }),
    });

    useEffect(() => {
        if (state.data?.question) inputRef.current?.focus();
    }, [state.data?.question]);

    if (state.isLoading) return <SkeletonList rows={3} />;
    if (state.isError) {
        return <div className="notice notice--danger">{errorMessage(state.error, "לא הצלחנו לטעון את המסע")}</div>;
    }
    const q = state.data;
    const current = q.chapters.find((c) => c.current);

    const onSubmit = (e) => {
        e.preventDefault();
        if (!answer.trim()) {
            setFeedback({ ok: false, text: "נא לכתוב תשובה" });
            return;
        }
        submit.mutate(answer.trim());
    };

    return (
        <section className="glass p-6 sm:p-8 space-y-6 reveal" aria-labelledby="quest-title">
            <div className="flex flex-wrap items-center justify-between gap-3">
                <div className="flex items-center gap-3">
                    <span className="text-5xl" aria-hidden="true">{q.emoji}</span>
                    <h2 id="quest-title" className="display text-3xl">{q.title}</h2>
                </div>
                <button type="button" className="btn btn-ghost btn-sm" onClick={onBack}>חזרה לכל המסעות</button>
            </div>

            <p className="text-ink text-lg leading-relaxed">{q.status === "COMPLETED" ? q.outro : q.intro}</p>

            <ol className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3" aria-label="פרקי המסע">
                {q.chapters.map((c) => (
                    <li key={c.number} className={`row-card space-y-1 ${c.current ? "bg-accent-soft" : ""}`}
                        aria-current={c.current ? "step" : undefined}>
                        <span className="faint text-xs">פרק <span className="num">{c.number}</span></span>
                        <div className="font-semibold text-ink">{c.done ? "✅ " : c.current ? "⭐ " : "🔒 "}{c.title}</div>
                        <div className="faint text-xs">{c.skill}</div>
                    </li>
                ))}
            </ol>

            {q.status === "NOT_STARTED" && (
                <button type="button" className="btn btn-primary" onClick={() => start.mutate()} disabled={start.isPending}>
                    יוצאים לדרך!
                </button>
            )}

            {q.status === "IN_PROGRESS" && current && (
                <div className="space-y-4">
                    <div className="notice">
                        <strong>{current.title}: </strong>{current.story}
                    </div>
                    <p className="muted text-sm">
                        תשובות נכונות בפרק: <span className="num">{q.chapterCorrect}</span> מתוך <span className="num">{q.questionsToPass}</span>
                        {" "}· ניסיונות לשאלה: <span className="num">{q.triesLeft}</span>
                    </p>
                    {q.question ? (
                        <form onSubmit={onSubmit} className="space-y-3">
                            <p className="num text-3xl text-ink" dir="ltr" aria-label={`השאלה: ${q.question}`}>{q.question}</p>
                            <div className="field-wrap">
                                <label htmlFor="quest-answer" className="label">התשובה שלך</label>
                                <input
                                    id="quest-answer"
                                    ref={inputRef}
                                    className="field num"
                                    dir="ltr"
                                    inputMode="text"
                                    autoComplete="off"
                                    value={answer}
                                    onChange={(e) => setAnswer(e.target.value)}
                                />
                                <p className="faint text-xs">שבר כותבים כך: 3/4</p>
                            </div>
                            <button type="submit" className="btn btn-primary" disabled={submit.isPending}>בדיקה</button>
                        </form>
                    ) : (
                        <div className="notice notice--danger">לא הצלחנו להכין שאלה. נסו לרענן את הדף.</div>
                    )}
                </div>
            )}

            <div aria-live="polite">
                {feedback?.text && (
                    <div className={`notice ${feedback.ok ? "notice--mint" : "notice--danger"}`}>
                        {feedback.text}
                        {feedback.unlocked?.length > 0 && (
                            <p className="mt-2 font-semibold">
                                פרסים חדשים: {feedback.unlocked.map((u) => `${u.emoji} ${u.name}`).join(" · ")}
                            </p>
                        )}
                    </div>
                )}
            </div>

            <div className="flex flex-wrap gap-2" aria-label="פרסי המסע">
                {q.rewards.map((r) => (
                    <span key={r.code} className={`chip ${r.unlocked ? "chip--mint" : ""}`}>
                        <span aria-hidden="true">{r.unlocked ? r.emoji : "🔒"}</span> {r.name}
                    </span>
                ))}
            </div>
        </section>
    );
}

export default function Quests() {
    const queryClient = useQueryClient();
    const [searchParams, setSearchParams] = useSearchParams();
    const activeQuest = searchParams.get("quest");
    const [status, setStatus] = useState("");

    const quests = useQuery({ queryKey: QUESTS_KEY, queryFn: () => apiGet(`${QUEST_BASE_PATH}${LIST_QUESTS}`) });
    const cosmetics = useQuery({ queryKey: COSMETICS_KEY, queryFn: () => apiGet(`${QUEST_BASE_PATH}${QUEST_COSMETICS}`) });

    const equip = useMutation({
        mutationFn: (itemCode) => apiPost(`${QUEST_BASE_PATH}${EQUIP_COSMETIC}`, { itemCode }),
        onSuccess: (res) => {
            queryClient.setQueryData(COSMETICS_KEY, res.data);
            setStatus(res.errorCode || "");
        },
        onError: (e) => setStatus(errorMessage(e, "לא הצלחנו לעדכן")),
    });

    const open = (code) => setSearchParams({ quest: code });
    const back = () => setSearchParams({});

    return (
        <div className="page page--wide space-y-5" dir="rtl">
            <MyRoom cosmetics={cosmetics.data} />

            {activeQuest ? (
                <QuestPlay key={activeQuest} questCode={activeQuest} onBack={back} />
            ) : (
                <section className="glass p-6 sm:p-8 space-y-5 reveal" style={{ "--i": 1 }} aria-labelledby="quests-list-title">
                    <h2 id="quests-list-title" className="title text-2xl">בחירת מסע</h2>
                    {quests.isLoading && <SkeletonList rows={2} />}
                    {quests.isError && (
                        <div className="notice notice--danger">{errorMessage(quests.error, "לא הצלחנו לטעון את המסעות")}</div>
                    )}
                    {quests.data && quests.data.length === 0 && (
                        <EmptyState title="אין מסעות כרגע" body="בקרוב יתווספו מסעות חדשים." />
                    )}
                    {quests.data && quests.data.length > 0 && (
                        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                            {quests.data.map((quest, i) => (
                                <QuestCard key={quest.code} quest={quest} index={i} onOpen={open} />
                            ))}
                        </div>
                    )}
                </section>
            )}

            <div aria-live="polite">{status && <div className="notice notice--mint">{status}</div>}</div>
            <Wardrobe cosmetics={cosmetics.data} onEquip={(code) => equip.mutate(code)} pending={equip.isPending} />
        </div>
    );
}
