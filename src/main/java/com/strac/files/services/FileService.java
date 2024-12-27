package com.strac.files.services;

import com.strac.files.models.FileMetadata;
import com.strac.files.models.dto.FileMetadataDTO;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

/**
 * @author Charles on 22/12/2024
 */
public interface FileService {

    List<FileMetadataDTO> listAllFilesByUserId(String userId) throws IOException;

    FileMetadataDTO uploadFile(MultipartFile file, String userId) throws IOException;

    void deleteFileByDriveFileId(String userId, String driveFileId) throws IOException;

    void downloadFile(String fileId, String userId, HttpServletResponse response)
            throws IOException;
}
