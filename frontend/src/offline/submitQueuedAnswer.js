import axios from "axios";
import { STUDENT_BASE_PATH, SUBMIT_ANSWER } from "../constants/pages.constants.js";
import { authConfig } from "../lib/api.js";

// Sends one queued offline answer the way QuestionPractice does online.
export function submitQueuedAnswer(answer) {
    const params = new URLSearchParams({
        userId: answer.userId,
        id: answer.id,
        subTopic: answer.subTopic,
        answer: answer.answer,
    });
    return axios.post(`${STUDENT_BASE_PATH}${SUBMIT_ANSWER}?${params.toString()}`, null, authConfig())
        .then((res) => res.data?.data);
}

