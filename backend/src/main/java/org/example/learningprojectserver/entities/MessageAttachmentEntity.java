package org.example.learningprojectserver.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** A file attached to a message. One row is shared by every copy of a broadcast (same batchId). */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "message_attachment_entity")
public class MessageAttachmentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private String contentType;

    @Column(nullable = false)
    private long sizeBytes;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(nullable = false, columnDefinition = "LONGBLOB")
    private byte[] data;

    @Column(nullable = false)
    private String uploaderUserId;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}
