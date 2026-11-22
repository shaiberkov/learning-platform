package org.example.learningprojectserver.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.apache.http.HttpResponse;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.example.learningprojectserver.entities.PushSubscriptionEntity;
import org.example.learningprojectserver.repository.PushSubscriptionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.security.Security;

/**
 * Sends Web Push messages with VAPID. Keys come from the environment
 * (VAPID_PUBLIC_KEY, VAPID_PRIVATE_KEY, optional VAPID_SUBJECT). Without them the
 * service stays disabled: nothing is sent and the client hides the push switch.
 * Generate a key pair once with `npx web-push generate-vapid-keys`.
 */
@Service
@RequiredArgsConstructor
public class WebPushService {

    private static final Logger log = LoggerFactory.getLogger(WebPushService.class);

    private final PushSubscriptionRepository pushSubscriptionRepository;
    private final Environment environment;

    private PushService pushService;
    private String publicKey;

    @PostConstruct
    void init() {
        String pub = environment.getProperty("VAPID_PUBLIC_KEY");
        String priv = environment.getProperty("VAPID_PRIVATE_KEY");
        String subject = environment.getProperty("VAPID_SUBJECT", "mailto:support@learning-app.local");
        if (pub == null || pub.isBlank() || priv == null || priv.isBlank()) {
            log.info("Web Push disabled: VAPID_PUBLIC_KEY / VAPID_PRIVATE_KEY are not set");
            return;
        }
        try {
            if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
                Security.addProvider(new BouncyCastleProvider());
            }
            pushService = new PushService(pub, priv, subject);
            publicKey = pub;
        } catch (Exception e) {
            log.warn("Web Push disabled: the VAPID keys could not be loaded ({})", e.getMessage());
            pushService = null;
        }
    }

    public boolean isEnabled() {
        return pushService != null;
    }

    public String getPublicKey() {
        return publicKey;
    }

    /** Best effort: a failed or expired subscription never fails the caller. */
    public void send(String userId, String jsonPayload) {
        if (!isEnabled()) return;
        for (PushSubscriptionEntity sub : pushSubscriptionRepository.findByUserId(userId)) {
            try {
                HttpResponse response = pushService.send(
                        new Notification(sub.getEndpoint(), sub.getP256dh(), sub.getAuth(), jsonPayload));
                int status = response.getStatusLine().getStatusCode();
                if (status == 404 || status == 410) {
                    pushSubscriptionRepository.delete(sub);
                }
            } catch (Exception e) {
                log.warn("Web Push to a device of user {} failed: {}", userId, e.getMessage());
            }
        }
    }
}
