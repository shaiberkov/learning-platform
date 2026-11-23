package org.example.learningprojectserver.repository;

import org.example.learningprojectserver.entities.StudentWellbeingConsentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentWellbeingConsentRepository extends JpaRepository<StudentWellbeingConsentEntity, String> {
}
