package org.example.learningprojectserver.service.portfolio;

import org.example.learningprojectserver.dto.PortfolioDTOs;
import org.example.learningprojectserver.entities.PortfolioProjectEntity;
import org.example.learningprojectserver.entities.PortfolioShareEntity;
import org.example.learningprojectserver.entities.StudentBadgeEntity;
import org.example.learningprojectserver.mappers.PortfolioProjectEntityToDTOMapper;
import org.example.learningprojectserver.mappers.StudentBadgeEntityToBadgeMapper;
import org.example.learningprojectserver.repository.*;
import org.example.learningprojectserver.repository.PortfolioQueryRepository.ScoreRow;
import org.example.learningprojectserver.repository.PortfolioQueryRepository.StudentHeader;
import org.example.learningprojectserver.repository.PortfolioQueryRepository.YearRecord;
import org.example.learningprojectserver.response.BasicResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PortfolioServiceTest {

    private static final String STUDENT = "s1";

    @Mock PortfolioQueryRepository queryRepository;
    @Mock PortfolioProjectRepository projectRepository;
    @Mock PortfolioShareRepository shareRepository;
    @Mock StudentBadgeRepository badgeRepository;
    @Mock StudentEngagementRepository engagementRepository;
    @Mock QuestProgressRepository questProgressRepository;
    @Mock StudentCosmeticRepository cosmeticRepository;

    private PortfolioService service;

    @BeforeEach
    void setUp() {
        service = new PortfolioService(queryRepository, projectRepository, shareRepository, badgeRepository,
                engagementRepository, questProgressRepository, cosmeticRepository,
                new StudentBadgeEntityToBadgeMapper(), new PortfolioProjectEntityToDTOMapper());
        when(queryRepository.findStudentHeader(STUDENT)).thenReturn(List.of(header("ט'", "ט2")));
        when(queryRepository.findArchivedYears(STUDENT)).thenReturn(List.of(
                year("תשפ\"ד", "ז'", 80.0), year("תשפ\"ה", "ח'", 85.5)));
        when(queryRepository.findActiveYearStart("SC1")).thenReturn(List.of(LocalDate.of(2026, 9, 1)));
        when(queryRepository.findTeacherTestScores(STUDENT)).thenReturn(List.of(
                score("10/06/2026 09:00", 50), score("05/10/2026 09:00", 90), score("12/11/2026 09:00", 95)));
        when(engagementRepository.findByStudentUserId(anyString())).thenReturn(Optional.empty());
    }

    @Test
    void yearsRunFromArchivedGradesToTheCurrentYear() {
        List<PortfolioDTOs.YearEntry> years = PortfolioService.buildYears(
                List.of(year("תשפ\"ד", "ז'", 80.0)), "ח'", "ח1",
                List.of(score("10/06/2026 09:00", 50), score("05/10/2026 09:00", 90), score("garbage", 10)),
                LocalDate.of(2026, 9, 1));

        assertEquals(2, years.size());
        assertEquals("ז'", years.get(0).gradeName());
        assertFalse(years.get(0).current());
        PortfolioDTOs.YearEntry current = years.get(1);
        assertTrue(current.current());
        assertEquals(1, current.testsCount());
        assertEquals(90.0, current.averageScore());
    }

    @Test
    void withoutAClassThereIsNoCurrentYear() {
        assertEquals(0, PortfolioService.buildYears(List.of(), null, null, List.of(), null).size());
    }

    @Test
    void portfolioGathersBadgesYearsAndShare() {
        StudentBadgeEntity badge = new StudentBadgeEntity();
        badge.setCode("STREAK_3");
        badge.setTitle("רצף");
        badge.setEarnedAt(LocalDateTime.now());
        when(badgeRepository.findByStudentUserIdOrderByEarnedAtDesc(STUDENT)).thenReturn(List.of(badge));

        BasicResponse<PortfolioDTOs.Portfolio> response = service.getMine(STUDENT);

        assertTrue(response.isSuccess());
        PortfolioDTOs.Portfolio p = response.getData();
        assertEquals(3, p.years().size());
        assertEquals(92.5, p.years().get(2).averageScore());
        assertEquals(1, p.badges().size());
        assertEquals(1, p.level());
        assertNull(p.share());
    }

    @Test
    void unknownStudentGetsAnError() {
        when(queryRepository.findStudentHeader("ghost")).thenReturn(List.of());
        assertFalse(service.getMine("ghost").isSuccess());
    }

    @Test
    void shareCreatesAnUnguessableTokenAndReplacesTheOldOne() {
        PortfolioShareEntity existing = new PortfolioShareEntity();
        existing.setStudentUserId(STUDENT);
        existing.setToken("a".repeat(64));
        when(shareRepository.findByStudentUserId(STUDENT)).thenReturn(Optional.of(existing));

        BasicResponse<PortfolioDTOs.Share> response = service.createShare(STUDENT, "  נועה <b> ");

        assertTrue(response.isSuccess());
        String token = response.getData().token();
        assertTrue(token.matches("[0-9a-f]{64}"));
        assertNotEquals("a".repeat(64), token);
        assertEquals("נועה b", response.getData().displayName());
        verify(shareRepository).save(existing);
    }

    @Test
    void displayNameDefaultsAndIsCapped() {
        assertEquals(PortfolioService.DEFAULT_DISPLAY_NAME, PortfolioService.cleanDisplayName("   "));
        assertEquals(PortfolioService.MAX_DISPLAY_NAME, PortfolioService.cleanDisplayName("x".repeat(100)).length());
    }

    @Test
    void revokeDeletesTheShare() {
        PortfolioShareEntity existing = new PortfolioShareEntity();
        when(shareRepository.findByStudentUserId(STUDENT)).thenReturn(Optional.of(existing));

        assertTrue(service.revokeShare(STUDENT).isSuccess());
        verify(shareRepository).delete(existing);
    }

    @Test
    void publicViewRejectsMalformedAndUnknownTokens() {
        assertNull(service.getPublic("../../etc"));
        assertNull(service.getPublic("short"));
        verify(shareRepository, never()).findByToken(anyString());

        when(shareRepository.findByToken("b".repeat(64))).thenReturn(Optional.empty());
        assertNull(service.getPublic("b".repeat(64)));
    }

    @Test
    void publicViewShowsTheChosenNameAndNoPrivateFields() {
        PortfolioShareEntity share = new PortfolioShareEntity();
        share.setStudentUserId(STUDENT);
        share.setToken("c".repeat(64));
        share.setDisplayName("נועה");
        share.setCreatedAt(LocalDateTime.now());
        when(shareRepository.findByToken("c".repeat(64))).thenReturn(Optional.of(share));

        PortfolioDTOs.PublicPortfolio view = service.getPublic("c".repeat(64));

        assertNotNull(view);
        assertEquals("נועה", view.displayName());
        assertEquals("ט'", view.gradeName());
        assertEquals(3, view.years().size());
        String text = view.toString();
        assertFalse(text.contains(STUDENT + ","), "the public view must not carry the user id");
        assertFalse(text.contains("ט2"), "the public view must not carry the class");
        assertFalse(text.contains("student-username"));
    }

    @Test
    void projectsAreAddedWithTheCurrentGradeAndDeletedOnlyByTheirOwner() {
        BasicResponse<PortfolioDTOs.Project> added = service.addProject(STUDENT,
                new PortfolioDTOs.ProjectRequest("  מודל של מערכת השמש ", " "));
        assertTrue(added.isSuccess());
        ArgumentCaptor<PortfolioProjectEntity> saved = ArgumentCaptor.forClass(PortfolioProjectEntity.class);
        verify(projectRepository).save(saved.capture());
        assertEquals("מודל של מערכת השמש", saved.getValue().getTitle());
        assertNull(saved.getValue().getDescription());
        assertEquals("ט'", saved.getValue().getGradeName());

        when(projectRepository.findByIdAndStudentUserId(7L, STUDENT)).thenReturn(Optional.empty());
        assertFalse(service.deleteProject(STUDENT, 7L).isSuccess());
        verify(projectRepository, never()).delete(any());
    }

    @Test
    void projectLimitIsEnforced() {
        when(projectRepository.countByStudentUserId(STUDENT)).thenReturn((long) PortfolioService.MAX_PROJECTS);
        assertFalse(service.addProject(STUDENT, new PortfolioDTOs.ProjectRequest("x", null)).isSuccess());
        verify(projectRepository, never()).save(any());
    }

    private static StudentHeader header(String grade, String className) {
        return new StudentHeader() {
            public String getUsername() { return "student-username"; }
            public String getGradeName() { return grade; }
            public String getClassName() { return className; }
            public String getSchoolName() { return "בית ספר"; }
            public String getSchoolCode() { return "SC1"; }
        };
    }

    private static YearRecord year(String name, String grade, Double avg) {
        return new YearRecord() {
            public String getYearName() { return name; }
            public LocalDate getStartDate() { return null; }
            public String getGradeName() { return grade; }
            public String getClassName() { return grade + "1"; }
            public Integer getTestsCount() { return 4; }
            public Double getAverageScore() { return avg; }
            public String getOutcome() { return "PROMOTED"; }
        };
    }

    private static ScoreRow score(String startTime, int score) {
        return new ScoreRow() {
            public String getStartTime() { return startTime; }
            public Integer getScore() { return score; }
        };
    }
}
