package org.example.learningprojectserver.utils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Builds an RFC 5545 iCalendar (.ics) document: CRLF line ends, lines folded at 75 octets,
 * text escaped. Timed events are written in UTC; all-day events as DATE values.
 */
public class IcsBuilder {

    private static final DateTimeFormatter UTC_STAMP = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final ZoneId ISRAEL = ZoneId.of("Asia/Jerusalem");

    private final StringBuilder out = new StringBuilder();
    private final String stamp = LocalDateTime.now(ZoneOffset.UTC).format(UTC_STAMP);

    public IcsBuilder(String calendarName) {
        line("BEGIN:VCALENDAR");
        line("VERSION:2.0");
        line("PRODID:-//Learning App//Calendar Feed//HE");
        line("CALSCALE:GREGORIAN");
        line("METHOD:PUBLISH");
        line("X-WR-CALNAME:" + escape(calendarName));
        line("X-WR-TIMEZONE:Asia/Jerusalem");
        line("X-PUBLISHED-TTL:PT1H");
    }

    /** A timed event; start is Israel local time. */
    public IcsBuilder event(String uid, String summary, String description, LocalDateTime startIsrael, int durationMinutes) {
        String start = startIsrael.atZone(ISRAEL).withZoneSameInstant(ZoneOffset.UTC).format(UTC_STAMP);
        String end = startIsrael.plusMinutes(Math.max(durationMinutes, 1)).atZone(ISRAEL)
                .withZoneSameInstant(ZoneOffset.UTC).format(UTC_STAMP);
        line("BEGIN:VEVENT");
        line("UID:" + uid);
        line("DTSTAMP:" + stamp);
        line("DTSTART:" + start);
        line("DTEND:" + end);
        line("SUMMARY:" + escape(summary));
        if (description != null) line("DESCRIPTION:" + escape(description));
        line("END:VEVENT");
        return this;
    }

    public IcsBuilder allDayEvent(String uid, String summary, LocalDate date) {
        line("BEGIN:VEVENT");
        line("UID:" + uid);
        line("DTSTAMP:" + stamp);
        line("DTSTART;VALUE=DATE:" + date.format(DATE));
        line("DTEND;VALUE=DATE:" + date.plusDays(1).format(DATE));
        line("SUMMARY:" + escape(summary));
        line("TRANSP:TRANSPARENT");
        line("END:VEVENT");
        return this;
    }

    public String build() {
        line("END:VCALENDAR");
        return out.toString();
    }

    static String escape(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                .replace(";", "\\;")
                .replace(",", "\\,")
                .replace("\r\n", "\\n")
                .replace("\n", "\\n")
                .replace("\r", "");
    }

    // Folds a content line so no physical line exceeds 75 octets (UTF-8), never splitting a character.
    private void line(String content) {
        int octets = 0;
        int i = 0;
        StringBuilder current = new StringBuilder();
        while (i < content.length()) {
            int cp = content.codePointAt(i);
            String ch = new String(Character.toChars(cp));
            int len = ch.getBytes(StandardCharsets.UTF_8).length;
            if (octets + len > 75) {
                out.append(current).append("\r\n ");
                current.setLength(0);
                octets = 1; // the leading space of the continuation line
            }
            current.append(ch);
            octets += len;
            i += Character.charCount(cp);
        }
        out.append(current).append("\r\n");
    }
}
