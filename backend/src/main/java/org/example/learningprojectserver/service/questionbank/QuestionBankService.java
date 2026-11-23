package org.example.learningprojectserver.service.questionbank;

import lombok.RequiredArgsConstructor;
import org.example.learningprojectserver.dto.BankQuestionDTO;
import org.example.learningprojectserver.entities.BankQuestionEntity;
import org.example.learningprojectserver.entities.TeacherEntity;
import org.example.learningprojectserver.mappers.BankQuestionEntityToBankQuestionDTOMapper;
import org.example.learningprojectserver.repository.BankQuestionRepository;
import org.example.learningprojectserver.repository.TeacherRepository;
import org.example.learningprojectserver.response.BasicResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The teachers' question bank: questions tagged by subject, topic, level and grade, private to their
 * author unless shared with the other teachers of the same school. Nothing here is cached.
 */
@Service
@RequiredArgsConstructor
public class QuestionBankService {

    static final int MAX_TEXT = 2000;
    static final int MAX_ANSWER = 500;
    static final int MAX_TAG = 100;

    private final BankQuestionRepository bankQuestionRepository;
    private final TeacherRepository teacherRepository;
    private final BankQuestionEntityToBankQuestionDTOMapper mapper;

    @Transactional
    public BasicResponse<BankQuestionDTO> addQuestion(String teacherId, BankQuestionDTO dto) {
        TeacherEntity teacher = teacherRepository.findByUserId(teacherId);
        String schoolCode = teacher == null ? null : teacherRepository.findSchoolCodeByUserId(teacherId);
        if (teacher == null || schoolCode == null) {
            return new BasicResponse<>(false, "המורה לא נמצא או אינו משויך לבית ספר");
        }
        String error = validate(dto);
        if (error != null) return new BasicResponse<>(false, error);

        BankQuestionEntity entity = new BankQuestionEntity();
        entity.setOwnerTeacherId(teacherId);
        entity.setOwnerName(teacher.getUsername());
        entity.setSchoolCode(schoolCode);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setShared(dto.isShared());
        copyContent(dto, entity);
        bankQuestionRepository.save(entity);

        BasicResponse<BankQuestionDTO> response = new BasicResponse<>(true, "השאלה נוספה למאגר");
        response.setData(toDto(entity, teacherId));
        return response;
    }

    @Transactional
    public BasicResponse<BankQuestionDTO> updateQuestion(String teacherId, Long questionId, BankQuestionDTO dto) {
        BankQuestionEntity entity = bankQuestionRepository.findById(questionId).orElse(null);
        if (entity == null || !entity.getOwnerTeacherId().equals(teacherId)) {
            return new BasicResponse<>(false, "ניתן לערוך רק שאלות שכתבת");
        }
        String error = validate(dto);
        if (error != null) return new BasicResponse<>(false, error);
        copyContent(dto, entity);
        entity.setShared(dto.isShared());
        bankQuestionRepository.save(entity);

        BasicResponse<BankQuestionDTO> response = new BasicResponse<>(true, "השאלה עודכנה");
        response.setData(toDto(entity, teacherId));
        return response;
    }

    @Transactional
    public BasicResponse<Void> deleteQuestion(String teacherId, Long questionId) {
        BankQuestionEntity entity = bankQuestionRepository.findById(questionId).orElse(null);
        if (entity == null || !entity.getOwnerTeacherId().equals(teacherId)) {
            return new BasicResponse<>(false, "ניתן למחוק רק שאלות שכתבת");
        }
        bankQuestionRepository.delete(entity);
        return new BasicResponse<>(true, "השאלה נמחקה מהמאגר");
    }

    @Transactional
    public BasicResponse<BankQuestionDTO> setShared(String teacherId, Long questionId, boolean shared) {
        BankQuestionEntity entity = bankQuestionRepository.findById(questionId).orElse(null);
        if (entity == null || !entity.getOwnerTeacherId().equals(teacherId)) {
            return new BasicResponse<>(false, "ניתן לשתף רק שאלות שכתבת");
        }
        entity.setShared(shared);
        bankQuestionRepository.save(entity);
        BasicResponse<BankQuestionDTO> response = new BasicResponse<>(true,
                shared ? "השאלה משותפת עם מורי בית הספר" : "השאלה הוסרה מהשיתוף");
        response.setData(toDto(entity, teacherId));
        return response;
    }

