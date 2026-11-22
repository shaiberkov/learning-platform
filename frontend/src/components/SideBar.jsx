


import React, {useContext, useEffect, useRef, useState} from "react";
import {Link, NavLink, Route, useNavigate} from "react-router-dom";
import { UserContext } from "../context/UserContext";
import logo from '../assets/logo.png';
import axios from "axios";
import {LOGOUT, USER_BASE_PATH} from "../constants/pages.constants.js";
import ThemeDial from "./ui/ThemeDial.jsx";

function SideBar() {
    const { user, setUser } = useContext(UserContext);
    const [open, setOpen] = useState(false);
    const [openSubmenus, setOpenSubmenus] = useState({});

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


    const getLinks = () => {
        if (!user) return [];
        if (user.role === "SCHOOLMANAGER") {
            return [
                { to: "school-mananger-dashboard", label: "מסך בית" },


                {
                    label: "מורים",
                    children: [
                        { to: "assign-teacher", label: "הוספת מורה" },
                        { to: "remove-teacher", label: "הסרת מורה" },
                        { to: "add-teaching-subject-to-teacher", label: "הוספת מקצוע למורה" },
                        { to: "remove-teaching-subject-to-teacher", label: "הסרת מקצוע ממורה" },
                        { to: "assign-teacher-to-class", label: "שיבוץ מורה לכיתות" },
                        { to: "assign-lesson-to-teacher", label: "שיבוץ מורה לשיעור" },
                    ],
                },


                {
                    label: "תלמידים",
                    children: [
                        { to: "assign-student-to-class", label: "שיבוץ תלמיד לכיתה" },

                    ],
                },

                {
                    label: "שכבות",
                    children: [
                        { to: "add-school-grades", label: "הוספת שכבות לבית ספר" },
                        { to: "remove-school-grades", label: "הסרת שכבות לבית ספר" },
                        { to: "add-classes-to-grade", label: "הוספת כיתות לשכבה" },
                        { to: "add-additional-class-to-grade", label: "הוספת כיתה נוספת לשכבה" },


                    ],
                },

                {
                    label: "כיתות",
                    children: [
                        { to: "classes-weekly-schedule", label: "מערכת שעות לפי כיתה" },

                    ],
                },

                {
                    label: "הודעות",
                    children: [
                        { to: "send-message", label: "שליחת הודעה" },
                    ],
                },
                { to: "audit-log", label: "יומן פעולות" },
            ];
        }


        return {
            SYSTEM_ADMIN: [
                { to: "system-admin-dashboard", label: "דף בית" },
                { to: "school-registration", label: "רישום בית ספר" },
                { to: "remove-manager-from-school", label: "הסרת מנהל בית ספר" },
                { to: "send-message", label: "שליחת הודעה" },
                {
                    label: "כלי ניהול",
                    children: [
                        { to: "audit-log", label: "יומן פעולות" },
                        { to: "system-health", label: "בריאות המערכת" },
                        { to: "feature-flags", label: "פיצ'רים לפי בית ספר" },
                        { to: "impersonate", label: "התחזות למשתמש" },
                    ],
                },
            ],
            TEACHER: [
                { to: "teacher-dashboard", label: "מסך בית" },
                { to: "teacher-weekly-schedule", label: "מערכת שעות" },
                { to: "test-configurator", label: "יצירת מבחן לתלמידים" },
                { to: "exam-builder", label: "בניית מבחן מתקדם" },
                { to: "teacher-exams", label: "המבחנים שלי" },
                { to: "class-skill-map", label: "מפת מיומנויות כיתתית" },
                { to: "hint-editor", label: "עריכת רמזים" },
            ],
            STUDENT: [
                { to: "student-dashboard", label: "מסך בית" },
                { to: "student-weekly-schedule", label: "מערכת שעות" },
                { to: "question-practice-selector", label: "תרגול שאלות" },
                { to: "test-configurator", label: "תרגול מבחנים" },
                { to: "teacher-tests", label: "מבחנים" },
                { to: "mistakes-notebook", label: "מחברת טעויות" },
                { to: "skill-map", label: "מפת מיומנויות" },
            ],
        }[user.role] || [];
    };

    const roleLabel = {
        SYSTEM_ADMIN: "מנהל מערכת",
        SCHOOLMANAGER: "מנהל בית ספר",
        TEACHER: "מורה",
        STUDENT: "תלמיד",
    }[user?.role];

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
                dir="rtl"
                className="fixed top-3 inset-x-3 sm:top-4 sm:inset-x-6 z-50 flex items-center justify-between gap-3 rounded-[22px] px-3 sm:px-4 py-2.5 glass glass--strong"
            >
                <div className="flex items-center gap-3 min-w-0">
                    {user && (
                        <button
                            type="button"
                            onClick={() => setOpen(!open)}
                            className="grid place-items-center w-11 h-11 rounded-2xl bg-surface-sunk text-ink border border-line hover:border-accent hover:text-accent transition-all duration-500 ease-[var(--ease-spring)] hover:scale-105 active:scale-95"
                            aria-label={open ? "סגור תפריט" : "פתח תפריט"}
                        >
                            {open ? (
                                <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5" viewBox="0 0 24 24" fill="none"
                                     stroke="currentColor" strokeWidth="1.8">
                                    <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12"/>
                                </svg>
                            ) : (
                                <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5" viewBox="0 0 24 24" fill="none"
                                     stroke="currentColor" strokeWidth="1.8">
                                    <path strokeLinecap="round" strokeLinejoin="round"
                                          d="M4 7h16M4 12h10M4 17h16"/>
                                </svg>
                            )}
                        </button>
                    )}
                    <div className="flex items-center gap-3 min-w-0">
                        <span className="grid place-items-center w-11 h-11 rounded-2xl bg-surface-strong border border-line shadow-[inset_0_1px_0_var(--inner)] overflow-hidden">
                            <img src={logo} alt="Logo" className="w-9 h-auto transition-transform duration-700 ease-[var(--ease-spring)] hover:scale-110 hover:rotate-6"/>
                        </span>
                        <div className="hidden sm:grid leading-tight min-w-0">
                            <span className="title text-[0.95rem] truncate">מרחב הלמידה</span>
                            <span className="eyebrow !text-[0.6rem]">
                                {user ? roleLabel : "Spatial Academy"}
                            </span>
                        </div>
                    </div>
                </div>

                <div className="flex items-center gap-2">
                    {user && (
                        <span className="hidden md:inline-flex items-center gap-2 rounded-full px-3 py-1.5 bg-surface-sunk border border-line text-sm text-ink-2">
                            <span className="w-2 h-2 rounded-full bg-mint shadow-[0_0_10px_var(--mint)]" />
                            {user.username}
                        </span>
                    )}

                    {!user && (
                        <div className="flex items-center gap-2">
                            <button
                                className="btn btn-ghost btn-sm"
                                onClick={() => navigate("/register")}
                            >
                                הרשמה
                            </button>
                            <button
                                className="btn btn-primary btn-sm"
                                onClick={() => navigate("/login")}
                            >
                                התחברות
                            </button>
                        </div>
                    )}

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
                    className={`fixed !top-0 right-0 z-60 flex h-[100dvh] w-[86vw] max-w-[22rem] transform flex-col
                                    p-3 transition-transform duration-700 ease-[var(--ease-out-expo)] ${
                        open ? "translate-x-0" : "translate-x-[110%]"
                    }`}
                >
                    <div className="glass glass--strong flex flex-col h-full rounded-[28px] p-4 overflow-y-auto" dir="rtl">
                        <div className="flex items-center justify-between px-2 pb-4 mb-3 border-b border-dashed border-line">
                            <div className="grid">
                                <span className="eyebrow">ניווט</span>
                                <span className="title text-lg mt-1">{user.username}</span>
                            </div>
                            <button
                                type="button"
                                onClick={() => setOpen(false)}
                                className="grid place-items-center w-10 h-10 rounded-xl text-ink-2 hover:text-accent hover:rotate-90 transition-all duration-500 ease-[var(--ease-spring)]"
                                aria-label="סגור סיידבר"
                            >
                                <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5" viewBox="0 0 24 24" fill="none"
                                     stroke="currentColor" strokeWidth="1.8">
                                    <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12"/>
                                </svg>
                            </button>
                        </div>

                        <ul className="flex-1 space-y-1 text-sm">
                            {getLinks().map((item, index) => {
                                if (item.children) {
                                    return (
                                        <li key={item.label} className={open ? "reveal" : ""} style={{ "--i": index }}>
                                            <button
                                                onClick={() => toggleSubmenu(item.label)}
                                                className="flex w-full items-center justify-between rounded-2xl px-4 py-3 text-[0.92rem] font-semibold text-ink hover:bg-surface-strong transition-colors"
                                            >
                                                {item.label}
                                                <svg
                                                    className={`h-4 w-4 transition-transform duration-500 ease-[var(--ease-spring)] ${openSubmenus[item.label] ? "rotate-180 text-accent" : "text-ink-3"}`}
                                                    viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"
                                                >
                                                    <path strokeLinecap="round" strokeLinejoin="round" d="M6 9l6 6 6-6"/>
                                                </svg>
                                            </button>
                                            {openSubmenus[item.label] && (
                                                <ul className="mr-5 mt-1 mb-2 space-y-1 border-r border-line pr-2" style={{ animation: "rise .5s var(--ease-out-expo) both" }}>
                                                    {item.children.map((child) => (
                                                        <li key={child.to}>
                                                            <NavLink
                                                                to={child.to}
                                                                className={navItemClass}
                                                                onClick={() => setOpen(false)}
                                                            >
                                                                <span className="w-1.5 h-1.5 rotate-45 rounded-[2px] bg-current opacity-60" />
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
                                                <span className="w-1.5 h-1.5 rotate-45 rounded-[2px] bg-current opacity-60" />
                                                {item.label}
                                            </NavLink>
                                        </li>
                                    );
                                }
                            })}
                        </ul>

                        <div className="pt-3 mt-3 border-t border-dashed border-line">
                            <button
                                onClick={handleLogout}
                                className="btn btn-ghost btn-block !justify-between !text-danger hover:!border-danger"
                            >
                                התנתק
                                <svg className="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
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
