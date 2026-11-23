package org.example.learningprojectserver.notification;

public enum NotificationType {
    SYSTEM_MESSAGE,
    NEW_TEST,
    NEW_HOMEWORK,
    HOMEWORK_REMINDER,
    // Live in-class quiz (LiveQuizService): students get the state, the teacher gets the results.
    LIVE_QUIZ_STATE,
    LIVE_QUIZ_RESULTS
}
