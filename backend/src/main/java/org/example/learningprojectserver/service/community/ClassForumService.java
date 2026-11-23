package org.example.learningprojectserver.service.community;

import lombok.RequiredArgsConstructor;
import org.example.learningprojectserver.dto.community.ClassForumDTOs.PostDTO;
import org.example.learningprojectserver.dto.community.ClassForumDTOs.ReplyRequest;
import org.example.learningprojectserver.dto.community.ClassForumDTOs.ThreadDTO;
import org.example.learningprojectserver.dto.community.ClassForumDTOs.ThreadRequest;
import org.example.learningprojectserver.dto.community.ClassRefDTO;
import org.example.learningprojectserver.entities.ForumPostEntity;
import org.example.learningprojectserver.entities.ForumThreadEntity;
import org.example.learningprojectserver.mappers.ForumPostEntityToPostDTOMapper;
import org.example.learningprojectserver.mappers.ForumThreadEntityToThreadDTOMapper;
import org.example.learningprojectserver.repository.ForumPostRepository;
import org.example.learningprojectserver.repository.ForumThreadRepository;
import org.example.learningprojectserver.response.BasicResponse;
import org.example.learningprojectserver.security.ClassCommunityAccess;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import static org.example.learningprojectserver.dto.community.ClassForumDTOs.*;

/**
 * The class forum: a student asks about an exercise, classmates and the teacher answer, and the class teacher
 * marks correct answers as approved. Everything is scoped to one class: a student sees their own class, a teacher
 * the classes they teach. Before asking, a student is shown similar threads (ForumSimilarity). Not cached: the
 * screens poll and must see new answers at once.
 */
@Service
@RequiredArgsConstructor
public class ClassForumService {

    static final int SIMILAR_LIMIT = 5;

    private final ForumThreadRepository threadRepository;
    private final ForumPostRepository postRepository;
    private final ClassCommunitySupport support;
    private final ForumThreadEntityToThreadDTOMapper threadMapper;
    private final ForumPostEntityToPostDTOMapper postMapper;

    @Transactional(readOnly = true)
    public BasicResponse<List<ClassRefDTO>> getClasses(String userId, String role) {
        return ok(null, support.classesOf(userId, role));
    }

    /** The class's threads, most recently active first; with a search text, the matching ones, best first. */
    @Transactional(readOnly = true)
    public BasicResponse<List<ThreadDTO>> listThreads(String userId, String role, Long classId, String search) {
        ClassRefDTO classRef = support.resolveClass(userId, role, classId);
        if (classRef == null) return new BasicResponse<>(false, ClassCommunitySupport.NO_CLASS);
        List<ForumThreadEntity> threads = threadRepository.findTop200ByClassRoomIdOrderByLastActivityAtDesc(classRef.getId());
        if (search == null || search.isBlank()) {
            return ok(null, threads.stream().map(t -> toDto(t, userId)).toList());
        }
        String needle = search.trim().toLowerCase(Locale.ROOT);
        List<ThreadDTO> found = threads.stream()
                .map(t -> {
                    int score = scoreOf(t, search, null);
                    if (contains(t, needle)) score = Math.max(score, 100);
                    ThreadDTO dto = toDto(t, userId);
                    dto.setMatchScore(score);
                    return dto;
                })
                .filter(d -> d.getMatchScore() >= ForumSimilarity.MIN_SCORE)
                .sorted(Comparator.comparing(ThreadDTO::getMatchScore).reversed())
                .toList();
        return ok(null, found);
    }

    /** Up to five threads of the class that look like the question being typed (title, exercise, topic). */
    @Transactional(readOnly = true)
    public BasicResponse<List<ThreadDTO>> findSimilar(String userId, String role, Long classId, String text, String topic) {
        ClassRefDTO classRef = support.resolveClass(userId, role, classId);
        if (classRef == null) return new BasicResponse<>(false, ClassCommunitySupport.NO_CLASS);
        if (text == null || text.isBlank()) return ok(null, List.of());
        List<ThreadDTO> similar = threadRepository.findTop200ByClassRoomIdOrderByLastActivityAtDesc(classRef.getId())
                .stream()
                .map(t -> {
                    ThreadDTO dto = toDto(t, userId);
                    dto.setMatchScore(scoreOf(t, text, topic));
                    return dto;
                })
                .filter(d -> d.getMatchScore() >= ForumSimilarity.MIN_SCORE)
                // Best match first; among equals, a thread with an approved answer helps more.
                .sorted(Comparator.comparing(ThreadDTO::getMatchScore).reversed()
                        .thenComparing(ThreadDTO::isResolved, Comparator.reverseOrder()))
                .limit(SIMILAR_LIMIT)
                .toList();
        return ok(null, similar);
    }

