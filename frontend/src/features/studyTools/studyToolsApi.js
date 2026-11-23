import axios from "axios";
import { ApiError, apiGet } from "../../lib/api.js";
import {
    FLASHCARDS,
    FLASHCARDS_CLASS,
    FLASHCARDS_DUE,
    FLASHCARDS_GENERATE,
    FLASHCARDS_SUMMARY,
    NOTES,
    NOTES_TOPICS,
    STUDY_TOOLS_BASE_PATH,
    flashcardPath,
    notePath,
} from "../../constants/pages.constants.js";

// [s04] Calls for the flashcards and the study notebook. The student is the logged-in user
// (httpOnly cookie), so no userId is sent. Writes return the whole BasicResponse (its errorCode is
// the Hebrew confirmation); a success=false response throws an ApiError with the Hebrew message.

const url = (path) => `${STUDY_TOOLS_BASE_PATH}${path}`;

function unwrap(response) {
    const body = response.data;
    if (body && body.success === false) throw new ApiError(body.errorCode || "");
    return body;
}

const post = async (path, body, params) => unwrap(await axios.post(url(path), body ?? null, { params }));
const put = async (path, body) => unwrap(await axios.put(url(path), body));
const del = async (path) => unwrap(await axios.delete(url(path)));

/* Flashcards */
export const fetchMyDeck = () => apiGet(url(FLASHCARDS));
export const fetchDueCards = () => apiGet(url(FLASHCARDS_DUE));
export const fetchCardSummary = () => apiGet(url(FLASHCARDS_SUMMARY));
export const fetchClassCards = () => apiGet(url(FLASHCARDS_CLASS));
/** { text } or { imageDataUrl }, plus subject/topic; returns the BasicResponse with drafts in data. */
export const generateCards = (request) => post(FLASHCARDS_GENERATE, request);
/** { cards: [{front, back}], subject, topic, source, share } */
export const saveCards = (request) => post(FLASHCARDS, request);
export const shareCard = (cardId, shared) => post(flashcardPath(cardId, "share"), null, { shared });
export const addCardToDeck = (cardId) => post(flashcardPath(cardId, "add-to-deck"));
export const reviewCard = (cardId, knew) => post(flashcardPath(cardId, "review"), null, { knew });
export const removeCard = (cardId) => del(flashcardPath(cardId));

/* Study notebook */
export const fetchNotes = (filters = {}) => apiGet(url(NOTES), filters);
export const fetchNoteTopics = () => apiGet(url(NOTES_TOPICS));
export const createNote = (note) => post(NOTES, note);
export const updateNote = (noteId, note) => put(notePath(noteId), note);
export const deleteNote = (noteId) => del(notePath(noteId));
