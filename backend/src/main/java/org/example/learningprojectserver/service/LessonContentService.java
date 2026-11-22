package org.example.learningprojectserver.service;

import lombok.RequiredArgsConstructor;
import org.example.learningprojectserver.dto.LessonContentDTO;
import org.example.learningprojectserver.dto.LessonContentRequest;
import org.example.learningprojectserver.entities.LessonContentEntity;
import org.example.learningprojectserver.entities.LessonProgressEntity;
import org.example.learningprojectserver.mappers.LessonContentEntityToDTOMapper;
import org.example.learningprojectserver.mappers.LessonContentEntityToDTOMapper.LessonWithProgress;
import org.example.learningprojectserver.repository.LessonContentRepository;
import org.example.learningprojectserver.repository.LessonProgressRepository;
import org.example.learningprojectserver.repository.TeacherRepository;
import org.example.learningprojectserver.response.BasicResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** Lesson pages: teachers write them, students of their classes read them (learn, practice, review). */
@Service
@RequiredArgsConstructor
public class LessonContentService {

    private static final Pattern YOUTUBE = Pattern.compile(
            "^(?:https?://)?(?:www\\.|m\\.)?(?:youtube(?:-nocookie)?\\.com/(?:watch\\?(?:.*&)?v=|embed/|shorts/)|youtu\\.be/)([A-Za-z0-9_-]{11}).*$");
    private static final Pattern VIMEO = Pattern.compile(
            "^(?:https?://)?(?:www\\.|player\\.)?vimeo\\.com/(?:video/)?(\\d+).*$");

    private static final int MAX_TITLE = 200;
    private static final int MAX_BODY = 20000;

    private final LessonContentRepository lessonRepository;
    private final LessonProgressRepository progressRepository;
    private final TeacherRepository teacherRepository;
    private final LessonContentEntityToDTOMapper mapper;
    private final EngagementEvents engagementEvents;

    /**
     * Turns a YouTube/Vimeo link into its embed URL. Empty input gives "" (no video);
     * anything else gives null (rejected), so only known players are ever embedded.
     */
    static String toEmbedUrl(String url) {
        if (url == null || url.isBlank()) return "";
        String u = url.trim();
        Matcher m = YOUTUBE.matcher(u);
        if (m.matches()) return "https://www.youtube-nocookie.com/embed/" + m.group(1);
        m = VIMEO.matcher(u);
        if (m.matches()) return "https://player.vimeo.com/video/" + m.group(1);
        return null;
    }

    private String validate(LessonContentRequest r) {
        if (r == null || r.title() == null || r.title().isBlank()) return "יש להזין כותרת לשיעור";
        if (r.title().length() > MAX_TITLE) return "הכותרת ארוכה מדי";
        if (r.body() != null && r.body().length() > MAX_BODY) return "תוכן השיעור ארוך מדי";
        if (r.subject() == null || r.subject().isBlank() || r.topic() == null || r.topic().isBlank()) {
            return "יש לבחור מקצוע ונושא כדי לקשר את השיעור לתרגול";
        }
        if (toEmbedUrl(r.videoUrl()) == null) return "קישור הווידאו חייב להיות מ-YouTube או מ-Vimeo";
        return null;
    }

    private void apply(LessonContentEntity l, LessonContentRequest r) {
        l.setTitle(r.title().trim());
        l.setSubject(r.subject());
        l.setTopic(r.topic());
        l.setSubTopic(r.subTopic() == null || r.subTopic().isBlank() ? null : r.subTopic());
        l.setBody(r.body());
        String embed = toEmbedUrl(r.videoUrl());
        l.setVideoEmbedUrl(embed == null || embed.isEmpty() ? null : embed);
        l.getExamples().clear();
        if (r.examples() != null) {
            r.examples().stream().filter(e -> e != null && !e.isBlank()).map(String::trim).forEach(l.getExamples()::add);
        }
        l.setUpdatedAt(LocalDateTime.now(EngagementService.ZONE));
    }

    private BasicResponse ok(String message, Object data) {
        BasicResponse<Object> response = new BasicResponse<>(true, message);
        response.setData(data);
        return response;
    }

