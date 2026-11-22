package org.example.learningprojectserver.service;

import lombok.RequiredArgsConstructor;
import org.example.learningprojectserver.dto.AdaptiveDTOs.*;
import org.example.learningprojectserver.entities.TopicMasteryEntity;
import org.example.learningprojectserver.repository.TopicMasteryRepository;
import org.example.learningprojectserver.response.BasicResponse;
import org.example.learningprojectserver.service.adaptive.EloRating;
import org.example.learningprojectserver.service.adaptive.SkillCatalog;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/** Elo mastery per student per sub-topic: adaptive level choice, skill map, class view. */
@Service
@RequiredArgsConstructor
public class MasteryService {

    private static final String NO_DATA = "NONE";

    private final TopicMasteryRepository topicMasteryRepository;

    /** The generator level for the student's next practice question in this sub-topic. */
    public int pickLevel(String userId, String subject, String subTopic) {
        double rating = topicMasteryRepository
                .findByStudentUserIdAndSubjectAndSubTopic(userId, subject, SkillCatalog.canonicalSubTopic(subTopic))
                .map(TopicMasteryEntity::getRating)
                .orElse(EloRating.INITIAL_RATING);
        return EloRating.pickLevel(rating);
    }

    /**
     * Moves the student's rating after one answer.
     *
     * @param level   generator level of the question; null (old rows) counts as the middle level
     * @param outcome 0..1, see {@code PracticeScoring.eloOutcome}
     */
    public MasteryUpdateDTO recordAnswer(String userId, String subject, String topic, String subTopic,
                                         Integer level, double outcome, boolean correct) {
        String canonical = SkillCatalog.canonicalSubTopic(subTopic);
        TopicMasteryEntity mastery = topicMasteryRepository
                .findByStudentUserIdAndSubjectAndSubTopic(userId, subject, canonical)
                .orElseGet(() -> {
                    TopicMasteryEntity m = new TopicMasteryEntity();
                    m.setStudentUserId(userId);
                    m.setSubject(subject);
                    m.setSubTopic(canonical);
                    m.setRating(EloRating.INITIAL_RATING);
                    return m;
                });
        mastery.setTopic(SkillCatalog.topicOf(canonical, topic));
        int effectiveLevel = level == null ? 3 : level;
        mastery.setRating(EloRating.update(mastery.getRating(), mastery.getAttempts(), effectiveLevel, outcome));
        mastery.setAttempts(mastery.getAttempts() + 1);
        if (correct) {
            mastery.setCorrectCount(mastery.getCorrectCount() + 1);
        }
        mastery.setUpdatedAt(LocalDateTime.now());
        topicMasteryRepository.save(mastery);
        return toUpdate(mastery.getRating());
    }

    private static MasteryUpdateDTO toUpdate(double rating) {
        return new MasteryUpdateDTO((int) Math.round(rating), EloRating.percent(rating), EloRating.color(rating),
                EloRating.pickLevel(rating));
    }

    /** All catalog topics with the student's mastery; never-practised sub-topics are NONE. */
    public BasicResponse getSkillMap(String userId) {
        Map<String, TopicMasteryEntity> bySubTopic = topicMasteryRepository.findByStudentUserId(userId).stream()
                .filter(m -> SkillCatalog.MATH.equals(m.getSubject()))
                .collect(Collectors.toMap(TopicMasteryEntity::getSubTopic, m -> m, (a, b) -> a));

        List<SkillTopicDTO> topics = new ArrayList<>();
        for (Map.Entry<String, List<String>> topic : SkillCatalog.mathTopics().entrySet()) {
            List<SkillNodeDTO> nodes = new ArrayList<>();
            List<Double> practised = new ArrayList<>();
            for (String subTopic : topic.getValue()) {
                TopicMasteryEntity m = bySubTopic.get(subTopic);
                if (m == null) {
                    nodes.add(new SkillNodeDTO(subTopic, null, 0, NO_DATA, 0, 0,
                            EloRating.pickLevel(EloRating.INITIAL_RATING)));
                } else {
                    practised.add(m.getRating());
                    nodes.add(new SkillNodeDTO(subTopic, (int) Math.round(m.getRating()), EloRating.percent(m.getRating()),
                            EloRating.color(m.getRating()), m.getAttempts(), m.getCorrectCount(),
                            EloRating.pickLevel(m.getRating())));
                }
            }
            Double avg = practised.isEmpty() ? null
                    : practised.stream().mapToDouble(Double::doubleValue).average().orElse(EloRating.INITIAL_RATING);
            topics.add(new SkillTopicDTO(SkillCatalog.MATH, topic.getKey(),
                    avg == null ? null : (int) Math.round(avg),
                    avg == null ? 0 : EloRating.percent(avg),
                    avg == null ? NO_DATA : EloRating.color(avg),
                    nodes));
        }
        BasicResponse response = new BasicResponse(true, null);
        response.setData(topics);
        return response;
    }

