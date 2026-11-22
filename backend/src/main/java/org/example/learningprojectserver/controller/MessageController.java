package org.example.learningprojectserver.controller;

import lombok.RequiredArgsConstructor;
import org.example.learningprojectserver.dto.AttachmentDownload;
import org.example.learningprojectserver.response.BasicResponse;
import org.example.learningprojectserver.service.MessageService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.example.learningprojectserver.constants.ControllerConstants.Message.*;

@RestController
@RequestMapping(MESSAGE_BASE_PATH)
@RequiredArgsConstructor
public class MessageController {

private final MessageService messageService;


    @GetMapping(RECIPIENT_TYPES)
    public BasicResponse getRecipientTypes(@RequestParam String userId) {
    return messageService.getRecipientTypes(userId);
    }


    // Accepts the old query-string form and multipart/form-data (needed for an attachment).
    // scheduledFor is ISO local time (2026-11-23T08:00); empty or past means send now.
    @PostMapping(SEND_MESSAGE)
    public BasicResponse sendMessage(
            @RequestParam String senderId,
            @RequestParam String recipientType,
            @RequestParam String recipientValue,
            @RequestParam String title,
            @RequestParam String content,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime scheduledFor,
            @RequestParam(required = false) MultipartFile file
    ) {
        return messageService.sendMessage(senderId, recipientType, recipientValue, title, content, scheduledFor, file);
    }

    @GetMapping(GET_ALL_RECEIVED_MESSAGES)
    public BasicResponse getAllRecivedMessages(@RequestParam String userId) {
        return messageService.getAllRecivedMessages(userId);
    }

    @GetMapping(RECEIVED_MESSAGES_PAGE)
    public BasicResponse getReceivedMessagesPage(@RequestParam String userId,
                                                 @RequestParam(required = false) Integer page,
                                                 @RequestParam(required = false) Integer size) {
        return messageService.getReceivedMessagesPage(userId, page, size);
    }

    @GetMapping(SENT_MESSAGES)
    public BasicResponse getSentMessages(@RequestParam String userId,
                                         @RequestParam(required = false) Integer page,
                                         @RequestParam(required = false) Integer size) {
        return messageService.getSentMessageSummaries(userId, page, size);
    }

    @GetMapping(READ_RECEIPTS)
    public BasicResponse getReadReceipts(@RequestParam String userId, @RequestParam String batchId) {
        return messageService.getReadReceipts(userId, batchId);
    }

    @PostMapping(MARK_READ)
    public BasicResponse markRead(@RequestParam String userId, @RequestParam Long messageId) {
        return messageService.markAsRead(userId, messageId);
    }

    @PostMapping(REPLY)
    public BasicResponse reply(@RequestParam String userId,
                               @RequestParam Long messageId,
                               @RequestParam String content,
                               @RequestParam(required = false) MultipartFile file) {
        return messageService.reply(userId, messageId, content, file);
    }

    @GetMapping(THREAD)
    public BasicResponse getThread(@RequestParam String userId, @RequestParam String threadId) {
        return messageService.getThread(userId, threadId);
    }

    @GetMapping(ATTACHMENT)
    public ResponseEntity<byte[]> getAttachment(@RequestParam String userId, @RequestParam Long attachmentId) {
        AttachmentDownload file = messageService.getAttachment(userId, attachmentId);
        if (file == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        MediaType type;
        try {
            type = MediaType.parseMediaType(file.contentType());
        } catch (Exception e) {
            type = MediaType.APPLICATION_OCTET_STREAM;
        }
        return ResponseEntity.ok()
                .contentType(type)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(file.fileName(), StandardCharsets.UTF_8).build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(file.data());
    }


    }
