package org.example.learningprojectserver.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A user created by the bulk import, with the one-time initial password to hand out. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BulkImportCreatedUserDTO {
    private int rowNumber;
    private String role;
    private String username;
    private String userId;
    private String initialPassword;
}
