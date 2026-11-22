package org.example.learningprojectserver.service;

import lombok.RequiredArgsConstructor;
import org.example.learningprojectserver.dto.StudentTestStatusDTO;
import org.example.learningprojectserver.dto.UpcomingEventDto;
import org.example.learningprojectserver.entities.CalendarTokenEntity;
import org.example.learningprojectserver.entities.TeacherEntity;
import org.example.learningprojectserver.entities.TeacherTestEntity;
import org.example.learningprojectserver.entities.UserEntity;
import org.example.learningprojectserver.enums.Role;
import org.example.learningprojectserver.repository.CalendarTokenRepository;
import org.example.learningprojectserver.repository.StudentRepository;
import org.example.learningprojectserver.repository.TeacherRepository;
import org.example.learningprojectserver.repository.UserRepository;
import org.example.learningprojectserver.response.BasicResponse;
import org.example.learningprojectserver.utils.HolidayUtils;
import org.example.learningprojectserver.utils.IcsBuilder;
import org.example.learningprojectserver.utils.LocalizedMessages;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;

import static org.example.learningprojectserver.constants.ControllerConstants.Calendar.CALENDAR_BASE_PATH;
import static org.example.learningprojectserver.constants.ControllerConstants.Calendar.FEED_PREFIX;

/**
 * Personal iCalendar feed: exams (a student's tests, or the tests a teacher created) plus
 * Israeli holidays. The feed URL carries a random token instead of a login, because calendar
 * apps (Google, Outlook, Apple) fetch it without credentials. Not cached: built per request.
 */
@Service
@RequiredArgsConstructor
public class CalendarService {

    private static final DateTimeFormatter TEST_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final SecureRandom RANDOM = new SecureRandom();

    private final CalendarTokenRepository calendarTokenRepository;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final LocalizedMessages messages;

    /** data: the feed path relative to the API host, e.g. /Learning-App/calendar/feed/ab12.ics */
    @Transactional
    public BasicResponse getOrCreateFeed(String userId) {
        if (userRepository.findUserByUserId(userId) == null) {
            return new BasicResponse(false, messages.get("common.userNotFound"));
        }
        CalendarTokenEntity token = calendarTokenRepository.findByUserId(userId);
        if (token == null) {
            token = new CalendarTokenEntity();
            token.setUserId(userId);
            token.setToken(newToken());
            token.setCreatedAt(LocalDateTime.now());
            calendarTokenRepository.save(token);
        }
        BasicResponse response = new BasicResponse(true, messages.get("calendar.tokenCreated"));
        response.setData(feedPath(token.getToken()));
        return response;
    }

    @Transactional
    public BasicResponse regenerate(String userId) {
        if (userRepository.findUserByUserId(userId) == null) {
            return new BasicResponse(false, messages.get("common.userNotFound"));
        }
        CalendarTokenEntity token = calendarTokenRepository.findByUserId(userId);
        if (token == null) {
            token = new CalendarTokenEntity();
            token.setUserId(userId);
        }
        token.setToken(newToken());
        token.setCreatedAt(LocalDateTime.now());
        calendarTokenRepository.save(token);
        BasicResponse response = new BasicResponse(true, messages.get("calendar.tokenRegenerated"));
        response.setData(feedPath(token.getToken()));
        return response;
    }

    /** The .ics text, or null when the token is unknown (the controller answers 404). */
    @Transactional(readOnly = true)
    public String buildFeed(String token) {
        if (token == null || token.length() > 64) return null;
        CalendarTokenEntity entity = calendarTokenRepository.findByToken(token);
        if (entity == null) return null;
        UserEntity user = userRepository.findUserByUserId(entity.getUserId());
        if (user == null) return null;

        IcsBuilder ics = new IcsBuilder(messages.get("calendar.name"));
        if (user.getRole() == Role.STUDENT) {
            for (StudentTestStatusDTO t : studentRepository.findAllTestsForStudent(user.getUserId())) {
                addExam(ics, t.getTestId(), t.getSubject(), t.getTopic(), t.getStartTime(), t.getTimeLimitMinutes());
            }
        } else if (user.getRole() == Role.TEACHER) {
            TeacherEntity teacher = teacherRepository.findTeacherWithTests(user.getUserId());
            if (teacher != null) {
                for (TeacherTestEntity t : teacher.getTests()) {
                    addExam(ics, t.getId(), t.getSubject(), t.getTopic(), t.getStartTime(), t.getTimeLimitMinutes());
                }
            }
        }

        // Holidays are cached as "in N days" from the last daily refresh.
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Jerusalem"));
        for (UpcomingEventDto h : HolidayUtils.getCachedEvents()) {
            LocalDate date = today.plusDays(h.getEventInDays());
            ics.allDayEvent("holiday-" + date + "-" + Integer.toHexString(h.getEventName().hashCode()) + "@learning-app",
                    h.getEventName(), date);
        }
        return ics.build();
    }

    private void addExam(IcsBuilder ics, Long id, String subject, String topic, String startTime, int minutes) {
        if (startTime == null) return;
        LocalDateTime start;
        try {
            start = LocalDateTime.parse(startTime, TEST_TIME);
        } catch (Exception e) {
            return; // a malformed start time skips the event rather than breaking the feed
        }
        ics.event("exam-" + id + "@learning-app",
                messages.get("calendar.exam", subject, topic), null, start, minutes);
    }

    private static String feedPath(String token) {
        return CALENDAR_BASE_PATH + FEED_PREFIX + token + ".ics";
    }

    private static String newToken() {
        byte[] bytes = new byte[24];
        RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }
}
