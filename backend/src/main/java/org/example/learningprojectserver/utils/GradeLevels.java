package org.example.learningprojectserver.utils;

import java.util.Map;

/**
 * Turns a grade name into its level number so grades can be ordered and promoted:
 * Hebrew letters by their numeric value ("ז'" = 7, "י\"א" = 11, "יב" = 12) or plain digits ("7").
 * Returns -1 when the name is not a recognisable grade.
 */
public final class GradeLevels {

    private static final Map<Character, Integer> LETTER_VALUES = Map.of(
            'א', 1, 'ב', 2, 'ג', 3, 'ד', 4, 'ה', 5,
            'ו', 6, 'ז', 7, 'ח', 8, 'ט', 9, 'י', 10);

    private GradeLevels() {}

    public static int level(String gradeName) {
        if (gradeName == null) {
            return -1;
        }
        String clean = gradeName.replaceAll("[\\s'\"׳״\\-]", "");
        if (clean.isEmpty()) {
            return -1;
        }
        if (clean.chars().allMatch(Character::isDigit)) {
            return Integer.parseInt(clean);
        }
        int sum = 0;
        for (char ch : clean.toCharArray()) {
            Integer value = LETTER_VALUES.get(ch);
            if (value == null) {
                return -1;
            }
            sum += value;
        }
        return sum;
    }
}
