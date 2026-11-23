package org.example.learningprojectserver.service.ai;

import lombok.RequiredArgsConstructor;
import org.example.learningprojectserver.entities.UserEntity;
import org.example.learningprojectserver.repository.SchoolManagerRepository;
import org.example.learningprojectserver.repository.StudentRepository;
import org.example.learningprojectserver.repository.TeacherRepository;
import org.example.learningprojectserver.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Map;

import static org.example.learningprojectserver.constants.SecurityConstants.JwtAuthenticationFilterConstants.PRINCIPAL_USER_ID_KEY;

/**
 * Resolves the caller of an AI endpoint from the JWT principal set by {@code JwtAuthenticationFilter}.
 * AI endpoints never take a userId parameter: the token decides who is asking, which is what makes
 * the quota and the exam check meaningful.
 */
@Service
@RequiredArgsConstructor
public class AiUserResolver {

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final SchoolManagerRepository schoolManagerRepository;

    public static String userIdFromPrincipal(Object principal) {
        if (principal instanceof Map<?, ?> map && map.get(PRINCIPAL_USER_ID_KEY) instanceof String userId) {
            return userId;
        }
        throw new AiException("יש להתחבר מחדש כדי להשתמש בעוזר ה-AI");
    }

    public AiUser resolve(String userId) {
        UserEntity user = userRepository.findUserByUserId(userId);
        if (user == null) {
            throw new AiException("המשתמש לא נמצא");
        }
        String schoolCode = switch (user.getRole()) {
            case STUDENT -> studentRepository.findSchoolCodeByUserId(userId);
            case TEACHER -> teacherRepository.findSchoolCodeByUserId(userId);
            case SCHOOLMANAGER -> schoolManagerRepository.findSchoolCodeByUserId(userId);
            case SYSTEM_ADMIN, PARENT -> null;
        };
        return new AiUser(userId, user.getRole(), schoolCode);
    }
}
