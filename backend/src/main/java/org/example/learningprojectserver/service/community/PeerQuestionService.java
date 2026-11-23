package org.example.learningprojectserver.service.community;

import lombok.RequiredArgsConstructor;
import org.example.learningprojectserver.dto.BankQuestionDTO;
import org.example.learningprojectserver.dto.community.ClassRefDTO;
import org.example.learningprojectserver.dto.community.PeerQuestionDTOs.AnswerResultDTO;
import org.example.learningprojectserver.dto.community.PeerQuestionDTOs.PeerQuestionDTO;
import org.example.learningprojectserver.dto.community.PeerQuestionDTOs.PracticeQuestionDTO;
import org.example.learningprojectserver.dto.community.PeerQuestionDTOs.SubmitRequest;
import org.example.learningprojectserver.entities.BankQuestionEntity;
import org.example.learningprojectserver.entities.PeerQuestionEntity;
import org.example.learningprojectserver.enums.PeerQuestionStatus;
import org.example.learningprojectserver.mappers.PeerQuestionEntityToDTOMapper;
import org.example.learningprojectserver.repository.BankQuestionRepository;
import org.example.learningprojectserver.repository.PeerQuestionRepository;
import org.example.learningprojectserver.response.BasicResponse;
import org.example.learningprojectserver.security.ClassCommunityAccess;
import org.example.learningprojectserver.service.questionbank.QuestionBankService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.example.learningprojectserver.dto.community.PeerQuestionDTOs.*;

/**
 * Students write multiple-choice questions for their classmates. The class teacher reviews them; an approved
 * question is added to the teacher's question bank (source STUDENT, through QuestionBankService) and served in
 * the class practice with credit to its author. Classmates' answers are counted on the bank question.
 * Nothing here is cached.
 */
@Service
@RequiredArgsConstructor
public class PeerQuestionService {

    /** A student may have this many questions waiting for review at once. */
    static final int MAX_PENDING_PER_STUDENT = 10;
    static final String BANK_SOURCE = "STUDENT";

    private final PeerQuestionRepository peerQuestionRepository;
    private final BankQuestionRepository bankQuestionRepository;
    private final QuestionBankService questionBankService;
    private final ClassCommunitySupport support;
    private final PeerQuestionEntityToDTOMapper mapper;

    @Transactional
    public BasicResponse<PeerQuestionDTO> submit(String userId, String role, SubmitRequest request) {
        if (!ClassCommunityAccess.isStudent(role)) {
            return new BasicResponse<>(false, "רק תלמידים כותבים שאלות לחברי הכיתה");
        }
        String error = validate(request);
        if (error != null) return new BasicResponse<>(false, error);
        ClassRefDTO classRef = support.resolveClass(userId, role, null);
        if (classRef == null) return new BasicResponse<>(false, ClassCommunitySupport.NO_CLASS);
        if (peerQuestionRepository.countByAuthorUserIdAndStatus(userId, PeerQuestionStatus.PENDING)
                >= MAX_PENDING_PER_STUDENT) {
            return new BasicResponse<>(false, "יש לך כבר " + MAX_PENDING_PER_STUDENT
                    + " שאלות שממתינות לאישור המורה. אפשר לכתוב שאלה חדשה אחרי שהמורה יבדוק אותן");
        }

        PeerQuestionEntity q = new PeerQuestionEntity();
        q.setClassRoomId(classRef.getId());
        q.setSchoolCode(classRef.getSchoolCode());
        q.setAuthorUserId(userId);
        q.setAuthorName(support.nameOf(userId));
        q.setQuestionText(request.getQuestionText().trim());
        q.setCorrectAnswer(request.getCorrectAnswer().trim());
        q.setDistractors(String.join("\n", cleanDistractors(request.getDistractors())));
        q.setTopic(blankToNull(request.getTopic()));
        q.setExplanation(blankToNull(request.getExplanation()));
        q.setStatus(PeerQuestionStatus.PENDING);
        q.setCreatedAt(LocalDateTime.now());
        peerQuestionRepository.save(q);

        support.notify(support.teachersOf(classRef.getId()), "שאלה חדשה של תלמיד ממתינה לאישור",
                q.getAuthorName() + " כתב/ה שאלה לחברי הכיתה", q.getAuthorName());
        return ok("השאלה נשלחה למורה לאישור", mapper.apply(q));
    }

