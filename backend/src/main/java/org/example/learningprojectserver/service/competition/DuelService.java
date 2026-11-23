package org.example.learningprojectserver.service.competition;

import lombok.RequiredArgsConstructor;
import org.example.learningprojectserver.dto.DuelDTOs.*;
import org.example.learningprojectserver.dto.MessageDTO;
import org.example.learningprojectserver.entities.DuelEntity;
import org.example.learningprojectserver.entities.DuelQuestionEntity;
import org.example.learningprojectserver.entities.QuestionEntity;
import org.example.learningprojectserver.entities.TopicMasteryEntity;
import org.example.learningprojectserver.enums.DuelMode;
import org.example.learningprojectserver.enums.DuelStatus;
import org.example.learningprojectserver.mappers.DuelEntityToDuelSummaryDTOMapper;
import org.example.learningprojectserver.notification.NotificationType;
import org.example.learningprojectserver.notification.dto.NotificationDTO;
import org.example.learningprojectserver.notification.dto.SystemMessageDTO;
import org.example.learningprojectserver.notification.publisher.NotificationEventPublisher;
import org.example.learningprojectserver.repository.DuelRepository;
import org.example.learningprojectserver.repository.TopicMasteryRepository;
import org.example.learningprojectserver.response.BasicResponse;
import org.example.learningprojectserver.service.adaptive.EloRating;
import org.example.learningprojectserver.service.adaptive.SkillCatalog;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 1-on-1 duels between students of one school. Everything lives in the database (duel, duel_question), so the
 * two screens follow each other by polling the duel's state; answers lock the duel row so the two players'
 * counters never overwrite each other.
 *
 * Matchmaking: suggested opponents are ordered by how close their Elo mastery of the sub-topic is to the
 * caller's, and the questions are generated at the level that fits the weaker of the two, so a weak student
 * is not crushed. COOP mode puts both students on one team against a fixed system score.
 */
@Service
@RequiredArgsConstructor
public class DuelService {

    static final int MAX_OPEN_INVITES = 5;
    static final int RECOMMENDED_GAP = 150;
    static final int MAX_OPPONENTS = 20;
    static final int LIST_LIMIT = 30;
    static final int MAX_ANSWER_LENGTH = 200;
    private static final List<DuelStatus> OPEN = List.of(DuelStatus.PENDING, DuelStatus.ACTIVE);

    private final DuelRepository duelRepository;
    private final TopicMasteryRepository topicMasteryRepository;
    private final DuelEntityToDuelSummaryDTOMapper summaryMapper;
    private final NotificationEventPublisher notificationEventPublisher;

    public BasicResponse<List<DuelTopicDTO>> getTopics() {
        List<DuelTopicDTO> topics = CompetitionCatalog.topics().entrySet().stream()
                .map(e -> new DuelTopicDTO(e.getKey(), e.getValue())).toList();
        return ok(null, topics);
    }

    /** Schoolmates ordered by mastery gap in the sub-topic (closest first), classmates first on a tie. */
    public BasicResponse<DuelOpponentsDTO> getOpponents(String userId, String subTopic) {
        if (!CompetitionCatalog.contains(subTopic)) return fail("הנושא שנבחר אינו זמין לדו-קרב");
        Object[] me = studentInfo(userId);
        if (me == null || me[1] == null) return fail("דו-קרב זמין רק לתלמידים המשויכים לבית ספר");
        Long myClass = (Long) me[2];

        List<Object[]> mates = duelRepository.findSchoolmates((String) me[1], userId);
        List<String> ids = new ArrayList<>(mates.stream().map(r -> (String) r[0]).toList());
        ids.add(userId);
        Map<String, Double> ratings = ratings(ids, subTopic);
        int myRating = (int) Math.round(ratings.getOrDefault(userId, EloRating.INITIAL_RATING));

        List<DuelOpponentDTO> opponents = mates.stream().map(r -> {
                    int rating = (int) Math.round(ratings.getOrDefault((String) r[0], EloRating.INITIAL_RATING));
                    int gap = Math.abs(rating - myRating);
                    boolean sameClass = myClass != null && myClass.equals(r[2]);
                    return new DuelOpponentDTO((String) r[0], (String) r[1], sameClass, rating, gap,
                            gap <= RECOMMENDED_GAP);
                })
                .sorted(Comparator.comparingInt(DuelOpponentDTO::ratingGap)
                        .thenComparing(o -> !o.sameClass())
                        .thenComparing(DuelOpponentDTO::username, Comparator.nullsLast(String::compareTo)))
                .limit(MAX_OPPONENTS)
                .toList();
        return ok(null, new DuelOpponentsDTO(myRating, opponents));
    }

