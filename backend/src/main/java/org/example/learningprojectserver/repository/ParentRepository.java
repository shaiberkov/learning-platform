package org.example.learningprojectserver.repository;

import org.example.learningprojectserver.dto.ChildSummaryDTO;
import org.example.learningprojectserver.entities.ParentEntity;
import org.example.learningprojectserver.entities.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ParentRepository extends JpaRepository<ParentEntity, Long> {

    @Query("SELECT p FROM ParentEntity p WHERE p.userId = :userId")
    ParentEntity findByUserId(@Param("userId") String userId);

    @Query("""
    SELECT new org.example.learningprojectserver.dto.ChildSummaryDTO(
        c.userId, c.username, cr.name, g.gradeName, sc.schoolName
    )
    FROM ParentEntity p
    JOIN p.children c
    LEFT JOIN c.classRoom cr
    LEFT JOIN cr.grade g
    LEFT JOIN c.schoolName sc
    WHERE p.userId = :parentId
    ORDER BY c.username
""")
    List<ChildSummaryDTO> findChildrenOfParent(@Param("parentId") String parentId);

    @Query("""
    SELECT COUNT(p) > 0 FROM ParentEntity p JOIN p.children c
    WHERE p.userId = :parentId AND c.userId = :childId
""")
    boolean isParentOf(@Param("parentId") String parentId, @Param("childId") String childId);

    /** Parents of the students in a class of the sender's school (sender is a teacher or the school manager). */
    @Query("""
    SELECT DISTINCT p FROM ParentEntity p JOIN p.children c
    WHERE c.classRoom.name = :className
      AND (c.schoolName.id IN (SELECT t.teachingSchool.id FROM TeacherEntity t WHERE t.userId = :senderId)
        OR c.schoolName.id IN (SELECT m.school.id FROM SchoolManagerEntity m WHERE m.userId = :senderId))
""")
    List<UserEntity> findParentsOfClassForSender(@Param("className") String className, @Param("senderId") String senderId);

    /** Parents of every student in the sender's school. */
    @Query("""
    SELECT DISTINCT p FROM ParentEntity p JOIN p.children c
    WHERE c.schoolName.id IN (SELECT t.teachingSchool.id FROM TeacherEntity t WHERE t.userId = :senderId)
       OR c.schoolName.id IN (SELECT m.school.id FROM SchoolManagerEntity m WHERE m.userId = :senderId)
""")
    List<UserEntity> findParentsOfSchoolForSender(@Param("senderId") String senderId);
}
