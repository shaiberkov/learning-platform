import { useEffect, useRef, useState } from "react";
import axios from "axios";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { FaUsers, FaDoorOpen, FaPlus, FaPaperPlane, FaArrowRight } from "react-icons/fa";
import { apiGet, apiPost, errorMessage } from "../../lib/api.js";
import { SkeletonList } from "../../components/ui/Skeleton.jsx";
import EmptyState from "../../components/ui/EmptyState.jsx";
import ProgressRing from "../../components/ui/ProgressRing.jsx";
import { BREAK, formatClock, roomPhase } from "./studyTimer.js";
import {
    STUDY_ROOMS_BASE_PATH, STUDY_ROOMS_LIST, STUDY_ROOMS_CREATE, STUDY_ROOMS_HEARTBEAT,
    STUDY_ROOMS_LEAVE, STUDY_ROOMS_CHAT,
} from "../../constants/pages.constants.js";

const HEARTBEAT_MS = 3000;
const PHASE_LABEL = { FOCUS: "ריכוז", BREAK: "הפסקה" };

// The room state plus this client's clock offset to the server, so the countdown matches every member's.
async function fetchRoom(roomId, task) {
    const res = await apiPost(`${STUDY_ROOMS_BASE_PATH}${STUDY_ROOMS_HEARTBEAT}`, { roomId, task: task || undefined });
    return { ...res.data, clientOffset: res.data.serverNowMs - Date.now() };
}

function useNow() {
    const [now, setNow] = useState(() => Date.now());
    useEffect(() => {
        const id = setInterval(() => setNow(Date.now()), 1000);
        return () => clearInterval(id);
    }, []);
    return now;
}

function RoomList({ onJoin }) {
    const [name, setName] = useState("");
    const rooms = useQuery({
        queryKey: ["studyRooms"],
        queryFn: () => apiGet(`${STUDY_ROOMS_BASE_PATH}${STUDY_ROOMS_LIST}`),
        refetchInterval: 5000,
    });
    const create = useMutation({
        mutationFn: (roomName) => apiPost(`${STUDY_ROOMS_BASE_PATH}${STUDY_ROOMS_CREATE}`, { name: roomName }),
        onSuccess: (res) => onJoin(res.data.id),
    });

    const submit = (e) => {
        e.preventDefault();
        if (name.trim().length >= 2) create.mutate(name.trim());
    };

    return (
        <div className="page page--wide space-y-5" dir="rtl">
            <div className="glass p-6 sm:p-8 space-y-5 reveal">
                <div className="flex items-center gap-3">
                    <span className="icon-badge"><FaUsers /></span>
                    <div>
                        <span className="eyebrow">Study rooms · חדרי למידה</span>
                        <h2 className="display text-3xl sm:text-4xl">לומדים יחד</h2>
                    </div>
                </div>
                <p className="muted">
                    פתחו חדר או הצטרפו לחדר של חברים מבית הספר. כולם לומדים בטיימר משותף: 25 דקות ריכוז ו-5 דקות הפסקה.
                    הצ'אט נפתח רק בהפסקה.
                </p>
                <form onSubmit={submit} className="flex flex-wrap items-end gap-3">
                    <div className="field-wrap flex-1 min-w-[14rem]">
                        <label htmlFor="room-name" className="label">שם החדר החדש</label>
                        <input id="room-name" className="field" maxLength={60} value={name}
                               placeholder="למשל: מתכוננים למבחן באלגברה"
                               onChange={(e) => setName(e.target.value)} />
                    </div>
                    <button type="submit" className="btn btn-primary" disabled={create.isPending || name.trim().length < 2}>
                        <FaPlus aria-hidden="true" /> פתיחת חדר
                    </button>
                </form>
                {create.isError && (
                    <div className="notice notice--danger" role="alert">{errorMessage(create.error, "לא הצלחנו לפתוח את החדר")}</div>
                )}
            </div>

            <div className="glass p-6 sm:p-8 space-y-4 reveal" style={{ "--i": 1 }}>
                <h3 className="title text-xl">חדרים פתוחים בבית הספר</h3>
                {rooms.isPending && <SkeletonList rows={3} />}
                {rooms.isError && (
                    <div className="notice notice--danger" role="alert">{errorMessage(rooms.error, "לא הצלחנו לטעון את החדרים")}</div>
                )}
                {rooms.data && rooms.data.length === 0 && (
                    <EmptyState title="אין עדיין חדרים פתוחים" body="פתחו חדר ראשון, וחברים מבית הספר יוכלו להצטרף." icon={<FaDoorOpen />} />
                )}
                {rooms.data && rooms.data.length > 0 && (
                    <ul className="space-y-3">
                        {rooms.data.map((room) => (
                            <li key={room.id} className="row-card flex flex-wrap items-center justify-between gap-3">
                                <div className="min-w-0 space-y-1">
                                    <p className="text-ink font-medium">{room.name}</p>
                                    <div className="flex flex-wrap gap-2">
                                        <span className="chip"><span className="num">{room.memberCount}</span>&nbsp;בחדר</span>
                                        <span className={`chip ${room.phase === BREAK ? "chip--mint" : "chip--amber"}`}>
                                            {PHASE_LABEL[room.phase]}
                                        </span>
                                    </div>
                                </div>
                                <button type="button" className="btn btn-ghost btn-sm" onClick={() => onJoin(room.id)}
                                        aria-label={`הצטרפות לחדר ${room.name}`}>
                                    <FaDoorOpen aria-hidden="true" /> הצטרפות
                                </button>
                            </li>
                        ))}
                    </ul>
                )}
            </div>
        </div>
    );
}

