package org.example.learningprojectserver.service.flashcards;

import lombok.RequiredArgsConstructor;
import org.example.learningprojectserver.dto.FlashcardDTOs.*;
import org.example.learningprojectserver.entities.FlashcardEntity;
import org.example.learningprojectserver.entities.FlashcardReviewEntity;
import org.example.learningprojectserver.mappers.FlashcardToDTOMapper;
import org.example.learningprojectserver.repository.FlashcardRepository;
import org.example.learningprojectserver.repository.FlashcardReviewRepository;
import org.example.learningprojectserver.response.BasicResponse;
import org.example.learningprojectserver.service.adaptive.Sm2Scheduler;
import org.example.learningprojectserver.service.ai.safety.ContentSafetyFilter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A student's flashcards. Every card in a student's deck has its own SM-2 schedule
 * ({@link FlashcardReviewEntity}), driven by the same {@link Sm2Scheduler} as the mistakes notebook,
 * and due cards are shown in the notebook's daily review. A card can be shared with the owner's class;
 * classmates then add it to their own deck. AI generation lives in {@link FlashcardGenerationService}.
 */
@Service
@RequiredArgsConstructor
public class FlashcardService {

    static final ZoneId SCHOOL_ZONE = ZoneId.of("Asia/Jerusalem");
    static final int MAX_DUE_CARDS = 50;

    static final String CARD_NOT_FOUND = "הכרטיסייה לא נמצאה";
    static final String NO_CLASS = "כדי לשתף כרטיסיות צריך להיות משויך/ת לכיתה";
    static final String UNSAFE_SHARE = "אי אפשר לשתף את הכרטיסייה הזו עם הכיתה. בדקו את הניסוח ונסו שוב.";

    private final FlashcardRepository flashcardRepository;
    private final FlashcardReviewRepository reviewRepository;
    private final ContentSafetyFilter safety;
    private final FlashcardToDTOMapper mapper;

    static LocalDate today() {
        return LocalDate.now(SCHOOL_ZONE);
    }

    /** The student's own cards and the classmates' cards they added, soonest review first. */
    public BasicResponse<List<FlashcardDTO>> myDeck(String userId) {
        return ok(null, withCards(reviewRepository.findByStudentUserIdOrderByDueDateAscIdAsc(userId), userId));
    }

    /** Cards due today or earlier, for the notebook's review session. */
    public BasicResponse<List<FlashcardDTO>> dueCards(String userId) {
        List<FlashcardReviewEntity> due = reviewRepository.findDue(userId, today()).stream()
                .limit(MAX_DUE_CARDS).toList();
        return ok(null, withCards(due, userId));
    }

    public BasicResponse<FlashcardSummaryDTO> summary(String userId) {
        return ok(null, new FlashcardSummaryDTO(reviewRepository.countDue(userId, today()),
                reviewRepository.countByStudentUserId(userId)));
    }

    /** Saves new cards to the student's deck (due today), optionally shared with their class. */
    @Transactional
    public BasicResponse<List<FlashcardDTO>> save(String userId, FlashcardSaveRequest request) {
        Long classRoomId = null;
        if (request.share()) {
            classRoomId = flashcardRepository.findClassRoomIdOfStudent(userId);
            if (classRoomId == null) {
                return new BasicResponse<>(false, NO_CLASS);
            }
            for (FlashcardInput input : request.cards()) {
                if (!isSafeToShare(input.front(), input.back())) {
                    return new BasicResponse<>(false, UNSAFE_SHARE);
                }
            }
        }
        String source = normalizeSource(request.source());
        LocalDateTime now = LocalDateTime.now(SCHOOL_ZONE);
        List<FlashcardDTO> saved = new ArrayList<>();
        for (FlashcardInput input : request.cards()) {
            FlashcardEntity card = new FlashcardEntity();
            card.setOwnerUserId(userId);
            card.setClassRoomId(classRoomId);
            card.setSubject(blankToNull(request.subject()));
            card.setTopic(blankToNull(request.topic()));
            card.setFront(input.front().trim());
            card.setBack(input.back().trim());
            card.setSource(source);
            card.setCreatedAt(now);
            FlashcardEntity stored = flashcardRepository.save(card);
            FlashcardReviewEntity review = reviewRepository.save(newReview(userId, stored.getId(), now));
            saved.add(mapper.apply(new FlashcardToDTOMapper.View(stored, review, userId)));
        }
        return ok(saved.size() == 1 ? "הכרטיסייה נשמרה" : "נשמרו " + saved.size() + " כרטיסיות", saved);
    }

