package com.strac.files.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author Charles on 22/12/2024
 */
@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "file_metadata")
public class FileMetadata extends BaseEntity {

    @JsonIgnoreProperties("files")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "drive_file_id", nullable = false)
    private String driveFileId;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private String mimeType;

    private Long size;

    @Column(name = "is_deleted")
    private boolean isDeleted = false;
}
