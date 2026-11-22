package org.example.learningprojectserver.dto;

/** A message attachment ready to stream back to the browser. */
public record AttachmentDownload(String fileName, String contentType, byte[] data) {
}
