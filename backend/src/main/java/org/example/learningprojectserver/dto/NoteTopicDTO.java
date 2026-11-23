package org.example.learningprojectserver.dto;

/** One topic in the study notebook's filter, with how many notes the student has on it. */
public record NoteTopicDTO(String topic, long count) {}
