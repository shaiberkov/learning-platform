package org.example.learningprojectserver.service.quest;

import lombok.RequiredArgsConstructor;
import org.example.learningprojectserver.dto.QuestDTOs;
import org.example.learningprojectserver.dto.QuestDTOs.AnswerResult;
import org.example.learningprojectserver.dto.QuestDTOs.ChapterView;
import org.example.learningprojectserver.dto.QuestDTOs.CosmeticItem;
import org.example.learningprojectserver.dto.QuestDTOs.Cosmetics;
import org.example.learningprojectserver.dto.QuestDTOs.QuestState;
import org.example.learningprojectserver.dto.QuestDTOs.QuestSummary;
import org.example.learningprojectserver.entities.QuestProgressEntity;
import org.example.learningprojectserver.entities.QuestionEntity;
import org.example.learningprojectserver.entities.StudentCosmeticEntity;
import org.example.learningprojectserver.mappers.StudentCosmeticEntityToItemMapper;
import org.example.learningprojectserver.repository.QuestProgressRepository;
import org.example.learningprojectserver.repository.StudentCosmeticRepository;
import org.example.learningprojectserver.response.BasicResponse;
import org.example.learningprojectserver.service.EngagementService;
import org.example.learningprojectserver.service.EngagementService.BadgeGrant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.example.learningprojectserver.dto.QuestDTOs.COMPLETED;
import static org.example.learningprojectserver.dto.QuestDTOs.IN_PROGRESS;
import static org.example.learningprojectserver.dto.QuestDTOs.NOT_STARTED;

/**
 * Story quests: a topic becomes a journey of chapters, each passed by answering generated questions.
 * Progress, the pending question and its answer live in student_quest_progress; finishing a quest
 * unlocks cosmetic items (avatar, room theme), a badge and XP. Not cached: every answer changes it.
 */
@Service
@RequiredArgsConstructor
public class QuestService {

    private static final Logger LOG = Logger.getLogger(QuestService.class.getName());

    static final int MAX_TRIES = 3;
    static final int XP_CORRECT = 10;
    static final int XP_QUEST_DONE = 50;

    static final String QUEST_NOT_FOUND = "המסע לא נמצא";
    static final String NOT_STARTED_MSG = "עוד לא התחלת את המסע הזה";
    static final String ALREADY_DONE = "המסע כבר הושלם. אפשר לבחור מסע אחר!";
    static final String EMPTY_ANSWER = "נא לכתוב תשובה";
    static final String ITEM_NOT_FOUND = "הפריט לא נמצא";
    static final String ITEM_LOCKED = "הפריט עדיין נעול. מסיימים את המסע כדי לפתוח אותו";

    private final QuestProgressRepository progressRepository;
    private final StudentCosmeticRepository cosmeticRepository;
    private final QuestQuestionSource questionSource;
    private final EngagementService engagementService;
    private final StudentCosmeticEntityToItemMapper cosmeticMapper;

    @Transactional(readOnly = true)
    public BasicResponse<List<QuestSummary>> listQuests(String studentId) {
        Map<String, QuestProgressEntity> progress = progressRepository.findByStudentUserId(studentId).stream()
                .collect(Collectors.toMap(QuestProgressEntity::getQuestCode, Function.identity(), (a, b) -> a));
        Map<String, StudentCosmeticEntity> owned = ownedItems(studentId);

        List<QuestSummary> list = new ArrayList<>();
        for (QuestCatalog.Quest quest : QuestCatalog.QUESTS) {
            QuestProgressEntity p = progress.get(quest.code());
            list.add(new QuestSummary(quest.code(), quest.title(), quest.emoji(), quest.tagline(), quest.gradeBand(),
                    quest.topic(), quest.chapters().size(),
                    p == null ? 0 : Math.min(p.getChapterIndex(), quest.chapters().size()),
                    status(p), rewards(quest, owned)));
        }
        return ok("", list);
    }

    @Transactional
    public BasicResponse<QuestState> getState(String studentId, String questCode) {
        QuestCatalog.Quest quest = QuestCatalog.quest(questCode);
        if (quest == null) {
            return new BasicResponse<>(false, QUEST_NOT_FOUND);
        }
        QuestProgressEntity p = progressRepository.findByStudentUserIdAndQuestCode(studentId, questCode).orElse(null);
        if (p != null && ensureQuestion(quest, p)) {
            progressRepository.save(p);
        }
        return ok("", state(quest, p, ownedItems(studentId)));
    }

