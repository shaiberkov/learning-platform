import { useContext, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { FaCheck, FaFeatherAlt, FaFlagCheckered, FaTimes } from "react-icons/fa";
import { UserContext } from "../../context/UserContext.jsx";
import { apiGet, apiPost, errorMessage } from "../../lib/api.js";
import { SkeletonList } from "../../components/ui/Skeleton.jsx";
import EmptyState from "../../components/ui/EmptyState.jsx";
import {
    WELLBEING_BASE_PATH, REFLECTION_CURRENT, REFLECTION_HISTORY, REFLECTION_SAVE, REFLECTION_MARK,
} from "../../constants/pages.constants.js";
import { GOAL_TYPES, STATUS_LABEL, goalPercent, goalType, shortDate } from "../../features/wellbeing/wellbeing.js";

// [s10] Weekly reflection journal: three short questions every Sunday and a goal for the week.

function StatusChip({ status }) {
    const s = STATUS_LABEL[status] ?? STATUS_LABEL.IN_PROGRESS;
    return <span className={`chip ${s.tone}`}>{s.text}</span>;
}

function GoalProgress({ reflection }) {
    const type = goalType(reflection.goalType);
    const percent = goalPercent(reflection.progress, reflection.goalTarget);
    if (percent === null) return null;
    return (
        <div className="space-y-1">
            <div className="flex justify-between text-sm muted">
                <span>{type.label}</span>
                <span className="num">{reflection.progress}/{reflection.goalTarget} {type.unit}</span>
            </div>
            <div className="h-2 rounded-full bg-surface-strong overflow-hidden" role="progressbar"
                 aria-valuemin={0} aria-valuemax={100} aria-valuenow={percent} aria-label="התקדמות ליעד">
                <div className="h-full bg-accent" style={{ width: `${percent}%` }} />
            </div>
        </div>
    );
}

function GoalCard({ reflection, title, onMark, marking }) {
    const free = reflection.goalType === "FREE";
    return (
        <div className="row-card space-y-3">
            <div className="flex flex-wrap items-center justify-between gap-2">
                <span className="eyebrow">{title} · <span className="num">{shortDate(reflection.weekStart)}</span></span>
                <StatusChip status={reflection.status} />
            </div>
            <p className="title text-lg">{reflection.nextGoal}</p>
            <GoalProgress reflection={reflection} />
            {free && reflection.status !== "ACHIEVED" && (
                <div className="flex flex-wrap gap-2">
                    <button type="button" className="btn btn-mint btn-sm" disabled={marking} onClick={() => onMark(reflection.id, true)}>
                        <FaCheck aria-hidden="true" /> השגתי את היעד
                    </button>
                    {reflection.achievedMark !== false && (
                        <button type="button" className="btn btn-ghost btn-sm" disabled={marking} onClick={() => onMark(reflection.id, false)}>
                            <FaTimes aria-hidden="true" /> עוד לא
                        </button>
                    )}
                </div>
            )}
            {!free && <p className="faint text-xs">{goalType(reflection.goalType).hint}</p>}
        </div>
    );
}

function ReflectionForm({ initial, onSaved }) {
    const [form, setForm] = useState({
        wentWell: initial?.wentWell ?? "",
        stuckOn: initial?.stuckOn ?? "",
        nextGoal: initial?.nextGoal ?? "",
        goalType: initial?.goalType ?? "FREE",
        goalTarget: initial?.goalTarget ?? "",
    });
    const [message, setMessage] = useState(null);
    const measurable = form.goalType !== "FREE";

    const save = useMutation({
        mutationFn: () => apiPost(`${WELLBEING_BASE_PATH}${REFLECTION_SAVE}`, undefined, {
            wentWell: form.wentWell,
            stuckOn: form.stuckOn,
            nextGoal: form.nextGoal,
            goalType: form.goalType,
            goalTarget: measurable && form.goalTarget !== "" ? Number(form.goalTarget) : null,
        }),
        onSuccess: (res) => {
            setMessage({ ok: true, text: res.errorCode });
            onSaved();
        },
        onError: (err) => setMessage({ ok: false, text: errorMessage(err, err?.userMessage || "השמירה נכשלה") }),
    });

    const submit = (e) => {
        e.preventDefault();
        if (!form.nextGoal.trim()) {
            setMessage({ ok: false, text: "יש לכתוב יעד לשבוע הבא" });
            return;
        }
        if (measurable && !(Number(form.goalTarget) > 0)) {
            setMessage({ ok: false, text: "ליעד מדיד צריך לבחור מספר" });
            return;
        }
        save.mutate();
    };

    const set = (key) => (e) => setForm({ ...form, [key]: e.target.value });

    return (
        <form className="space-y-4" onSubmit={submit} noValidate>
            <div>
                <label className="label" htmlFor="rf-well">מה הלך טוב השבוע?</label>
                <textarea id="rf-well" className="field min-h-[5rem]" maxLength={2000} value={form.wentWell} onChange={set("wentWell")} />
            </div>
            <div>
                <label className="label" htmlFor="rf-stuck">במה נתקעתי?</label>
                <textarea id="rf-stuck" className="field min-h-[5rem]" maxLength={2000} value={form.stuckOn} onChange={set("stuckOn")} />
            </div>
            <div>
                <label className="label" htmlFor="rf-goal">מה היעד שלי לשבוע הבא?</label>
                <input id="rf-goal" className="field" maxLength={500} value={form.nextGoal} onChange={set("nextGoal")}
                       placeholder="למשל: לפתור 30 שאלות בשברים" />
            </div>
            <fieldset className="space-y-2">
                <legend className="label">איך נבדוק אם היעד הושג?</legend>
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
                    {GOAL_TYPES.map((g) => (
                        <button key={g.value} type="button" aria-pressed={form.goalType === g.value}
                                className={`choice text-right ${form.goalType === g.value ? "is-active" : ""}`}
                                onClick={() => setForm({ ...form, goalType: g.value })}>
                            <span className="block font-semibold">{g.label}</span>
                            <span className="block faint text-xs">{g.hint}</span>
                        </button>
                    ))}
                </div>
            </fieldset>
            {measurable && (
                <div>
                    <label className="label" htmlFor="rf-target">כמה {goalType(form.goalType).unit}?</label>
                    <input id="rf-target" type="number" min={1} max={5000} className="field num max-w-[10rem]"
                           value={form.goalTarget} onChange={set("goalTarget")} />
                </div>
            )}
            {message && <div className={`notice ${message.ok ? "notice--mint" : "notice--danger"}`} role="status">{message.text}</div>}
            <button type="submit" className="btn btn-primary" disabled={save.isPending}>
                <FaFlagCheckered aria-hidden="true" /> {initial ? "עדכון הרפלקציה" : "שמירת הרפלקציה"}
            </button>
        </form>
    );
}

export default function ReflectionJournal() {
    const { user } = useContext(UserContext);
    const queryClient = useQueryClient();
    const currentKey = ["reflection-current", user?.userId];
    const historyKey = ["reflection-history", user?.userId];

    const current = useQuery({
        queryKey: currentKey,
        queryFn: () => apiGet(`${WELLBEING_BASE_PATH}${REFLECTION_CURRENT}`, undefined),
        enabled: !!user?.userId,
    });
    const history = useQuery({
        queryKey: historyKey,
        queryFn: () => apiGet(`${WELLBEING_BASE_PATH}${REFLECTION_HISTORY}`, undefined),
        enabled: !!user?.userId,
    });

    const refresh = () => {
        queryClient.invalidateQueries({ queryKey: currentKey });
        queryClient.invalidateQueries({ queryKey: historyKey });
        queryClient.invalidateQueries({ queryKey: ["wellbeing-prompt", user?.userId] });
    };

    const mark = useMutation({
        mutationFn: ({ id, achieved }) => apiPost(`${WELLBEING_BASE_PATH}${REFLECTION_MARK}`, { reflectionId: id, achieved }),
        onSuccess: refresh,
    });
    const onMark = (id, achieved) => mark.mutate({ id, achieved });

    const overview = current.data;
    const past = (history.data ?? []).filter((r) => r.weekStart !== overview?.weekStart && r.id !== overview?.previous?.id);

    return (
        <div className="page page--wide space-y-5" dir="rtl">
            <div className="glass p-6 sm:p-8 space-y-2 reveal">
                <div className="flex items-center gap-3">
                    <span className="icon-badge" aria-hidden="true"><FaFeatherAlt /></span>
                    <div>
                        <span className="eyebrow">Reflection · יומן רפלקציה ויעדים</span>
                        <h1 className="display text-3xl sm:text-4xl">שלוש שאלות לשבוע</h1>
                    </div>
                </div>
                <p className="muted text-sm">
                    בכל יום ראשון: מה הלך טוב, במה נתקעת, ומה היעד לשבוע. בסוף השבוע נראה כאן אם הוא הושג.
                    הרפלקציה פרטית ורק את/ה רואה אותה.
                </p>
            </div>

            {current.isLoading && <SkeletonList rows={2} />}
            {current.isError && <div className="notice notice--danger">{errorMessage(current.error, "טעינת היומן נכשלה")}</div>}
            {mark.isError && <div className="notice notice--danger">{errorMessage(mark.error, "העדכון נכשל")}</div>}

            {overview && (
                <div className="grid grid-cols-1 lg:grid-cols-12 gap-5">
                    <div className="glass p-6 space-y-4 lg:col-span-7 reveal" style={{ "--i": 1 }}>
                        <h2 className="title text-xl">
                            {overview.current ? "הרפלקציה של השבוע" : overview.sunday ? "יום ראשון: זמן לרפלקציה" : "רפלקציה לשבוע הזה"}
                        </h2>
                        <ReflectionForm key={overview.current?.id ?? "new"} initial={overview.current} onSaved={refresh} />
                    </div>
                    <div className="lg:col-span-5 space-y-4">
                        <div className="glass p-6 space-y-3 reveal" style={{ "--i": 2 }}>
                            <h2 className="title text-xl">היעדים שלי</h2>
                            {overview.current && <GoalCard reflection={overview.current} title="השבוע" onMark={onMark} marking={mark.isPending} />}
                            {overview.previous && <GoalCard reflection={overview.previous} title="השבוע שעבר" onMark={onMark} marking={mark.isPending} />}
                            {!overview.current && !overview.previous && (
                                <EmptyState title="עוד אין יעדים" body="כתוב/י את הרפלקציה הראשונה ובחר/י יעד לשבוע." />
                            )}
                        </div>
                        {past.length > 0 && (
                            <div className="glass p-6 space-y-3 reveal" style={{ "--i": 3 }}>
                                <h2 className="title text-xl">שבועות קודמים</h2>
                                <ul className="space-y-2">
                                    {past.map((r) => (
                                        <li key={r.id} className="row-card flex items-center justify-between gap-2">
                                            <span className="min-w-0">
                                                <span className="num faint text-xs ml-2">{shortDate(r.weekStart)}</span>
                                                <span className="text-ink">{r.nextGoal}</span>
                                            </span>
                                            <StatusChip status={r.status} />
                                        </li>
                                    ))}
                                </ul>
                            </div>
                        )}
                    </div>
                </div>
            )}
        </div>
    );
}