    @Transactional(readOnly = true)
    public BasicResponse<ThreadDTO> getThread(String userId, String role, Long threadId) {
        ForumThreadEntity thread = findVisibleThread(userId, role, threadId);
        if (thread == null) return new BasicResponse<>(false, "הדיון לא נמצא");
        ThreadDTO dto = toDto(thread, userId);
        dto.setPosts(postRepository.findByThreadIdOrderByCreatedAtAsc(threadId).stream()
                .map(p -> toDto(p, userId))
                .toList());
        return ok(null, dto);
    }

    @Transactional
    public BasicResponse<ThreadDTO> createThread(String userId, String role, ThreadRequest request) {
        if (!ClassCommunityAccess.isStudent(role)) {
            return new BasicResponse<>(false, "רק תלמידים פותחים שאלה בפורום הכיתה");
        }
        String error = validateThread(request);
        if (error != null) return new BasicResponse<>(false, error);
        ClassRefDTO classRef = support.resolveClass(userId, role, null);
        if (classRef == null) return new BasicResponse<>(false, ClassCommunitySupport.NO_CLASS);

        LocalDateTime now = LocalDateTime.now();
        ForumThreadEntity thread = new ForumThreadEntity();
        thread.setClassRoomId(classRef.getId());
        thread.setSchoolCode(classRef.getSchoolCode());
        thread.setAuthorUserId(userId);
        thread.setAuthorName(support.nameOf(userId));
        thread.setTitle(request.getTitle().trim());
        thread.setBody(blankToNull(request.getBody()));
        thread.setTopic(blankToNull(request.getTopic()));
        thread.setExerciseText(blankToNull(request.getExerciseText()));
        thread.setCreatedAt(now);
        thread.setLastActivityAt(now);
        threadRepository.save(thread);

        support.notify(support.teachersOf(classRef.getId()), "שאלה חדשה בפורום הכיתה",
                thread.getAuthorName() + " שאל/ה: " + thread.getTitle(), thread.getAuthorName());
        return ok("השאלה פורסמה בפורום הכיתה", toDto(thread, userId));
    }

    @Transactional
    public BasicResponse<PostDTO> reply(String userId, String role, Long threadId, ReplyRequest request) {
        if (!(ClassCommunityAccess.isStudent(role) || ClassCommunityAccess.isTeacher(role))) {
            return new BasicResponse<>(false, "רק תלמידי הכיתה והמורה שלה עונים בפורום");
        }
        if (request == null || request.getBody() == null || request.getBody().isBlank()) {
            return new BasicResponse<>(false, "יש לכתוב תשובה");
        }
        if (request.getBody().length() > MAX_BODY) return new BasicResponse<>(false, "התשובה ארוכה מדי");
        ForumThreadEntity thread = findVisibleThread(userId, role, threadId);
        if (thread == null) return new BasicResponse<>(false, "הדיון לא נמצא");

        ForumPostEntity post = new ForumPostEntity();
        post.setThreadId(threadId);
        post.setAuthorUserId(userId);
        post.setAuthorName(support.nameOf(userId));
        post.setAuthorRole(role);
        post.setBody(request.getBody().trim());
        post.setCreatedAt(LocalDateTime.now());
        postRepository.save(post);

        thread.setAnswerCount((int) postRepository.countByThreadId(threadId));
        thread.setLastActivityAt(post.getCreatedAt());
        threadRepository.save(thread);

        if (!userId.equals(thread.getAuthorUserId())) {
            support.notify(List.of(thread.getAuthorUserId()), "תשובה חדשה לשאלה שלך",
                    post.getAuthorName() + " ענה/תה על: " + thread.getTitle(), post.getAuthorName());
        }
        return ok("התשובה נוספה", toDto(post, userId));
    }