    /** Starts the quest, or returns where the student already is. */
    @Transactional
    public BasicResponse<QuestState> start(String studentId, String questCode) {
        QuestCatalog.Quest quest = QuestCatalog.quest(questCode);
        if (quest == null) {
            return new BasicResponse<>(false, QUEST_NOT_FOUND);
        }
        QuestProgressEntity p = progressRepository.findByStudentUserIdAndQuestCode(studentId, questCode).orElse(null);
        if (p == null) {
            p = new QuestProgressEntity();
            p.setStudentUserId(studentId);
            p.setQuestCode(questCode);
            p.setStartedAt(LocalDateTime.now());
        }
        ensureQuestion(quest, p);
        p.setUpdatedAt(LocalDateTime.now());
        progressRepository.save(p);
        return ok("המסע התחיל. בהצלחה!", state(quest, p, ownedItems(studentId)));
    }

    @Transactional
    public BasicResponse<AnswerResult> answer(String studentId, String questCode, String answer) {
        QuestCatalog.Quest quest = QuestCatalog.quest(questCode);
        if (quest == null) {
            return new BasicResponse<>(false, QUEST_NOT_FOUND);
        }
        QuestProgressEntity p = progressRepository.findByStudentUserIdAndQuestCode(studentId, questCode).orElse(null);
        if (p == null) {
            return new BasicResponse<>(false, NOT_STARTED_MSG);
        }
        if (p.getCompletedAt() != null) {
            return new BasicResponse<>(false, ALREADY_DONE);
        }
        if (answer == null || answer.isBlank()) {
            return new BasicResponse<>(false, EMPTY_ANSWER);
        }
        if (p.getPendingAnswer() == null) {
            // No question was waiting (e.g. a generator failed earlier): hand out one now, nothing is graded.
            ensureQuestion(quest, p);
            progressRepository.save(p);
            return new BasicResponse<>(false, "הנה שאלה חדשה. נסו לפתור אותה");
        }

        boolean correct = answersMatch(answer, p.getPendingAnswer());
        boolean chapterDone = false;
        boolean questDone = false;
        String revealed = null;
        String message;
        List<CosmeticItem> unlocked = List.of();

        if (correct) {
            p.setTotalCorrect(p.getTotalCorrect() + 1);
            p.setChapterCorrect(p.getChapterCorrect() + 1);
            p.setWrongAttempts(0);
            clearQuestion(p);
            QuestCatalog.Chapter chapter = quest.chapters().get(p.getChapterIndex());
            message = "נכון! כל הכבוד";
            if (p.getChapterCorrect() >= chapter.questionsToPass()) {
                chapterDone = true;
                p.setChapterIndex(p.getChapterIndex() + 1);
                p.setChapterCorrect(0);
                if (p.getChapterIndex() >= quest.chapters().size()) {
                    questDone = true;
                    p.setCompletedAt(LocalDateTime.now());
                    unlocked = unlockRewards(studentId, quest);
                    message = quest.outro();
                } else {
                    message = "סיימת את הפרק \"" + chapter.title() + "\"! ממשיכים לפרק הבא: "
                            + quest.chapters().get(p.getChapterIndex()).title();
                }
            }
        } else {
            p.setWrongAttempts(p.getWrongAttempts() + 1);
            if (p.getWrongAttempts() >= MAX_TRIES) {
                revealed = p.getPendingAnswer();
                p.setWrongAttempts(0);
                clearQuestion(p);
                message = "לא נורא! התשובה הנכונה הייתה " + revealed + ". הנה שאלה חדשה";
            } else {
                int left = MAX_TRIES - p.getWrongAttempts();
                message = "כמעט! נסו שוב (" + (left == 1 ? "נשאר ניסיון אחד" : "נשארו " + left + " ניסיונות") + ")";
            }
        }

        ensureQuestion(quest, p);
        p.setUpdatedAt(LocalDateTime.now());
        progressRepository.save(p);

        if (correct) {
            rewardEngagement(studentId, quest, questDone);
        }
        AnswerResult result = new AnswerResult(correct, message, revealed, chapterDone, questDone, unlocked,
                state(quest, p, ownedItems(studentId)));
        return ok(message, result);
    }

    @Transactional(readOnly = true)
    public BasicResponse<Cosmetics> getCosmetics(String studentId) {
        return ok("", cosmetics(ownedItems(studentId)));
    }

