import axios from "axios";
import Cookies from "universal-cookie";
import {AUTH_HEADER, SCHOOL_ADMINISTRATION_BASE_PATH, PARENT_BASE_PATH} from "../../constants/pages.constants.js";
import {BEARER_PREFIX} from "../../constants/shared.constant.js";

// Small request helpers for the school-administration and parent screens.
// Query values go through axios `params`, so Hebrew class and grade names are encoded.

const authHeaders = () => ({[AUTH_HEADER]: `${BEARER_PREFIX}${new Cookies().get("token")}`});

const unwrap = (promise) =>
    promise
        .then((res) => res.data)
        .catch((err) => {
            console.error(err);
            return {success: false, errorCode: "שגיאה בחיבור לשרת"};
        });

export const schoolGet = (path, params) =>
    unwrap(axios.get(`${SCHOOL_ADMINISTRATION_BASE_PATH}${path}`, {params, headers: authHeaders()}));

export const schoolPost = (path, params, body, contentType) =>
    unwrap(axios.post(`${SCHOOL_ADMINISTRATION_BASE_PATH}${path}`, body ?? null, {
        params,
        headers: {...authHeaders(), ...(contentType ? {"Content-Type": contentType} : {})},
    }));

export const parentGet = (path, params) =>
    unwrap(axios.get(`${PARENT_BASE_PATH}${path}`, {params, headers: authHeaders()}));

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
