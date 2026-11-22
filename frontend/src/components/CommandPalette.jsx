import { useContext, useEffect, useMemo, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { useTranslation } from "react-i18next";
import { UserContext } from "../context/UserContext.jsx";
import { GLOBAL_SEARCH, SEARCH_BASE_PATH } from "../constants/pages.constants.js";
import { apiGet } from "../lib/api.js";

export const OPEN_PALETTE_EVENT = "lp:open-palette";

// Static actions per role; `to` must be a route that exists for that role in AppRoutes.
const ACTIONS = {
    SYSTEM_ADMIN: [
        { key: "registerSchool", to: "/school-registration" },
        { key: "sendMessage", to: "/send-message" },
    ],
    SCHOOLMANAGER: [
        { key: "addTeacher", to: "/assign-teacher" },
        { key: "assignStudent", to: "/assign-student-to-class" },
        { key: "classSchedule", to: "/classes-weekly-schedule" },
        { key: "sendMessage", to: "/send-message" },
    ],
    TEACHER: [
        { key: "createExam", to: "/test-configurator" },
        { key: "sendMessage", to: "/send-message" },
    ],
    STUDENT: [
        { key: "practiceNow", to: "/question-practice-selector" },
        { key: "practiceTest", to: "/test-configurator" },
    ],
};
const COMMON_ACTIONS = [
    { key: "openMessages", to: "/messages" },
    { key: "openSettings", to: "/settings" },
    { key: "accessibility", to: "/accessibility" },
];

// Where a search hit leads, per role (there are no per-entity pages yet).
function targetFor(role, hit) {
    switch (hit.type) {
        case "EXAM": return role === "STUDENT" ? "/teacher-tests" : role === "TEACHER" ? "/teacher-dashboard" : "/school-mananger-dashboard";
        case "CLASS": return role === "SCHOOLMANAGER" ? "/classes-weekly-schedule" : "/teacher-weekly-schedule";
        case "STUDENT": return role === "SCHOOLMANAGER" ? "/assign-student-to-class" : "/send-message";
        case "TEACHER": return "/assign-teacher-to-class";
        case "SCHOOL": return "/system-admin-dashboard";
        default: return null;
    }
}

function useDebounced(value, ms) {
    const [debounced, setDebounced] = useState(value);
    useEffect(() => {
        const id = setTimeout(() => setDebounced(value), ms);
        return () => clearTimeout(id);
    }, [value, ms]);
    return debounced;
}

// Ctrl+K / ⌘K: search students, exams, classes (scoped by role on the server) and run actions.
export default function CommandPalette() {
    const { user } = useContext(UserContext);
    const [open, setOpen] = useState(false);
    const [query, setQuery] = useState("");
    const [active, setActive] = useState(0);
    const inputRef = useRef(null);
    const returnFocusRef = useRef(null);
    const navigate = useNavigate();
    const { t, i18n } = useTranslation();
    const debounced = useDebounced(query.trim(), 250);

    useEffect(() => {
        if (!user) return;
        const onKey = (e) => {
            if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === "k") {
                e.preventDefault();
                setOpen((o) => !o);
            }
        };
        const onOpen = () => setOpen(true);
        window.addEventListener("keydown", onKey);
        window.addEventListener(OPEN_PALETTE_EVENT, onOpen);
        return () => {
            window.removeEventListener("keydown", onKey);
            window.removeEventListener(OPEN_PALETTE_EVENT, onOpen);
        };
    }, [user]);

    useEffect(() => {
        if (open) {
            returnFocusRef.current = document.activeElement;
            setQuery("");
            setActive(0);
            setTimeout(() => inputRef.current?.focus(), 0);
        } else {
            returnFocusRef.current?.focus?.();
        }
    }, [open]);

    const { data: hits = [], isFetching } = useQuery({
        queryKey: ["search", user?.userId, debounced],
        queryFn: () => apiGet(`${SEARCH_BASE_PATH}${GLOBAL_SEARCH}`, { userId: user.userId, q: debounced }),
        enabled: open && !!user?.userId && debounced.length >= 2,
    });

    const items = useMemo(() => {
        if (!user) return [];
        const q = query.trim().toLowerCase();
        const actions = [...(ACTIONS[user.role] || []), ...COMMON_ACTIONS]
            .map((a) => ({ kind: "action", id: `a-${a.key}`, label: t(`palette.${a.key}`), to: a.to }))
            .filter((a) => !q || a.label.toLowerCase().includes(q));
        const results = (debounced.length >= 2 ? hits : []).map((h, i) => ({
            kind: "hit", id: `h-${h.type}-${h.key}-${i}`, label: h.title, sub: h.subtitle,
            type: h.type, to: targetFor(user.role, h), hit: h,
        }));
        return [...actions, ...results];
    }, [user, query, debounced, hits, t]);

    useEffect(() => { setActive(0); }, [debounced]);

    if (!user || !open) return null;

    const choose = (item) => {
        if (!item?.to) return;
        setOpen(false);
        navigate(item.to, item.hit ? { state: { searchHit: item.hit } } : undefined);
    };

    const onKeyDown = (e) => {
        if (e.key === "ArrowDown") {
            e.preventDefault();
            setActive((i) => Math.min(i + 1, items.length - 1));
        } else if (e.key === "ArrowUp") {
            e.preventDefault();
            setActive((i) => Math.max(i - 1, 0));
        } else if (e.key === "Enter") {
            e.preventDefault();
            choose(items[active]);
        } else if (e.key === "Escape") {
            e.preventDefault();
            setOpen(false);
        } else if (e.key === "Tab") {
            e.preventDefault(); // keep focus inside the dialog
        }
    };

    const actionItems = items.filter((i) => i.kind === "action");
    const hitItems = items.filter((i) => i.kind === "hit");
    const indexOf = (item) => items.indexOf(item);
    const optionId = (item) => `palette-${item.id}`;

    const renderItem = (item) => (
        <li
            key={item.id}
            id={optionId(item)}
            role="option"
            aria-selected={indexOf(item) === active}
            className="palette-item flex items-center justify-between gap-3 rounded-xl px-3 py-2.5 cursor-pointer"
            onMouseEnter={() => setActive(indexOf(item))}
            onClick={() => choose(item)}
        >
            <span className="min-w-0">
                <span className="block text-sm font-medium truncate">{item.label}</span>
                {item.sub && <span className="block faint text-xs truncate">{item.sub}</span>}
            </span>
            {item.type && <span className="chip shrink-0">{t(`palette.types.${item.type}`)}</span>}
        </li>
    );

    return (
        <div className="modal-backdrop !items-start pt-[12vh]" onMouseDown={() => setOpen(false)}>
            <div
                role="dialog"
                aria-modal="true"
                aria-label={t("palette.label")}
                dir={i18n.dir()}
                className="modal !max-w-xl w-[min(36rem,calc(100vw-2rem))] !p-3"
                onMouseDown={(e) => e.stopPropagation()}
            >
                <input
                    ref={inputRef}
                    value={query}
                    onChange={(e) => setQuery(e.target.value)}
                    onKeyDown={onKeyDown}
                    className="field !text-base"
                    placeholder={t("palette.placeholder")}
                    role="combobox"
                    aria-expanded="true"
                    aria-controls="palette-list"
                    aria-autocomplete="list"
                    aria-activedescendant={items[active] ? optionId(items[active]) : undefined}
                    aria-label={t("palette.label")}
                />
                <ul id="palette-list" role="listbox" aria-label={t("palette.label")} className="mt-3 max-h-[50vh] overflow-y-auto space-y-1">
                    {actionItems.length > 0 && (
                        <li role="presentation" className="eyebrow px-3 pt-1">{t("palette.actions")}</li>
                    )}
                    {actionItems.map(renderItem)}
                    {debounced.length >= 2 && (
                        <li role="presentation" className="eyebrow px-3 pt-3">{t("palette.results")}</li>
                    )}
                    {hitItems.map(renderItem)}
                </ul>
                <p className="faint text-xs px-3 pt-3" aria-live="polite">
                    {query.trim().length < 2 ? t("palette.typeMore")
                        : isFetching ? t("palette.searching")
                            : debounced.length >= 2 && hitItems.length === 0 ? t("palette.noResults") : t("palette.hint")}
                </p>
            </div>
        </div>
    );
}
