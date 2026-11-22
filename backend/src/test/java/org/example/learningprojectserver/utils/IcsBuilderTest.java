package org.example.learningprojectserver.utils;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class IcsBuilderTest {

    @Test
    void timedEventIsWrittenInUtcWithCrlf() {
        String ics = new IcsBuilder("לוח")
                .event("exam-1@learning-app", "מבחן: מתמטיקה", null, LocalDateTime.of(2026, 1, 10, 9, 0), 45)
                .build();

        assertTrue(ics.startsWith("BEGIN:VCALENDAR\r\n"));
        assertTrue(ics.endsWith("END:VCALENDAR\r\n"));
        // Israel is UTC+2 in January.
        assertTrue(ics.contains("DTSTART:20260110T070000Z\r\n"));
        assertTrue(ics.contains("DTEND:20260110T074500Z\r\n"));
        assertFalse(ics.replace("\r\n", "").contains("\n"), "only CRLF line ends");
    }

    @Test
    void allDayEventEndsTheNextDay() {
        String ics = new IcsBuilder("x").allDayEvent("h@x", "חנוכה", LocalDate.of(2026, 12, 5)).build();
        assertTrue(ics.contains("DTSTART;VALUE=DATE:20261205\r\n"));
        assertTrue(ics.contains("DTEND;VALUE=DATE:20261206\r\n"));
    }

    @Test
    void escapesSpecialCharacters() {
        assertEquals("a\\, b\\; c\\\\ d\\ne", IcsBuilder.escape("a, b; c\\ d\ne"));
    }

    @Test
    void longHebrewLinesAreFoldedWithinSeventyFiveOctets() {
        String longText = "מבחן ארוך מאוד ".repeat(20);
        String ics = new IcsBuilder("x").allDayEvent("u@x", longText, LocalDate.of(2026, 1, 1)).build();
        for (String line : ics.split("\r\n")) {
            assertTrue(line.getBytes(StandardCharsets.UTF_8).length <= 75, "line too long: " + line);
        }
        String unfolded = ics.replace("\r\n ", "");
        assertTrue(unfolded.contains("SUMMARY:" + longText));
    }
}
