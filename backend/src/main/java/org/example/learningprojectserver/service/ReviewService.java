package org.example.learningprojectserver.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.learningprojectserver.dto.AdaptiveDTOs.ReviewAnswerDTO;
import org.example.learningprojectserver.dto.AdaptiveDTOs.ReviewItemDTO;
import org.example.learningprojectserver.dto.AdaptiveDTOs.ReviewSummaryDTO;
import org.example.learningprojectserver.entities.QuestionEntity;
import org.example.learningprojectserver.entities.ReviewItemEntity;
import org.example.learningprojectserver.repository.ReviewItemRepository;
import org.example.learningprojectserver.response.BasicResponse;
import org.example.learningprojectserver.service.adaptive.SkillCatalog;
import org.example.learningprojectserver.service.adaptive.Sm2Scheduler;
import org.example.learningprojectserver.service.diagnosis.MistakeClassifier;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/** The "mistakes notebook": every wrong answer is queued for review and scheduled with SM-2. */
@Service
@RequiredArgsConstructor
public class ReviewService {

    /** Due dates follow the school's calendar day, not the server's. */
    static final ZoneId SCHOOL_ZONE = ZoneId.of("Asia/Jerusalem");
    private static final int MAX_DUE_ITEMS = 50;

    private final ReviewItemRepository reviewItemRepository;

    static LocalDate today() {
        return LocalDate.now(SCHOOL_ZONE);
    }

    /**
     * Queues a wrongly answered question, or restarts its schedule if it is already in the notebook.
     * [s06] The given answer is kept and the mistake type is diagnosed by rules (MistakeClassifier).
     */
    public void addMistake(String userId, QuestionEntity question, String source, String givenAnswer) {
        if (question == null || question.getId() == null) {
            return;
        }
        ReviewItemEntity item = reviewItemRepository.findByStudentUserIdAndQuestionId(userId, question.getId())
                .orElseGet(() -> {
                    ReviewItemEntity i = new ReviewItemEntity();
                    i.setStudentUserId(userId);
                    i.setQuestionId(question.getId());
                    i.setSubject(question.getSubject());
                    String subTopic = SkillCatalog.canonicalSubTopic(question.getSubTopic());
                    i.setSubTopic(subTopic);
                    i.setTopic(SkillCatalog.topicOf(subTopic, question.getTopic()));
                    i.setQuestionText(question.getQuestionText());
                    i.setAnswer(question.getAnswer() == null ? "" : question.getAnswer().trim());
                    i.setSource(source);
                    i.setCreatedAt(LocalDateTime.now());
                    return i;
                });
        Sm2Scheduler.resetAsNewMistake(item, today());
        MistakeClassifier.Diagnosis diagnosis = MistakeClassifier.classify(item.getQuestionText(), item.getAnswer(), givenAnswer);
        item.setGivenAnswer(givenAnswer == null || givenAnswer.length() <= 500 ? givenAnswer : givenAnswer.substring(0, 500));
        item.setErrorType(diagnosis.type());
        item.setErrorTypeSource("RULE");
        item.setErrorNote(diagnosis.reason());
        reviewItemRepository.save(item);
    }

    public BasicResponse getDueItems(String userId) {
        List<ReviewItemDTO> items = reviewItemRepository.findDue(userId, today()).stream()
                .limit(MAX_DUE_ITEMS)
                .map(i -> new ReviewItemDTO(i.getId(), i.getSubject(), i.getTopic(), i.getSubTopic(),
                        i.getQuestionText(), i.getSource(), i.getReviewCount(), i.getLapses(), i.getDueDate(),
                        i.getErrorType() == null ? null : i.getErrorType().name()))
                .toList();
        BasicResponse response = new BasicResponse(true, null);
        response.setData(items);
        return response;
    }

    public BasicResponse getSummary(String userId) {
        LocalDate today = today();
        ReviewSummaryDTO summary = new ReviewSummaryDTO(
                reviewItemRepository.countDue(userId, today),
                reviewItemRepository.countDueOn(userId, today.plusDays(1)),
                reviewItemRepository.countByStudentUserId(userId),
                reviewItemRepository.countLearned(userId));
        BasicResponse response = new BasicResponse(true, null);
        response.setData(summary);
        return response;
    }

    @Transactional
    public BasicResponse answer(String userId, Long itemId, String answer) {
        ReviewItemEntity item = reviewItemRepository.findByIdAndStudentUserId(itemId, userId).orElse(null);
        if (item == null) {
            return new BasicResponse(false, "השאלה לא נמצאה במחברת הטעויות שלך");
        }
        LocalDate today = today();
        if (item.getDueDate().isAfter(today)) {
            return new BasicResponse(false, "השאלה הזו עוד לא מתוזמנת לחזרה היום");
        }
        boolean correct = answer != null && item.getAnswer().equalsIgnoreCase(answer.trim());
        Sm2Scheduler.review(item, correct ? Sm2Scheduler.QUALITY_CORRECT : Sm2Scheduler.QUALITY_WRONG, today);
        item.setLastReviewedAt(LocalDateTime.now());
        reviewItemRepository.save(item);

        BasicResponse response = new BasicResponse(true, null);
        response.setData(new ReviewAnswerDTO(correct, item.getAnswer(), item.getDueDate(), item.getIntervalDays(),
                reviewItemRepository.countDue(userId, today)));
        return response;
    }
}
