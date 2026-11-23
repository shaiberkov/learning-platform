package org.example.learningprojectserver.controller;

import lombok.RequiredArgsConstructor;
import org.example.learningprojectserver.response.BasicResponse;
import org.example.learningprojectserver.security.CurrentUser;
import org.example.learningprojectserver.service.StudyRoomService;
import org.springframework.web.bind.annotation.*;

import static org.example.learningprojectserver.constants.ControllerConstants.StudyRooms.*;

/**
 * Shared study rooms. Under /student/** (STUDENT and SYSTEM_ADMIN in SecurityConfig); the acting
 * student is always the caller, and the service only serves rooms of the caller's school.
 */
@RestController
@RequestMapping(STUDY_ROOMS_BASE_PATH)
@RequiredArgsConstructor
public class StudyRoomController {

    private final StudyRoomService studyRoomService;

    @GetMapping(LIST_ROOMS)
    public BasicResponse listRooms() {
        return studyRoomService.listRooms(CurrentUser.userId());
    }

    @PostMapping(CREATE_ROOM)
    public BasicResponse createRoom(@RequestParam String name) {
        return studyRoomService.createRoom(CurrentUser.userId(), name);
    }

    @PostMapping(ROOM_HEARTBEAT)
    public BasicResponse heartbeat(@RequestParam Long roomId, @RequestParam(required = false) String task) {
        return studyRoomService.heartbeat(CurrentUser.userId(), roomId, task);
    }

    @PostMapping(LEAVE_ROOM)
    public BasicResponse leave(@RequestParam Long roomId) {
        return studyRoomService.leave(CurrentUser.userId(), roomId);
    }

    @PostMapping(ROOM_CHAT)
    public BasicResponse chat(@RequestParam Long roomId, @RequestParam String text) {
        return studyRoomService.sendChat(CurrentUser.userId(), roomId, text);
    }
}
