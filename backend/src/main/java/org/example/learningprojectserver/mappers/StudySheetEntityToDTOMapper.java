package org.example.learningprojectserver.mappers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.learningprojectserver.dto.StudySummaryDTOs.Sheet;
import org.example.learningprojectserver.dto.StudySummaryDTOs.SheetContent;
import org.example.learningprojectserver.dto.StudySummaryDTOs.SheetSummary;
import org.example.learningprojectserver.entities.StudySheetEntity;
import org.springframework.stereotype.Component;

import java.util.List;

/** Study sheet row → API shape; the stored JSON content becomes {@link SheetContent}. */
@Component
public class StudySheetEntityToDTOMapper implements Mapper<StudySheetEntity, Sheet> {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Override
    public Sheet apply(StudySheetEntity s) {
        return new Sheet(s.getId(), s.getTitle(), s.getTopics(), s.getTestId(), s.isAiGenerated(), s.getCreatedAt(),
                readContent(s.getContent()));
    }

    public SheetSummary toSummary(StudySheetEntity s) {
        return new SheetSummary(s.getId(), s.getTitle(), s.getTopics(), s.isAiGenerated(), s.getCreatedAt());
    }

    public static String writeContent(SheetContent content) {
        try {
            return JSON.writeValueAsString(content);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    static SheetContent readContent(String json) {
        try {
            return JSON.readValue(json, SheetContent.class);
        } catch (JsonProcessingException | IllegalArgumentException e) {
            return new SheetContent("", List.of(), List.of(), List.of(), List.of());
        }
    }
}
