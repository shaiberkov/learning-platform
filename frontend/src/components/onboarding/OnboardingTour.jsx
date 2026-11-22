import { useContext, useEffect, useRef, useState } from "react";
import { useTranslation } from "react-i18next";
import { UserContext } from "../../context/UserContext.jsx";

export const START_TOUR_EVENT = "lp:start-tour";
const doneKey = (userId) => `lp-tour-done-${userId}`;

// Steps per role. `target` is a [data-tour] element to spotlight (skipped if not on screen).
const COMMON_START = [
    { title: "tour.welcome", body: "tour.welcomeBody" },
    { title: "tour.menu", body: "tour.menuBody", target: "menu" },
    { title: "tour.search", body: "tour.searchBody", target: "search" },
    { title: "tour.bell", body: "tour.bellBody", target: "bell" },
];
const ROLE_STEPS = {
    STUDENT: [{ title: "tour.studentPractice", body: "tour.studentPracticeBody" }],
    TEACHER: [{ title: "tour.teacherTests", body: "tour.teacherTestsBody" }],
    SCHOOLMANAGER: [{ title: "tour.managerSetup", body: "tour.managerSetupBody" }],
    SYSTEM_ADMIN: [{ title: "tour.adminSchools", body: "tour.adminSchoolsBody" }],
};

function isDone(userId) {
    try {
        return localStorage.getItem(doneKey(userId)) === "1";
    } catch {
        return true; // no storage: do not show the tour on every visit
    }
}

function markDone(userId) {
    try {
        localStorage.setItem(doneKey(userId), "1");
    } catch {
        /* storage unavailable */
    }
}

// A short guided tour shown once per user on first login, replayable from Settings.
export default function OnboardingTour() {
    const { user } = useContext(UserContext);
    const [step, setStep] = useState(-1);
    const cardRef = useRef(null);
    const { t, i18n } = useTranslation();

    const steps = user ? [...COMMON_START, ...(ROLE_STEPS[user.role] || []), { title: "tour.label", body: "tour.finish" }] : [];

    useEffect(() => {
        if (!user?.userId) return;
        if (!isDone(user.userId)) {
            const id = setTimeout(() => setStep(0), 800); // let the dashboard render first
            return () => clearTimeout(id);
        }
    }, [user?.userId]);

    useEffect(() => {
        const start = () => setStep(0);
        window.addEventListener(START_TOUR_EVENT, start);
        return () => window.removeEventListener(START_TOUR_EVENT, start);
    }, []);

    const current = step >= 0 ? steps[step] : null;

    // Spotlight the target and move focus into the card for keyboard and screen-reader users.
    useEffect(() => {
        if (!current) return;
        const el = current.target ? document.querySelector(`[data-tour="${current.target}"]`) : null;
        el?.classList.add("tour-spotlight");
        el?.scrollIntoView?.({ block: "nearest" });
        cardRef.current?.focus();
        return () => el?.classList.remove("tour-spotlight");
    }, [current]);

    if (!user || !current) return null;

    const finish = () => {
        markDone(user.userId);
        setStep(-1);
    };
    const last = step === steps.length - 1;

    return (
        <div
            ref={cardRef}
            tabIndex={-1}
            role="dialog"
            aria-modal="false"
            aria-labelledby="tour-title"
            aria-describedby="tour-body"
            dir={i18n.dir()}
            onKeyDown={(e) => { if (e.key === "Escape") finish(); }}
            className="fixed z-[100] bottom-5 inset-x-4 sm:inset-x-0 sm:mx-auto sm:w-[26rem] glass glass--strong !rounded-[24px] p-5 shadow-[var(--shadow-2)] outline-none"
            style={{ animation: "rise .5s var(--ease-out-expo) both" }}
        >
            <span className="eyebrow">{t("tour.step", { n: step + 1, total: steps.length })}</span>
            <h2 id="tour-title" className="title text-lg mt-1">{t(current.title)}</h2>
            <p id="tour-body" className="muted text-sm mt-2 leading-relaxed">{t(current.body)}</p>
            <div className="flex items-center justify-between gap-2 mt-4">
                <button type="button" className="btn btn-ghost btn-sm" onClick={finish}>
                    {t("common.skip")}
                </button>
                <div className="flex gap-2">
                    {step > 0 && (
                        <button type="button" className="btn btn-ghost btn-sm" onClick={() => setStep(step - 1)}>
                            {t("common.back")}
                        </button>
                    )}
                    <button type="button" className="btn btn-primary btn-sm" onClick={() => (last ? finish() : setStep(step + 1))}>
                        {last ? t("common.done") : t("common.next")}
                    </button>
                </div>
            </div>
        </div>
    );
}
