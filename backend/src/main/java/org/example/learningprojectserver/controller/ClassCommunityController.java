package org.example.learningprojectserver.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.learningprojectserver.dto.community.ClassForumDTOs.ReplyRequest;
import org.example.learningprojectserver.dto.community.ClassForumDTOs.ThreadRequest;
import org.example.learningprojectserver.dto.community.PeerQuestionDTOs.AnswerRequest;
import org.example.learningprojectserver.dto.community.PeerQuestionDTOs.ReviewRequest;
import org.example.learningprojectserver.dto.community.PeerQuestionDTOs.SubmitRequest;
import org.example.learningprojectserver.response.BasicResponse;
import org.example.learningprojectserver.security.CurrentUser;
import org.example.learningprojectserver.service.community.ClassForumService;
import org.example.learningprojectserver.service.community.PeerQuestionService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import static org.example.learningprojectserver.constants.ControllerConstants.ClassCommunity.*;

/**
 * Class forum and student-authored questions. The caller is always the authenticated user (CurrentUser);
 * ids in the request (class, thread, post, question) are checked by @communityAuthz (ClassCommunityAccess).
 */
@RestController
@RequestMapping(CLASS_COMMUNITY_BASE_PATH)
@RequiredArgsConstructor
public class ClassCommunityController {

    private final ClassForumService forumService;
    private final PeerQuestionService peerQuestionService;

    // ---- Forum ----

    @GetMapping(FORUM_CLASSES)
    public BasicResponse forumClasses() {
        return forumService.getClasses(CurrentUser.userId(), CurrentUser.role());
    }

    @PreAuthorize("@communityAuthz.canAccessClass(#classId)")
    @GetMapping(FORUM_THREADS)
    public BasicResponse threads(@RequestParam(required = false) Long classId,
                                 @RequestParam(required = false) String search) {
        return forumService.listThreads(CurrentUser.userId(), CurrentUser.role(), classId, search);
    }

    @PreAuthorize("@communityAuthz.canAccessClass(#classId)")
    @GetMapping(FORUM_SIMILAR)
    public BasicResponse similar(@RequestParam(required = false) Long classId,
                                 @RequestParam(required = false) String text,
                                 @RequestParam(required = false) String topic) {
        return forumService.findSimilar(CurrentUser.userId(), CurrentUser.role(), classId, text, topic);
    }

    @PreAuthorize("@communityAuthz.canAccessThread(#threadId)")
    @GetMapping(FORUM_THREAD)
    public BasicResponse thread(@RequestParam Long threadId) {
        return forumService.getThread(CurrentUser.userId(), CurrentUser.role(), threadId);
    }

    @PostMapping(FORUM_CREATE_THREAD)
    public BasicResponse createThread(@Valid @RequestBody ThreadRequest request) {
        return forumService.createThread(CurrentUser.userId(), CurrentUser.role(), request);
    }

    @PreAuthorize("@communityAuthz.canAccessThread(#threadId)")
    @PostMapping(FORUM_REPLY)
    public BasicResponse reply(@RequestParam Long threadId, @Valid @RequestBody ReplyRequest request) {
        return forumService.reply(CurrentUser.userId(), CurrentUser.role(), threadId, request);
    }

    @PreAuthorize("@communityAuthz.canModeratePost(#postId)")
    @PostMapping(FORUM_APPROVE_POST)
    public BasicResponse approvePost(@RequestParam Long postId, @RequestParam(defaultValue = "true") boolean approved) {
        return forumService.setApproved(CurrentUser.userId(), CurrentUser.role(), postId, approved);
    }

    @PreAuthorize("@communityAuthz.canModeratePost(#postId)")
    @DeleteMapping(FORUM_DELETE_POST)
    public BasicResponse deletePost(@RequestParam Long postId) {
        return forumService.deletePost(CurrentUser.userId(), CurrentUser.role(), postId);
    }

    @PreAuthorize("@communityAuthz.canModerateThread(#threadId)")
    @DeleteMapping(FORUM_DELETE_THREAD)
    public BasicResponse deleteThread(@RequestParam Long threadId) {
        return forumService.deleteThread(CurrentUser.userId(), CurrentUser.role(), threadId);
    }

    // ---- Student-authored questions ----

    @PostMapping(PEER_SUBMIT)
    public BasicResponse submitQuestion(@Valid @RequestBody SubmitRequest request) {
        return peerQuestionService.submit(CurrentUser.userId(), CurrentUser.role(), request);
    }

    @GetMapping(PEER_MINE)
    public BasicResponse myQuestions() {
        return peerQuestionService.mine(CurrentUser.userId());
    }

    @PreAuthorize("@communityAuthz.canAccessClass(#classId)")
    @GetMapping(PEER_PRACTICE)
    public BasicResponse practice(@RequestParam(required = false) Long classId) {
        return peerQuestionService.practice(CurrentUser.userId(), CurrentUser.role(), classId);
    }

    @PreAuthorize("@communityAuthz.canPracticePeerQuestion(#peerQuestionId)")
    @PostMapping(PEER_ANSWER)
    public BasicResponse answer(@RequestParam Long peerQuestionId, @Valid @RequestBody AnswerRequest request) {
        return peerQuestionService.answer(CurrentUser.userId(), CurrentUser.role(), peerQuestionId, request.getAnswer());
    }

    @PreAuthorize("@communityAuthz.canAccessClass(#classId)")
    @GetMapping(PEER_REVIEW_QUEUE)
    public BasicResponse reviewQueue(@RequestParam(required = false) Long classId,
                                     @RequestParam(required = false) String status) {
        return peerQuestionService.reviewQueue(CurrentUser.userId(), CurrentUser.role(), classId, status);
    }

    @PreAuthorize("@communityAuthz.canReviewPeerQuestion(#peerQuestionId)")
    @PostMapping(PEER_APPROVE)
    public BasicResponse approveQuestion(@RequestParam Long peerQuestionId,
                                         @Valid @RequestBody(required = false) ReviewRequest request) {
        return peerQuestionService.approve(CurrentUser.userId(), CurrentUser.role(), peerQuestionId,
                request == null ? null : request.getNote());
    }

    @PreAuthorize("@communityAuthz.canReviewPeerQuestion(#peerQuestionId)")
    @PostMapping(PEER_REJECT)
    public BasicResponse rejectQuestion(@RequestParam Long peerQuestionId,
                                        @Valid @RequestBody(required = false) ReviewRequest request) {
        return peerQuestionService.reject(CurrentUser.userId(), CurrentUser.role(), peerQuestionId,
                request == null ? null : request.getNote());
    }
}
