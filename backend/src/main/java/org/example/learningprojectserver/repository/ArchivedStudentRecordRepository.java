package org.example.learningprojectserver.repository;

import org.example.learningprojectserver.entities.ArchivedStudentRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ArchivedStudentRecordRepository extends JpaRepository<ArchivedStudentRecordEntity, Long> {

    List<ArchivedStudentRecordEntity> findBySchoolYear_IdOrderByGradeNameAscClassNameAscStudentNameAsc(Long schoolYearId);

    long countBySchoolYear_Id(Long schoolYearId);
}
