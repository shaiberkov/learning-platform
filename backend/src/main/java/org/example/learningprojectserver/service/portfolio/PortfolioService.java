package org.example.learningprojectserver.service.portfolio;

import lombok.RequiredArgsConstructor;
import org.example.learningprojectserver.dto.EngagementSummaryDTO;
import org.example.learningprojectserver.dto.PortfolioDTOs.Portfolio;
import org.example.learningprojectserver.dto.PortfolioDTOs.Project;
import org.example.learningprojectserver.dto.PortfolioDTOs.ProjectRequest;
import org.example.learningprojectserver.dto.PortfolioDTOs.PublicBadge;
import org.example.learningprojectserver.dto.PortfolioDTOs.PublicPortfolio;
import org.example.learningprojectserver.dto.PortfolioDTOs.PublicProject;
import org.example.learningprojectserver.dto.PortfolioDTOs.PublicYear;
import org.example.learningprojectserver.dto.PortfolioDTOs.QuestTrophy;
import org.example.learningprojectserver.dto.PortfolioDTOs.Share;
import org.example.learningprojectserver.dto.PortfolioDTOs.YearEntry;
import org.example.learningprojectserver.entities.PortfolioProjectEntity;
import org.example.learningprojectserver.entities.PortfolioShareEntity;
import org.example.learningprojectserver.entities.QuestProgressEntity;
import org.example.learningprojectserver.entities.StudentCosmeticEntity;
import org.example.learningprojectserver.entities.StudentEngagementEntity;
import org.example.learningprojectserver.mappers.PortfolioProjectEntityToDTOMapper;
import org.example.learningprojectserver.mappers.StudentBadgeEntityToBadgeMapper;
import org.example.learningprojectserver.repository.PortfolioProjectRepository;
import org.example.learningprojectserver.repository.PortfolioQueryRepository;
import org.example.learningprojectserver.repository.PortfolioQueryRepository.ScoreRow;
import org.example.learningprojectserver.repository.PortfolioQueryRepository.StudentHeader;
import org.example.learningprojectserver.repository.PortfolioQueryRepository.YearRecord;
import org.example.learningprojectserver.repository.PortfolioShareRepository;
import org.example.learningprojectserver.repository.QuestProgressRepository;
import org.example.learningprojectserver.repository.StudentBadgeRepository;
import org.example.learningprojectserver.repository.StudentCosmeticRepository;
import org.example.learningprojectserver.repository.StudentEngagementRepository;
import org.example.learningprojectserver.response.BasicResponse;
import org.example.learningprojectserver.service.EngagementService;
import org.example.learningprojectserver.service.quest.QuestCatalog;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.regex.Pattern;

/**
 * A student's portfolio: badges, projects, finished quests and the grade history across school years
 * (archived year snapshots plus the year in progress). Sharing creates a random 64-hex-char token for a
 * read-only public page; sharing again replaces it and revoking deletes it. Not cached: built per request.
 */
@Service
@RequiredArgsConstructor
public class PortfolioService {

    static final int MAX_PROJECTS = 50;
    static final int MAX_DISPLAY_NAME = 60;
    static final String DEFAULT_DISPLAY_NAME = "תלמיד/ה";
    static final String STUDENT_NOT_FOUND = "לא נמצא תלמיד";
    static final String CURRENT_YEAR = "השנה הנוכחית";

    private static final Pattern TOKEN_FORMAT = Pattern.compile("^[0-9a-f]{64}$");
    private static final DateTimeFormatter TEST_START_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final SecureRandom RANDOM = new SecureRandom();

    private final PortfolioQueryRepository queryRepository;
    private final PortfolioProjectRepository projectRepository;
    private final PortfolioShareRepository shareRepository;
    private final StudentBadgeRepository badgeRepository;
    private final StudentEngagementRepository engagementRepository;
    private final QuestProgressRepository questProgressRepository;
    private final StudentCosmeticRepository cosmeticRepository;
    private final StudentBadgeEntityToBadgeMapper badgeMapper;
    private final PortfolioProjectEntityToDTOMapper projectMapper;

    @Transactional(readOnly = true)
    public BasicResponse<Portfolio> getMine(String studentId) {
        StudentHeader header = header(studentId);
        if (header == null) {
            return new BasicResponse<>(false, STUDENT_NOT_FOUND);
        }
        StudentEngagementEntity engagement = engagementRepository.findByStudentUserId(studentId).orElse(null);
        int xp = engagement == null ? 0 : engagement.getTotalXp();
        QuestCatalog.Item avatar = equippedAvatar(studentId);
        Share share = shareRepository.findByStudentUserId(studentId)
                .map(s -> new Share(s.getToken(), s.getDisplayName(), s.getCreatedAt()))
                .orElse(null);

        Portfolio portfolio = new Portfolio(header.getUsername(), header.getSchoolName(), header.getGradeName(),
                header.getClassName(), xp, level(xp), engagement == null ? 0 : engagement.getLongestStreak(),
                avatar.emoji(), avatar.name(), badges(studentId), years(studentId, header),
                projectRepository.findByStudentUserIdOrderByCreatedAtDesc(studentId).stream().map(projectMapper).toList(),
                trophies(studentId), share);
        return ok("", portfolio);
    }

