package org.example.learningprojectserver.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.example.learningprojectserver.enums.DuelMode;
import org.example.learningprojectserver.enums.DuelStatus;

import java.time.LocalDateTime;
import java.util.List;

/** API shapes of 1-on-1 duels. */
public final class DuelDTOs {

    private DuelDTOs() {}

    /** One duel as both players see it: the live progress counters of both sides. */
    public record DuelSummaryDTO(Long id, DuelMode mode, DuelStatus status, String topic, String subTopic, int level,
                                 String challengerId, String challengerName, String opponentId, String opponentName,
                                 int questionCount, int challengerAnswered, int challengerCorrect,
                                 int opponentAnswered, int opponentCorrect, Integer systemScore, String outcome,
                                 LocalDateTime createdAt) {}

    /** A question from the caller's side. correctAnswer is sent only once the duel is finished. */
    public record DuelQuestionDTO(int position, String questionText, String myAnswer, Boolean myCorrect,
                                  String correctAnswer) {}

    /** The duel screen: summary plus the caller's questions (empty until the duel starts). */
    public record DuelStateDTO(DuelSummaryDTO duel, List<DuelQuestionDTO> questions) {}

    /** A suggested opponent, ordered by how close their mastery of the sub-topic is to the caller's. */
    public record DuelOpponentDTO(String userId, String username, boolean sameClass, int rating, int ratingGap,
                                  boolean recommended) {}

    public record DuelOpponentsDTO(int myRating, List<DuelOpponentDTO> opponents) {}

    public record DuelTopicDTO(String topic, List<String> subTopics) {}

    public record DuelInviteRequest(
            @NotBlank(message = "יש לבחור יריב") String opponentId,
            @NotBlank(message = "יש לבחור נושא") String subTopic,
            @NotNull(message = "יש לבחור סוג דו-קרב") DuelMode mode) {}

    public record DuelAnswerRequest(
            @NotNull(message = "חסר מספר שאלה") Integer position,
            @NotBlank(message = "יש להקליד תשובה") String answer) {}
}