    /** The student's own questions, with the review result and how classmates did on the approved ones. */
    @Transactional(readOnly = true)
    public BasicResponse<List<PeerQuestionDTO>> mine(String userId) {
        List<PeerQuestionEntity> questions = peerQuestionRepository.findByAuthorUserIdOrderByCreatedAtDesc(userId);
        Map<Long, BankQuestionEntity> bank = bankQuestions(questions);
        return ok(null, questions.stream().map(q -> withStats(q, bank)).toList());
    }

    /** The approved questions of the student's class, options shuffled and without the answer. */
    @Transactional(readOnly = true)
    public BasicResponse<List<PracticeQuestionDTO>> practice(String userId, String role, Long classId) {
        ClassRefDTO classRef = support.resolveClass(userId, role, classId);
        if (classRef == null) return new BasicResponse<>(false, ClassCommunitySupport.NO_CLASS);
        List<PracticeQuestionDTO> list = peerQuestionRepository
                .findByClassRoomIdAndStatusOrderByReviewedAtDesc(classRef.getId(), PeerQuestionStatus.APPROVED)
                .stream()
                .map(q -> {
                    PracticeQuestionDTO dto = new PracticeQuestionDTO();
                    dto.setId(q.getId());
                    dto.setQuestionText(q.getQuestionText());
                    dto.setTopic(q.getTopic());
                    dto.setAuthorName(q.getAuthorName());
                    dto.setMine(userId != null && userId.equals(q.getAuthorUserId()));
                    List<String> options = new ArrayList<>();
                    options.add(q.getCorrectAnswer());
                    options.addAll(PeerQuestionEntityToDTOMapper.splitLines(q.getDistractors()));
                    Collections.shuffle(options);
                    dto.setOptions(options);
                    return dto;
                })
                .toList();
        return ok(null, list);
    }

    /** Checks a classmate's answer; answers to someone else's question count on the bank question. */
    @Transactional
    public BasicResponse<AnswerResultDTO> answer(String userId, String role, Long peerQuestionId, String answer) {
        if (answer == null || answer.isBlank()) return new BasicResponse<>(false, "יש לבחור תשובה");
        PeerQuestionEntity q = peerQuestionId == null ? null : peerQuestionRepository.findById(peerQuestionId).orElse(null);
        if (q == null || q.getStatus() != PeerQuestionStatus.APPROVED
                || !support.belongsTo(userId, role, q.getClassRoomId())) {
            return new BasicResponse<>(false, "השאלה לא נמצאה בתרגול הכיתה");
        }
        boolean correct = answer.trim().equals(q.getCorrectAnswer().trim());
        if (q.getBankQuestionId() != null && !q.getAuthorUserId().equals(userId)) {
            peerQuestionRepository.recordBankAnswer(q.getBankQuestionId(), correct ? 1 : 0);
        }
        AnswerResultDTO result = new AnswerResultDTO();
        result.setCorrect(correct);
        result.setCorrectAnswer(q.getCorrectAnswer());
        result.setExplanation(q.getExplanation());
        result.setAuthorName(q.getAuthorName());
        return ok(correct ? "נכון!" : "לא מדויק", result);
    }