    @Transactional
    public BasicResponse<DuelStateDTO> invite(String userId, DuelInviteRequest request) {
        String subTopic = request.subTopic() == null ? null : request.subTopic().trim();
        if (!CompetitionCatalog.contains(subTopic)) return fail("הנושא שנבחר אינו זמין לדו-קרב");
        if (userId.equals(request.opponentId())) return fail("אי אפשר להזמין את עצמך לדו-קרב");
        Object[] me = studentInfo(userId);
        if (me == null || me[1] == null) return fail("דו-קרב זמין רק לתלמידים המשויכים לבית ספר");
        Object[] other = studentInfo(request.opponentId());
        if (other == null || !Objects.equals(me[1], other[1])) return fail("התלמיד שנבחר אינו בבית הספר שלך");
        if (duelRepository.countOpenBetween(userId, request.opponentId(), OPEN) > 0) {
            return fail("כבר יש דו-קרב פתוח ביניכם");
        }
        if (duelRepository.countOpenByChallenger(userId, OPEN) >= MAX_OPEN_INVITES) {
            return fail("יש לך יותר מדי דו-קרבות פתוחים. סיים או בטל אחד מהם קודם");
        }

        Map<String, Double> ratings = ratings(List.of(userId, request.opponentId()), subTopic);
        double mine = ratings.getOrDefault(userId, EloRating.INITIAL_RATING);
        double theirs = ratings.getOrDefault(request.opponentId(), EloRating.INITIAL_RATING);

        DuelEntity duel = new DuelEntity();
        duel.setChallengerId(userId);
        duel.setChallengerName((String) me[0]);
        duel.setOpponentId(request.opponentId());
        duel.setOpponentName((String) other[0]);
        duel.setSchoolCode((String) me[1]);
        duel.setMode(request.mode());
        duel.setStatus(DuelStatus.PENDING);
        duel.setSubject(SkillCatalog.MATH);
        duel.setTopic(CompetitionCatalog.topicOf(subTopic));
        duel.setSubTopic(subTopic);
        duel.setLevel(duelLevel(request.mode(), mine, theirs));
        if (request.mode() == DuelMode.COOP) {
            duel.setSystemScore(systemScore(duel.getLevel()));
        }
        duel.setCreatedAt(LocalDateTime.now());
        for (int i = 0; i < CompetitionCatalog.DUEL_QUESTION_COUNT; i++) {
            QuestionEntity generated = CompetitionCatalog.generate(subTopic, duel.getLevel());
            DuelQuestionEntity q = new DuelQuestionEntity();
            q.setDuel(duel);
            q.setPosition(i);
            q.setQuestionText(generated.getQuestionText());
            q.setAnswer(generated.getAnswer());
            duel.getQuestions().add(q);
        }
        duelRepository.save(duel);
        notifyInvite(duel);
        return ok("ההזמנה נשלחה", state(duel, userId));
    }

    @Transactional
    public BasicResponse<DuelStateDTO> accept(String userId, Long duelId) {
        DuelEntity duel = duelRepository.findForUpdate(duelId).orElse(null);
        if (duel == null || !userId.equals(duel.getOpponentId())) return fail("הדו-קרב לא נמצא");
        if (duel.getStatus() != DuelStatus.PENDING) return fail("ההזמנה כבר אינה פתוחה");
        duel.setStatus(DuelStatus.ACTIVE);
        duel.setStartedAt(LocalDateTime.now());
        return ok("הדו-קרב התחיל!", state(duel, userId));
    }