    /** Equips an unlocked item; equipping a default item takes off the current one of that kind. */
    @Transactional
    public BasicResponse<Cosmetics> equip(String studentId, String itemCode) {
        QuestCatalog.Item item = QuestCatalog.item(itemCode);
        if (item == null) {
            return new BasicResponse<>(false, ITEM_NOT_FOUND);
        }
        List<StudentCosmeticEntity> owned = cosmeticRepository.findByStudentUserId(studentId);
        boolean isDefault = QuestCatalog.isDefault(itemCode);
        if (!isDefault && owned.stream().noneMatch(c -> c.getItemCode().equals(itemCode))) {
            return new BasicResponse<>(false, ITEM_LOCKED);
        }
        for (StudentCosmeticEntity c : owned) {
            if (c.getKind().equals(item.kind())) {
                c.setEquipped(c.getItemCode().equals(itemCode));
            }
        }
        cosmeticRepository.saveAll(owned);
        Map<String, StudentCosmeticEntity> byCode = owned.stream()
                .collect(Collectors.toMap(StudentCosmeticEntity::getItemCode, Function.identity(), (a, b) -> a));
        return ok("עודכן! \"" + item.name() + "\" בשימוש", cosmetics(byCode));
    }

    // ---- answer checking ----

    /**
     * Same value as the generator's answer: exact text after trimming spaces and direction marks, or the
     * same number when both are integers, decimals or fractions ("2/4" equals "1/2", "0.5" equals "1/2").
     */
    static boolean answersMatch(String given, String expected) {
        String g = clean(given);
        String e = clean(expected);
        if (g.isEmpty() || e.isEmpty()) {
            return false;
        }
        if (g.equalsIgnoreCase(e)) {
            return true;
        }
        BigInteger[] gf = rational(g);
        BigInteger[] ef = rational(e);
        return gf != null && ef != null && gf[0].multiply(ef[1]).equals(ef[0].multiply(gf[1]));
    }

    private static String clean(String s) {
        if (s == null) return "";
        return s.replaceAll("[\\u200E\\u200F\\u202A-\\u202E\\s]", "").replace('−', '-');
    }

    /** {numerator, denominator} with a positive denominator, or null when the text is not a number. */
    static BigInteger[] rational(String s) {
        try {
            int slash = s.indexOf('/');
            if (slash < 0) {
                BigDecimal d = new BigDecimal(s);
                BigInteger num = d.unscaledValue();
                BigInteger den = BigInteger.TEN.pow(Math.max(d.scale(), 0));
                if (d.scale() < 0) {
                    num = num.multiply(BigInteger.TEN.pow(-d.scale()));
                }
                return new BigInteger[]{num, den};
            }
            BigInteger num = new BigInteger(s.substring(0, slash));
            BigInteger den = new BigInteger(s.substring(slash + 1));
            if (den.signum() == 0) return null;
            if (den.signum() < 0) {
                num = num.negate();
                den = den.negate();
            }
            return new BigInteger[]{num, den};
        } catch (NumberFormatException | ArithmeticException ex) {
            return null;
        }
    }

    // ---- internals ----

    /** Gives the progress a question when it has none and the quest is not finished; true when it changed. */
    private boolean ensureQuestion(QuestCatalog.Quest quest, QuestProgressEntity p) {
        if (p.getCompletedAt() != null || p.getPendingAnswer() != null
                || p.getChapterIndex() >= quest.chapters().size()) {
            return false;
        }
        QuestCatalog.Chapter chapter = quest.chapters().get(p.getChapterIndex());
        try {
            QuestionEntity q = questionSource.generate(chapter.subTopic(), chapter.difficulty());
            p.setPendingQuestion(q.getQuestionText());
            p.setPendingAnswer(q.getAnswer());
            return true;
        } catch (RuntimeException e) {
            LOG.log(Level.WARNING, "Quest question generation failed for " + chapter.subTopic(), e);
            return false;
        }
    }

    private static void clearQuestion(QuestProgressEntity p) {
        p.setPendingQuestion(null);
        p.setPendingAnswer(null);
    }

    private List<CosmeticItem> unlockRewards(String studentId, QuestCatalog.Quest quest) {
        List<CosmeticItem> unlocked = new ArrayList<>();
        for (String code : quest.rewardItems()) {
            QuestCatalog.Item item = QuestCatalog.item(code);
            if (item == null || cosmeticRepository.existsByStudentUserIdAndItemCode(studentId, code)) {
                continue;
            }
            StudentCosmeticEntity c = new StudentCosmeticEntity();
            c.setStudentUserId(studentId);
            c.setItemCode(code);
            c.setKind(item.kind());
            c.setEquipped(false);
            c.setUnlockedAt(LocalDateTime.now());
            cosmeticRepository.save(c);
            unlocked.add(StudentCosmeticEntityToItemMapper.describe(item, true, false));
        }
        return unlocked;
    }