    /** The teacher's review queue: questions in the given status (PENDING by default) from their classes. */
    @Transactional(readOnly = true)
    public BasicResponse<List<PeerQuestionDTO>> reviewQueue(String userId, String role, Long classId, String status) {
        if (!ClassCommunityAccess.isTeacher(role) && !ClassCommunityAccess.isAdmin(role)) {
            return new BasicResponse<>(false, "רק מורים בודקים שאלות של תלמידים");
        }
        PeerQuestionStatus wanted;
        try {
            wanted = status == null || status.isBlank() ? PeerQuestionStatus.PENDING
                    : PeerQuestionStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return new BasicResponse<>(false, "סטטוס לא מוכר");
        }
        List<Long> classIds;
        if (classId != null) {
            if (!support.belongsTo(userId, role, classId)) return new BasicResponse<>(false, ClassCommunitySupport.NO_CLASS);
            classIds = List.of(classId);
        } else {
            classIds = support.classesOf(userId, role).stream().map(ClassRefDTO::getId).toList();
        }
        if (classIds.isEmpty()) return ok(null, List.of());
        List<PeerQuestionEntity> questions = peerQuestionRepository.findByClassRoomIdInAndStatusOrderByCreatedAtAsc(classIds, wanted);
        Map<Long, BankQuestionEntity> bank = bankQuestions(questions);
        return ok(null, questions.stream().map(q -> withStats(q, bank)).toList());
    }

    /** Approves a pending question: it joins the teacher's bank and the class practice, credited to its author. */
    @Transactional
    public BasicResponse<PeerQuestionDTO> approve(String teacherId, String role, Long peerQuestionId, String note) {
        if (!ClassCommunityAccess.isTeacher(role)) {
            return new BasicResponse<>(false, "רק המורה של הכיתה מאשר שאלות");
        }
        PeerQuestionEntity q = findReviewable(teacherId, role, peerQuestionId);
        if (q == null) return new BasicResponse<>(false, "השאלה לא נמצאה");
        if (q.getStatus() != PeerQuestionStatus.PENDING) return new BasicResponse<>(false, "השאלה כבר נבדקה");
        if (note != null && note.length() > MAX_NOTE) return new BasicResponse<>(false, "ההערה ארוכה מדי");

        BankQuestionDTO bankDto = new BankQuestionDTO();
        bankDto.setQuestionText(q.getQuestionText());
        bankDto.setAnswer(q.getCorrectAnswer());
        List<String> options = new ArrayList<>();
        options.add(q.getCorrectAnswer());
        options.addAll(PeerQuestionEntityToDTOMapper.splitLines(q.getDistractors()));
        bankDto.setOptions(options);
        bankDto.setTopic(q.getTopic());
        bankDto.setShared(false);
        BasicResponse<BankQuestionDTO> added = questionBankService.addQuestion(teacherId, bankDto);
        if (!added.isSuccess() || added.getData() == null) {
            return new BasicResponse<>(false, added.getErrorCode());
        }
        Long bankId = added.getData().getId();
        bankQuestionRepository.findById(bankId).ifPresent(b -> {
            b.setSource(BANK_SOURCE);
            b.setExplanation(q.getExplanation());
            bankQuestionRepository.save(b);
        });

        q.setStatus(PeerQuestionStatus.APPROVED);
        q.setBankQuestionId(bankId);
        q.setReviewerTeacherId(teacherId);
        q.setReviewNote(blankToNull(note));
        q.setReviewedAt(LocalDateTime.now());
        peerQuestionRepository.save(q);

        support.notify(List.of(q.getAuthorUserId()), "השאלה שכתבת אושרה ✓",
                "השאלה שלך נכנסה לתרגול של הכיתה" + (q.getReviewNote() == null ? "" : ". הערת המורה: " + q.getReviewNote()),
                support.nameOf(teacherId));
        return ok("השאלה אושרה ונכנסה לתרגול הכיתה ולמאגר השאלות שלך", mapper.apply(q));
    }

    @Transactional
    public BasicResponse<PeerQuestionDTO> reject(String teacherId, String role, Long peerQuestionId, String note) {
        if (!ClassCommunityAccess.isTeacher(role) && !ClassCommunityAccess.isAdmin(role)) {
            return new BasicResponse<>(false, "רק המורה של הכיתה דוחה שאלות");
        }
        if (note == null || note.isBlank()) {
            return new BasicResponse<>(false, "יש לכתוב לתלמיד מה לתקן");
        }
        if (note.length() > MAX_NOTE) return new BasicResponse<>(false, "ההערה ארוכה מדי");
        PeerQuestionEntity q = findReviewable(teacherId, role, peerQuestionId);
        if (q == null) return new BasicResponse<>(false, "השאלה לא נמצאה");
        if (q.getStatus() != PeerQuestionStatus.PENDING) return new BasicResponse<>(false, "השאלה כבר נבדקה");

        q.setStatus(PeerQuestionStatus.REJECTED);
        q.setReviewerTeacherId(teacherId);
        q.setReviewNote(note.trim());
        q.setReviewedAt(LocalDateTime.now());
        peerQuestionRepository.save(q);

        support.notify(List.of(q.getAuthorUserId()), "המורה החזיר/ה את השאלה שכתבת",
                "הערת המורה: " + q.getReviewNote(), support.nameOf(teacherId));
        return ok("השאלה הוחזרה לתלמיד עם ההערה", mapper.apply(q));
    }

