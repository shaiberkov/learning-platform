package org.example.learningprojectserver.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class BulkImportPreviewDTO {
    private List<BulkImportRowDTO> rows = new ArrayList<>();
    private int validCount;
    private int errorCount;
    /** Problems with the file itself (missing header columns, empty file). */
    private List<String> fileErrors = new ArrayList<>();
}
