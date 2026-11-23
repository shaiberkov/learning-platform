package org.example.learningprojectserver.dto;

/** A teacher test given to (some of) a class, read for the gradebook. */
public record TeacherTestColumnDTO(Long testId, String subject, String topic, String startTime) {}