    @Transactional
    public BasicResponse<Project> addProject(String studentId, ProjectRequest request) {
        StudentHeader header = header(studentId);
        if (header == null) {
            return new BasicResponse<>(false, STUDENT_NOT_FOUND);
        }
        if (request == null || request.title() == null || request.title().isBlank()) {
            return new BasicResponse<>(false, "נא לכתוב שם לפרויקט");
        }
        if (projectRepository.countByStudentUserId(studentId) >= MAX_PROJECTS) {
            return new BasicResponse<>(false, "אפשר לשמור עד " + MAX_PROJECTS + " פרויקטים. מחקו פרויקט ישן כדי להוסיף חדש");
        }
        PortfolioProjectEntity project = new PortfolioProjectEntity();
        project.setStudentUserId(studentId);
        project.setTitle(request.title().trim());
        String description = request.description() == null ? null : request.description().trim();
        project.setDescription(description == null || description.isEmpty() ? null : description);
        project.setGradeName(header.getGradeName());
        project.setCreatedAt(LocalDateTime.now());
        projectRepository.save(project);
        return ok("הפרויקט נוסף לפורטפוליו", projectMapper.apply(project));
    }

    @Transactional
    public BasicResponse<Void> deleteProject(String studentId, Long projectId) {
        PortfolioProjectEntity project = projectId == null ? null
                : projectRepository.findByIdAndStudentUserId(projectId, studentId).orElse(null);
        if (project == null) {
            return new BasicResponse<>(false, "הפרויקט לא נמצא");
        }
        projectRepository.delete(project);
        return new BasicResponse<>(true, "הפרויקט נמחק");
    }

    /** Creates the share link, or replaces its token (the previous link stops working). */
    @Transactional
    public BasicResponse<Share> createShare(String studentId, String displayName) {
        if (header(studentId) == null) {
            return new BasicResponse<>(false, STUDENT_NOT_FOUND);
        }
        PortfolioShareEntity share = shareRepository.findByStudentUserId(studentId).orElseGet(() -> {
            PortfolioShareEntity s = new PortfolioShareEntity();
            s.setStudentUserId(studentId);
            return s;
        });
        share.setToken(newToken());
        share.setDisplayName(cleanDisplayName(displayName));
        share.setCreatedAt(LocalDateTime.now());
        shareRepository.save(share);
        return ok("נוצר קישור שיתוף חדש. קישור קודם, אם היה, כבר לא עובד",
                new Share(share.getToken(), share.getDisplayName(), share.getCreatedAt()));
    }

    @Transactional
    public BasicResponse<Void> revokeShare(String studentId) {
        shareRepository.findByStudentUserId(studentId).ifPresent(shareRepository::delete);
        return new BasicResponse<>(true, "השיתוף בוטל. הקישור כבר לא עובד");
    }

    /** The public view for a share token, or null when the token is unknown or malformed (the controller answers 404). */
    @Transactional(readOnly = true)
    public PublicPortfolio getPublic(String token) {
        if (token == null || !TOKEN_FORMAT.matcher(token).matches()) {
            return null;
        }
        PortfolioShareEntity share = shareRepository.findByToken(token).orElse(null);
        if (share == null) {
            return null;
        }
        String studentId = share.getStudentUserId();
        StudentHeader header = header(studentId);
        if (header == null) {
            return null;
        }
        StudentEngagementEntity engagement = engagementRepository.findByStudentUserId(studentId).orElse(null);
        int xp = engagement == null ? 0 : engagement.getTotalXp();

        List<PublicBadge> badges = badges(studentId).stream()
                .map(b -> new PublicBadge(b.title(), b.description(), b.earnedAt() == null ? null : b.earnedAt().toLocalDate()))
                .toList();
        List<PublicYear> years = years(studentId, header).stream()
                .map(y -> new PublicYear(y.yearName(), y.gradeName(), y.testsCount(), y.averageScore(), y.current()))
                .toList();
        List<PublicProject> projects = projectRepository.findByStudentUserIdOrderByCreatedAtDesc(studentId).stream()
                .map(p -> new PublicProject(p.getTitle(), p.getDescription(), p.getGradeName(), p.getCreatedAt().toLocalDate()))
                .toList();
        return new PublicPortfolio(share.getDisplayName(), header.getGradeName(), level(xp), xp,
                equippedAvatar(studentId).emoji(), badges, years, projects, trophies(studentId),
                share.getCreatedAt().toLocalDate());
    }

