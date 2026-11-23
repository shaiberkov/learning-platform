// The simulations of the gallery. Keys must match SimulationService.SIMULATION_KEYS on the server;
// topics are the practice topics (SkillCatalog) a simulation is linked from.

export const SIMULATIONS = [
    {
        key: "parabola",
        title: "הפרבולה בתנועה",
        question: "מה קורה לפרבולה כשמגדילים את a?",
        description: "גררו את הקודקוד והזיזו את המחוון של a. ראו איך משתנים נקודות החיתוך עם ציר x והנוסחה.",
        topics: ["משוואות", "הנדסה אנליטית"],
    },
    {
        key: "line-slope",
        title: "שיפוע ומרחק",
        question: "מה קורה לשיפוע כשהקו נעשה תלול יותר?",
        description: "גררו שתי נקודות ותראו את השיפוע, משוואת הישר והמרחק ביניהן מתעדכנים.",
        topics: ["הנדסה אנליטית", "וקטורים"],
    },
    {
        key: "triangle",
        title: "משולש דינמי",
        question: "האם סכום הזוויות משתנה כשמזיזים קודקוד?",
        description: "גררו את קודקודי המשולש. הזוויות, אורכי הצלעות והשטח מחושבים מחדש בכל תזוזה.",
        topics: ["הנדסה אנליטית", "וקטורים"],
    },
    {
        key: "motion",
        title: "מהירות ותאוצה",
        question: "מה קורה כשהתאוצה מנוגדת לכיוון התנועה?",
        description: "בחרו מהירות התחלתית ותאוצה, לחצו על הפעלה וצפו במכונית ובגרפים של מיקום ומהירות.",
        topics: ["חשבון אינטגרלי ודפרנציאלי", "שאלות מילוליות", "וקטורים"],
    },
];

export const simulationByKey = (key) => SIMULATIONS.find((s) => s.key === key) || null;

/** Simulations linked from a practice topic (topic name, not sub-topic). */
export const simulationsForTopic = (topic) => (topic ? SIMULATIONS.filter((s) => s.topics.includes(topic)) : []);
