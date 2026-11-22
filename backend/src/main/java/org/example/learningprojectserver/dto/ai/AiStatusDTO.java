package org.example.learningprojectserver.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** What the UI needs to show before a chat starts: is AI on, the quota, the retention policy, exam lock. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AiStatusDTO {
    private boolean available;
    private int dailyLimit;
    private int usedToday;
    private int retentionDays;
    private boolean examLocked;
}
