package org.example.learningprojectserver.controller;

import lombok.RequiredArgsConstructor;
import org.example.learningprojectserver.dto.StudySummaryDTOs.GenerateRequest;
import org.example.learningprojectserver.dto.StudySummaryDTOs.Options;
import org.example.learningprojectserver.dto.StudySummaryDTOs.Podcast;
import org.example.learningprojectserver.dto.StudySummaryDTOs.PodcastSummary;
import org.example.learningprojectserver.dto.StudySummaryDTOs.Sheet;
import org.example.learningprojectserver.dto.StudySummaryDTOs.SheetSummary;
import org.example.learningprojectserver.response.BasicResponse;
import org.example.learningprojectserver.security.CurrentUser;
import org.example.learningprojectserver.service.studysummary.StudySummaryService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.example.learningprojectserver.constants.ControllerConstants.StudySummary.*;

/**
 * Personal summary sheet and study podcast. Under /student/** (STUDENT, SYSTEM_ADMIN). The student
 * is always the caller from the JWT; sheet and podcast ids are looked up within the caller's own rows.
 */
@RestController
@RequestMapping(STUDY_SUMMARY_BASE_PATH)
@RequiredArgsConstructor
public class StudySummaryController {

    private final StudySummaryService service;

    @GetMapping(STUDY_OPTIONS)
    public BasicResponse<Options> options() {
        return service.options(CurrentUser.userId());
    }

    @GetMapping(STUDY_SHEETS)
    public BasicResponse<List<SheetSummary>> sheets() {
        return service.listSheets(CurrentUser.userId());
    }

    @PostMapping(STUDY_SHEETS)
    public BasicResponse<Sheet> generateSheet(@RequestBody GenerateRequest request) {
        return service.generateSheet(CurrentUser.userId(), request);
    }

    @GetMapping(STUDY_SHEET)
    public BasicResponse<Sheet> sheet(@PathVariable Long sheetId) {
        return service.getSheet(CurrentUser.userId(), sheetId);
    }

    @DeleteMapping(STUDY_SHEET)
    public BasicResponse<Void> deleteSheet(@PathVariable Long sheetId) {
        return service.deleteSheet(CurrentUser.userId(), sheetId);
    }

    @GetMapping(STUDY_PODCASTS)
    public BasicResponse<List<PodcastSummary>> podcasts() {
        return service.listPodcasts(CurrentUser.userId());
    }

    @PostMapping(STUDY_PODCASTS)
    public BasicResponse<Podcast> generatePodcast(@RequestBody GenerateRequest request) {
        return service.generatePodcast(CurrentUser.userId(), request);
    }

    @GetMapping(STUDY_PODCAST)
    public BasicResponse<Podcast> podcast(@PathVariable Long podcastId) {
        return service.getPodcast(CurrentUser.userId(), podcastId);
    }

    @DeleteMapping(STUDY_PODCAST)
    public BasicResponse<Void> deletePodcast(@PathVariable Long podcastId) {
        return service.deletePodcast(CurrentUser.userId(), podcastId);
    }

    /** Produces (once) the AI voice recording. Without an AI key answers with the Hebrew "not configured". */
    @PostMapping(STUDY_PODCAST_AUDIO)
    public BasicResponse<Podcast> createAudio(@PathVariable Long podcastId) {
        return service.createAudio(CurrentUser.userId(), podcastId);
    }

    /** The stored MP3; when there is none, a JSON BasicResponse with the Hebrew reason. */
    @GetMapping(STUDY_PODCAST_AUDIO)
    public ResponseEntity<?> audio(@PathVariable Long podcastId) {
        StudySummaryService.AudioResult result = service.audio(CurrentUser.userId(), podcastId);
        if (result.error() != null) {
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON)
                    .body(new BasicResponse<Void>(false, result.error()));
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(result.contentType()))
                .header("Cache-Control", "private, max-age=3600")
                .body(result.audio());
    }
}
