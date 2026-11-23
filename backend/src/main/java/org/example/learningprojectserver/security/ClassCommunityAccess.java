package org.example.learningprojectserver.security;

import lombok.RequiredArgsConstructor;
import org.example.learningprojectserver.dto.community.ClassRefDTO;
import org.example.learningprojectserver.entities.ForumPostEntity;
import org.example.learningprojectserver.entities.ForumThreadEntity;
import org.example.learningprojectserver.entities.PeerQuestionEntity;
import org.example.learningprojectserver.enums.PeerQuestionStatus;
import org.example.learningprojectserver.enums.Role;
import org.example.learningprojectserver.repository.ClassCommunityQueryRepository;
import org.example.learningprojectserver.repository.ForumPostRepository;
import org.example.learningprojectserver.repository.ForumThreadRepository;
import org.example.learningprojectserver.repository.PeerQuestionRepository;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Ownership checks for the class forum and the student-authored questions, used from @PreAuthorize as
 * {@code @communityAuthz.<method>(...)}. A student reaches only their own class; a teacher only the classes
 * they teach, and only a teacher moderates. A system admin passes every check.
 */
@Component("communityAuthz")
@RequiredArgsConstructor
public class ClassCommunityAccess {

    private final ClassCommunityQueryRepository queryRepository;
    private final ForumThreadRepository threadRepository;
    private final ForumPostRepository postRepository;
    private final PeerQuestionRepository peerQuestionRepository;

    public static boolean isAdmin(String role) {
        return Role.SYSTEM_ADMIN.name().equals(role);
    }

    public static boolean isTeacher(String role) {
        return Role.TEACHER.name().equals(role);
    }

    public static boolean isStudent(String role) {
        return Role.STUDENT.name().equals(role);
    }

    /** The student's own class, or the classes the teacher teaches. Empty for anyone else. */
    public List<ClassRefDTO> classesOf(String userId, String role) {
        if (userId == null) return List.of();
        if (isStudent(role)) return queryRepository.findStudentClass(userId);
        if (isTeacher(role)) return queryRepository.findTeacherClasses(userId);
        return List.of();
    }

    public boolean belongsTo(String userId, String role, Long classId) {
        if (isAdmin(role)) return true;
        return classId != null && classesOf(userId, role).stream().anyMatch(c -> classId.equals(c.getId()));
    }

    /** A null class id means "my class" and is resolved by the service. */
    public boolean canAccessClass(Long classId) {
        return classId == null || belongsTo(CurrentUser.userId(), CurrentUser.role(), classId);
    }

    public boolean canAccessThread(Long threadId) {
        ForumThreadEntity thread = threadId == null ? null : threadRepository.findById(threadId).orElse(null);
        return thread != null && belongsTo(CurrentUser.userId(), CurrentUser.role(), thread.getClassRoomId());
    }

    public boolean canModerateThread(Long threadId) {
        String role = CurrentUser.role();
        return (isTeacher(role) || isAdmin(role)) && canAccessThread(threadId);
    }

    public boolean canModeratePost(Long postId) {
        ForumPostEntity post = postId == null ? null : postRepository.findById(postId).orElse(null);
        return post != null && canModerateThread(post.getThreadId());
    }

    public boolean canReviewPeerQuestion(Long peerQuestionId) {
        String role = CurrentUser.role();
        if (!(isTeacher(role) || isAdmin(role)) || peerQuestionId == null) return false;
        PeerQuestionEntity q = peerQuestionRepository.findById(peerQuestionId).orElse(null);
        return q != null && belongsTo(CurrentUser.userId(), role, q.getClassRoomId());
    }

    /** Approving puts the question into the reviewer's own bank, so only the class teacher approves. */
    public boolean canApprovePeerQuestion(Long peerQuestionId) {
        return isTeacher(CurrentUser.role()) && canReviewPeerQuestion(peerQuestionId);
    }

    public boolean canPracticePeerQuestion(Long peerQuestionId) {
        String role = CurrentUser.role();
        if (!(isStudent(role) || isAdmin(role)) || peerQuestionId == null) return false;
        PeerQuestionEntity q = peerQuestionRepository.findById(peerQuestionId).orElse(null);
        return q != null && q.getStatus() == PeerQuestionStatus.APPROVED
                && belongsTo(CurrentUser.userId(), role, q.getClassRoomId());
    }
}
