package org.example.learningprojectserver.repository;

import org.example.learningprojectserver.entities.SchoolYearEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SchoolYearRepository extends JpaRepository<SchoolYearEntity, Long> {

    @Query("SELECT y FROM SchoolYearEntity y WHERE y.school.schoolCode = :schoolCode ORDER BY y.startDate DESC, y.id DESC")
    List<SchoolYearEntity> findBySchoolCode(@Param("schoolCode") String schoolCode);

    @Query("SELECT y FROM SchoolYearEntity y WHERE y.id = :id AND y.school.schoolCode = :schoolCode")
    SchoolYearEntity findByIdAndSchoolCode(@Param("id") Long id, @Param("schoolCode") String schoolCode);
}
