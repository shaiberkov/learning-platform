package org.example.learningprojectserver.dto.exam;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/** Body of the clone-exam endpoint: the new copy's schedule and audience. */
@Data
@NoArgsConstructor
public class CloneExamRequest {
    private String title;
    private String startTime;
    private Integer timeLimitMinutes;
    private String publishAt;
    private List<String> studentIds = new ArrayList<>();
    private boolean template;
    private boolean versions;
}
