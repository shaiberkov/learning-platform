package org.example.learningprojectserver.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.learningprojectserver.enums.Feature;

import java.time.LocalDateTime;

/** A system admin's on/off choice for one feature in one school. */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "school_feature_flag", uniqueConstraints = {
        @UniqueConstraint(name = "uk_school_feature", columnNames = {"schoolCode", "feature"})
})
public class SchoolFeatureFlagEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String schoolCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 64)
    private Feature feature;

    @Column(nullable = false)
    private boolean enabled;

    private LocalDateTime updatedAt;
    private String updatedBy;
}
