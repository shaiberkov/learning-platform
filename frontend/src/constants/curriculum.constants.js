// Subjects, topics and sub-topics a lesson page can link to. Mirrors topicsData in
// components/QuestionPracticeSelector.jsx (the practice route uses the same names), so a
// lesson's "practice" step opens /exercises/<subject>/<topic>/<sub-topic>.
export const CURRICULUM = {
    מתמטיקה: [
        { name: "מספרים מרוכבים", exercises: ["חיבור מספרים מרוכבים", "הכפלת מספרים מרוכבים"] },
        { name: "משוואות", exercises: ["מישוואה עם נעלם אחד", "פתרון משוואות ריבועיות"] },
        { name: "שברים", exercises: ["חיבור שברים", "חיסור שברים"] },
        { name: "מספרים שלמים", exercises: ["חיבור מספרים שלמים", "חיסור מספרים שלמים"] },
        { name: "חשבון דיפרנציאלי ואינטגרלי", exercises: ["גזירה", "אינטגרציה"] },
        { name: "מטריצות", exercises: ["חיבור מטריצות", "כפל מטריצות"] },
        { name: "וקטורים", exercises: ["חיבור וקטורים", "כפל וקטורים"] },
        { name: "שאלות מילוליות", exercises: ["פתרון שאלות מילוליות"] },
    ],
};

export const SUBJECTS = Object.keys(CURRICULUM);

export const topicsOf = (subject) => CURRICULUM[subject] ?? [];

export const subTopicsOf = (subject, topic) =>
    topicsOf(subject).find((t) => t.name === topic)?.exercises ?? [];
