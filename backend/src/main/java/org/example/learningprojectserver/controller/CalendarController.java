package org.example.learningprojectserver.controller;

import lombok.RequiredArgsConstructor;
import org.example.learningprojectserver.response.BasicResponse;
import org.example.learningprojectserver.service.CalendarService;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;

import static org.example.learningprojectserver.constants.ControllerConstants.Calendar.*;

@RestController
@RequestMapping(CALENDAR_BASE_PATH)
@RequiredArgsConstructor
public class CalendarController {

    private static final MediaType TEXT_CALENDAR = new MediaType("text", "calendar", StandardCharsets.UTF_8);

    private final CalendarService calendarService;

    @GetMapping(TOKEN)
    public BasicResponse getFeed(@RequestParam String userId) {
        return calendarService.getOrCreateFeed(userId);
    }

    @PostMapping(REGENERATE_TOKEN)
    public BasicResponse regenerate(@RequestParam String userId) {
        return calendarService.regenerate(userId);
    }

    @GetMapping(FEED)
    public ResponseEntity<String> feed(@PathVariable String token) {
        String ics = calendarService.buildFeed(token);
        if (ics == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return ResponseEntity.ok()
                .contentType(TEXT_CALENDAR)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"learning-calendar.ics\"")
                .cacheControl(CacheControl.noCache())
                .body(ics);
    }
}
