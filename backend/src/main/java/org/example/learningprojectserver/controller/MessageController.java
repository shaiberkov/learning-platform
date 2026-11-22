package org.example.learningprojectserver.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
            @NotBlank(message = "חסר מזהה השולח") @RequestParam String senderId,
            @NotBlank(message = "יש לבחור סוג נמענים") @RequestParam String recipientType,
            @RequestParam String recipientValue,
            @NotBlank(message = "יש להזין כותרת להודעה") @Size(max = 255, message = "כותרת ההודעה ארוכה מדי (עד 255 תווים)") @RequestParam String title,
            @NotBlank(message = "יש להזין תוכן להודעה") @Size(max = 255, message = "תוכן ההודעה ארוך מדי (עד 255 תווים)") @RequestParam String content
    ) {
        return messageService.sendMessage(senderId, recipientType, recipientValue, title, content);
    }

    @PreAuthorize("@authz.isSelf(#userId)")
    @GetMapping(GET_ALL_RECEIVED_MESSAGES)
    public BasicResponse getAllRecivedMessages(@RequestParam String userId) {
        return messageService.getAllRecivedMessages(userId);
    }


    }

