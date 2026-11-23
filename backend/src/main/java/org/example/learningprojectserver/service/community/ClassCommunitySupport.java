package org.example.learningprojectserver.service.community;

import lombok.RequiredArgsConstructor;
import org.example.learningprojectserver.dto.MessageDTO;
import org.example.learningprojectserver.dto.community.ClassRefDTO;
import org.example.learningprojectserver.notification.NotificationType;
import org.example.learningprojectserver.notification.dto.NotificationDTO;
import org.example.learningprojectserver.notification.dto.SystemMessageDTO;
import org.example.learningprojectserver.notification.publisher.NotificationEventPublisher;
import org.example.learningprojectserver.repository.ClassCommunityQueryRepository;
import org.example.learningprojectserver.security.ClassCommunityAccess;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/** What the class forum and the student-authored questions share: the caller's class, names, notifications. */
@Component
@RequiredArgsConstructor
public class ClassCommunitySupport {

    static final String NO_CLASS = "לא נמצאה כיתה. תלמיד צריך להיות משויך לכיתה, ומורה צריך לבחור כיתה שהוא מלמד";

    private final ClassCommunityAccess access;
    private final ClassCommunityQueryRepository queryRepository;
    private final NotificationEventPublisher notificationEventPublisher;

    public List<ClassRefDTO> classesOf(String userId, String role) {
        return access.classesOf(userId, role);
    }

    /**
     * The class a request is about: a student's own class (any other id is refused), a class the teacher
     * teaches (the first one when none is named), or any named class for a system admin. Null when none.
     */
    public ClassRefDTO resolveClass(String userId, String role, Long classId) {
        if (ClassCommunityAccess.isAdmin(role)) {
            return classId == null ? null : new ClassRefDTO(classId, null, null);
        }
        List<ClassRefDTO> mine = access.classesOf(userId, role);
        if (classId == null) {
            return mine.isEmpty() ? null : mine.get(0);
        }
        return mine.stream().filter(c -> classId.equals(c.getId())).findFirst().orElse(null);
    }

    public boolean belongsTo(String userId, String role, Long classId) {
        return access.belongsTo(userId, role, classId);
    }

    public String nameOf(String userId) {
        List<String> names = queryRepository.findUsername(userId);
        return names.isEmpty() || names.get(0) == null ? "משתמש" : names.get(0);
    }

    public List<String> teachersOf(Long classId) {
        return queryRepository.findTeacherIdsOfClass(classId);
    }

    /** A notification-center message (also SSE / push per the recipient's preferences). Never fails the write. */
    public void notify(Collection<String> recipients, String title, String content, String senderName) {
        List<String> to = recipients == null ? List.of()
                : recipients.stream().filter(Objects::nonNull).distinct().toList();
        if (to.isEmpty()) return;
        try {
            MessageDTO message = new MessageDTO(title, content, LocalDateTime.now(), senderName);
            notificationEventPublisher.publish(to,
                    new NotificationDTO<>(NotificationType.SYSTEM_MESSAGE, new SystemMessageDTO(message)));
        } catch (RuntimeException ignored) {
            // A notification is a side effect: the post or review is already saved.
        }
    }
}