    public BasicResponse getTeacherClasses(String teacherId) {
        List<TeacherClassDTO> classes = topicMasteryRepository.findTeacherClassRooms(teacherId).stream()
                .map(r -> new TeacherClassDTO((Long) r[0], (String) r[1], (String) r[2]))
                .toList();
        BasicResponse response = new BasicResponse(true, null);
        response.setData(classes);
        return response;
    }

    /** Mastery of every student in one of the teacher's classes, per sub-topic, with the class average. */
    public BasicResponse getClassSkillMap(String teacherId, Long classRoomId) {
        Optional<Object[]> classRow = topicMasteryRepository.findTeacherClassRooms(teacherId).stream()
                .filter(r -> Objects.equals(r[0], classRoomId))
                .findFirst();
        if (classRow.isEmpty()) {
            return new BasicResponse(false, "הכיתה לא נמצאה ברשימת הכיתות שלך");
        }

        List<Object[]> students = topicMasteryRepository.findStudentsOfClassRoom(classRoomId);
        List<String> studentIds = students.stream().map(r -> (String) r[0]).toList();
        Map<String, Map<String, TopicMasteryEntity>> byStudent = new HashMap<>();
        if (!studentIds.isEmpty()) {
            for (TopicMasteryEntity m : topicMasteryRepository.findByStudentUserIdIn(studentIds)) {
                if (SkillCatalog.MATH.equals(m.getSubject())) {
                    byStudent.computeIfAbsent(m.getStudentUserId(), k -> new HashMap<>()).put(m.getSubTopic(), m);
                }
            }
        }

        List<ClassSubTopicDTO> columns = new ArrayList<>();
        for (Map.Entry<String, List<String>> topic : SkillCatalog.mathTopics().entrySet()) {
            for (String subTopic : topic.getValue()) {
                List<Double> ratings = byStudent.values().stream()
                        .map(m -> m.get(subTopic))
                        .filter(Objects::nonNull)
                        .map(TopicMasteryEntity::getRating)
                        .toList();
                if (ratings.isEmpty()) {
                    columns.add(new ClassSubTopicDTO(topic.getKey(), subTopic, null, NO_DATA, 0));
                } else {
                    double avg = ratings.stream().mapToDouble(Double::doubleValue).average().orElse(0);
                    columns.add(new ClassSubTopicDTO(topic.getKey(), subTopic, (int) Math.round(avg),
                            EloRating.color(avg), ratings.size()));
                }
            }
        }

        List<ClassStudentRowDTO> rows = new ArrayList<>();
        for (Object[] s : students) {
            String id = (String) s[0];
            Map<String, ClassSkillCellDTO> cells = new LinkedHashMap<>();
            byStudent.getOrDefault(id, Map.of()).forEach((subTopic, m) ->
                    cells.put(subTopic, new ClassSkillCellDTO((int) Math.round(m.getRating()),
                            EloRating.color(m.getRating()), m.getAttempts())));
            rows.add(new ClassStudentRowDTO(id, (String) s[1], cells));
        }

        BasicResponse response = new BasicResponse(true, null);
        response.setData(new ClassSkillMapDTO(classRoomId, (String) classRow.get()[1], columns, rows));
        return response;
    }
}
