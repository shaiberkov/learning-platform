package org.example.learningprojectserver.service;

import lombok.RequiredArgsConstructor;
import org.example.learningprojectserver.dto.NotificationItemDTO;
import org.example.learningprojectserver.dto.NotificationPreferenceDTO;
import org.example.learningprojectserver.dto.PushSubscriptionRequest;
import org.example.learningprojectserver.entities.NotificationEntity;
import org.example.learningprojectserver.entities.NotificationPreferenceEntity;
import org.example.learningprojectserver.entities.PushSubscriptionEntity;
import org.example.learningprojectserver.notification.NotificationType;
import org.example.learningprojectserver.repository.NotificationPreferenceRepository;
import org.example.learningprojectserver.repository.NotificationRepository;
import org.example.learningprojectserver.repository.PushSubscriptionRepository;
import org.example.learningprojectserver.repository.UserRepository;
import org.example.learningprojectserver.response.BasicResponse;
import org.example.learningprojectserver.response.PageResponse;
import org.example.learningprojectserver.utils.LocalizedMessages;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The notification center: stored history per user, read state, channel preferences
 * and Web Push subscriptions. Live delivery stays in NotificationEventListener.
 */
@Service
@RequiredArgsConstructor
public class NotificationCenterService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_BODY = 2000;

    private final NotificationRepository notificationRepository;
    private final NotificationPreferenceRepository preferenceRepository;
    private final PushSubscriptionRepository pushSubscriptionRepository;
    private final UserRepository userRepository;
    private final WebPushService webPushService;
    private final LocalizedMessages messages;

    @Transactional
    public List<NotificationEntity> store(Collection<String> userIds, NotificationType type,
                                          String title, String body, String link) {
        LocalDateTime now = LocalDateTime.now();
        List<NotificationEntity> rows = new ArrayList<>();
        for (String userId : userIds) {
            NotificationEntity n = new NotificationEntity();
            n.setUserId(userId);
            n.setType(type);
            n.setTitle(truncate(title, 255));
            n.setBody(truncate(body, MAX_BODY));
            n.setLink(link);
            n.setCreatedAt(now);
            rows.add(n);
        }
        return notificationRepository.saveAll(rows);
    }

    /** Preferences for many users at once; users without a row get the defaults. */
    public Map<String, NotificationPreferenceEntity> preferencesFor(Collection<String> userIds) {
        Map<String, NotificationPreferenceEntity> stored = preferenceRepository.findByUserIdIn(userIds).stream()
                .collect(Collectors.toMap(NotificationPreferenceEntity::getUserId, Function.identity(), (a, b) -> a));
        return userIds.stream().distinct().collect(Collectors.toMap(Function.identity(),
                id -> stored.getOrDefault(id, new NotificationPreferenceEntity(id))));
    }

    /**
     * afterId set: the notifications missed since that id (used after an SSE reconnect).
     * Otherwise: one page of the history, newest first.
     */
    public BasicResponse list(String userId, Long afterId, Integer page, Integer size) {
        int pageSize = PageResponse.clampSize(size, DEFAULT_PAGE_SIZE);
        BasicResponse response = new BasicResponse(true, null);
        if (afterId != null) {
            List<NotificationItemDTO> missed = notificationRepository
                    .findByUserIdAndIdGreaterThanOrderByIdAsc(userId, afterId, PageRequest.of(0, pageSize))
                    .stream().map(this::toDto).toList();
            response.setData(missed);
            return response;
        }
        response.setData(PageResponse.of(notificationRepository.findByUserIdOrderByIdDesc(
                userId, PageRequest.of(PageResponse.clampPage(page), pageSize)), this::toDto));
        return response;
    }

    public BasicResponse unreadCount(String userId) {
        BasicResponse response = new BasicResponse(true, null);
        response.setData(notificationRepository.countByUserIdAndReadAtIsNull(userId));
        return response;
    }

    @Transactional
    public BasicResponse markRead(String userId, Long notificationId) {
        NotificationEntity n = notificationRepository.findByIdAndUserId(notificationId, userId);
        if (n == null) {
            return new BasicResponse(false, messages.get("notification.notFound"));
        }
        if (n.getReadAt() == null) {
            n.setReadAt(LocalDateTime.now());
        }
        return new BasicResponse(true, null);
    }

    @Transactional
    public BasicResponse markAllRead(String userId) {
        BasicResponse response = new BasicResponse(true, null);
        response.setData(notificationRepository.markAllRead(userId, LocalDateTime.now()));
        return response;
    }

    public BasicResponse getPreferences(String userId) {
        if (userRepository.findUserByUserId(userId) == null) {
            return new BasicResponse(false, messages.get("common.userNotFound"));
        }
        NotificationPreferenceEntity p = preferencesFor(List.of(userId)).get(userId);
        BasicResponse response = new BasicResponse(true, null);
        response.setData(new NotificationPreferenceDTO(p.isInApp(), p.isEmail(), p.isPush(), webPushService.isEnabled()));
        return response;
    }

    @Transactional
    public BasicResponse updatePreferences(String userId, boolean inApp, boolean email, boolean push) {
        if (userRepository.findUserByUserId(userId) == null) {
            return new BasicResponse(false, messages.get("common.userNotFound"));
        }
        NotificationPreferenceEntity p = preferenceRepository.findByUserId(userId);
        if (p == null) {
            p = new NotificationPreferenceEntity(userId);
        }
        p.setInApp(inApp);
        p.setEmail(email);
        p.setPush(push);
        preferenceRepository.save(p);
        BasicResponse response = new BasicResponse(true, messages.get("notification.prefs.saved"));
        response.setData(new NotificationPreferenceDTO(inApp, email, push, webPushService.isEnabled()));
        return response;
    }

    public BasicResponse pushPublicKey() {
        if (!webPushService.isEnabled()) {
            return new BasicResponse(false, messages.get("notification.push.disabled"));
        }
        BasicResponse response = new BasicResponse(true, null);
        response.setData(webPushService.getPublicKey());
        return response;
    }

    @Transactional
    public BasicResponse subscribe(String userId, PushSubscriptionRequest request) {
        if (!webPushService.isEnabled()) {
            return new BasicResponse(false, messages.get("notification.push.disabled"));
        }
        if (request == null || isBlank(request.getEndpoint()) || request.getKeys() == null
                || isBlank(request.getKeys().getP256dh()) || isBlank(request.getKeys().getAuth())
                || request.getEndpoint().length() > 700) {
            return new BasicResponse(false, messages.get("notification.push.invalid"));
        }
        if (userRepository.findUserByUserId(userId) == null) {
            return new BasicResponse(false, messages.get("common.userNotFound"));
        }
        // One row per device: a re-subscribe of the same endpoint replaces the old keys and owner.
        pushSubscriptionRepository.deleteAll(pushSubscriptionRepository.findByEndpoint(request.getEndpoint()));
        PushSubscriptionEntity sub = new PushSubscriptionEntity();
        sub.setUserId(userId);
        sub.setEndpoint(request.getEndpoint());
        sub.setP256dh(request.getKeys().getP256dh());
        sub.setAuth(request.getKeys().getAuth());
        sub.setCreatedAt(LocalDateTime.now());
        pushSubscriptionRepository.save(sub);

        NotificationPreferenceEntity p = preferenceRepository.findByUserId(userId);
        if (p == null) {
            p = new NotificationPreferenceEntity(userId);
        }
        p.setPush(true);
        preferenceRepository.save(p);
        return new BasicResponse(true, messages.get("notification.push.subscribed"));
    }

    @Transactional
    public BasicResponse unsubscribe(String userId, String endpoint) {
        pushSubscriptionRepository.findByEndpoint(endpoint).stream()
                .filter(s -> s.getUserId().equals(userId))
                .forEach(pushSubscriptionRepository::delete);
        return new BasicResponse(true, messages.get("notification.push.unsubscribed"));
    }

    private NotificationItemDTO toDto(NotificationEntity n) {
        return new NotificationItemDTO(n.getId(), n.getType(), n.getTitle(), n.getBody(), n.getLink(),
                n.getCreatedAt(), n.getReadAt() != null);
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }
}