    /** scope "mine" = own questions only; anything else = own plus those shared in the school. */
    @Transactional(readOnly = true)
    public BasicResponse<List<BankQuestionDTO>> search(String teacherId, String scope, String text,
                                                       String subject, String topic, String level, String grade) {
        String schoolCode = teacherRepository.findSchoolCodeByUserId(teacherId);
        if (schoolCode == null) {
            return new BasicResponse<>(false, "המורה לא נמצא או אינו משויך לבית ספר");
        }
        List<BankQuestionDTO> questions = bankQuestionRepository.search(
                        teacherId, schoolCode, !"mine".equals(scope),
                        blankToNull(subject), blankToNull(topic), blankToNull(level), blankToNull(grade),
                        blankToNull(text))
                .stream()
                .map(q -> toDto(q, teacherId))
                .toList();
        BasicResponse<List<BankQuestionDTO>> response = new BasicResponse<>(true, null);
        response.setData(questions);
        return response;
    }

    /** Distinct tag values among the questions the teacher can see, for the search filters. */
    @Transactional(readOnly = true)
    public BasicResponse<Map<String, List<String>>> getTags(String teacherId) {
        String schoolCode = teacherRepository.findSchoolCodeByUserId(teacherId);
        if (schoolCode == null) {
            return new BasicResponse<>(false, "המורה לא נמצא או אינו משויך לבית ספר");
        }
        List<BankQuestionEntity> visible = bankQuestionRepository.search(
                teacherId, schoolCode, true, null, null, null, null, null);
        Map<String, List<String>> tags = new LinkedHashMap<>();
        tags.put("subjects", distinct(visible, BankQuestionEntity::getSubject));
        tags.put("topics", distinct(visible, BankQuestionEntity::getTopic));
        tags.put("levels", distinct(visible, BankQuestionEntity::getLevel));
        tags.put("grades", distinct(visible, BankQuestionEntity::getGrade));
        BasicResponse<Map<String, List<String>>> response = new BasicResponse<>(true, null);
        response.setData(tags);
        return response;
    }

    @Transactional
    public BasicResponse<Map<String, Object>> importQuestions(String teacherId, MultipartFile file, boolean shared) {
        TeacherEntity teacher = teacherRepository.findByUserId(teacherId);
        String schoolCode = teacher == null ? null : teacherRepository.findSchoolCodeByUserId(teacherId);
        if (teacher == null || schoolCode == null) {
            return new BasicResponse<>(false, "המורה לא נמצא או אינו משויך לבית ספר");
        }
        if (file == null || file.isEmpty()) {
            return new BasicResponse<>(false, "לא נבחר קובץ");
        }
        String name = Optional.ofNullable(file.getOriginalFilename()).orElse("").toLowerCase(Locale.ROOT);
        if (!(name.endsWith(".csv") || name.endsWith(".xlsx") || name.endsWith(".xls") || name.endsWith(".txt"))) {
            return new BasicResponse<>(false, "ניתן לייבא קובץ CSV או Excel בלבד");
        }

        QuestionImportParser.Result parsed;
        try {
            parsed = QuestionImportParser.parse(file.getBytes(), name);
        } catch (IOException | RuntimeException e) {
            return new BasicResponse<>(false, "לא ניתן לקרוא את הקובץ. ודאו שזהו קובץ CSV או Excel תקין");
        }

        List<String> errors = new ArrayList<>(parsed.errors());
        List<BankQuestionEntity> toSave = new ArrayList<>();
        int row = 0;
        for (BankQuestionDTO dto : parsed.questions()) {
            row++;
            String error = validate(dto);
            if (error != null) {
                errors.add("שאלה " + row + ": " + error);
                continue;
            }
            BankQuestionEntity entity = new BankQuestionEntity();
            entity.setOwnerTeacherId(teacherId);
            entity.setOwnerName(teacher.getUsername());
            entity.setSchoolCode(schoolCode);
            entity.setCreatedAt(LocalDateTime.now());
            entity.setShared(shared);
            copyContent(dto, entity);
            toSave.add(entity);
        }
        bankQuestionRepository.saveAll(toSave);

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("imported", toSave.size());
        summary.put("errors", errors);
        BasicResponse<Map<String, Object>> response = new BasicResponse<>(!toSave.isEmpty(),
                toSave.isEmpty() ? "לא יובאו שאלות" : "יובאו " + toSave.size() + " שאלות למאגר");
        response.setData(summary);
        return response;
    }

