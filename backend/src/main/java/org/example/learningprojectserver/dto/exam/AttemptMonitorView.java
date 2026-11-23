package org.example.learningprojectserver.dto.exam;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/** A student's attempt as the teacher sees it, with the tab-leave log. */
@Data
@NoArgsConstructor
public class AttemptMonitorView {
    private Long attemptId;
    private String studentId;
    private String studentName;
    private String status;
    private String startedAt;
    private String deadline;
    private String submittedAt;
    private Integer score;
    private int tabLeaveCount;
    private List<Event> events = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Event {
        private String type;
        private String at;
    }
}