    /** Shares one of the student's own cards with their class, or makes it private again. */
    public BasicResponse<FlashcardDTO> setShared(String userId, Long cardId, boolean shared) {
        FlashcardEntity card = flashcardRepository.findById(cardId)
                .filter(c -> c.getOwnerUserId().equals(userId)).orElse(null);
        if (card == null) {
            return new BasicResponse<>(false, CARD_NOT_FOUND);
        }
        if (shared) {
            Long classRoomId = flashcardRepository.findClassRoomIdOfStudent(userId);
            if (classRoomId == null) {
                return new BasicResponse<>(false, NO_CLASS);
            }
            if (!isSafeToShare(card.getFront(), card.getBack())) {
                return new BasicResponse<>(false, UNSAFE_SHARE);
            }
            card.setClassRoomId(classRoomId);
        } else {
            card.setClassRoomId(null);
        }
        flashcardRepository.save(card);
        FlashcardReviewEntity review = reviewRepository.findByStudentUserIdAndFlashcardId(userId, cardId).orElse(null);
        return ok(shared ? "הכרטיסייה שותפה עם הכיתה" : "השיתוף בוטל",
                mapper.apply(new FlashcardToDTOMapper.View(card, review, userId)));
    }

    /** Cards classmates shared with the student's class (not the student's own). */
    public BasicResponse<List<FlashcardDTO>> classCards(String userId) {
        Long classRoomId = flashcardRepository.findClassRoomIdOfStudent(userId);
        if (classRoomId == null) {
            return ok(null, List.of());
        }
        Map<Long, FlashcardReviewEntity> mine = reviewRepository.findByStudentUserIdOrderByDueDateAscIdAsc(userId)
                .stream().collect(Collectors.toMap(FlashcardReviewEntity::getFlashcardId, Function.identity(), (a, b) -> a));
        List<FlashcardDTO> cards = flashcardRepository.findByClassRoomIdOrderByIdDesc(classRoomId).stream()
                .filter(c -> !c.getOwnerUserId().equals(userId))
                .map(c -> mapper.apply(new FlashcardToDTOMapper.View(c, mine.get(c.getId()), userId)))
                .toList();
        return ok(null, cards);
    }

    /** Adds a card a classmate shared with the student's class to the student's deck. */
    public BasicResponse<FlashcardDTO> addToDeck(String userId, Long cardId) {
        FlashcardEntity card = flashcardRepository.findById(cardId).orElse(null);
        if (card == null || !isVisibleTo(card, userId)) {
            return new BasicResponse<>(false, CARD_NOT_FOUND);
        }
        FlashcardReviewEntity review = reviewRepository.findByStudentUserIdAndFlashcardId(userId, cardId)
                .orElseGet(() -> reviewRepository.save(newReview(userId, cardId, LocalDateTime.now(SCHOOL_ZONE))));
        return ok("הכרטיסייה נוספה לחפיסה שלך", mapper.apply(new FlashcardToDTOMapper.View(card, review, userId)));
    }