    /** XP and the quest badge; a failure here never undoes the answer. */
    private void rewardEngagement(String studentId, QuestCatalog.Quest quest, boolean questDone) {
        try {
            List<BadgeGrant> badges = questDone
                    ? List.of(new BadgeGrant("QUEST:" + quest.code(), "מסע הושלם: " + quest.title(),
                    "השלמת את כל הפרקים במסע \"" + quest.title() + "\""))
                    : List.of();
            engagementService.recordActivity(studentId, XP_CORRECT + (questDone ? XP_QUEST_DONE : 0), badges);
        } catch (RuntimeException e) {
            LOG.log(Level.WARNING, "Quest engagement update failed for " + studentId, e);
        }
    }

    private Map<String, StudentCosmeticEntity> ownedItems(String studentId) {
        return cosmeticRepository.findByStudentUserId(studentId).stream()
                .collect(Collectors.toMap(StudentCosmeticEntity::getItemCode, Function.identity(), (a, b) -> a));
    }

    private Cosmetics cosmetics(Map<String, StudentCosmeticEntity> owned) {
        List<CosmeticItem> items = new ArrayList<>();
        CosmeticItem avatar = null;
        CosmeticItem room = null;
        for (QuestCatalog.Item item : QuestCatalog.allItems()) {
            StudentCosmeticEntity c = owned.get(item.code());
            CosmeticItem view = c != null ? cosmeticMapper.apply(c)
                    : StudentCosmeticEntityToItemMapper.describe(item, QuestCatalog.isDefault(item.code()), false);
            items.add(view);
            if (view.equipped()) {
                if (QuestCatalog.AVATAR.equals(item.kind())) avatar = view;
                else room = view;
            }
        }
        if (avatar == null) avatar = StudentCosmeticEntityToItemMapper.describe(QuestCatalog.DEFAULT_AVATAR, true, true);
        if (room == null) room = StudentCosmeticEntityToItemMapper.describe(QuestCatalog.DEFAULT_ROOM, true, true);
        CosmeticItem finalAvatar = avatar;
        CosmeticItem finalRoom = room;
        // Mark the defaults as equipped when nothing else of their kind is.
        items = items.stream().map(i -> {
            if (i.code().equals(finalAvatar.code()) || i.code().equals(finalRoom.code())) {
                return new CosmeticItem(i.code(), i.kind(), i.name(), i.emoji(), i.tone(), i.decor(),
                        i.questTitle(), true, true);
            }
            return i;
        }).toList();
        return new Cosmetics(avatar, room, items);
    }

    private static List<CosmeticItem> rewards(QuestCatalog.Quest quest, Map<String, StudentCosmeticEntity> owned) {
        List<CosmeticItem> list = new ArrayList<>();
        for (String code : quest.rewardItems()) {
            QuestCatalog.Item item = QuestCatalog.item(code);
            if (item != null) {
                StudentCosmeticEntity c = owned.get(code);
                list.add(StudentCosmeticEntityToItemMapper.describe(item, c != null, c != null && c.isEquipped()));
            }
        }
        return list;
    }

    private static String status(QuestProgressEntity p) {
        if (p == null) return NOT_STARTED;
        return p.getCompletedAt() != null ? COMPLETED : IN_PROGRESS;
    }

    private static QuestState state(QuestCatalog.Quest quest, QuestProgressEntity p,
                                    Map<String, StudentCosmeticEntity> owned) {
        int index = p == null ? 0 : Math.min(p.getChapterIndex(), quest.chapters().size());
        boolean done = p != null && p.getCompletedAt() != null;
        List<ChapterView> chapters = new ArrayList<>();
        for (int i = 0; i < quest.chapters().size(); i++) {
            QuestCatalog.Chapter c = quest.chapters().get(i);
            chapters.add(new ChapterView(i + 1, c.title(), c.story(), c.subTopic(), c.questionsToPass(),
                    i < index, p != null && !done && i == index));
        }
        int toPass = index < quest.chapters().size() ? quest.chapters().get(index).questionsToPass() : 0;
        return new QuestState(quest.code(), quest.title(), quest.emoji(), quest.intro(), quest.outro(), status(p),
                index, quest.chapters().size(), chapters,
                p == null ? 0 : p.getChapterCorrect(), toPass,
                p == null ? MAX_TRIES : MAX_TRIES - p.getWrongAttempts(),
                p == null || done ? null : p.getPendingQuestion(),
                rewards(quest, owned));
    }

    private static <T> BasicResponse<T> ok(String message, T data) {
        BasicResponse<T> response = new BasicResponse<>(true, message);
        response.setData(data);
        return response;
    }
}