    /** A question the teacher may use (own or shared in the school), with its answer. Null if none. */
    @Transactional(readOnly = true)
    public BankQuestionDTO findUsableQuestion(String teacherId, Long questionId) {
        BankQuestionEntity entity = bankQuestionRepository.findById(questionId).orElse(null);
        if (entity == null) return null;
        boolean own = entity.getOwnerTeacherId().equals(teacherId);
        boolean sharedInSchool = entity.isShared()
                && entity.getSchoolCode().equals(teacherRepository.findSchoolCodeByUserId(teacherId));
        return (own || sharedInSchool) ? toDto(entity, teacherId) : null;
    }

    /** Called when a live-quiz question closes: one more time asked, plus its answers. */
    @Transactional
    public void recordUsage(Long questionId, int attempts, int correct) {
        bankQuestionRepository.recordUsage(questionId, attempts, correct);
    }

    public static String validate(BankQuestionDTO dto) {
        if (dto == null || dto.getQuestionText() == null || dto.getQuestionText().isBlank()) {
            return "יש להזין את נוסח השאלה";
        }
        if (dto.getAnswer() == null || dto.getAnswer().isBlank()) {
            return "יש להזין את התשובה הנכונה";
        }
        if (dto.getQuestionText().length() > MAX_TEXT) return "נוסח השאלה ארוך מדי";
        if (dto.getAnswer().length() > MAX_ANSWER) return "התשובה ארוכה מדי";
        for (String tag : Arrays.asList(dto.getSubject(), dto.getTopic(), dto.getLevel(), dto.getGrade())) {
            if (tag != null && tag.length() > MAX_TAG) return "אחת התגיות ארוכה מדי";
        }
        List<String> options = cleanOptions(dto.getOptions());
        if (!options.isEmpty()) {
            if (options.size() < 2) return "בשאלה אמריקאית יש להזין לפחות שתי אפשרויות";
            if (!options.contains(dto.getAnswer().trim())) return "התשובה הנכונה חייבת להיות אחת מהאפשרויות";
            if (String.join("\n", options).length() > MAX_TEXT) return "האפשרויות ארוכות מדי";
        }
        return null;
    }

    private static void copyContent(BankQuestionDTO dto, BankQuestionEntity entity) {
        entity.setQuestionText(dto.getQuestionText().trim());
        entity.setAnswer(dto.getAnswer().trim());
        List<String> options = cleanOptions(dto.getOptions());
        entity.setOptions(options.isEmpty() ? null : String.join("\n", options));
        entity.setSubject(blankToNull(dto.getSubject()));
        entity.setTopic(blankToNull(dto.getTopic()));
        entity.setLevel(blankToNull(dto.getLevel()));
        entity.setGrade(blankToNull(dto.getGrade()));
    }

    private static List<String> cleanOptions(List<String> options) {
        if (options == null) return List.of();
        return options.stream()
                .filter(Objects::nonNull)
                .map(o -> o.replace("\n", " ").trim())
                .filter(o -> !o.isEmpty())
                .distinct()
                .toList();
    }

    private BankQuestionDTO toDto(BankQuestionEntity entity, String teacherId) {
        BankQuestionDTO dto = mapper.apply(entity);
        dto.setMine(entity.getOwnerTeacherId().equals(teacherId));
        return dto;
    }

    private static List<String> distinct(List<BankQuestionEntity> questions, Function<BankQuestionEntity, String> tag) {
        return questions.stream().map(tag).filter(Objects::nonNull)
                .collect(Collectors.toCollection(TreeSet::new)).stream().toList();
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
