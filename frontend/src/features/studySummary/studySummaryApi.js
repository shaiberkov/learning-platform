// [s05] Requests of the summary sheet and study podcast screens. The student is the logged-in
// user (cookie session); no user id is sent.
import axios from "axios";
import { ApiError, apiGet, apiPost } from "../../lib/api.js";
import {
    STUDY_OPTIONS,
    STUDY_PODCASTS,
    STUDY_SHEETS,
    STUDY_SUMMARY_BASE_PATH,
} from "../../constants/pages.constants.js";

const url = (path) => `${STUDY_SUMMARY_BASE_PATH}${path}`;

export const fetchStudyOptions = () => apiGet(url(STUDY_OPTIONS));

export const fetchSheets = () => apiGet(url(STUDY_SHEETS));
export const fetchSheet = (id) => apiGet(url(`${STUDY_SHEETS}/${id}`));
export const createSheet = (request) => apiPost(url(STUDY_SHEETS), undefined, request);

export const fetchPodcasts = () => apiGet(url(STUDY_PODCASTS));
export const fetchPodcast = (id) => apiGet(url(`${STUDY_PODCASTS}/${id}`));
export const createPodcast = (request) => apiPost(url(STUDY_PODCASTS), undefined, request);
export const createPodcastAudio = (id) => apiPost(url(`${STUDY_PODCASTS}/${id}/audio`));

async function deleteAt(path) {
    const body = (await axios.delete(url(path))).data;
    if (body && body.success === false) throw new ApiError(body.errorCode || "");
    return body;
}

export const deleteSheet = (id) => deleteAt(`${STUDY_SHEETS}/${id}`);
export const deletePodcast = (id) => deleteAt(`${STUDY_PODCASTS}/${id}`);

/** The stored AI recording as an object URL. The caller revokes it. */
export async function fetchPodcastAudioUrl(id) {
    const response = await axios.get(url(`${STUDY_PODCASTS}/${id}/audio`), { responseType: "blob" });
    const blob = response.data;
    if (blob.type?.includes("json")) {
        const body = JSON.parse(await blob.text());
        throw new ApiError(body.errorCode || "");
    }
    return URL.createObjectURL(blob);
}
