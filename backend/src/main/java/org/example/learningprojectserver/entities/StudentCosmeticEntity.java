package org.example.learningprojectserver.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** A cosmetic reward (avatar item or room theme) a student unlocked by finishing a quest. */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "student_cosmetic", uniqueConstraints = {
        @UniqueConstraint(name = "uk_student_cosmetic_item", columnNames = {"studentUserId", "itemCode"})
}, indexes = {
        @Index(name = "idx_student_cosmetic_student", columnList = "studentUserId")
})
public class StudentCosmeticEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String studentUserId;

    @Column(nullable = false, length = 40)
    private String itemCode;

    /** AVATAR or ROOM, copied from the catalog so "one equipped item per kind" needs no catalog lookup. */
    @Column(nullable = false, length = 16)
    private String kind;

    private boolean equipped;

    @Column(nullable = false)
    private LocalDateTime unlockedAt;
}