    /**
     * The owner deletes the card (for every student who added it); anyone else only removes it
     * from their own deck.
     */
    @Transactional
    public BasicResponse<Void> remove(String userId, Long cardId) {
        FlashcardEntity card = flashcardRepository.findById(cardId).orElse(null);
        if (card != null && card.getOwnerUserId().equals(userId)) {
            reviewRepository.deleteByFlashcardId(cardId);
            flashcardRepository.delete(card);
            return new BasicResponse<>(true, "הכרטיסייה נמחקה");
        }
        FlashcardReviewEntity review = reviewRepository.findByStudentUserIdAndFlashcardId(userId, cardId).orElse(null);
        if (review == null) {
            return new BasicResponse<>(false, CARD_NOT_FOUND);
        }
        reviewRepository.delete(review);
        return new BasicResponse<>(true, "הכרטיסייה הוסרה מהחפיסה שלך");
    }

    /** One self-graded review (knew it or not), scheduled with SM-2 like a notebook item. */
    @Transactional
    public BasicResponse<FlashcardReviewResultDTO> review(String userId, Long cardId, boolean knew) {
        FlashcardReviewEntity review = reviewRepository.findByStudentUserIdAndFlashcardId(userId, cardId).orElse(null);
        if (review == null) {
            return new BasicResponse<>(false, "הכרטיסייה לא נמצאת בחפיסה שלך");
        }
        LocalDate today = today();
        if (review.getDueDate().isAfter(today)) {
            return new BasicResponse<>(false, "הכרטיסייה הזו עוד לא מתוזמנת לחזרה היום");
        }
        Sm2Scheduler.review(review, knew ? Sm2Scheduler.QUALITY_CORRECT : Sm2Scheduler.QUALITY_WRONG, today);
        review.setLastReviewedAt(LocalDateTime.now(SCHOOL_ZONE));
        reviewRepository.save(review);
        return ok(null, new FlashcardReviewResultDTO(knew, review.getDueDate(), review.getIntervalDays(),
                reviewRepository.countDue(userId, today)));
    }

    private boolean isVisibleTo(FlashcardEntity card, String userId) {
        if (card.getOwnerUserId().equals(userId)) {
            return true;
        }
        return card.getClassRoomId() != null
                && card.getClassRoomId().equals(flashcardRepository.findClassRoomIdOfStudent(userId));
    }

    private FlashcardReviewEntity newReview(String userId, Long cardId, LocalDateTime now) {
        FlashcardReviewEntity review = new FlashcardReviewEntity();
        review.setStudentUserId(userId);
        review.setFlashcardId(cardId);
        review.setCreatedAt(now);
        Sm2Scheduler.resetAsNewMistake(review, today());
        return review;
    }

    /** Pairs each schedule row with its card, keeping the rows' order and skipping deleted cards. */
    private List<FlashcardDTO> withCards(List<FlashcardReviewEntity> reviews, String userId) {
        if (reviews.isEmpty()) {
            return List.of();
        }
        Map<Long, FlashcardEntity> cards = flashcardRepository
                .findAllById(reviews.stream().map(FlashcardReviewEntity::getFlashcardId).toList()).stream()
                .collect(Collectors.toMap(FlashcardEntity::getId, Function.identity()));
        return reviews.stream()
                .filter(r -> cards.containsKey(r.getFlashcardId()))
                .map(r -> mapper.apply(new FlashcardToDTOMapper.View(cards.get(r.getFlashcardId()), r, userId)))
                .toList();
    }

    private boolean isSafeToShare(String front, String back) {
        return safety.checkInput(Objects.toString(front, "")).allowed()
                && safety.checkInput(Objects.toString(back, "")).allowed();
    }

    static String normalizeSource(String source) {
        if (FlashcardEntity.SOURCE_AI_TEXT.equals(source) || FlashcardEntity.SOURCE_AI_IMAGE.equals(source)) {
            return source;
        }
        return FlashcardEntity.SOURCE_MANUAL;
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private static <T> BasicResponse<T> ok(String message, T data) {
        BasicResponse<T> response = new BasicResponse<>(true, message);
        response.setData(data);
        return response;
    }
}
