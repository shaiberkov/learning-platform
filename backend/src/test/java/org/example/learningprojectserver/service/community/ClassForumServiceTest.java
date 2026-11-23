package org.example.learningprojectserver.service.community;

import org.example.learningprojectserver.dto.community.ClassForumDTOs.PostDTO;
import org.example.learningprojectserver.dto.community.ClassForumDTOs.ReplyRequest;
import org.example.learningprojectserver.dto.community.ClassForumDTOs.ThreadDTO;
import org.example.learningprojectserver.dto.community.ClassForumDTOs.ThreadRequest;
import org.example.learningprojectserver.dto.community.ClassRefDTO;
import org.example.learningprojectserver.entities.ForumPostEntity;
import org.example.learningprojectserver.entities.ForumThreadEntity;
import org.example.learningprojectserver.mappers.ForumPostEntityToPostDTOMapper;
import org.example.learningprojectserver.mappers.ForumThreadEntityToThreadDTOMapper;
import org.example.learningprojectserver.repository.ForumPostRepository;
import org.example.learningprojectserver.repository.ForumThreadRepository;
import org.example.learningprojectserver.response.BasicResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClassForumServiceTest {

    @Mock ForumThreadRepository threadRepository;
    @Mock ForumPostRepository postRepository;
    @Mock ClassCommunitySupport support;
    @Spy ForumThreadEntityToThreadDTOMapper threadMapper = new ForumThreadEntityToThreadDTOMapper();
    @Spy ForumPostEntityToPostDTOMapper postMapper = new ForumPostEntityToPostDTOMapper();
    @InjectMocks ClassForumService service;

    private static final ClassRefDTO CLASS_7 = new ClassRefDTO(7L, "ז1", "S1");

    private ForumThreadEntity thread(long id, String title, String exercise, boolean resolved) {
        ForumThreadEntity t = new ForumThreadEntity();
        t.setId(id);
        t.setClassRoomId(7L);
        t.setAuthorUserId("asker");
        t.setAuthorName("דנה");
        t.setTitle(title);
        t.setExerciseText(exercise);
        t.setResolved(resolved);
        t.setCreatedAt(LocalDateTime.now());
        t.setLastActivityAt(LocalDateTime.now());
        return t;
    }

    private ForumPostEntity post(long id, long threadId, String author) {
        ForumPostEntity p = new ForumPostEntity();
        p.setId(id);
        p.setThreadId(threadId);
        p.setAuthorUserId(author);
        p.setAuthorName(author);
        p.setBody("פותרים כך");
        p.setCreatedAt(LocalDateTime.now());
        return p;
    }

    @Test
    void studentCreatesThreadInOwnClassAndTeachersAreNotified() {
        when(support.resolveClass("s1", "STUDENT", null)).thenReturn(CLASS_7);
        when(support.nameOf("s1")).thenReturn("דנה");
        when(support.teachersOf(7L)).thenReturn(List.of("t1"));
        ThreadRequest request = new ThreadRequest();
        request.setTitle("  איך פותרים משוואה ריבועית?  ");
        request.setExerciseText("x^2-5x+6=0");

        BasicResponse<ThreadDTO> response = service.createThread("s1", "STUDENT", request);

        assertTrue(response.isSuccess());
        ArgumentCaptor<ForumThreadEntity> saved = ArgumentCaptor.forClass(ForumThreadEntity.class);
        verify(threadRepository).save(saved.capture());
        assertEquals(7L, saved.getValue().getClassRoomId());
        assertEquals("איך פותרים משוואה ריבועית?", saved.getValue().getTitle());
        assertTrue(response.getData().isMine());
        verify(support).notify(eq(List.of("t1")), anyString(), contains("איך פותרים"), eq("דנה"));
    }

    @Test
    void teacherCannotOpenThread() {
        ThreadRequest request = new ThreadRequest();
        request.setTitle("שאלה");
        BasicResponse<ThreadDTO> response = service.createThread("t1", "TEACHER", request);
        assertFalse(response.isSuccess());
        verify(threadRepository, never()).save(any());
    }

    @Test
    void studentWithoutClassGetsHebrewError() {
        when(support.resolveClass("s1", "STUDENT", null)).thenReturn(null);
        ThreadRequest request = new ThreadRequest();
        request.setTitle("שאלה");
        BasicResponse<ThreadDTO> response = service.createThread("s1", "STUDENT", request);
        assertFalse(response.isSuccess());
        assertEquals(ClassCommunitySupport.NO_CLASS, response.getErrorCode());
    }

    @Test
    void similarReturnsMatchingThreadsBestFirstAndSkipsUnrelated() {
        when(support.resolveClass("s2", "STUDENT", null)).thenReturn(CLASS_7);
        when(threadRepository.findTop200ByClassRoomIdOrderByLastActivityAtDesc(7L)).thenReturn(List.of(
                thread(1, "שברים עם מכנה משותף", "1/2 + 1/3", false),
                thread(2, "פתרון משוואה ריבועית", "x^2-5x+6=0", true),
                thread(3, "משוואה ריבועית עם שורש", null, false)));

        BasicResponse<List<ThreadDTO>> response =
                service.findSimilar("s2", "STUDENT", null, "איך פותרים את המשוואה הריבועית", null);

        assertTrue(response.isSuccess());
        List<Long> ids = response.getData().stream().map(ThreadDTO::getId).toList();
        assertFalse(ids.contains(1L));
        assertEquals(2, ids.size());
        // Equal scores: the thread with an approved answer comes first.
        assertEquals(2L, ids.get(0));
    }

    @Test
    void replyFromOtherClassIsRefused() {
        ForumThreadEntity t = thread(5, "שאלה", null, false);
        when(threadRepository.findById(5L)).thenReturn(Optional.of(t));
        when(support.belongsTo("outsider", "STUDENT", 7L)).thenReturn(false);
        ReplyRequest request = new ReplyRequest();
        request.setBody("תשובה");

        BasicResponse<PostDTO> response = service.reply("outsider", "STUDENT", 5L, request);

        assertFalse(response.isSuccess());
        verify(postRepository, never()).save(any());
    }

    @Test
    void replyUpdatesCountAndNotifiesAsker() {
        ForumThreadEntity t = thread(5, "שאלה על שברים", null, false);
        when(threadRepository.findById(5L)).thenReturn(Optional.of(t));
        when(support.belongsTo("s2", "STUDENT", 7L)).thenReturn(true);
        when(support.nameOf("s2")).thenReturn("נועם");
        when(postRepository.countByThreadId(5L)).thenReturn(3L);
        ReplyRequest request = new ReplyRequest();
        request.setBody("מוצאים מכנה משותף");

        BasicResponse<PostDTO> response = service.reply("s2", "STUDENT", 5L, request);

        assertTrue(response.isSuccess());
        assertEquals(3, t.getAnswerCount());
        verify(support).notify(eq(List.of("asker")), anyString(), anyString(), eq("נועם"));
    }

    @Test
    void teacherApprovesAnswerMarksThreadResolvedAndNotifiesAuthor() {
        ForumThreadEntity t = thread(5, "שאלה", null, false);
        ForumPostEntity p = post(11, 5, "s2");
        when(postRepository.findById(11L)).thenReturn(Optional.of(p));
        when(threadRepository.findById(5L)).thenReturn(Optional.of(t));
        when(support.belongsTo("t1", "TEACHER", 7L)).thenReturn(true);
        when(support.nameOf("t1")).thenReturn("המורה רונית");

        BasicResponse<PostDTO> response = service.setApproved("t1", "TEACHER", 11L, true);

        assertTrue(response.isSuccess());
        assertTrue(p.isApproved());
        assertEquals("המורה רונית", p.getApprovedByName());
        assertTrue(t.isResolved());
        verify(support).notify(eq(List.of("s2")), contains("אושרה"), anyString(), eq("המורה רונית"));
    }

    @Test
    void removingTheLastApprovalUnresolvesTheThread() {
        ForumThreadEntity t = thread(5, "שאלה", null, true);
        ForumPostEntity p = post(11, 5, "s2");
        p.setApproved(true);
        when(postRepository.findById(11L)).thenReturn(Optional.of(p));
        when(threadRepository.findById(5L)).thenReturn(Optional.of(t));
        when(support.belongsTo("t1", "TEACHER", 7L)).thenReturn(true);
        when(postRepository.existsByThreadIdAndApprovedTrue(5L)).thenReturn(false);

        service.setApproved("t1", "TEACHER", 11L, false);

        assertFalse(p.isApproved());
        assertFalse(t.isResolved());
        verify(support, never()).notify(any(), anyString(), anyString(), any());
    }

    @Test
    void studentCannotApprove() {
        BasicResponse<PostDTO> response = service.setApproved("s1", "STUDENT", 11L, true);
        assertFalse(response.isSuccess());
        verifyNoInteractions(postRepository);
    }

    @Test
    void searchFindsThreadByExerciseText() {
        when(support.resolveClass("s1", "STUDENT", null)).thenReturn(CLASS_7);
        when(threadRepository.findTop200ByClassRoomIdOrderByLastActivityAtDesc(7L)).thenReturn(List.of(
                thread(1, "עזרה בבקשה", "1/2 + 1/3", false),
                thread(2, "משהו אחר", "x+1=2", false)));

        BasicResponse<List<ThreadDTO>> response = service.listThreads("s1", "STUDENT", null, "1/3");

        assertEquals(List.of(1L), response.getData().stream().map(ThreadDTO::getId).toList());
    }
}