    /** The invited student declines, or the challenger cancels, a duel that has not started. */
    @Transactional
    public BasicResponse<DuelStateDTO> decline(String userId, Long duelId) {
        DuelEntity duel = duelRepository.findForUpdate(duelId).orElse(null);
        if (duel == null || !isParticipant(duel, userId)) return fail("הדו-קרב לא נמצא");
        if (duel.getStatus() != DuelStatus.PENDING) return fail("אפשר לבטל רק הזמנה שעוד לא התחילה");
        duel.setStatus(DuelStatus.DECLINED);
        duel.setFinishedAt(LocalDateTime.now());
        return ok("ההזמנה בוטלה", state(duel, userId));
    }

    @Transactional
    public BasicResponse<DuelStateDTO> answer(String userId, Long duelId, DuelAnswerRequest request) {
        DuelEntity duel = duelRepository.findForUpdate(duelId).orElse(null);
        if (duel == null || !isParticipant(duel, userId)) return fail("הדו-קרב לא נמצא");
        if (duel.getStatus() != DuelStatus.ACTIVE) return fail("הדו-קרב אינו פעיל");
        String answer = request.answer() == null ? "" : request.answer().trim();
        if (answer.isEmpty()) return fail("יש להקליד תשובה");
        if (answer.length() > MAX_ANSWER_LENGTH) return fail("התשובה ארוכה מדי");
        DuelQuestionEntity q = duel.getQuestions().stream()
                .filter(x -> request.position() != null && x.getPosition() == request.position())
                .findFirst().orElse(null);
        if (q == null) return fail("השאלה לא נמצאה");

        boolean challenger = userId.equals(duel.getChallengerId());
        boolean correct = CompetitionCatalog.answersMatch(answer, q.getAnswer());
        LocalDateTime now = LocalDateTime.now();
        if (challenger) {
            if (q.getChallengerAnswer() != null) return fail("כבר ענית על שאלה זו");
            q.setChallengerAnswer(answer);
            q.setChallengerCorrect(correct);
            q.setChallengerAnsweredAt(now);
            duel.setChallengerAnswered(duel.getChallengerAnswered() + 1);
            if (correct) duel.setChallengerCorrect(duel.getChallengerCorrect() + 1);
            if (duel.getChallengerAnswered() >= duel.getQuestions().size()) duel.setChallengerFinishedAt(now);
        } else {
            if (q.getOpponentAnswer() != null) return fail("כבר ענית על שאלה זו");
            q.setOpponentAnswer(answer);
            q.setOpponentCorrect(correct);
            q.setOpponentAnsweredAt(now);
            duel.setOpponentAnswered(duel.getOpponentAnswered() + 1);
            if (correct) duel.setOpponentCorrect(duel.getOpponentCorrect() + 1);
            if (duel.getOpponentAnswered() >= duel.getQuestions().size()) duel.setOpponentFinishedAt(now);
        }
        if (duel.getChallengerFinishedAt() != null && duel.getOpponentFinishedAt() != null) {
            duel.setStatus(DuelStatus.FINISHED);
            duel.setFinishedAt(now);
            duel.setOutcome(outcome(duel));
        }
        return ok(correct ? "נכון!" : "לא נכון", state(duel, userId));
    }

    @Transactional(readOnly = true)
    public BasicResponse<DuelStateDTO> getState(String userId, Long duelId) {
        DuelEntity duel = duelRepository.findById(duelId).orElse(null);
        if (duel == null || !isParticipant(duel, userId)) return fail("הדו-קרב לא נמצא");
        return ok(null, state(duel, userId));
    }

    @Transactional(readOnly = true)
    public BasicResponse<List<DuelSummaryDTO>> getMyDuels(String userId) {
        List<DuelSummaryDTO> list = duelRepository.findForStudent(userId, PageRequest.of(0, LIST_LIMIT)).stream()
                .map(summaryMapper).toList();
        return ok(null, list);
    }

    // ---- rules ----

    /** Competitive: the level that fits the weaker player. Coop: the pair's average. */
    static int duelLevel(DuelMode mode, double ratingA, double ratingB) {
        double basis = mode == DuelMode.COOP ? (ratingA + ratingB) / 2 : Math.min(ratingA, ratingB);
        return EloRating.pickLevel(basis);
    }

