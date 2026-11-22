package org.example.learningprojectserver.controller;


import org.springframework.security.access.prepost.PreAuthorize;
import lombok.RequiredArgsConstructor;
import org.example.learningprojectserver.service.ActiveUserService;
import org.springframework.web.bind.annotation.*;

import static org.example.learningprojectserver.constants.ControllerConstants.ActiveUser.*;

@RestController
@RequestMapping(CONNECTED_USERS_BASE_PATH)
@RequiredArgsConstructor
public class ActiveUserController {

    private final ActiveUserService activeUserService;

    @PreAuthorize("@authz.isSelf(#userId)")
    @PostMapping(CONNECT_USER)
    public void userConnected(@RequestParam String userId) {
        activeUserService.connectUser(userId);

    }

    @PreAuthorize("@authz.isSelf(#userId)")
    @PostMapping(DISCONNECT_USER)
    public void userDisconnected(@RequestParam String userId) {
        activeUserService.disconnectUser(userId);
}
}