function RoomView({ roomId, onLeave }) {
    const queryClient = useQueryClient();
    const [task, setTask] = useState("");
    const [text, setText] = useState("");
    const taskRef = useRef("");
    const now = useNow();

    const room = useQuery({
        queryKey: ["studyRoom", roomId],
        queryFn: () => fetchRoom(roomId, taskRef.current),
        refetchInterval: HEARTBEAT_MS,
        retry: false,
    });

    // Leaving the screen leaves the room (presence also expires on its own after a few seconds).
    useEffect(() => () => {
        axios.post(`${STUDY_ROOMS_BASE_PATH}${STUDY_ROOMS_LEAVE}`, null, { params: { roomId }, silentError: true })
            .catch(() => {});
    }, [roomId]);

    const send = useMutation({
        mutationFn: (message) => apiPost(`${STUDY_ROOMS_BASE_PATH}${STUDY_ROOMS_CHAT}`, { roomId, text: message }),
        onSuccess: (res) => {
            setText("");
            queryClient.setQueryData(["studyRoom", roomId], { ...res.data, clientOffset: res.data.serverNowMs - Date.now() });
        },
    });

    const saveTask = (e) => {
        e.preventDefault();
        taskRef.current = task.trim();
        room.refetch();
    };

    if (room.isPending) {
        return <div className="page page--wide" dir="rtl"><SkeletonList rows={3} /></div>;
    }
    if (room.isError) {
        return (
            <div className="page page--wide" dir="rtl">
                <div className="glass p-6 sm:p-8 space-y-4">
                    <div className="notice notice--danger" role="alert">{errorMessage(room.error, "לא הצלחנו להיכנס לחדר")}</div>
                    <button type="button" className="btn btn-ghost" onClick={onLeave}><FaArrowRight aria-hidden="true" /> לכל החדרים</button>
                </div>
            </div>
        );
    }

    const data = room.data;
    const phase = roomPhase(data.timerStartedAtMs, now + data.clientOffset, data.focusMinutes, data.breakMinutes);
    const isBreak = phase.phase === BREAK;
    const ringValue = Math.round(phase.progress * 100);

    return (
        <div className="page page--wide space-y-5" dir="rtl">
            <div className="glass p-6 sm:p-8 space-y-5 reveal">
                <div className="flex flex-wrap items-center justify-between gap-3">
                    <div>
                        <span className="eyebrow">חדר למידה</span>
                        <h2 className="display text-3xl">{data.name}</h2>
                    </div>
                    <button type="button" className="btn btn-ghost btn-sm" onClick={onLeave}>
                        <FaArrowRight aria-hidden="true" /> יציאה מהחדר
                    </button>
                </div>

                <div className="flex flex-wrap items-center gap-6">
                    <ProgressRing value={ringValue} size={160} stroke={12}>
                        <div className="text-center">
                            <span className="num text-3xl font-semibold text-ink" role="timer" aria-live="off">
                                {formatClock(phase.secondsLeft)}
                            </span>
                            <p className="faint text-xs">{PHASE_LABEL[phase.phase]}</p>
                        </div>
                    </ProgressRing>
                    <div className="space-y-2 flex-1 min-w-[14rem]">
                        <p className="title text-lg" aria-live="polite">
                            {isBreak ? "הפסקה! אפשר לדבר בצ'אט ולנוח" : "זמן ריכוז. הצ'אט ייפתח בהפסקה"}
                        </p>
                        <form onSubmit={saveTask} className="flex flex-wrap items-end gap-2">
                            <div className="field-wrap flex-1 min-w-[12rem]">
                                <label htmlFor="room-task" className="label">על מה אני עובד/ת עכשיו</label>
                                <input id="room-task" className="field" maxLength={60} value={task}
                                       placeholder="למשל: משוואות" onChange={(e) => setTask(e.target.value)} />
                            </div>
                            <button type="submit" className="btn btn-ghost btn-sm">עדכון</button>
                        </form>
                    </div>
                </div>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
                <div className="glass p-6 space-y-3 reveal" style={{ "--i": 1 }}>
                    <h3 className="title text-lg">מי בחדר עכשיו (<span className="num">{data.members.length}</span>)</h3>
                    <ul className="space-y-2">
                        {data.members.map((m, i) => (
                            <li key={`${m.name}-${i}`} className="row-card flex items-center gap-2">
                                <span className="text-ink font-medium">{m.name}{m.you ? " (את/ה)" : ""}</span>
                                <span className="muted text-sm">{m.task ? `: ${m.task}` : ""}</span>
                            </li>
                        ))}
                    </ul>
                </div>

                <div className="glass p-6 space-y-3 reveal" style={{ "--i": 2 }}>
                    <h3 className="title text-lg">צ'אט ההפסקה</h3>
                    {!isBreak && (
                        <EmptyState title="הצ'אט סגור בזמן ריכוז" body={`ייפתח בעוד ${formatClock(phase.secondsLeft)} דקות.`} />
                    )}
                    {isBreak && (
                        <>
                            <ul className="space-y-2 max-h-64 overflow-y-auto" aria-live="polite">
                                {data.chat.length === 0 && <li className="faint text-sm">עוד אין הודעות בהפסקה הזו.</li>}
                                {data.chat.map((m) => (
                                    <li key={`${m.sentAtMs}-${m.name}`} className={`row-card ${m.you ? "bg-accent-soft" : ""}`}>
                                        <span className="text-ink font-medium">{m.name}: </span>
                                        <span className="text-ink-2">{m.text}</span>
                                    </li>
                                ))}
                            </ul>
                            <form className="flex gap-2 items-end" onSubmit={(e) => {
                                e.preventDefault();
                                if (text.trim()) send.mutate(text.trim());
                            }}>
                                <div className="field-wrap flex-1">
                                    <label htmlFor="room-chat" className="label">הודעה</label>
                                    <input id="room-chat" className="field" maxLength={300} value={text}
                                           onChange={(e) => setText(e.target.value)} />
                                </div>
                                <button type="submit" className="btn btn-primary btn-sm" disabled={send.isPending || !text.trim()}
                                        aria-label="שליחת הודעה">
                                    <FaPaperPlane aria-hidden="true" />
                                </button>
                            </form>
                            {send.isError && (
                                <div className="notice notice--danger" role="alert">{errorMessage(send.error, "ההודעה לא נשלחה")}</div>
                            )}
                        </>
                    )}
                </div>
            </div>
        </div>
    );
}

// Shared study rooms: a common Pomodoro timer, who is studying what, and a chat that opens only in the break.
export default function StudyRooms() {
    const [roomId, setRoomId] = useState(null);
    const queryClient = useQueryClient();

    const leave = () => {
        setRoomId(null);
        queryClient.invalidateQueries({ queryKey: ["studyRooms"] });
    };

    return roomId
        ? <RoomView key={roomId} roomId={roomId} onLeave={leave} />
        : <RoomList onJoin={setRoomId} />;
}
