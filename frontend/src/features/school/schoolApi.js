import axios from "axios";
import {SCHOOL_ADMINISTRATION_BASE_PATH, PARENT_BASE_PATH} from "../../constants/pages.constants.js";

// Small request helpers for the school-administration and parent screens.
// Query values go through axios `params`, so Hebrew class and grade names are encoded.
// Auth is the httpOnly cookie that axios sends (Utils/httpSetup.js).

const unwrap = (promise) =>
    promise
        .then((res) => res.data)
        .catch((err) => {
            console.error(err);
            return {success: false, errorCode: "שגיאה בחיבור לשרת"};
        });

export const schoolGet = (path, params) =>
    unwrap(axios.get(`${SCHOOL_ADMINISTRATION_BASE_PATH}${path}`, {params}));

export const schoolPost = (path, params, body, contentType) =>
    unwrap(axios.post(`${SCHOOL_ADMINISTRATION_BASE_PATH}${path}`, body ?? null, {
        params,
        headers: contentType ? {"Content-Type": contentType} : {},
    }));

export const parentGet = (path, params) =>
    unwrap(axios.get(`${PARENT_BASE_PATH}${path}`, {params}));

// Builds a CSV file from rows (first row = header) and starts its download.
export function downloadCsv(filename, rows) {
    const escape = (v) => {
        const s = v == null ? "" : String(v);
        return /[",\n\r]/.test(s) ? `"${s.replace(/"/g, '""')}"` : s;
    };
    const text = "﻿" + rows.map((r) => r.map(escape).join(",")).join("\r\n");
    const url = URL.createObjectURL(new Blob([text], {type: "text/csv;charset=utf-8"}));
    const a = document.createElement("a");
    a.href = url;
    a.download = filename;
    a.click();
    URL.revokeObjectURL(url);
}

export const DAYS = [
    {value: "SUNDAY", label: "ראשון"},
    {value: "MONDAY", label: "שני"},
    {value: "TUESDAY", label: "שלישי"},
    {value: "WEDNESDAY", label: "רביעי"},
    {value: "THURSDAY", label: "חמישי"},
    {value: "FRIDAY", label: "שישי"},
];
