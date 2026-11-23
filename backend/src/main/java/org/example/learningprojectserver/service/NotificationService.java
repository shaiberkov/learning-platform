package org.example.learningprojectserver.service;

import org.example.learningprojectserver.notification.dto.NotificationDTO;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class NotificationService {
    // Tells the browser how long to wait before EventSource reconnects on its own.
    private static final long RECONNECT_MILLIS = 5000;

    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter connect(String userId) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        emitters.put(userId,emitter);

        // Remove only this emitter: a newer connection of the same user may already have replaced it.
        emitter.onCompletion(() -> emitters.remove(userId, emitter));
        emitter.onTimeout(() -> emitters.remove(userId, emitter));
        emitter.onError((e) -> emitters.remove(userId, emitter));

        try {
            emitter.send(SseEmitter.event().name("connected").reconnectTime(RECONNECT_MILLIS).data("ok"));
        } catch (Exception e) {
            emitters.remove(userId, emitter);
        }
        return emitter;
    }




    public <T> void sendNotification(String userId, NotificationDTO<T> notification) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter != null) {
            try {
                SseEmitter.SseEventBuilder event = SseEmitter.event()
                        .name("message")
                        .data(notification);
                if (notification.getId() != null) {
                    event.id(String.valueOf(notification.getId()));
                }
                emitter.send(event);
            } catch (Exception e) {
                emitters.remove(userId, emitter);
            }
        }
    }

    // Keeps idle connections open through proxies (Render closes silent streams) and drops dead ones.
    @Scheduled(fixedRate = 25000)
    public void heartbeat() {
        emitters.forEach((userId, emitter) -> {
            try {
                emitter.send(SseEmitter.event().comment("ping"));
            } catch (Exception e) {
                emitters.remove(userId, emitter);
            }
        });
    }
}
