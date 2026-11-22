package org.example.learningprojectserver.response;

import org.example.learningprojectserver.dto.AdaptiveDTOs.MasteryUpdateDTO;

public class SubmitAnswerResponse extends BasicResponse {

    private boolean isCorrect;   // משתנה לציון אם התשובה נכונה
    private String message;      // הודעה מתאימה למשתמש
    private int hintsUsed;
    private int pointsEarned;
    private MasteryUpdateDTO mastery;
    private boolean addedToReview;

    public SubmitAnswerResponse(boolean success, String errorCode, boolean isCorrect, String message) {
        super(success, errorCode);
        this.isCorrect = isCorrect;
        this.message = message;
    }
    public SubmitAnswerResponse() {

    }

    @Override
    public String toString() {
        return "SubmitAnswerResponse{" +
                "isCorrect=" + isCorrect +
                ", message='" + message + '\'' +
                '}';
    }

    public boolean isCorrect() {
        return isCorrect;
    }

    public void setCorrect(boolean correct) {
        isCorrect = correct;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getHintsUsed() {
        return hintsUsed;
    }

    public void setHintsUsed(int hintsUsed) {
        this.hintsUsed = hintsUsed;
    }

    public int getPointsEarned() {
        return pointsEarned;
    }

    public void setPointsEarned(int pointsEarned) {
        this.pointsEarned = pointsEarned;
    }

    public MasteryUpdateDTO getMastery() {
        return mastery;
    }

    public void setMastery(MasteryUpdateDTO mastery) {
        this.mastery = mastery;
    }

    public boolean isAddedToReview() {
        return addedToReview;
    }

    public void setAddedToReview(boolean addedToReview) {
        this.addedToReview = addedToReview;
    }
}

