package org.example.learningprojectserver.service.exam;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.learningprojectserver.dto.exam.ExamQuestionInput;
import org.example.learningprojectserver.dto.exam.ExamQuestionView;
import org.example.learningprojectserver.entities.TestQuestionEntity;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ExamQuestionCodecTest {

    private final ExamQuestionCodec codec = new ExamQuestionCodec();

    private TestQuestionEntity multiSelect() {
        ExamQuestionInput in = new ExamQuestionInput();
        in.setKind("MULTI_SELECT");
        in.setText("בחרו את המספרים הזוגיים");
        in.setOptions(List.of("1", "2", "3", "4", "5", "6"));
        in.setCorrectOptions(List.of(1, 3, 5));
        TestQuestionEntity q = codec.toEntity(in, "מתמטיקה", "זוגיות");
        q.setId(11L);
        return q;
    }

    @Test
    void viewNeverContainsTheAnswerKey() throws Exception {
        TestQuestionEntity q = multiSelect();
        String json = new ObjectMapper().writeValueAsString(codec.toView(q, 42L, true));
        assertFalse(json.contains("answerKey"));
        assertFalse(json.contains("correct"));
        assertFalse(json.contains(q.getAnswerKeyJson()));
    }

    @Test
    void optionOrderIsDeterministicPerAttemptSeed() {
        TestQuestionEntity q = multiSelect();
        List<Integer> first = ids(codec.toView(q, 42L, true).getOptions());
        List<Integer> again = ids(codec.toView(q, 42L, true).getOptions());
        assertEquals(first, again);

        boolean someSeedDiffers = false;
        for (long seed = 1; seed < 20 && !someSeedDiffers; seed++) {
            someSeedDiffers = !ids(codec.toView(q, seed, true).getOptions()).equals(first);
        }
        assertTrue(someSeedDiffers);
        assertEquals(List.of(0, 1, 2, 3, 4, 5), ids(codec.toView(q, 42L, false).getOptions()));
    }

    @Test
    void orderingItemsAreNeverShownInTheCorrectOrder() {
        ExamQuestionInput in = new ExamQuestionInput();
        in.setKind("ORDERING");
        in.setText("סדרו מהקטן לגדול");
        in.setOptions(List.of("1", "2"));
        TestQuestionEntity q = codec.toEntity(in, "מתמטיקה", "סדר");
        q.setId(3L);
        for (long seed = 0; seed < 30; seed++) {
            assertEquals(List.of(1, 0), ids(codec.toView(q, seed, false).getOptions()));
        }
    }

    @Test
    void copyKeepsEverythingButIdAndTest() {
        TestQuestionEntity q = multiSelect();
        q.setPosition(4);
        TestQuestionEntity copy = codec.copy(q);
        assertNull(copy.getId());
        assertEquals(q.getKind(), copy.getKind());
        assertEquals(q.getOptionsJson(), copy.getOptionsJson());
        assertEquals(q.getAnswerKeyJson(), copy.getAnswerKeyJson());
        assertEquals(q.getPrompt(), copy.getPrompt());
        assertEquals(4, copy.getPosition());
    }

    private static List<Integer> ids(List<ExamQuestionView.Option> options) {
        return options.stream().map(ExamQuestionView.Option::getId).toList();
    }
}
