package org.example.learningprojectserver.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** The JSON that PushSubscription.toJSON() produces in the browser. */
@Getter
@Setter
@NoArgsConstructor
public class PushSubscriptionRequest {
    private String endpoint;
    private Keys keys;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Keys {
        private String p256dh;
        private String auth;
    }
}
