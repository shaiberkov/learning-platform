package org.example.learningprojectserver.repository;

import org.example.learningprojectserver.entities.GradebookAssignmentEntity;
import org.example.learningprojectserver.entities.GradebookGradeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface GradebookGradeRepository extends JpaRepository<GradebookGradeEntity, Long> {

    @Query("SELECT g FROM GradebookGradeEntity g JOIN FETCH g.assignment a WHERE a.id IN :assignmentIds")
    List<GradebookGradeEntity> findByAssignmentIds(@Param("assignmentIds") Collection<Long> assignmentIds);

    Optional<GradebookGradeEntity> findByAssignmentAndStudentUserId(GradebookAssignmentEntity assignment, String studentUserId);

    @Modifying
    @Query("DELETE FROM GradebookGradeEntity g WHERE g.assignment.id = :assignmentId")
    void deleteByAssignmentId(@Param("assignmentId") Long assignmentId);
}
