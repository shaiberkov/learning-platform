import dayjs from "dayjs";

// Form state and request shapes for the exam builder and the clone dialog.

export const AUTHOR_KINDS = ["MULTI_SELECT", "NUMERIC", "MATCHING", "ORDERING", "SHORT_OPEN"];

export const emptyQuestion = (kind = "MULTI_SELECT") => ({
    kind,
    text: "",
    points: 1,
    options: ["", ""],
    correctOptions: [],
    numericAnswer: "",
    tolerance: "",
    matchLeft: ["", ""],
    matchRight: ["", ""],
    matchKey: [],
    acceptedAnswers: [""],
});

// The request shape the server expects for this question.
export const toPayload = (q) => {
    const base = { kind: q.kind, text: q.text, points: Number(q.points) || 1 };
    switch (q.kind) {
        case "MULTI_SELECT":
            return { ...base, options: q.options, correctOptions: q.correctOptions };
        case "NUMERIC":
            return {
                ...base,
                numericAnswer: q.numericAnswer === "" ? null : Number(String(q.numericAnswer).replace(",", ".")),
                tolerance: q.tolerance === "" ? null : Number(String(q.tolerance).replace(",", ".")),
            };
        case "MATCHING":
            return { ...base, matchLeft: q.matchLeft, matchRight: q.matchRight, matchKey: q.matchKey };
        case "ORDERING":
            return { ...base, options: q.options };
        case "SHORT_OPEN":
            return { ...base, acceptedAnswers: q.acceptedAnswers };
        default:
            return base;
    }
};


export const emptySchedule = () => ({
    template: false,
    startTime: null,
    timeLimitMinutes: 45,
    scheduledPublish: false,
    publishAt: null,
    studentIds: "",
    versions: false,
});

const fmt = (date) => (date ? dayjs(date).format("DD/MM/YYYY HH:mm") : null);

// Request fields the server expects (dd/MM/yyyy HH:mm times, a list of student ids).
export const schedulePayload = (s) => ({
    template: s.template,
    startTime: s.template ? null : fmt(s.startTime),
    timeLimitMinutes: Number(s.timeLimitMinutes) || 0,
    publishAt: !s.template && s.scheduledPublish ? fmt(s.publishAt) : null,
    studentIds: s.template ? [] : s.studentIds.split(",").map((x) => x.trim()).filter(Boolean),
    versions: !s.template && s.versions,
});