    /** Null when valid, else the Hebrew reason. */
    public static String validate(SubmitRequest request) {
        if (request == null || request.getQuestionText() == null || request.getQuestionText().isBlank()) {
            return "יש לכתוב את נוסח השאלה";
        }
        if (request.getCorrectAnswer() == null || request.getCorrectAnswer().isBlank()) {
            return "יש לכתוב את התשובה הנכונה";
        }
        if (request.getQuestionText().length() > MAX_TEXT) return "נוסח השאלה ארוך מדי";
        if (request.getCorrectAnswer().length() > MAX_OPTION) return "התשובה ארוכה מדי";
        if (request.getTopic() != null && request.getTopic().length() > MAX_TOPIC) return "שם הנושא ארוך מדי";
        if (request.getExplanation() != null && request.getExplanation().length() > MAX_EXPLANATION) {
            return "ההסבר ארוך מדי";
        }
        String correct = request.getCorrectAnswer().trim();
        List<String> distractors = cleanDistractors(request.getDistractors());
        if (distractors.stream().anyMatch(d -> d.equalsIgnoreCase(correct))) {
            return "תשובה שגויה לא יכולה להיות זהה לתשובה הנכונה";
        }
        if (distractors.size() < MIN_DISTRACTORS) {
            return "יש לכתוב לפחות " + MIN_DISTRACTORS + " תשובות שגויות שונות (מסיחים)";
        }
        if (distractors.size() > MAX_DISTRACTORS) return "אפשר לכתוב עד " + MAX_DISTRACTORS + " תשובות שגויות";
        if (distractors.stream().anyMatch(d -> d.length() > MAX_OPTION)) return "אחת התשובות השגויות ארוכה מדי";
        if (correct.length() + String.join("\n", distractors).length() + 1 > MAX_TEXT) {
            return "התשובות ארוכות מדי";
        }
        return null;
    }

    static List<String> cleanDistractors(List<String> distractors) {
        if (distractors == null) return List.of();
        List<String> out = new ArrayList<>();
        for (String d : distractors) {
            if (d == null) continue;
            String clean = d.replace("\n", " ").trim();
            if (!clean.isEmpty() && out.stream().noneMatch(o -> o.equalsIgnoreCase(clean))) out.add(clean);
        }
        return out;
    }

    private PeerQuestionEntity findReviewable(String userId, String role, Long peerQuestionId) {
        PeerQuestionEntity q = peerQuestionId == null ? null : peerQuestionRepository.findById(peerQuestionId).orElse(null);
        return q != null && support.belongsTo(userId, role, q.getClassRoomId()) ? q : null;
    }

    private Map<Long, BankQuestionEntity> bankQuestions(List<PeerQuestionEntity> questions) {
        List<Long> ids = questions.stream().map(PeerQuestionEntity::getBankQuestionId).filter(Objects::nonNull).toList();
        if (ids.isEmpty()) return Map.of();
        return bankQuestionRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(BankQuestionEntity::getId, Function.identity()));
    }

    private PeerQuestionDTO withStats(PeerQuestionEntity q, Map<Long, BankQuestionEntity> bank) {
        PeerQuestionDTO dto = mapper.apply(q);
        BankQuestionEntity b = q.getBankQuestionId() == null ? null : bank.get(q.getBankQuestionId());
        if (b != null) {
            dto.setAttempts(b.getAttempts());
            dto.setCorrectAttempts(b.getCorrectAttempts());
        }
        return dto;
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
