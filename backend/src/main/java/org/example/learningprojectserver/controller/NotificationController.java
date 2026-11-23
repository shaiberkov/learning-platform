package org.example.learningprojectserver.controller;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;

import lombok.RequiredArgsConstructor;
import org.example.learningprojectserver.dto.PushSubscriptionRequest;
import org.example.learningprojectserver.response.BasicResponse;
import org.example.learningprojectserver.security.CurrentUser;
import org.example.learningprojectserver.service.NotificationCenterService;
import org.example.learningprojectserver.service.NotificationService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.example.learningprojectserver.constants.ControllerConstants.Notification.*;

@RestController
@RequestMapping(NOTIFICATION_BASE_PATH)
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final NotificationCenterService notificationCenterService;

    /**
     * Opens the caller's own SSE stream. The user comes from the JWT (access_token cookie, sent by
     * EventSource with withCredentials), never from the request, so nobody can take over another
     * user's stream.
     */
    @GetMapping(CONNECT)
    public SseEmitter connect() {
        String userId = CurrentUser.userId();
        if (userId == null) {
            throw new AccessDeniedException("יש להתחבר מחדש");
        }
        return notificationService.connect(userId);
    }

    @PreAuthorize("@authz.isSelf(#userId)")
    @GetMapping(LIST)
    public BasicResponse list(@RequestParam String userId,
                              @RequestParam(required = false) Long afterId,
                              @RequestParam(required = false) Integer page,
                              @RequestParam(required = false) Integer size) {
        return notificationCenterService.list(userId, afterId, page, size);
    }

    @PreAuthorize("@authz.isSelf(#userId)")
    @GetMapping(UNREAD_COUNT)
    public BasicResponse unreadCount(@RequestParam String userId) {
        return notificationCenterService.unreadCount(userId);
    }

    @PreAuthorize("@authz.isSelf(#userId)")
    @PostMapping(MARK_READ)
    public BasicResponse markRead(@RequestParam String userId, @RequestParam Long notificationId) {
        return notificationCenterService.markRead(userId, notificationId);
    }

    @PreAuthorize("@authz.isSelf(#userId)")
    @PostMapping(MARK_ALL_READ)
    public BasicResponse markAllRead(@RequestParam String userId) {
        return notificationCenterService.markAllRead(userId);
    }

    @PreAuthorize("@authz.isSelf(#userId)")
    @GetMapping(PREFERENCES)
    public BasicResponse getPreferences(@RequestParam String userId) {
        return notificationCenterService.getPreferences(userId);
    }

    @PreAuthorize("@authz.isSelf(#userId)")
    @PostMapping(PREFERENCES)
    public BasicResponse updatePreferences(@RequestParam String userId,
                                           @RequestParam boolean inApp,
                                           @RequestParam boolean email,
                                           @RequestParam boolean push) {
        return notificationCenterService.updatePreferences(userId, inApp, email, push);
    }

    @GetMapping(PUSH_PUBLIC_KEY)
    public BasicResponse pushPublicKey() {
        return notificationCenterService.pushPublicKey();
    }

    @PreAuthorize("@authz.isSelf(#userId)")
    @PostMapping(PUSH_SUBSCRIBE)
    public BasicResponse subscribe(@RequestParam String userId, @RequestBody PushSubscriptionRequest request) {
        return notificationCenterService.subscribe(userId, request);
    }

    @PreAuthorize("@authz.isSelf(#userId)")
    @PostMapping(PUSH_UNSUBSCRIBE)
    public BasicResponse unsubscribe(@RequestParam String userId, @RequestParam String endpoint) {
        return notificationCenterService.unsubscribe(userId, endpoint);
    }
}
