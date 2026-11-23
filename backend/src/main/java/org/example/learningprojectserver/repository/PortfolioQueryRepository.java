package org.example.learningprojectserver.repository;

import org.example.learningprojectserver.entities.UserEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

/** Read-only lookups that build a student's portfolio (service/portfolio/PortfolioService). */
public interface PortfolioQueryRepository extends Repository<UserEntity, Long> {

    interface StudentHeader {
        String getUsername();
        String getGradeName();
        String getClassName();
        String getSchoolName();
        String getSchoolCode();
    }

    interface YearRecord {
        String getYearName();
        LocalDate getStartDate();
        String getGradeName();
        String getClassName();
        Integer getTestsCount();
        Double getAverageScore();
        String getOutcome();
    }

    interface ScoreRow {
        String getStartTime();
        Integer getScore();
    }

    @Query("""
        SELECT s.username AS username, g.gradeName AS gradeName, c.name AS className,
               sc.schoolName AS schoolName, sc.schoolCode AS schoolCode
        FROM StudentEntity s
        LEFT JOIN s.classRoom c
        LEFT JOIN c.grade g
        LEFT JOIN s.schoolName sc
        WHERE s.userId = :userId
    """)
    List<StudentHeader> findStudentHeader(@Param("userId") String userId);

    @Query("""
        SELECT y.name AS yearName, y.startDate AS startDate, r.gradeName AS gradeName, r.className AS className,
               r.testsCount AS testsCount, r.averageScore AS averageScore, r.outcome AS outcome
        FROM ArchivedStudentRecordEntity r
        JOIN r.schoolYear y
        WHERE r.studentUserId = :userId
        ORDER BY y.startDate ASC, y.id ASC
    """)
    List<YearRecord> findArchivedYears(@Param("userId") String userId);

    @Query("""
        SELECT test.startTime AS startTime, r.score AS score
        FROM TeacherTestResultEntity r
        JOIN r.student s
        JOIN r.test test
        WHERE s.userId = :userId
    """)
    List<ScoreRow> findTeacherTestScores(@Param("userId") String userId);

    /** Start date of the school's open, active year (empty when the school has none). */
    @Query("""
        SELECT y.startDate FROM SchoolYearEntity y
        WHERE y.school.schoolCode = :schoolCode AND y.active = true AND y.closed = false
        ORDER BY y.startDate DESC
    """)
    List<LocalDate> findActiveYearStart(@Param("schoolCode") String schoolCode);

    /** End date of the school's most recently closed year, or null. */
    @Query("SELECT MAX(y.endDate) FROM SchoolYearEntity y WHERE y.school.schoolCode = :schoolCode AND y.closed = true")
    LocalDate findLastClosedYearEnd(@Param("schoolCode") String schoolCode);
}