    // ---- pure helpers (unit tested) ----

    /**
     * Archived years in order, then the year in progress when the student is in a class. The current year's
     * average counts teacher tests on or after windowStart (all tests when windowStart is null).
     */
    static List<YearEntry> buildYears(List<YearRecord> archived, String currentGrade, String currentClass,
                                      List<ScoreRow> scores, LocalDate windowStart) {
        List<YearEntry> years = new ArrayList<>();
        for (YearRecord r : archived) {
            years.add(new YearEntry(r.getYearName(), r.getGradeName(), r.getClassName(), r.getTestsCount(),
                    r.getAverageScore(), r.getOutcome(), false));
        }
        if (currentGrade != null) {
            List<Integer> current = new ArrayList<>();
            for (ScoreRow s : scores) {
                if (s.getScore() == null) continue;
                if (windowStart == null) {
                    current.add(s.getScore());
                    continue;
                }
                LocalDate date = testDate(s.getStartTime());
                if (date != null && !date.isBefore(windowStart)) {
                    current.add(s.getScore());
                }
            }
            Double average = current.isEmpty() ? null
                    : BigDecimal.valueOf(current.stream().mapToInt(Integer::intValue).average().orElse(0))
                    .setScale(1, RoundingMode.HALF_UP).doubleValue();
            years.add(new YearEntry(CURRENT_YEAR, currentGrade, currentClass, current.size(), average, null, true));
        }
        return years;
    }

    static String cleanDisplayName(String displayName) {
        if (displayName == null) return DEFAULT_DISPLAY_NAME;
        String trimmed = displayName.replaceAll("[\\p{Cntrl}<>]", "").trim();
        if (trimmed.isEmpty()) return DEFAULT_DISPLAY_NAME;
        return trimmed.length() > MAX_DISPLAY_NAME ? trimmed.substring(0, MAX_DISPLAY_NAME) : trimmed;
    }

    static String newToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    static int level(int xp) {
        return xp / EngagementService.XP_PER_LEVEL + 1;
    }

    private static LocalDate testDate(String startTime) {
        if (startTime == null) return null;
        try {
            return LocalDateTime.parse(startTime.trim(), TEST_START_FORMAT).toLocalDate();
        } catch (RuntimeException e) {
            return null;
        }
    }

    // ---- internals ----

    private StudentHeader header(String studentId) {
        if (studentId == null) return null;
        List<StudentHeader> rows = queryRepository.findStudentHeader(studentId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private List<YearEntry> years(String studentId, StudentHeader header) {
        LocalDate windowStart = null;
        if (header.getSchoolCode() != null) {
            List<LocalDate> active = queryRepository.findActiveYearStart(header.getSchoolCode());
            windowStart = active.isEmpty() ? null : active.get(0);
            if (windowStart == null) {
                LocalDate lastEnd = queryRepository.findLastClosedYearEnd(header.getSchoolCode());
                windowStart = lastEnd == null ? null : lastEnd.plusDays(1);
            }
        }
        return buildYears(queryRepository.findArchivedYears(studentId), header.getGradeName(), header.getClassName(),
                queryRepository.findTeacherTestScores(studentId), windowStart);
    }

    private List<EngagementSummaryDTO.Badge> badges(String studentId) {
        return badgeRepository.findByStudentUserIdOrderByEarnedAtDesc(studentId).stream().map(badgeMapper).toList();
    }

    private List<QuestTrophy> trophies(String studentId) {
        return questProgressRepository.findByStudentUserId(studentId).stream()
                .filter(p -> p.getCompletedAt() != null)
                .sorted(Comparator.comparing(QuestProgressEntity::getCompletedAt))
                .map(p -> {
                    QuestCatalog.Quest quest = QuestCatalog.quest(p.getQuestCode());
                    return quest == null ? null
                            : new QuestTrophy(quest.code(), quest.title(), quest.emoji(), p.getCompletedAt());
                })
                .filter(t -> t != null)
                .toList();
    }

    private QuestCatalog.Item equippedAvatar(String studentId) {
        return cosmeticRepository.findByStudentUserId(studentId).stream()
                .filter(StudentCosmeticEntity::isEquipped)
                .map(c -> QuestCatalog.item(c.getItemCode()))
                .filter(i -> i != null && QuestCatalog.AVATAR.equals(i.kind()))
                .findFirst()
                .orElse(QuestCatalog.DEFAULT_AVATAR);
    }

    private static <T> BasicResponse<T> ok(String message, T data) {
        BasicResponse<T> response = new BasicResponse<>(true, message);
        response.setData(data);
        return response;
    }
}
