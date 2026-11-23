package org.example.learningprojectserver.service.ai.insights;

import org.example.learningprojectserver.dto.ai.InsightStatsDTO;
import org.example.learningprojectserver.entities.TestAnswerRecordEntity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Pure statistics over a week of test answers, no AI involved:
 * <ul>
 *     <li>struggling: a student who answered at least {@value #MIN_ANSWERS_PER_STUDENT_TOPIC} questions
 *     of a topic with under 50% correct;</li>
 *     <li>suspicious question: at least {@value #MIN_ANSWERS_PER_QUESTION} non-blank answers, and one
 *     wrong answer chosen by at least two students and by half or more of those who answered. Either
 *     the answer key is wrong or the class shares a misconception; both are worth a look.</li>
 * </ul>
 */
final class InsightCalculator {

    static final int MIN_ANSWERS_PER_STUDENT_TOPIC = 3;
    static final int MIN_ANSWERS_PER_QUESTION = 3;
    private static final int MAX_ITEMS = 30;

    private InsightCalculator() {
    }

    static InsightStatsDTO compute(List<TestAnswerRecordEntity> records, Map<String, String> studentNames) {
        InsightStatsDTO stats = new InsightStatsDTO();
        stats.setTotalAnswers(records.size());
        stats.setStudentCount((int) records.stream().map(TestAnswerRecordEntity::getStudentUserId).distinct().count());

        Map<String, int[]> byTopic = new LinkedHashMap<>();
        Map<String, Map<String, int[]>> byStudentTopic = new LinkedHashMap<>();
        Map<Long, List<TestAnswerRecordEntity>> byQuestion = new LinkedHashMap<>();
        for (TestAnswerRecordEntity r : records) {
            String topic = topicOf(r);
            tally(byTopic.computeIfAbsent(topic, k -> new int[2]), r.isCorrect());
            tally(byStudentTopic.computeIfAbsent(r.getStudentUserId(), k -> new LinkedHashMap<>())
                    .computeIfAbsent(topic, k -> new int[2]), r.isCorrect());
            if (r.getQuestionId() != null) {
                byQuestion.computeIfAbsent(r.getQuestionId(), k -> new ArrayList<>()).add(r);
            }
        }

        byTopic.forEach((topic, c) -> stats.getTopics().add(new InsightStatsDTO.TopicStat(topic, c[1], pct(c[0], c[1]))));
        stats.getTopics().sort(Comparator.comparingInt(InsightStatsDTO.TopicStat::getAccuracyPct));

        Map<String, String> labels = new HashMap<>();
        List<InsightStatsDTO.StrugglingStudent> struggling = new ArrayList<>();
        byStudentTopic.forEach((studentId, topics) -> topics.forEach((topic, c) -> {
            if (c[1] >= MIN_ANSWERS_PER_STUDENT_TOPIC && c[0] * 2 < c[1]) {
                String label = labels.computeIfAbsent(studentId, k -> "S" + (labels.size() + 1));
                struggling.add(new InsightStatsDTO.StrugglingStudent(studentId,
                        studentNames.getOrDefault(studentId, studentId), label, topic, c[1], pct(c[0], c[1])));
            }
        }));
        struggling.sort(Comparator.comparingInt(InsightStatsDTO.StrugglingStudent::getAccuracyPct));
        stats.setStrugglingStudents(struggling.stream().limit(MAX_ITEMS).collect(Collectors.toList()));

        List<InsightStatsDTO.SuspiciousQuestion> suspicious = new ArrayList<>();
        byQuestion.forEach((questionId, answers) -> {
            List<TestAnswerRecordEntity> nonBlank = answers.stream()
                    .filter(a -> a.getGivenAnswer() != null && !a.getGivenAnswer().isBlank())
                    .toList();
            if (nonBlank.size() < MIN_ANSWERS_PER_QUESTION) {
                return;
            }
            Map<String, Integer> wrongCounts = new HashMap<>();
            Map<String, String> wrongDisplay = new HashMap<>();
            for (TestAnswerRecordEntity a : nonBlank) {
                if (!a.isCorrect()) {
                    String key = normalizeAnswer(a.getGivenAnswer());
                    wrongCounts.merge(key, 1, Integer::sum);
                    wrongDisplay.putIfAbsent(key, a.getGivenAnswer().trim());
                }
            }
            wrongCounts.entrySet().stream().max(Map.Entry.comparingByValue()).ifPresent(top -> {
                int count = top.getValue();
                if (count >= 2 && count * 2 >= nonBlank.size()) {
                    TestAnswerRecordEntity sample = answers.get(0);
                    suspicious.add(new InsightStatsDTO.SuspiciousQuestion(questionId, sample.getQuestionText(),
                            topicOf(sample), sample.getCorrectAnswer(), nonBlank.size(), wrongDisplay.get(top.getKey()),
                            count, pct(count, nonBlank.size())));
                }
            });
        });
        suspicious.sort(Comparator.comparingInt(InsightStatsDTO.SuspiciousQuestion::getCommonWrongPct).reversed());
        stats.setSuspiciousQuestions(suspicious.stream().limit(MAX_ITEMS).collect(Collectors.toList()));
        return stats;
    }

    private static void tally(int[] counter, boolean correct) {
        if (correct) {
            counter[0]++;
        }
        counter[1]++;
    }

    private static int pct(int part, int whole) {
        return whole == 0 ? 0 : (int) Math.round(part * 100.0 / whole);
    }

    private static String topicOf(TestAnswerRecordEntity r) {
        if (r.getSubTopic() != null && !r.getSubTopic().isBlank()) {
            return r.getSubTopic();
        }
        return r.getTopic() == null || r.getTopic().isBlank() ? "כללי" : r.getTopic();
    }

    private static String normalizeAnswer(String answer) {
        return answer.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
    }
}
