package org.example.learningprojectserver.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/** One parsed CSV row with the problems found in it. A row with errors is not imported. */
@Getter
@Setter
@NoArgsConstructor
public class BulkImportRowDTO {
    private int rowNumber;
    /** STUDENT, TEACHER or PARENT (null when the role cell is not recognised) */
    private String role;
    private String username;
    private String userId;
    private String email;
    private String phone;
    private String className;
    private List<String> subjects = new ArrayList<>();
    private List<String> children = new ArrayList<>();
    private List<String> errors = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();

    public boolean isValid() {
        return errors.isEmpty();
    }
}
