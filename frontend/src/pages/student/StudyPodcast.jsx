// [s05] Study podcast: a ~5 minute spoken summary built from the student's material and mistakes.
// Played with the AI voice (MP3 from the server) when the AI is configured, or read by the
// browser's Hebrew voice (speechSynthesis) without any key. Playback speed is adjustable.
import { useEffect, useRef, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { FaHeadphones, FaPause, FaPlay, FaStop, FaTrash } from "react-icons/fa";
import EmptyState from "../../components/ui/EmptyState.jsx";
import { Skeleton, SkeletonList } from "../../components/ui/Skeleton.jsx";
import { errorMessage } from "../../lib/api.js";
import SummaryPicker from "../../features/studySummary/SummaryPicker.jsx";
import useBrowserSpeech from "../../features/studySummary/useBrowserSpeech.js";
import {
    createPodcast,
    createPodcastAudio,
    deletePodcast,
    fetchPodcast,
    fetchPodcastAudioUrl,
    fetchPodcasts,
    fetchStudyOptions,
} from "../../features/studySummary/studySummaryApi.js";
import { PLAYBACK_RATES, buildGenerateRequest, formatCreatedAt } from "../../features/studySummary/studySummaryUtils.js";

function AiAudioPlayer({ podcastId, rate }) {
    const audioRef = useRef(null);
    const [src, setSrc] = useState(null);
    const [error, setError] = useState(null);

    useEffect(() => {
        let objectUrl = null;
        let cancelled = false;
        fetchPodcastAudioUrl(podcastId)
            .then((url) => {
                if (cancelled) URL.revokeObjectURL(url);
                else {
                    objectUrl = url;
                    setSrc(url);
                }
            })
            .catch((e) => !cancelled && setError(errorMessage(e, "לא הצלחנו לטעון את ההקראה")));
        return () => {
            cancelled = true;
            if (objectUrl) URL.revokeObjectURL(objectUrl);
        };
    }, [podcastId]);

    useEffect(() => {
        if (audioRef.current) audioRef.current.playbackRate = rate;
    }, [rate, src]);

    if (error) return <p className="notice notice--danger" role="alert">{error}</p>;
    if (!src) return <Skeleton className="h-12 w-full" />;
    return (
        <audio
            ref={audioRef}
            src={src}
            controls
            className="w-full"
            aria-label="הקראה בקול AI"
            onLoadedMetadata={(e) => { e.currentTarget.playbackRate = rate; }}
        />
    );
}

function PodcastPlayer({ podcast, aiAvailable, onDelete, deleting }) {
    const queryClient = useQueryClient();
    const [rate, setRate] = useState(1);
    const [showScript, setShowScript] = useState(false);
    const speech = useBrowserSpeech();

    const voice = useMutation({
        mutationFn: () => createPodcastAudio(podcast.id),
        onSuccess: (response) => {
            speech.stop();
            queryClient.setQueryData(["studySummary", "podcast", podcast.id], response.data);
            queryClient.invalidateQueries({ queryKey: ["studySummary", "podcasts"] });
        },
    });

    const changeRate = (value) => {
        const next = Number(value);
        setRate(next);
        speech.setRate(next);
    };

    return (
        <article className="glass p-6 sm:p-8 space-y-5" aria-labelledby="podcast-title">
            <header className="flex flex-wrap items-start justify-between gap-3">
                <div>
                    <span className="eyebrow">סיכום קולי · {formatCreatedAt(podcast.createdAt)}</span>
                    <h2 id="podcast-title" className="display text-2xl sm:text-3xl mt-1">{podcast.title}</h2>
                    <div className="flex flex-wrap gap-2 mt-2">
                        <span className="chip">כ-<span className="num">{podcast.estimatedMinutes}</span> דקות</span>
                        {!podcast.aiGenerated && <span className="chip chip--amber">תסריט בסיסי ללא AI</span>}
                    </div>
                </div>
                <button type="button" className="btn btn-ghost btn-sm" onClick={onDelete} disabled={deleting}
                        aria-label="מחיקת הסיכום הקולי">
                    <FaTrash aria-hidden="true" />
                </button>
            </header>

            <div className="field-wrap max-w-xs">
                <label htmlFor="podcast-rate" className="label">מהירות השמעה</label>
                <select id="podcast-rate" className="field" value={rate} onChange={(e) => changeRate(e.target.value)}>
                    {PLAYBACK_RATES.map((r) => <option key={r} value={r}>{`x${r}`}</option>)}
                </select>
            </div>

            <section className="glass--sunk p-4 sm:p-5 space-y-3" aria-labelledby="podcast-ai-voice">
                <h3 id="podcast-ai-voice" className="title text-base">קול AI</h3>
                {podcast.hasAudio ? (
                    <AiAudioPlayer podcastId={podcast.id} rate={rate} />
                ) : aiAvailable ? (
                    <>
                        <button type="button" className="btn btn-primary" disabled={voice.isPending} onClick={() => voice.mutate()}>
                            {voice.isPending ? "מפיקים הקראה..." : "הפיקו הקראה בקול AI"}
                        </button>
                        {voice.isError && (
                            <p className="notice notice--danger" role="alert">
                                {errorMessage(voice.error, "לא הצלחנו להפיק הקראה. אפשר להשתמש בהקראה של הדפדפן.")}
                            </p>
                        )}
                    </>
                ) : (
                    <p className="faint text-sm">עוזר ה-AI עדיין לא הופעל בשרת, אז אין קול AI. ההקראה של הדפדפן עובדת גם בלעדיו.</p>
                )}
            </section>

            <section className="glass--sunk p-4 sm:p-5 space-y-3" aria-labelledby="podcast-browser-voice">
                <h3 id="podcast-browser-voice" className="title text-base">הקראה בדפדפן (עברית)</h3>
                {!speech.supported ? (
                    <p className="faint text-sm">הדפדפן הזה לא תומך בהקראה קולית.</p>
                ) : (
                    <>
                        <div className="flex flex-wrap gap-2">
                            {speech.status === "idle" && (
                                <button type="button" className="btn btn-mint" onClick={() => speech.play(podcast.script)}>
                                    <FaPlay aria-hidden="true" /> השמעה
                                </button>
                            )}
                            {speech.status === "playing" && (
                                <button type="button" className="btn btn-ghost" onClick={speech.pause}>
                                    <FaPause aria-hidden="true" /> השהיה
                                </button>
                            )}
                            {speech.status === "paused" && (
                                <button type="button" className="btn btn-mint" onClick={speech.resume}>
                                    <FaPlay aria-hidden="true" /> המשך
                                </button>
                            )}
                            {speech.status !== "idle" && (
                                <button type="button" className="btn btn-ghost" onClick={speech.stop}>
                                    <FaStop aria-hidden="true" /> עצירה
                                </button>
                            )}
                        </div>
                        {speech.status !== "idle" && (
                            <p className="faint text-xs" aria-live="polite">
                                משפט <span className="num">{speech.position.index + 1}</span> מתוך{" "}
                                <span className="num">{speech.position.total}</span>
                            </p>
                        )}
                    </>
                )}
            </section>

            <div>
                <button type="button" className="btn btn-ghost btn-sm" aria-expanded={showScript}
                        aria-controls="podcast-script" onClick={() => setShowScript((v) => !v)}>
                    {showScript ? "הסתרת התסריט" : "הצגת התסריט"}
                </button>
                {showScript && (
                    <p id="podcast-script" className="muted leading-loose whitespace-pre-line mt-3">{podcast.script}</p>
                )}
            </div>
        </article>
    );
}

export default function StudyPodcast() {
    const queryClient = useQueryClient();
    const [choice, setChoice] = useState({ testId: "", topics: [] });
    const [selectedId, setSelectedId] = useState(null);
    const [failed, setFailed] = useState(null);

    const options = useQuery({ queryKey: ["studySummary", "options"], queryFn: fetchStudyOptions });
    const podcasts = useQuery({ queryKey: ["studySummary", "podcasts"], queryFn: fetchPodcasts });
    const podcast = useQuery({
        queryKey: ["studySummary", "podcast", selectedId],
        queryFn: () => fetchPodcast(selectedId),
        enabled: selectedId != null,
    });

    const create = useMutation({
        mutationFn: (basic) => createPodcast(buildGenerateRequest(choice, basic)),
        onMutate: () => setFailed(null),
        onSuccess: (response) => {
            queryClient.setQueryData(["studySummary", "podcast", response.data.id], response.data);
            setSelectedId(response.data.id);
            queryClient.invalidateQueries({ queryKey: ["studySummary", "podcasts"] });
        },
        onError: (error) => setFailed(errorMessage(error, "לא הצלחנו להכין את הסיכום. נסו שוב.")),
    });

    const remove = useMutation({
        mutationFn: deletePodcast,
        onSuccess: () => {
            setSelectedId(null);
            queryClient.invalidateQueries({ queryKey: ["studySummary", "podcasts"] });
        },
        onError: (error) => setFailed(errorMessage(error, "המחיקה נכשלה")),
    });

    const aiAvailable = options.data?.aiAvailable;

    return (
        <div className="page page--wide space-y-4 sm:space-y-5" dir="rtl">
            <div className="glass p-6 sm:p-8 reveal">
                <div className="flex items-center gap-3">
                    <span className="icon-badge icon-badge--mint"><FaHeadphones aria-hidden="true" /></span>
                    <div>
                        <span className="eyebrow">Study podcast · סיכום קולי</span>
                        <h1 className="display text-3xl sm:text-4xl mt-1">סיכום קולי בסגנון פודקאסט</h1>
                    </div>
                </div>
                <p className="muted mt-4 leading-relaxed">
                    סיכום של כ-5 דקות להאזנה, מבוסס על החומר של המבחן ועל הטעויות שלכם. אפשר להאזין בקול AI או בהקראה
                    של הדפדפן, ולשנות את מהירות ההשמעה.
                </p>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-12 gap-4 sm:gap-5">
                <div className="md:col-span-5 space-y-4">
                    <section className="glass p-5 sm:p-6 space-y-4 reveal" style={{ "--i": 1 }} aria-labelledby="podcast-new">
                        <h2 id="podcast-new" className="title text-lg">סיכום חדש</h2>
                        {options.isLoading ? (
                            <div role="status" aria-live="polite" className="space-y-3">
                                <span className="sr-only">טוען...</span>
                                <Skeleton className="h-10 w-full" />
                                <Skeleton className="h-8 w-2/3" />
                            </div>
                        ) : (
                            <SummaryPicker idPrefix="podcast" options={options.data} value={choice} onChange={setChoice} />
                        )}
                        {aiAvailable === false && (
                            <p className="notice">עוזר ה-AI עדיין לא הופעל בשרת. אפשר להכין תסריט בסיסי ולהאזין לו בהקראה של הדפדפן.</p>
                        )}
                        {failed && <p className="notice notice--danger" role="alert">{failed}</p>}
                        <div className="flex flex-wrap gap-2">
                            {aiAvailable !== false && (
                                <button type="button" className="btn btn-primary" disabled={create.isPending}
                                        onClick={() => create.mutate(false)}>
                                    {create.isPending ? "כותבים את התסריט..." : "הכינו לי סיכום קולי"}
                                </button>
                            )}
                            {(aiAvailable === false || failed) && (
                                <button type="button" className={`btn ${aiAvailable === false ? "btn-primary" : "btn-ghost"}`}
                                        disabled={create.isPending} onClick={() => create.mutate(true)}>
                                    תסריט בסיסי בלי AI
                                </button>
                            )}
                        </div>
                    </section>

                    <section className="glass p-5 sm:p-6 space-y-3 reveal" style={{ "--i": 2 }} aria-labelledby="podcast-saved">
                        <h2 id="podcast-saved" className="title text-lg">הסיכומים שלי</h2>
                        {podcasts.isLoading ? (
                            <SkeletonList rows={3} />
                        ) : !podcasts.data?.length ? (
                            <EmptyState title="עדיין אין סיכומים קוליים" body="סיכום שתכינו יישמר כאן ותוכלו להאזין לו שוב." />
                        ) : (
                            <ul className="space-y-2">
                                {podcasts.data.map((p) => (
                                    <li key={p.id}>
                                        <button type="button" onClick={() => setSelectedId(p.id)}
                                                className={`choice ${selectedId === p.id ? "is-active" : ""}`}
                                                aria-current={selectedId === p.id ? "true" : undefined}>
                                            <span className="flex-1">
                                                <span className="block">{p.title}</span>
                                                <span className="faint text-xs num">{formatCreatedAt(p.createdAt)}</span>
                                            </span>
                                            {p.hasAudio && <FaHeadphones aria-label="יש הקראת AI" />}
                                        </button>
                                    </li>
                                ))}
                            </ul>
                        )}
                    </section>
                </div>

                <div className="md:col-span-7">
                    {selectedId == null ? (
                        <div className="glass p-6 reveal" style={{ "--i": 3 }}>
                            <EmptyState title="בחרו או הכינו סיכום קולי" body="הנגן יופיע כאן." />
                        </div>
                    ) : podcast.isLoading ? (
                        <div className="glass p-6"><SkeletonList rows={3} /></div>
                    ) : podcast.isError ? (
                        <p className="notice notice--danger" role="alert">{errorMessage(podcast.error, "לא הצלחנו לפתוח את הסיכום")}</p>
                    ) : podcast.data ? (
                        <PodcastPlayer
                            key={podcast.data.id}
                            podcast={podcast.data}
                            aiAvailable={aiAvailable}
                            deleting={remove.isPending}
                            onDelete={() => window.confirm("למחוק את הסיכום הקולי?") && remove.mutate(podcast.data.id)}
                        />
                    ) : null}
                </div>
            </div>
        </div>
    );
}
