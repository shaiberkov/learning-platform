package org.example.learningprojectserver.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPreferenceDTO {
    // In-app: live pop-up alerts. The history in the notification center is always kept.
    private boolean inApp;
    private boolean email;
    private boolean push;
    // False until VAPID keys are configured on the server: the client then hides the push switch.
    private boolean pushAvailable;
}
