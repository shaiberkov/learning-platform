import axios from "axios";
import { apiGet, apiPost, ApiError } from "../../lib/api.js";
import { ClassCommunityPaths as P } from "../../constants/pages.constants.js";

// Calls for the class forum and the student-authored questions. The caller is the logged-in user
// (the server reads it from the session cookie), so no user id is sent. GETs return the data;
// writes return the whole BasicResponse; a success=false answer throws an ApiError with the Hebrew text.

const url = (path) => `${P.CLASS_COMMUNITY_BASE_PATH}${path}`;

async function apiDelete(path, params) {
    const body = (await axios.delete(url(path), { params })).data;
    if (body && body.success === false) throw new ApiError(body.errorCode || "");
    return body;
}

export const forumApi = {
    classes: () => apiGet(url(P.FORUM_CLASSES)),
    threads: (classId, search) => apiGet(url(P.FORUM_THREADS), { classId, search: search || undefined }),
    similar: (classId, text) => apiGet(url(P.FORUM_SIMILAR), { classId, text }),
    thread: (threadId) => apiGet(url(P.FORUM_THREAD), { threadId }),
    createThread: (body) => apiPost(url(P.FORUM_CREATE_THREAD), null, body),
    reply: (threadId, body) => apiPost(url(P.FORUM_REPLY), { threadId }, { body }),
    approve: (postId, approved) => apiPost(url(P.FORUM_APPROVE_POST), { postId, approved }),
    deletePost: (postId) => apiDelete(P.FORUM_DELETE_POST, { postId }),
    deleteThread: (threadId) => apiDelete(P.FORUM_DELETE_THREAD, { threadId }),
};

export const peerApi = {
    submit: (body) => apiPost(url(P.PEER_SUBMIT), null, body),
    mine: () => apiGet(url(P.PEER_MINE)),
    practice: () => apiGet(url(P.PEER_PRACTICE)),
    answer: (peerQuestionId, answer) => apiPost(url(P.PEER_ANSWER), { peerQuestionId }, { answer }),
    reviewQueue: (classId, status) => apiGet(url(P.PEER_REVIEW_QUEUE), { classId: classId || undefined, status }),
    approve: (peerQuestionId, note) => apiPost(url(P.PEER_APPROVE), { peerQuestionId }, { note: note || null }),
    reject: (peerQuestionId, note) => apiPost(url(P.PEER_REJECT), { peerQuestionId }, { note }),
};
