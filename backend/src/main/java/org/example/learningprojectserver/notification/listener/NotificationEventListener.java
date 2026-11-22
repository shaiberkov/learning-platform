package org.example.learningprojectserver.notification.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.learningprojectserver.dto.MessageDTO;
import org.example.learningprojectserver.dto.StudentTestStatusDTO;
import org.example.learningprojectserver.entities.NotificationEntity;
import org.example.learningprojectserver.entities.NotificationPreferenceEntity;
import org.example.learningprojectserver.entities.UserEntity;
import org.example.learningprojectserver.notification.NotificationType;
import org.example.learningprojectserver.notification.dto.HomeworkNotificationDTO;
import org.example.learningprojectserver.notification.dto.NewTestMessageDTO;
import org.example.learningprojectserver.notification.dto.SystemMessageDTO;
import org.example.learningprojectserver.notification.event.NotificationEvent;
import org.example.learningprojectserver.notification.dto.NotificationDTO;
import org.example.learningprojectserver.repository.UserRepository;
import org.example.learningprojectserver.service.NotificationCenterService;
import org.example.learningprojectserver.service.NotificationService;
import org.example.learningprojectserver.service.WebPushService;
import org.example.learningprojectserver.utils.ApiEmailProcessor;
import org.example.learningprojectserver.utils.LocalizedMessages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


/**
 * Fans a NotificationEvent out to every channel: stores it in the notification center,
 * pushes it live over SSE, and sends email / Web Push to users who turned those on.
 * Each channel is best effort: a failure in one never stops the others.
 */
@Component
public class NotificationEventListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventListener.class);

    private final NotificationService notificationService;
    private final NotificationCenterService notificationCenterService;
    private final WebPushService webPushService;
    private final UserRepository userRepository;
    private final LocalizedMessages messages;
    private final ObjectMapper objectMapper;
@Autowired
    public NotificationEventListener(NotificationService notificationService,
                                     NotificationCenterService notificationCenterService,
                                     WebPushService webPushService,
                                     UserRepository userRepository,
                                     LocalizedMessages messages,
                                     ObjectMapper objectMapper) {
        this.notificationService = notificationService;
        this.notificationCenterService = notificationCenterService;
        this.webPushService = webPushService;
        this.userRepository = userRepository;
        this.messages = messages;
        this.objectMapper = objectMapper;
    }

    @Async
    @EventListener
    public <T> void handle(NotificationEvent<T> event) {
        NotificationDTO<T> dto = event.getNotificationDTO();
        // Live quiz updates are a running screen state, not something to keep: deliver live only
        // (no notification center row, push or email).
        if (dto.getType() == NotificationType.LIVE_QUIZ_STATE || dto.getType() == NotificationType.LIVE_QUIZ_RESULTS) {
            for (String userId : event.getRecipients()) {
                notificationService.sendNotification(userId, dto);
            }
            return;
        }
        describe(dto);

        List<NotificationEntity> stored;
        try {
            stored = notificationCenterService.store(event.getRecipients(), dto.getType(),
                    dto.getTitle(), dto.getBody(), dto.getLink());
        } catch (Exception e) {
            // Storage failed: still deliver live, as before the notification center existed.
            log.warn("Storing notifications failed: {}", e.getMessage());
            for (String userId : event.getRecipients()) {
                notificationService.sendNotification(userId, dto);
            }
            return;
        }

        Map<String, NotificationPreferenceEntity> prefs =
                notificationCenterService.preferencesFor(event.getRecipients());

        for (NotificationEntity row : stored) {
            NotificationDTO<T> personal = new NotificationDTO<>(dto.getType(), dto.getPayload());
            personal.setId(row.getId());
            personal.setTitle(row.getTitle());
            personal.setBody(row.getBody());
            personal.setLink(row.getLink());
            personal.setCreatedAt(row.getCreatedAt());
            // SSE always carries the data (it updates screens such as the tests table);
            // the in-app preference decides on the client whether a pop-up is shown.
            notificationService.sendNotification(row.getUserId(), personal);

            NotificationPreferenceEntity pref = prefs.get(row.getUserId());
            if (pref != null && pref.isPush()) {
                sendPush(row);
            }
        }

        List<String> emailRecipients = prefs.values().stream()
                .filter(NotificationPreferenceEntity::isEmail)
                .map(NotificationPreferenceEntity::getUserId)
                .toList();
        if (!emailRecipients.isEmpty()) {
            sendEmails(emailRecipients, dto);
        }
    }

    private <T> void describe(NotificationDTO<T> dto) {
        if (dto.getTitle() != null) return;
        Object payload = dto.getPayload();
        if (dto.getType() == NotificationType.SYSTEM_MESSAGE && payload instanceof SystemMessageDTO sm
                && sm.getMessage() != null) {
            MessageDTO m = sm.getMessage();
            dto.setTitle(messages.get("notification.newMessage.title", m.getSenderName()) + ": " + m.getTitle());
            dto.setBody(m.getContent());
            dto.setLink("/messages");
        } else if (dto.getType() == NotificationType.NEW_TEST && payload instanceof NewTestMessageDTO nt
                && nt.getUserTestStatusDTO() != null) {
            StudentTestStatusDTO t = nt.getUserTestStatusDTO();
            dto.setTitle(messages.get("notification.newTest.title"));
            dto.setBody(messages.get("notification.newTest.body", t.getSubject(), t.getTopic(), t.getStartTime()));
            dto.setLink("/teacher-tests");
        } else if ((dto.getType() == NotificationType.NEW_HOMEWORK || dto.getType() == NotificationType.HOMEWORK_REMINDER)
                && payload instanceof HomeworkNotificationDTO hw) {
            dto.setTitle(hw.message());
            dto.setBody(hw.title() + " · " + hw.dueDate());
            dto.setLink("/homework");
        } else {
            dto.setTitle(String.valueOf(dto.getType()));
        }
    }

    private void sendPush(NotificationEntity row) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("id", row.getId());
            payload.put("title", row.getTitle());
            payload.put("body", row.getBody());
            payload.put("link", row.getLink());
            webPushService.send(row.getUserId(), objectMapper.writeValueAsString(payload));
        } catch (Exception e) {
            log.warn("Web Push failed: {}", e.getMessage());
        }
    }

    private <T> void sendEmails(List<String> userIds, NotificationDTO<T> dto) {
        try {
            String subject = messages.get("notification.email.subject", dto.getTitle());
            String body = dto.getBody() == null ? dto.getTitle() : dto.getBody();
            for (UserEntity user : userRepository.findUsersByUserIds(userIds)) {
                if (user.getEmail() != null && !user.getEmail().isBlank()) {
                    ApiEmailProcessor.sendEmail(user.getEmail(), subject, body);
                }
            }
        } catch (Exception e) {
            log.warn("Notification email failed: {}", e.getMessage());
        }
    }
}
