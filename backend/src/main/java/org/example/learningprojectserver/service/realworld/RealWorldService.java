package org.example.learningprojectserver.service.realworld;

import lombok.RequiredArgsConstructor;
import org.example.learningprojectserver.dto.RealWorldDTOs.RealWorldOverrideRequest;
import org.example.learningprojectserver.dto.RealWorldDTOs.RealWorldTopicDTO;
import org.example.learningprojectserver.entities.RealWorldOverrideEntity;
import org.example.learningprojectserver.enums.Role;
import org.example.learningprojectserver.mappers.RealWorldOverrideEntityToDTOMapper;
import org.example.learningprojectserver.repository.RealWorldOverrideRepository;
import org.example.learningprojectserver.response.BasicResponse;
import org.example.learningprojectserver.security.AccessGuard;
import org.example.learningprojectserver.service.adaptive.SkillCatalog;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * "Why does it matter to me?" cards: the built-in text of RealWorldCatalog, or the text a teacher
 * of the caller's school wrote for that topic. Overrides are per school, so every student of the
 * school sees the same card. Not cached: one small indexed query per request.
 */
@Service
@RequiredArgsConstructor
public class RealWorldService {

    static final int MAX_TEXT_LENGTH = 1000;
    private static final String TEACHER_ONLY = "רק מורה שמשויך לבית ספר יכול לערוך את ההסברים";
    private static final String UNKNOWN_TOPIC = "נושא לא נתמך";

    private final RealWorldOverrideRepository overrideRepository;
    private final RealWorldOverrideEntityToDTOMapper overrideMapper;
    private final AccessGuard accessGuard;

    /** Every topic's card as the caller's school sees it. */
    public BasicResponse<List<RealWorldTopicDTO>> getTopics(String userId, String role) {
        String schoolCode = accessGuard.schoolCodeOf(userId, role);
        Map<String, RealWorldOverrideEntity> overrides = schoolCode == null ? Map.of()
                : overrideRepository.findBySchoolCode(schoolCode).stream()
                        .collect(Collectors.toMap(RealWorldOverrideEntity::getTopic, Function.identity(), (a, b) -> a));
        List<RealWorldTopicDTO> topics = RealWorldCatalog.entries().keySet().stream()
                .map(topic -> overrides.containsKey(topic) ? overrideMapper.apply(overrides.get(topic)) : builtIn(topic))
                .toList();
        return ok(topics);
    }

    /** The card of one topic; a sub-topic name resolves to its topic. */
    public BasicResponse<RealWorldTopicDTO> getTopic(String userId, String role, String topicOrSubTopic) {
        String topic = RealWorldCatalog.resolveTopic(topicOrSubTopic);
        if (topic == null) {
            return new BasicResponse<>(false, "אין הסבר מהחיים לנושא הזה");
        }
        String schoolCode = accessGuard.schoolCodeOf(userId, role);
        Optional<RealWorldOverrideEntity> override = schoolCode == null ? Optional.empty()
                : overrideRepository.findBySchoolCodeAndTopic(schoolCode, topic);
        return ok(override.map(overrideMapper).orElseGet(() -> builtIn(topic)));
    }

    /** A teacher replaces the card of one topic for their school. */
    public BasicResponse<RealWorldTopicDTO> saveOverride(String teacherId, RealWorldOverrideRequest request) {
        String schoolCode = accessGuard.schoolCodeOf(teacherId, Role.TEACHER.name());
        if (schoolCode == null) {
            return new BasicResponse<>(false, TEACHER_ONLY);
        }
        String topic = request == null ? null : RealWorldCatalog.resolveTopic(request.topic());
        if (topic == null) {
            return new BasicResponse<>(false, UNKNOWN_TOPIC);
        }
        String example = request.everydayExample() == null ? "" : request.everydayExample().trim();
        String career = request.careerLink() == null ? "" : request.careerLink().trim();
        if (example.isEmpty() || career.isEmpty()) {
            return new BasicResponse<>(false, "יש למלא גם דוגמה מהחיים וגם חיבור למקצוע");
        }
        if (example.length() > MAX_TEXT_LENGTH || career.length() > MAX_TEXT_LENGTH) {
            return new BasicResponse<>(false, "הטקסט ארוך מדי (עד 1000 תווים)");
        }
        RealWorldOverrideEntity entity = overrideRepository.findBySchoolCodeAndTopic(schoolCode, topic)
                .orElseGet(RealWorldOverrideEntity::new);
        entity.setSchoolCode(schoolCode);
        entity.setTopic(topic);
        entity.setEverydayExample(example);
        entity.setCareerLink(career);
        entity.setUpdatedByTeacherId(teacherId);
        entity.setUpdatedAt(LocalDateTime.now());
        RealWorldOverrideEntity saved = overrideRepository.save(entity);
        BasicResponse<RealWorldTopicDTO> response = new BasicResponse<>(true, "ההסבר נשמר");
        response.setData(overrideMapper.apply(saved));
        return response;
    }

    /** Back to the built-in card for the teacher's school. */
    public BasicResponse<RealWorldTopicDTO> resetOverride(String teacherId, String topicName) {
        String schoolCode = accessGuard.schoolCodeOf(teacherId, Role.TEACHER.name());
        if (schoolCode == null) {
            return new BasicResponse<>(false, TEACHER_ONLY);
        }
        String topic = RealWorldCatalog.resolveTopic(topicName);
        if (topic == null) {
            return new BasicResponse<>(false, UNKNOWN_TOPIC);
        }
        overrideRepository.findBySchoolCodeAndTopic(schoolCode, topic).ifPresent(overrideRepository::delete);
        BasicResponse<RealWorldTopicDTO> response = new BasicResponse<>(true, "חזרנו להסבר המקורי");
        response.setData(builtIn(topic));
        return response;
    }

    private static RealWorldTopicDTO builtIn(String topic) {
        RealWorldCatalog.Entry entry = RealWorldCatalog.entries().get(topic);
        return new RealWorldTopicDTO(topic, SkillCatalog.mathTopics().getOrDefault(topic, List.of()),
                entry.everydayExample(), entry.careerLink(), false, null);
    }

    private static <T> BasicResponse<T> ok(T data) {
        BasicResponse<T> response = new BasicResponse<>(true, null);
        response.setData(data);
        return response;
    }
}
