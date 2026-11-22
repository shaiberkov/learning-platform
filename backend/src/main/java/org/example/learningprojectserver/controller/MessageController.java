package org.example.learningprojectserver.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import lombok.RequiredArgsConstructor;
import org.example.learningprojectserver.response.BasicResponse;
import org.example.learningprojectserver.service.MessageService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.example.learningprojectserver.constants.ControllerConstants.Message.*;

@RestController
@RequestMapping(MESSAGE_BASE_PATH)
@RequiredArgsConstructor
public class MessageController {

private final MessageService messageService;


    @PreAuthorize("@authz.isSelf(#userId)")
    @GetMapping(RECIPIENT_TYPES)
    public BasicResponse getRecipientTypes(@RequestParam String userId) {
    return messageService.getRecipientTypes(userId);
    }


    @PreAuthorize("@authz.isSelf(#senderId) and @features.enabledForCurrentUser('MESSAGES')")
    @PostMapping(SEND_MESSAGE)
    public BasicResponse sendMessage(
            @RequestParam String senderId,
            @RequestParam String recipientType,
            @RequestParam String recipientValue,
            @RequestParam String title,
            @RequestParam String content
    ) {
        return messageService.sendMessage(senderId, recipientType, recipientValue, title, content);
    }

    @PreAuthorize("@authz.isSelf(#userId)")
    @GetMapping(GET_ALL_RECEIVED_MESSAGES)
    public BasicResponse getAllRecivedMessages(@RequestParam String userId) {
        return messageService.getAllRecivedMessages(userId);
    }


    }

