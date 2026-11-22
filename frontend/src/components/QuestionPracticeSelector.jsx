import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import { FaBook, FaLayerGroup, FaPuzzlePiece, FaPlay } from "react-icons/fa";

const topicsData = {
    מתמטיקה: [
        { name: "מספרים מרוכבים", exercises: ["חיבור מספרים מרוכבים", "הכפלת מספרים מרוכבים"] },
        { name: "משוואות", exercises: ["מישוואה עם נעלם אחד", "פתרון משוואות ריבועיות"] },
        { name: "שברים", exercises: ["חיבור שברים", "חיסור שברים"] },
        { name: "מספרים שלמים", exercises: ["חיבור מספרים שלמים", "חיסור מספרים שלמים"] },
        { name: "חשבון דיפרנציאלי ואינטגרלי", exercises: ["גזירה", "אינטגרציה"] },
        { name: "מטריצות", exercises: ["חיבור מטריצות", "כפל מטריצות"] },
        { name: "וקטורים", exercises: ["חיבור וקטורים", "כפל וקטורים"] },
        { name: "שאלות מילוליות", exercises: ["פתרון שאלות מילוליות"] }
    ],
};

function QuestionPracticeSelector() {
    const [selectedSubject, setSelectedSubject] = useState(null);
    const [selectedTopic, setSelectedTopic] = useState(null);
    const [selectedExercise, setSelectedExercise] = useState(null);

    const navigate = useNavigate();

    const handleNavigate = () => {
        if (selectedSubject && selectedTopic && selectedExercise) {
            const path = `/exercises/${selectedSubject}/${selectedTopic.name}/${selectedExercise}`;
            navigate(path);
        }
    };

    const step = selectedExercise ? 3 : selectedTopic ? 2 : selectedSubject ? 1 : 0;

    return (


        <div className="page page--wide" dir="rtl">
            <div className="grid lg:grid-cols-[1fr_20rem] gap-4 sm:gap-5 items-start">
                <div className="glass p-6 sm:p-9 space-y-8 reveal">
                    <div className="space-y-3">
                        <span className="eyebrow">Practice studio · תרגול</span>
                        <h2 className="display text-4xl sm:text-5xl flex items-center gap-3">
                            בחר תרגול
                        </h2>
                        <div className="track !h-1.5 max-w-sm"><span style={{ width: `${(step / 3) * 100}%` }} /></div>
                    </div>

                    <div className="space-y-3">
                        <label className="label">
                            <span className="num text-ink-3">01</span>
                            <FaBook />
                            בחר מקצוע:
                        </label>
                        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                            {Object.keys(topicsData).map(subject => (
                                <button
                                    key={subject}
                                    onClick={() => {
                                        setSelectedSubject(subject);
                                        setSelectedTopic(null);
                                        setSelectedExercise(null);
                                    }}
                                    className={`choice ${
                                        selectedSubject === subject ? "is-active" : ""
                                    }`}
                                >
                                    {subject}
                                </button>
                            ))}
                        </div>
                    </div>

                    {selectedSubject && (
                        <div className="space-y-3 reveal">
                            <label className="label">
                                <span className="num text-ink-3">02</span>
                                <FaLayerGroup />
                                בחר נושא:
                            </label>
                            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                                {topicsData[selectedSubject].map((topic, index) => (
                                    <button
                                        key={topic.name}
                                        onClick={() => {
                                            setSelectedTopic(topic);
                                            setSelectedExercise(null);
                                        }}
                                        className={`choice reveal ${
                                            selectedTopic?.name === topic.name ? "is-active" : ""
                                        }`}
                                        style={{ "--i": index }}
                                    >
                                        {topic.name}
                                    </button>
                                ))}
                            </div>
                        </div>
                    )}

                    {selectedTopic && (
                        <div className="space-y-3 reveal">
                            <label className="label">
                                <span className="num text-ink-3">03</span>
                                <FaPuzzlePiece />
                                בחר תרגיל מתוך "{selectedTopic.name}":
                            </label>
                            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                                {selectedTopic.exercises.map((ex, index) => (
                                    <button
                                        key={ex}
                                        onClick={() => setSelectedExercise(ex)}
                                        className={`choice reveal ${
                                            selectedExercise === ex ? "is-active" : ""
                                        }`}
                                        style={{ "--i": index }}
                                    >
                                        {ex}
                                    </button>
                                ))}
                            </div>
                        </div>
                    )}
                </div>

                <aside className="glass tile p-6 lg:sticky lg:top-32 space-y-5 reveal" style={{ "--i": 1 }}>
                    <span className="eyebrow">המסלול שלך</span>
                    <ol className="space-y-3">
                        {[
                            ["מקצוע", selectedSubject],
                            ["נושא", selectedTopic?.name],
                            ["תרגיל", selectedExercise],
                        ].map(([label, value], i) => (
                            <li key={label} className="flex items-center gap-3">
                                <span className={`num grid place-items-center w-8 h-8 rounded-xl text-xs transition-all duration-500 ${value ? "bg-accent text-[var(--accent-ink)] shadow-[0_0_14px_var(--glow)]" : "bg-surface-sunk text-ink-3"}`}>
                                    {String(i + 1).padStart(2, "0")}
                                </span>
                                <span className="grid">
                                    <span className="faint text-xs">{label}</span>
                                    <span className="text-sm font-semibold text-ink">{value || "—"}</span>
                                </span>
                            </li>
                        ))}
                    </ol>

                    {selectedSubject && selectedTopic && selectedExercise && (
                        <div className="reveal">
                            <button
                                onClick={handleNavigate}
                                className="btn btn-primary btn-block"
                            >
                                <FaPlay />
                                התחל תרגול
                            </button>
                        </div>
                    )}
                </aside>
            </div>
        </div>

    )

}

export default QuestionPracticeSelector;
