



import React, {useContext, useEffect, useRef, useState} from "react";
import {NavLink, useNavigate} from "react-router-dom";
import { useTranslation } from "react-i18next";
import { UserContext, forgetUser } from "../context/UserContext";
import logo from '../assets/logo.png';
import axios from "axios";
import {LOGOUT, USER_BASE_PATH} from "../constants/pages.constants.js";
import ThemeDial from "./ui/ThemeDial.jsx";
import NotificationBell from "./notifications/NotificationBell.jsx";
import LanguageSwitcher from "./LanguageSwitcher.jsx";
import { OPEN_PALETTE_EVENT } from "./CommandPalette.jsx";

function SideBar() {
    const { user, setUser } = useContext(UserContext);
    const [open, setOpen] = useState(false);
    const [openSubmenus, setOpenSubmenus] = useState({});
    const { t, i18n } = useTranslation();
    const menuButtonRef = useRef(null);
    const drawerRef = useRef(null);

    const navigate = useNavigate();

    const toggleSubmenu = (label) => {
        setOpenSubmenus((prev) => ({
            ...prev,
            [label]: !prev[label],
        }));
    };

    const handleLogout = async () => {
        try {
            await axios.post(`${USER_BASE_PATH}${LOGOUT}`);
        } catch (error) {
            console.error('Error during logout:', error);
        }
        forgetUser();
        setUser(null);
        navigate("/login");
    };

    useEffect(() => {
        const sidebar = document.getElementById("sidebar");
        const navbar = document.getElementById("navbar");
        if (sidebar && navbar) {
            sidebar.style.top = `${navbar.clientHeight - 1}px`;
        }
    }, []);

    // Keyboard: Esc closes the drawer and returns focus to the menu button; focus moves
    // into the drawer when it opens.
    useEffect(() => {
        if (!open) return;
        drawerRef.current?.querySelector("a, button")?.focus();
        const onKey = (e) => {
            if (e.key === "Escape") {
                setOpen(false);
                menuButtonRef.current?.focus();
            }
        };
        document.addEventListener("keydown", onKey);
        return () => document.removeEventListener("keydown", onKey);
    }, [open]);


    const getLinks = () => {
        if (!user) return [];
        const common = [
            { to: "messages", label: t("nav.messages") },
            { to: "settings", label: t("nav.settings") },
        ];
        if (user.role === "SCHOOLMANAGER") {
            return [
                { to: "school-mananger-dashboard", label: t("nav.home") },


                {
                    id: "teachers",
                    label: t("nav.teachers"),
                    children: [
                        { to: "assign-teacher", label: t("nav.addTeacher") },
                        { to: "remove-teacher", label: t("nav.removeTeacher") },
                        { to: "add-teaching-subject-to-teacher", label: t("nav.addSubject") },
                        { to: "remove-teaching-subject-to-teacher", label: t("nav.removeSubject") },
                        { to: "assign-teacher-to-class", label: t("nav.assignTeacherClasses") },
                        { to: "assign-lesson-to-teacher", label: t("nav.assignLesson") },
                    ],
                },


                {
                    id: "students",
                    label: t("nav.students"),
                    children: [
                        { to: "assign-student-to-class", label: t("nav.assignStudent") },

                    ],
                },

                {
                    id: "grades",
                    label: t("nav.grades"),
                    children: [
                        { to: "add-school-grades", label: t("nav.addGrades") },
                        { to: "remove-school-grades", label: t("nav.removeGrades") },
                        { to: "add-classes-to-grade", label: t("nav.addClasses") },
                        { to: "add-additional-class-to-grade", label: t("nav.addClass") },


                    ],
                },

                {
                    id: "classes",
                    label: t("nav.classes"),
                    children: [
                        { to: "classes-weekly-schedule", label: t("nav.classSchedule") },
                        { to: "attendance-report", label: "דוח היעדרויות" },

                    ],
                },

                {
                    id: "messages",
                    label: t("nav.messages"),
                    children: [
                        { to: "send-message", label: t("nav.sendMessage") },
                        { to: "messages", label: t("messages.allMessages") },
                        { to: "message-parents", label: "הודעה להורים" },
                    ],
                },

                {
                    id: "school-admin",
                    label: "ניהול בית הספר",
                    children: [
                        { to: "school-analytics", label: "לוח ציונים ונתונים" },
                        { to: "timetable-builder", label: "בניית מערכת שעות" },
                        { to: "school-years", label: "שנות לימודים" },
                        { to: "bulk-import", label: "ייבוא משתמשים מקובץ" },
                        { to: "parent-links", label: "קישור הורים לתלמידים" },
                    ],
                },
                { to: "audit-log", label: "יומן פעולות" },

                {
                    id: "ai",
                    label: "AI",
                    children: [
                        { to: "ai-school", label: "שימוש ובטיחות AI" },
                    ],
                },
                { to: "settings", label: t("nav.settings") },
            ];
        }


        return {
            SYSTEM_ADMIN: [
                { to: "system-admin-dashboard", label: t("nav.home") },
                { to: "school-registration", label: t("nav.schoolRegistration") },
                { to: "remove-manager-from-school", label: t("nav.removeSchoolManager") },
                { to: "send-message", label: t("nav.sendMessage") },
                {
                    id: "admin-tools",
                    label: "כלי ניהול",
                    children: [
                        { to: "audit-log", label: "יומן פעולות" },
                        { to: "system-health", label: "בריאות המערכת" },
                        { to: "feature-flags", label: "פיצ'רים לפי בית ספר" },
                        { to: "impersonate", label: "התחזות למשתמש" },
                    ],
                },
                ...common,
            ],
            TEACHER: [
                { to: "teacher-dashboard", label: t("nav.home") },
                { to: "teacher-weekly-schedule", label: t("nav.schedule") },
                { to: "test-configurator", label: t("nav.createTest") },
                { to: "exam-builder", label: "בניית מבחן מתקדם" },
                { to: "teacher-exams", label: "המבחנים שלי" },
                { to: "class-skill-map", label: "מפת מיומנויות כיתתית" },
                { to: "hint-editor", label: "עריכת רמזים" },
                { to: "lesson-authoring", label: "כתיבת שיעורים" },
                { to: "homework-board", label: "שיעורי בית" },
                { to: "class-challenges", label: "אתגרי כיתה" },
                { to: "student-accommodations", label: "התאמות לתלמידים" },
                { to: "ai-questions", label: "יצירת שאלות עם AI" },
                { to: "ai-grading", label: "בדיקת שאלות פתוחות" },
                { to: "ai-insights", label: "תובנות שבועיות" },
                { to: "question-bank", label: "מאגר שאלות" },
                { to: "gradebook", label: "יומן ציונים" },
                { to: "live-quiz-host", label: "חידון בכיתה" },
                { to: "attendance", label: "נוכחות" },
                { to: "message-parents", label: "הודעה להורים" },
                { to: "send-message", label: t("nav.sendMessage") },
                ...common,
            ],
            PARENT: [
                { to: "parent-dashboard", label: "מסך בית" },
                ...common,
            ],
            STUDENT: [
                { to: "student-dashboard", label: t("nav.home") },
                ...(user.schoolCode ? [
                    { to: "student-weekly-schedule", label: t("nav.schedule") },
                    { to: "question-practice-selector", label: t("nav.practice") },
                    { to: "test-configurator", label: t("nav.practiceTests") },
                    { to: "teacher-tests", label: t("nav.tests") },
                    { to: "mistakes-notebook", label: "מחברת טעויות" },
                    { to: "skill-map", label: "מפת מיומנויות" },
                    { to: "lessons", label: "שיעורים" },
                    { to: "homework", label: "שיעורי בית" },
                    { to: "study-plan", label: "תוכנית למידה" },
                    { to: "achievements", label: "ההתקדמות שלי" },
                    { to: "ai-tutor", label: "מורה-עזר AI" },
                    { to: "open-questions", label: "שאלות פתוחות" },
                    { to: "live-quiz", label: "חידון בכיתה" },
                    // [s05] Summary sheet and study podcast.
                    { to: "study-summary-sheet", label: "דף סיכום למבחן" },
                    { to: "study-podcast", label: "סיכום קולי" },
                ] : []),
                { to: "accessibility-settings", label: "הגדרות נגישות" },
                ...common,
            ],
        }[user.role] || [];
    };

    const roleLabel = user?.role ? t(`roles.${user.role}`) : "";

    const navItemClass = ({ isActive }) =>
        `group flex items-center gap-3 rounded-2xl px-4 py-3 text-[0.92rem] font-medium transition-all duration-500 ease-[var(--ease-spring)] ${
            isActive
                ? "bg-accent-soft text-accent shadow-[inset_0_0_0_1px_var(--accent)]"
                : "text-ink-2 hover:bg-surface-strong hover:text-ink hover:-translate-x-1"
        }`;

    return (
        <>

            <nav
                id="navbar"
                dir={i18n.dir()}
                aria-label={t("app.name")}
                className="fixed top-3 inset-x-3 sm:top-4 sm:inset-x-6 z-50 flex items-center justify-between gap-3 rounded-[22px] px-3 sm:px-4 py-2.5 glass glass--strong"
            >
                <div className="flex items-center gap-3 min-w-0">
                    {user && (
                        <button
                            ref={menuButtonRef}
                            type="button"
                            data-tour="menu"
                            onClick={() => setOpen(!open)}
                            className="grid place-items-center w-11 h-11 rounded-2xl bg-surface-sunk text-ink border border-line hover:border-accent hover:text-accent transition-all duration-500 ease-[var(--ease-spring)] hover:scale-105 active:scale-95"
                            aria-label={open ? t("a11y.closeMenu") : t("a11y.openMenu")}
                            aria-expanded={open}
                            aria-controls="sidebar"
                        >
                            {open ? (
                                <svg aria-hidden="true" xmlns="http://www.w3.org/2000/svg" className="h-5 w-5" viewBox="0 0 24 24" fill="none"
                                     stroke="currentColor" strokeWidth="1.8">
                                    <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12"/>
                                </svg>
                            ) : (
                                <svg aria-hidden="true" xmlns="http://www.w3.org/2000/svg" className="h-5 w-5" viewBox="0 0 24 24" fill="none"
                                     stroke="currentColor" strokeWidth="1.8">
                                    <path strokeLinecap="round" strokeLinejoin="round"
                                          d="M4 7h16M4 12h10M4 17h16"/>
                                </svg>
                            )}
                        </button>
                    )}
                    <div className="flex items-center gap-3 min-w-0">
                        <span className="grid place-items-center w-11 h-11 rounded-2xl bg-surface-strong border border-line shadow-[inset_0_1px_0_var(--inner)] overflow-hidden">
                            <img src={logo} alt={t("a11y.logo")} className="w-9 h-auto transition-transform duration-700 ease-[var(--ease-spring)] hover:scale-110 hover:rotate-6"/>
                        </span>
                        <div className="hidden sm:grid leading-tight min-w-0">
                            <span className="title text-[0.95rem] truncate">{t("app.name")}</span>
                            <span className="eyebrow !text-[0.6rem]">
                                {user ? roleLabel : t("app.tagline")}
                            </span>
                        </div>
                    </div>
                </div>

                <div className="flex items-center gap-2">
                    {user && (
                        <button
                            type="button"
                            data-tour="search"
                            onClick={() => window.dispatchEvent(new Event(OPEN_PALETTE_EVENT))}
                            className="inline-flex items-center gap-2 h-11 rounded-2xl px-3 bg-surface-sunk border border-line text-ink-2 hover:border-accent hover:text-accent transition-colors"
                            aria-label={`${t("nav.search")} (${t("nav.searchHint")})`}
                            aria-keyshortcuts="Control+K Meta+K"
                        >
                            <svg aria-hidden="true" className="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8">
                                <circle cx="11" cy="11" r="7" /><path strokeLinecap="round" d="M20 20l-3.5-3.5" />
                            </svg>
                            <kbd className="hidden lg:inline num text-[0.7rem]" aria-hidden="true">{t("nav.searchHint")}</kbd>
                        </button>
                    )}

                    <NotificationBell />

                    {user && (
                        <span className="hidden md:inline-flex items-center gap-2 rounded-full px-3 py-1.5 bg-surface-sunk border border-line text-sm text-ink-2">
                            <span aria-hidden="true" className="w-2 h-2 rounded-full bg-mint shadow-[0_0_10px_var(--mint)]" />
                            {user.username}
                        </span>
                    )}

                    {!user && (
                        <div className="flex items-center gap-2">
                            <button
                                className="btn btn-ghost btn-sm"
                                onClick={() => navigate("/register")}
                            >
                                {t("nav.register")}
                            </button>
                            <button
                                className="btn btn-primary btn-sm"
                                onClick={() => navigate("/login")}
                            >
                                {t("nav.login")}
                            </button>
                        </div>
                    )}

                    <span className="hidden sm:inline-flex"><LanguageSwitcher compact /></span>
                    <ThemeDial />
                </div>

            </nav>

            {user && open && (
                <div
                    className="fixed inset-0 z-[55] bg-[color-mix(in_oklab,var(--bg)_40%,transparent)] backdrop-blur-sm"
                    style={{ animation: "fade .35s var(--ease-out-expo) both" }}
                    onClick={() => setOpen(false)}
                    aria-hidden="true"
                />
            )}

            {user && (
                <nav
                    id="sidebar"
                    ref={drawerRef}
                    aria-label={t("a11y.mainNav")}
                    inert={!open}
                    className={`fixed !top-0 start-0 z-60 flex h-[100dvh] w-[86vw] max-w-[22rem] transform flex-col
                                    p-3 transition-transform duration-700 ease-[var(--ease-out-expo)] ${
                        open ? "translate-x-0" : "ltr:-translate-x-[110%] rtl:translate-x-[110%]"
                    }`}
                >
                    <div className="glass glass--strong flex flex-col h-full rounded-[28px] p-4 overflow-y-auto" dir={i18n.dir()}>
                        <div className="flex items-center justify-between px-2 pb-4 mb-3 border-b border-dashed border-line">
                            <div className="grid">
                                <span className="eyebrow">{t("nav.navigation")}</span>
                                <span className="title text-lg mt-1">{user.username}</span>
                            </div>
                            <button
                                type="button"
                                onClick={() => { setOpen(false); menuButtonRef.current?.focus(); }}
                                className="grid place-items-center w-10 h-10 rounded-xl text-ink-2 hover:text-accent hover:rotate-90 transition-all duration-500 ease-[var(--ease-spring)]"
                                aria-label={t("a11y.closeMenu")}
                            >
                                <svg aria-hidden="true" xmlns="http://www.w3.org/2000/svg" className="h-5 w-5" viewBox="0 0 24 24" fill="none"
                                     stroke="currentColor" strokeWidth="1.8">
                                    <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12"/>
                                </svg>
                            </button>
                        </div>

                        <ul className="flex-1 space-y-1 text-sm">
                            {getLinks().map((item, index) => {
                                if (item.children) {
                                    const submenuId = `submenu-${item.id}`;
                                    return (
                                        <li key={item.id} className={open ? "reveal" : ""} style={{ "--i": index }}>
                                            <button
                                                type="button"
                                                onClick={() => toggleSubmenu(item.id)}
                                                aria-expanded={!!openSubmenus[item.id]}
                                                aria-controls={submenuId}
                                                className="flex w-full items-center justify-between rounded-2xl px-4 py-3 text-[0.92rem] font-semibold text-ink hover:bg-surface-strong transition-colors"
                                            >
                                                {item.label}
                                                <svg
                                                    aria-hidden="true"
                                                    className={`h-4 w-4 transition-transform duration-500 ease-[var(--ease-spring)] ${openSubmenus[item.id] ? "rotate-180 text-accent" : "text-ink-3"}`}
                                                    viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"
                                                >
                                                    <path strokeLinecap="round" strokeLinejoin="round" d="M6 9l6 6 6-6"/>
                                                </svg>
                                            </button>
                                            {openSubmenus[item.id] && (
                                                <ul id={submenuId} className="ms-5 mt-1 mb-2 space-y-1 border-s border-line ps-2" style={{ animation: "rise .5s var(--ease-out-expo) both" }}>
                                                    {item.children.map((child) => (
                                                        <li key={child.to}>
                                                            <NavLink
                                                                to={child.to}
                                                                className={navItemClass}
                                                                onClick={() => setOpen(false)}
                                                            >
                                                                <span aria-hidden="true" className="w-1.5 h-1.5 rotate-45 rounded-[2px] bg-current opacity-60" />
                                                                {child.label}
                                                            </NavLink>
                                                        </li>
                                                    ))}
                                                </ul>
                                            )}
                                        </li>
                                    );
                                } else {
                                    return (
                                        <li key={item.to} className={open ? "reveal" : ""} style={{ "--i": index }}>
                                            <NavLink
                                                to={item.to}
                                                className={navItemClass}
                                                onClick={() => setOpen(false)}
                                            >
                                                <span aria-hidden="true" className="w-1.5 h-1.5 rotate-45 rounded-[2px] bg-current opacity-60" />
                                                {item.label}
                                            </NavLink>
                                        </li>
                                    );
                                }
                            })}
                        </ul>

                        <div className="sm:hidden pt-3 mt-3 border-t border-dashed border-line">
                            <LanguageSwitcher />
                        </div>

                        <div className="pt-3 mt-3 border-t border-dashed border-line">
                            <button
                                type="button"
                                onClick={handleLogout}
                                className="btn btn-ghost btn-block !justify-between !text-danger hover:!border-danger"
                            >
                                {t("nav.logout")}
                                <svg aria-hidden="true" className="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                    <path strokeLinecap="round" strokeLinejoin="round" d="M15 12H3m0 0l4-4m-4 4l4 4M13 5h5a2 2 0 012 2v10a2 2 0 01-2 2h-5"/>
                                </svg>
                            </button>
                        </div>
                    </div>

                </nav>
            )}
        </>
    );
}

export default SideBar;