    @Transactional
    public BasicResponse create(String teacherId, LessonContentRequest request) {
        if (teacherRepository.findByUserId(teacherId) == null) return new BasicResponse(false, "המורה לא נמצא");
        String error = validate(request);
        if (error != null) return new BasicResponse(false, error);
        LessonContentEntity l = new LessonContentEntity();
        l.setTeacherUserId(teacherId);
        l.setCreatedAt(LocalDateTime.now(EngagementService.ZONE));
        apply(l, request);
        lessonRepository.save(l);
        return ok("השיעור נשמר", mapper.apply(new LessonWithProgress(l, null)));
    }

    @Transactional
    public BasicResponse update(String teacherId, Long lessonId, LessonContentRequest request) {
        LessonContentEntity l = lessonRepository.findById(lessonId).orElse(null);
        if (l == null || !l.getTeacherUserId().equals(teacherId)) return new BasicResponse(false, "השיעור לא נמצא");
        String error = validate(request);
        if (error != null) return new BasicResponse(false, error);
        apply(l, request);
        lessonRepository.save(l);
        return ok("השיעור עודכן", mapper.apply(new LessonWithProgress(l, null)));
    }

    @Transactional
    public BasicResponse delete(String teacherId, Long lessonId) {
        LessonContentEntity l = lessonRepository.findById(lessonId).orElse(null);
        if (l == null || !l.getTeacherUserId().equals(teacherId)) return new BasicResponse(false, "השיעור לא נמצא");
        progressRepository.deleteByLessonContentId(lessonId);
        lessonRepository.delete(l);
        return new BasicResponse(true, "השיעור נמחק");
    }

    @Transactional(readOnly = true)
    public BasicResponse getTeacherLessons(String teacherId) {
        List<LessonContentDTO> list = lessonRepository.findByTeacherUserIdOrderByUpdatedAtDesc(teacherId).stream()
                .map(l -> mapper.apply(new LessonWithProgress(l, null))).toList();
        return ok(null, list);
    }

    @Transactional(readOnly = true)
    public BasicResponse getStudentLessons(String studentId) {
        Map<Long, LessonProgressEntity> progress = progressRepository.findByStudentUserId(studentId).stream()
                .collect(Collectors.toMap(LessonProgressEntity::getLessonContentId, Function.identity(), (a, b) -> a));
        List<LessonContentDTO> list = lessonRepository.findLessonsForStudent(studentId).stream()
                .map(l -> mapper.apply(new LessonWithProgress(l, progress.get(l.getId())))).toList();
        return ok(null, list);
    }

    @Transactional(readOnly = true)
    public BasicResponse getLesson(Long lessonId, String userId) {
        LessonContentEntity l = lessonRepository.findById(lessonId).orElse(null);
        if (l == null) return new BasicResponse(false, "השיעור לא נמצא");
        LessonProgressEntity p = userId == null ? null
                : progressRepository.findFirstByStudentUserIdAndLessonContentId(userId, lessonId).orElse(null);
        return ok(null, mapper.apply(new LessonWithProgress(l, p)));
    }

    /** stage: LEARNED, PRACTICED or REVIEWED. Each stage gives XP once. */
    @Transactional
    public BasicResponse updateProgress(String studentId, Long lessonId, String stage) {
        LessonContentEntity l = lessonRepository.findById(lessonId).orElse(null);
        if (l == null) return new BasicResponse(false, "השיעור לא נמצא");
        LessonProgressEntity p = progressRepository.findFirstByStudentUserIdAndLessonContentId(studentId, lessonId)
                .orElseGet(() -> {
                    LessonProgressEntity n = new LessonProgressEntity();
                    n.setStudentUserId(studentId);
                    n.setLessonContentId(lessonId);
                    return n;
                });
        boolean changed;
        switch (stage == null ? "" : stage) {
            case "LEARNED" -> { changed = !p.isLearned(); p.setLearned(true); }
            case "PRACTICED" -> { changed = !p.isPracticed(); p.setPracticed(true); }
            case "REVIEWED" -> { changed = !p.isReviewed(); p.setReviewed(true); }
            default -> { return new BasicResponse(false, "שלב לא מוכר"); }
        }
        p.setUpdatedAt(LocalDateTime.now(EngagementService.ZONE));
        progressRepository.save(p);
        if (changed) {
            engagementEvents.lessonStage(studentId, p.isLearned() && p.isPracticed() && p.isReviewed());
        }
        return ok(null, mapper.apply(new LessonWithProgress(l, p)));
    }
}