    /** The score the pair has to beat in coop mode, out of 2 x DUEL_QUESTION_COUNT answers. */
    static int systemScore(int level) {
        return Math.min(2 * CompetitionCatalog.DUEL_QUESTION_COUNT - 1, 5 + level / 2);
    }

    /** More correct answers wins; on a tie, whoever finished first; otherwise a draw. Coop: team vs system. */
    static String outcome(DuelEntity d) {
        if (d.getMode() == DuelMode.COOP) {
            int team = d.getChallengerCorrect() + d.getOpponentCorrect();
            int system = d.getSystemScore() == null ? 0 : d.getSystemScore();
            return team > system ? "TEAM" : team < system ? "SYSTEM" : "DRAW";
        }
        int diff = Integer.compare(d.getChallengerCorrect(), d.getOpponentCorrect());
        if (diff == 0 && d.getChallengerFinishedAt() != null && d.getOpponentFinishedAt() != null) {
            diff = -d.getChallengerFinishedAt().compareTo(d.getOpponentFinishedAt());
        }
        return diff > 0 ? "CHALLENGER" : diff < 0 ? "OPPONENT" : "DRAW";
    }

    // ---- internals ----

    private DuelStateDTO state(DuelEntity duel, String userId) {
        boolean challenger = userId.equals(duel.getChallengerId());
        boolean started = duel.getStatus() == DuelStatus.ACTIVE || duel.getStatus() == DuelStatus.FINISHED;
        boolean reveal = duel.getStatus() == DuelStatus.FINISHED;
        List<DuelQuestionDTO> questions = !started ? List.of() : duel.getQuestions().stream()
                .sorted(Comparator.comparingInt(DuelQuestionEntity::getPosition))
                .map(q -> new DuelQuestionDTO(q.getPosition(), q.getQuestionText(),
                        challenger ? q.getChallengerAnswer() : q.getOpponentAnswer(),
                        challenger ? q.getChallengerCorrect() : q.getOpponentCorrect(),
                        reveal ? q.getAnswer() : null))
                .toList();
        return new DuelStateDTO(summaryMapper.apply(duel), questions);
    }

    private static boolean isParticipant(DuelEntity duel, String userId) {
        return userId != null && (userId.equals(duel.getChallengerId()) || userId.equals(duel.getOpponentId()));
    }

    /** [username, schoolCode, classRoomId], or null when the user is not a student. */
    private Object[] studentInfo(String userId) {
        if (userId == null) return null;
        List<Object[]> rows = duelRepository.findStudentInfo(userId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private Map<String, Double> ratings(Collection<String> userIds, String subTopic) {
        String canonical = SkillCatalog.canonicalSubTopic(subTopic);
        Map<String, Double> ratings = new HashMap<>();
        for (TopicMasteryEntity m : topicMasteryRepository.findByStudentUserIdIn(userIds)) {
            if (SkillCatalog.MATH.equals(m.getSubject()) && canonical.equals(m.getSubTopic())) {
                ratings.put(m.getStudentUserId(), m.getRating());
            }
        }
        return ratings;
    }

    private void notifyInvite(DuelEntity duel) {
        String kind = duel.getMode() == DuelMode.COOP ? "דו-קרב שיתופי" : "דו-קרב";
        MessageDTO message = new MessageDTO();
        message.setTitle("הוזמנת ל" + kind);
        message.setContent(duel.getChallengerName() + " מזמין אותך ל" + kind + " ב" + duel.getSubTopic());
        message.setSenderName(duel.getChallengerName());
        message.setSentAt(LocalDateTime.now());
        NotificationDTO<SystemMessageDTO> dto = new NotificationDTO<>(NotificationType.SYSTEM_MESSAGE,
                new SystemMessageDTO(message));
        dto.setTitle(message.getTitle());
        dto.setBody(message.getContent());
        dto.setLink("/duels");
        notificationEventPublisher.publish(List.of(duel.getOpponentId()), dto);
    }

    private static <T> BasicResponse<T> ok(String message, T data) {
        BasicResponse<T> response = new BasicResponse<>(true, message);
        response.setData(data);
        return response;
    }

    private static <T> BasicResponse<T> fail(String message) {
        return new BasicResponse<>(false, message);
    }
}
