package com.strac.files.models.dto;

import com.strac.files.models.FileMetadata;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @author Charles on 25/12/2024
 */
@Data
public class FileMetadataDTO {
    private String driveFileId;
    private String fileName;
    private String mimeType;
    private LocalDateTime updatedAt;

    public static FileMetadataDTO fromEntity(FileMetadata entity) {
        FileMetadataDTO dto = new FileMetadataDTO();
        dto.setDriveFileId(entity.getDriveFileId());
        dto.setFileName(entity.getFileName());
        dto.setMimeType(entity.getMimeType());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }
}