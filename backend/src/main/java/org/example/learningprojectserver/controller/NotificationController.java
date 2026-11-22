package org.example.learningprojectserver.controller;

import lombok.RequiredArgsConstructor;
import org.example.learningprojectserver.dto.PushSubscriptionRequest;
import org.example.learningprojectserver.response.BasicResponse;
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

    @GetMapping(CONNECT)
    public SseEmitter connect(@RequestParam String userId) {
        return notificationService.connect(userId);
    }

    @GetMapping(LIST)
    public BasicResponse list(@RequestParam String userId,
                              @RequestParam(required = false) Long afterId,
                              @RequestParam(required = false) Integer page,
                              @RequestParam(required = false) Integer size) {
        return notificationCenterService.list(userId, afterId, page, size);
    }

    @GetMapping(UNREAD_COUNT)
    public BasicResponse unreadCount(@RequestParam String userId) {
        return notificationCenterService.unreadCount(userId);
    }

    @PostMapping(MARK_READ)
    public BasicResponse markRead(@RequestParam String userId, @RequestParam Long notificationId) {
        return notificationCenterService.markRead(userId, notificationId);
    }

    @PostMapping(MARK_ALL_READ)
    public BasicResponse markAllRead(@RequestParam String userId) {
        return notificationCenterService.markAllRead(userId);
    }

    @GetMapping(PREFERENCES)
    public BasicResponse getPreferences(@RequestParam String userId) {
        return notificationCenterService.getPreferences(userId);
    }

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

    @PostMapping(PUSH_SUBSCRIBE)
    public BasicResponse subscribe(@RequestParam String userId, @RequestBody PushSubscriptionRequest request) {
        return notificationCenterService.subscribe(userId, request);
    }

    @PostMapping(PUSH_UNSUBSCRIBE)
    public BasicResponse unsubscribe(@RequestParam String userId, @RequestParam String endpoint) {
        return notificationCenterService.unsubscribe(userId, endpoint);
    }
}
