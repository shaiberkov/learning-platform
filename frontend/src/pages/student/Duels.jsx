import { useContext, useEffect, useMemo, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { FaFistRaised, FaHandshake, FaCheckCircle, FaTimesCircle } from "react-icons/fa";
import { UserContext } from "../../context/UserContext.jsx";
import { apiGet, apiPost, errorMessage } from "../../lib/api.js";
import {
    DUEL_ACCEPT,
    DUEL_ANSWER,
    DUEL_BASE_PATH,
    DUEL_DECLINE,
    DUEL_FORFEIT,
    DUEL_INVITE,
    DUEL_LIST,
    DUEL_OPPONENTS,
    DUEL_TOPICS,
    duelPath,
} from "../../constants/pages.constants.js";
import EmptyState from "../../components/ui/EmptyState.jsx";
import { SkeletonList } from "../../components/ui/Skeleton.jsx";
import {
    DUEL_MODE_LABELS,
    duelResult,
    duelSides,
    duelStatusLabel,
    nextQuestion,
} from "../../features/competition/competitionHelpers.js";

// [s03] 1-on-1 duels: the lobby at /duels and the live duel at /duels/:duelId. Both screens poll the server.
const LIST_POLL_MS = 3000;
const DUEL_POLL_MS = 1500;

function Duels() {
    const { duelId } = useParams();
    return duelId ? <DuelArena duelId={duelId} /> : <DuelLobby />;
}

function DuelLobby() {
    const { user } = useContext(UserContext);
    const navigate = useNavigate();
    const queryClient = useQueryClient();
    const [topic, setTopic] = useState("");
    const [subTopic, setSubTopic] = useState("");
    const [mode, setMode] = useState("COMPETITIVE");
    const [opponentId, setOpponentId] = useState("");
    const [message, setMessage] = useState(null);

    const topics = useQuery({ queryKey: ["duels", "topics"], queryFn: () => apiGet(`${DUEL_BASE_PATH}${DUEL_TOPICS}`) });
    const mine = useQuery({
        queryKey: ["duels", "mine"],
        queryFn: () => apiGet(`${DUEL_BASE_PATH}${DUEL_LIST}`),
        refetchInterval: LIST_POLL_MS,
    });
    const opponents = useQuery({
        queryKey: ["duels", "opponents", subTopic],
        queryFn: () => apiGet(`${DUEL_BASE_PATH}${DUEL_OPPONENTS}`, { subTopic }),
        enabled: !!subTopic,
    });

    const subTopics = useMemo(
        () => topics.data?.find((t) => t.topic === topic)?.subTopics ?? [],
        [topics.data, topic]
    );

    useEffect(() => {
        setOpponentId(opponents.data?.opponents?.[0]?.userId ?? "");
    }, [opponents.data]);

    const invite = useMutation({
        mutationFn: () => apiPost(`${DUEL_BASE_PATH}${DUEL_INVITE}`, null, { opponentId, subTopic, mode }),
        onSuccess: (res) => {
            queryClient.invalidateQueries({ queryKey: ["duels", "mine"] });
            navigate(`/duels/${res.data.duel.id}`);
        },
        onError: (e) => setMessage({ tone: "danger", text: errorMessage(e, "שליחת ההזמנה נכשלה") }),
    });

    const act = useMutation({
        mutationFn: ({ id, action }) => apiPost(duelPath(id, action)),
        onSuccess: (res, { id, action }) => {
            queryClient.invalidateQueries({ queryKey: ["duels", "mine"] });
            if (action === DUEL_ACCEPT) navigate(`/duels/${id}`);
            else setMessage({ tone: "mint", text: res.errorCode });
        },
        onError: (e) => setMessage({ tone: "danger", text: errorMessage(e, "הפעולה נכשלה") }),
    });

    return (
        <div className="page page--medium space-y-5" dir="rtl">
            <header className="glass p-6 sm:p-8 reveal flex items-center gap-4">
                <span className="icon-badge icon-badge--amber"><FaFistRaised /></span>
                <div>
                    <span className="eyebrow">Duels · דו-קרבות</span>
                    <h1 className="title text-2xl mt-1">דו-קרב של 5 שאלות</h1>
                    <p className="muted text-sm mt-1">
                        הזמינו חבר מבית הספר, ענו על אותן שאלות וראו את ההתקדמות של השני בזמן אמת.
                        היריבים המוצעים קרובים אליכם ברמה, והשאלות מותאמות לחלש מביניכם.
                    </p>
                </div>
            </header>

            {message && (
                <div role="status" className={`notice ${message.tone === "danger" ? "notice--danger" : "notice--mint"}`}>
                    {message.text}
                </div>
            )}

            <section className="glass p-6 space-y-4 reveal" style={{ "--i": 1 }} aria-labelledby="my-duels-title">
                <h2 id="my-duels-title" className="title text-lg">הדו-קרבות שלי</h2>
                {mine.isLoading && <SkeletonList rows={2} />}
                {mine.isError && <div className="notice notice--danger">{errorMessage(mine.error, "טעינת הדו-קרבות נכשלה")}</div>}
                {mine.data?.length === 0 && (
                    <EmptyState title="עוד אין דו-קרבות" body="בחרו נושא ויריב למטה ושלחו הזמנה." icon={<FaFistRaised />} />
                )}
                <ul className="space-y-3">
                    {mine.data?.map((d) => {
                        const sides = duelSides(d, user?.userId);
                        const incoming = d.status === "PENDING" && !sides.iAmChallenger;
                        const outgoing = d.status === "PENDING" && sides.iAmChallenger;
                        return (
                            <li key={d.id} className="row-card flex flex-wrap items-center gap-3 justify-between">
                                <div className="space-y-1">
                                    <p className="font-medium">
                                        {d.mode === "COOP" ? `את/ה ו${sides.theirName} נגד המערכת` : `נגד ${sides.theirName}`}
                                    </p>
                                    <p className="muted text-sm">{d.subTopic} · {DUEL_MODE_LABELS[d.mode]}</p>
                                </div>
                                <div className="flex flex-wrap items-center gap-2">
                                    <span className={`chip ${d.status === "ACTIVE" || incoming ? "chip--mint" : ""}`}>
                                        {duelStatusLabel(d, user?.userId)}
                                    </span>
                                    {incoming && (
                                        <>
                                            <button className="btn btn-primary btn-sm" disabled={act.isPending}
                                                    onClick={() => act.mutate({ id: d.id, action: DUEL_ACCEPT })}>
                                                קבלה
                                            </button>
                                            <button className="btn btn-ghost btn-sm" disabled={act.isPending}
                                                    onClick={() => act.mutate({ id: d.id, action: DUEL_DECLINE })}>
                                                דחייה
                                            </button>
                                        </>
                                    )}
                                    {outgoing && (
                                        <button className="btn btn-ghost btn-sm" disabled={act.isPending}
                                                onClick={() => act.mutate({ id: d.id, action: DUEL_DECLINE })}>
                                            ביטול
                                        </button>
                                    )}
                                    {(d.status === "ACTIVE" || d.status === "FINISHED" || outgoing) && (
                                        <Link to={`/duels/${d.id}`} className="btn btn-ghost btn-sm">
                                            {d.status === "ACTIVE" ? "לדו-קרב" : "צפייה"}
                                        </Link>
                                    )}
                                </div>
                            </li>
                        );
                    })}
                </ul>
            </section>

            <section className="glass p-6 space-y-5 reveal" style={{ "--i": 2 }} aria-labelledby="new-duel-title">
                <h2 id="new-duel-title" className="title text-lg">דו-קרב חדש</h2>

                <div className="grid sm:grid-cols-2 gap-4">
                    <label className="field-wrap">
                        <span className="label">נושא</span>
                        <select className="field" value={topic}
                                onChange={(e) => { setTopic(e.target.value); setSubTopic(""); }}>
                            <option value="">בחרו נושא</option>
                            {topics.data?.map((t) => <option key={t.topic} value={t.topic}>{t.topic}</option>)}
                        </select>
                    </label>
                    <label className="field-wrap">
                        <span className="label">תת-נושא</span>
                        <select className="field" value={subTopic} disabled={!topic}
                                onChange={(e) => setSubTopic(e.target.value)}>
                            <option value="">בחרו תת-נושא</option>
                            {subTopics.map((s) => <option key={s} value={s}>{s}</option>)}
                        </select>
                    </label>
                </div>

                <fieldset className="space-y-2">
                    <legend className="label">סוג הדו-קרב</legend>
                    <div className="grid sm:grid-cols-2 gap-3">
                        <button type="button" aria-pressed={mode === "COMPETITIVE"}
                                className={`choice ${mode === "COMPETITIVE" ? "is-active" : ""}`}
                                onClick={() => setMode("COMPETITIVE")}>
                            <FaFistRaised aria-hidden="true" /> אחד על אחד: מי עונה נכון יותר
                        </button>
                        <button type="button" aria-pressed={mode === "COOP"}
                                className={`choice ${mode === "COOP" ? "is-active" : ""}`}
                                onClick={() => setMode("COOP")}>
                            <FaHandshake aria-hidden="true" /> שיתופי: שניכם יחד נגד המערכת
                        </button>
                    </div>
                </fieldset>

                {subTopic && (
                    <div className="space-y-3">
                        <div className="flex items-center justify-between">
                            <h3 className="font-medium">בחירת יריב</h3>
                            {opponents.data && (
                                <span className="chip">הרמה שלך: <span className="num">{opponents.data.myRating}</span></span>
                            )}
                        </div>
                        {opponents.isLoading && <SkeletonList rows={3} />}
                        {opponents.isError && (
                            <div className="notice notice--danger">{errorMessage(opponents.error, "טעינת היריבים נכשלה")}</div>
                        )}
                        {opponents.data?.opponents?.length === 0 && (
                            <EmptyState title="אין תלמידים נוספים בבית הספר" body="אפשר יהיה להזמין חברים כשיצטרפו." />
                        )}
                        <div role="radiogroup" aria-label="יריבים מוצעים" className="grid gap-2 max-h-80 overflow-y-auto">
                            {opponents.data?.opponents?.map((o) => (
                                <button key={o.userId} type="button" role="radio" aria-checked={opponentId === o.userId}
                                        className={`choice justify-between ${opponentId === o.userId ? "is-active" : ""}`}
                                        onClick={() => setOpponentId(o.userId)}>
                                    <span>{o.username}</span>
                                    <span className="flex gap-2 items-center">
                                        {o.recommended && <span className="chip chip--mint">התאמה טובה</span>}
                                        {o.sameClass && <span className="chip">מהכיתה</span>}
                                        <span className="num text-sm" aria-label={`רמה ${o.rating}`}>{o.rating}</span>
                                    </span>
                                </button>
                            ))}
                        </div>
                    </div>
                )}

                <button className="btn btn-primary" disabled={!subTopic || !opponentId || invite.isPending}
                        onClick={() => { setMessage(null); invite.mutate(); }}>
                    {invite.isPending ? "שולח..." : "שליחת הזמנה"}
                </button>
            </section>
        </div>
    );
}

function ProgressBar({ label, value, max }) {
    const pct = max ? Math.round((value / max) * 100) : 0;
    return (
        <div className="space-y-1">
            <div className="flex justify-between text-sm">
                <span>{label}</span>
                <span className="num">{value}/{max}</span>
            </div>
            <div role="progressbar" aria-label={label} aria-valuemin={0} aria-valuemax={max} aria-valuenow={value}
                 className="h-2.5 rounded-full bg-surface-strong overflow-hidden">
                <div className="h-full bg-accent transition-all duration-500" style={{ width: `${pct}%` }} />
            </div>
        </div>
    );
}

function DuelArena({ duelId }) {
    const { user } = useContext(UserContext);
    const queryClient = useQueryClient();
    const [typed, setTyped] = useState("");
    const [feedback, setFeedback] = useState(null);

    const state = useQuery({
        queryKey: ["duel", duelId],
        queryFn: () => apiGet(duelPath(duelId)),
        refetchInterval: (query) => {
            const status = query.state.data?.duel?.status;
            return status === "PENDING" || status === "ACTIVE" ? DUEL_POLL_MS : false;
        },
    });

    const answer = useMutation({
        mutationFn: ({ position, value }) => apiPost(duelPath(duelId, DUEL_ANSWER), null, { position, answer: value }),
        onSuccess: (res) => {
            queryClient.setQueryData(["duel", duelId], res.data);
            setFeedback({ tone: res.errorCode === "נכון!" ? "mint" : "danger", text: res.errorCode });
            setTyped("");
        },
        onError: (e) => setFeedback({ tone: "danger", text: errorMessage(e, "שליחת התשובה נכשלה") }),
    });

    const act = useMutation({
        mutationFn: (action) => apiPost(duelPath(duelId, action)),
        onSuccess: (res) => queryClient.setQueryData(["duel", duelId], res.data),
        onError: (e) => setFeedback({ tone: "danger", text: errorMessage(e, "הפעולה נכשלה") }),
    });

    if (state.isLoading) {
        return <div className="page page--narrow" dir="rtl"><SkeletonList rows={3} /></div>;
    }
    if (state.isError || !state.data) {
        return (
            <div className="page page--narrow" dir="rtl">
                <div className="glass p-6">
                    <EmptyState title="הדו-קרב לא נמצא" body={errorMessage(state.error, "")} actionLabel="לכל הדו-קרבות" actionTo="/duels" />
                </div>
            </div>
        );
    }

    const { duel, questions } = state.data;
    const sides = duelSides(duel, user?.userId);
    const current = duel.status === "ACTIVE" ? nextQuestion(questions) : null;
    const coop = duel.mode === "COOP";
    const result = duel.status === "FINISHED" ? duelResult(duel, user?.userId) : null;

    return (
        <div className="page page--narrow space-y-5" dir="rtl">
            <div className="glass p-6 sm:p-8 space-y-6 reveal">
                <div className="flex items-center gap-4">
                    <span className="icon-badge icon-badge--amber">{coop ? <FaHandshake /> : <FaFistRaised />}</span>
                    <div>
                        <span className="eyebrow">{DUEL_MODE_LABELS[duel.mode]} · רמה <span className="num">{duel.level}</span></span>
                        <h1 className="title text-2xl mt-1">
                            {coop ? `את/ה ו${sides.theirName} נגד המערכת` : `${sides.myName} נגד ${sides.theirName}`}
                        </h1>
                        <p className="muted text-sm">{duel.topic} · {duel.subTopic}</p>
                    </div>
                </div>

                <section aria-label="התקדמות" aria-live="polite" className="space-y-3">
                    <ProgressBar label={`את/ה: ${sides.myCorrect} נכונות`} value={sides.myAnswered} max={duel.questionCount} />
                    <ProgressBar label={`${sides.theirName}: ${sides.theirCorrect} נכונות`} value={sides.theirAnswered} max={duel.questionCount} />
                    {coop && (
                        <p className="text-sm">
                            ניקוד הצוות: <span className="num">{sides.myCorrect + sides.theirCorrect}</span>
                            {" "}· המערכת: <span className="num">{duel.systemScore}</span>. צריך לעבור את המערכת יחד.
                        </p>
                    )}
                </section>

                {duel.status === "PENDING" && (sides.iAmChallenger ? (
                    <div className="space-y-3">
                        <div className="notice">ממתינים ש{sides.theirName} יאשר את ההזמנה. המסך יתעדכן לבד.</div>
                        <button className="btn btn-ghost" disabled={act.isPending} onClick={() => act.mutate(DUEL_DECLINE)}>ביטול ההזמנה</button>
                    </div>
                ) : (
                    <div className="space-y-3">
                        <div className="notice notice--mint">{sides.theirName} הזמין/ה אותך לדו-קרב!</div>
                        <div className="flex gap-2">
                            <button className="btn btn-primary" disabled={act.isPending} onClick={() => act.mutate(DUEL_ACCEPT)}>קבלה והתחלה</button>
                            <button className="btn btn-ghost" disabled={act.isPending} onClick={() => act.mutate(DUEL_DECLINE)}>דחייה</button>
                        </div>
                    </div>
                ))}

                {duel.status === "DECLINED" && <div className="notice">הדו-קרב בוטל.</div>}

                {current && (
                    <section className="space-y-4" aria-labelledby="duel-question">
                        <span className="chip">שאלה <span className="num">{current.position + 1}</span> מתוך <span className="num">{duel.questionCount}</span></span>
                        <p id="duel-question" className="display text-2xl whitespace-pre-wrap break-words" dir="ltr">{current.questionText}</p>
                        <form className="flex gap-2" onSubmit={(e) => {
                            e.preventDefault();
                            if (typed.trim()) answer.mutate({ position: current.position, value: typed });
                        }}>
                            <input className="field text-lg" dir="ltr" value={typed} maxLength={200} autoFocus
                                   aria-label="התשובה שלך" placeholder="התשובה שלך"
                                   onChange={(e) => setTyped(e.target.value)} />
                            <button className="btn btn-primary" type="submit" disabled={answer.isPending || !typed.trim()}>שליחה</button>
                        </form>
                    </section>
                )}

                {duel.status === "ACTIVE" && !current && (
                    <div className="notice notice--mint">סיימת! מחכים ש{sides.theirName} יסיים/תסיים...</div>
                )}

                {duel.status === "ACTIVE" && (
                    <button className="btn btn-ghost" disabled={act.isPending}
                            onClick={() => {
                                const warn = coop
                                    ? "לסיים את הדו-קרב עכשיו? שאלות שלא נענו ייחשבו כטעויות."
                                    : "לעזוב את הדו-קרב? אם היריב/ה עדיין פעיל/ה, זה ייחשב כהפסד.";
                                if (window.confirm(warn)) act.mutate(DUEL_FORFEIT);
                            }}>
                        {coop ? "סיום הדו-קרב" : "עזיבת הדו-קרב"}
                    </button>
                )}

                {feedback && duel.status !== "PENDING" && (
                    <div role="status" className={`notice ${feedback.tone === "mint" ? "notice--mint" : "notice--danger"}`}>{feedback.text}</div>
                )}

                {result && (
                    <div className={`notice ${result.tone === "mint" ? "notice--mint" : result.tone === "danger" ? "notice--danger" : ""} text-lg`}>
                        {result.label}
                    </div>
                )}

                {duel.status === "FINISHED" && (
                    <ul className="space-y-2" aria-label="סיכום השאלות">
                        {questions.map((q) => (
                            <li key={q.position} className="row-card space-y-1">
                                <p dir="ltr" className="text-start">{q.questionText}</p>
                                <p className="text-sm flex items-center gap-2">
                                    {q.myCorrect ? <FaCheckCircle className="text-mint" aria-label="נכון" /> : <FaTimesCircle className="text-danger" aria-label="לא נכון" />}
                                    התשובה שלך: <span className="num">{q.myAnswer ?? "-"}</span> · התשובה הנכונה: <span className="num">{q.correctAnswer}</span>
                                </p>
                            </li>
                        ))}
                    </ul>
                )}

                <Link to="/duels" className="btn btn-ghost">לכל הדו-קרבות</Link>
            </div>
        </div>
    );
}

export default Duels;