    /** The class teacher marks an answer as approved (or takes the approval back). */
    @Transactional
    public BasicResponse<PostDTO> setApproved(String userId, String role, Long postId, boolean approved) {
        if (!(ClassCommunityAccess.isTeacher(role) || ClassCommunityAccess.isAdmin(role))) {
            return new BasicResponse<>(false, "רק המורה של הכיתה מאשר תשובות");
        }
        ForumPostEntity post = postRepository.findById(postId).orElse(null);
        ForumThreadEntity thread = post == null ? null : findVisibleThread(userId, role, post.getThreadId());
        if (thread == null) return new BasicResponse<>(false, "התשובה לא נמצאה");

        boolean wasApproved = post.isApproved();
        post.setApproved(approved);
        post.setApprovedByName(approved ? support.nameOf(userId) : null);
        postRepository.save(post);
        thread.setResolved(approved || postRepository.existsByThreadIdAndApprovedTrue(thread.getId()));
        threadRepository.save(thread);

        if (approved && !wasApproved && !userId.equals(post.getAuthorUserId())) {
            support.notify(List.of(post.getAuthorUserId()), "התשובה שלך אושרה ✓",
                    "המורה אישר/ה את התשובה שלך לשאלה: " + thread.getTitle(), post.getApprovedByName());
        }
        return ok(approved ? "התשובה סומנה כמאושרת" : "האישור הוסר", toDto(post, userId));
    }

    @Transactional
    public BasicResponse<Void> deletePost(String userId, String role, Long postId) {
        if (!(ClassCommunityAccess.isTeacher(role) || ClassCommunityAccess.isAdmin(role))) {
            return new BasicResponse<>(false, "רק המורה של הכיתה מוחק תשובות");
        }
        ForumPostEntity post = postRepository.findById(postId).orElse(null);
        ForumThreadEntity thread = post == null ? null : findVisibleThread(userId, role, post.getThreadId());
        if (thread == null) return new BasicResponse<>(false, "התשובה לא נמצאה");
        postRepository.delete(post);
        postRepository.flush();
        thread.setAnswerCount((int) postRepository.countByThreadId(thread.getId()));
        thread.setResolved(postRepository.existsByThreadIdAndApprovedTrue(thread.getId()));
        threadRepository.save(thread);
        return new BasicResponse<>(true, "התשובה נמחקה");
    }

    @Transactional
    public BasicResponse<Void> deleteThread(String userId, String role, Long threadId) {
        if (!(ClassCommunityAccess.isTeacher(role) || ClassCommunityAccess.isAdmin(role))) {
            return new BasicResponse<>(false, "רק המורה של הכיתה מוחק דיונים");
        }
        ForumThreadEntity thread = findVisibleThread(userId, role, threadId);
        if (thread == null) return new BasicResponse<>(false, "הדיון לא נמצא");
        postRepository.deleteByThreadId(threadId);
        threadRepository.delete(thread);
        return new BasicResponse<>(true, "הדיון נמחק");
    }

    static String validateThread(ThreadRequest request) {
        if (request == null || request.getTitle() == null || request.getTitle().isBlank()) {
            return "יש לכתוב כותרת לשאלה";
        }
        if (request.getTitle().length() > MAX_TITLE) return "הכותרת ארוכה מדי";
        if (request.getBody() != null && request.getBody().length() > MAX_BODY) return "תיאור השאלה ארוך מדי";
        if (request.getTopic() != null && request.getTopic().length() > MAX_TOPIC) return "שם הנושא ארוך מדי";
        if (request.getExerciseText() != null && request.getExerciseText().length() > MAX_EXERCISE) {
            return "נוסח התרגיל ארוך מדי";
        }
        return null;
    }

    private ForumThreadEntity findVisibleThread(String userId, String role, Long threadId) {
        if (threadId == null) return null;
        ForumThreadEntity thread = threadRepository.findById(threadId).orElse(null);
        return thread != null && support.belongsTo(userId, role, thread.getClassRoomId()) ? thread : null;
    }

    private static int scoreOf(ForumThreadEntity t, String text, String topic) {
        return ForumSimilarity.score(text, topic, t.getTitle(), t.getExerciseText(), t.getBody(), t.getTopic());
    }

    private static boolean contains(ForumThreadEntity t, String needle) {
        return lower(t.getTitle()).contains(needle) || lower(t.getExerciseText()).contains(needle)
                || lower(t.getTopic()).contains(needle);
    }

    private static String lower(String s) {
        return s == null ? "" : s.toLowerCase(Locale.ROOT);
    }

    private ThreadDTO toDto(ForumThreadEntity thread, String userId) {
        ThreadDTO dto = threadMapper.apply(thread);
        dto.setMine(userId != null && userId.equals(thread.getAuthorUserId()));
        return dto;
    }

    private PostDTO toDto(ForumPostEntity post, String userId) {
        PostDTO dto = postMapper.apply(post);
        dto.setMine(userId != null && userId.equals(post.getAuthorUserId()));
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
