package com.strac.files.services.impl;

import com.google.api.client.http.HttpResponseException;
import com.google.api.client.http.InputStreamContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.strac.files.models.FileMetadata;
import com.strac.files.models.User;
import com.strac.files.models.dto.FileMetadataDTO;
import com.strac.files.models.repositories.FileRepository;
import com.strac.files.models.repositories.UserRepository;
import com.strac.files.services.DriveService;
import com.strac.files.services.FileService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Charles on 22/12/2024
 */

@Slf4j
@Service
@Transactional
public class FileServiceImpl implements FileService {

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private FileRepository fileRepository;

    @Autowired
    private DriveService driveService;

    // List all files by user ID
    @Transactional
    public List<FileMetadataDTO> listAllFilesByUserId(String userId) throws IOException {
        List<FileMetadata> cachedFiles = fileRepository.findAllByUser_OauthId(userId);

        // If a file has been updated with the last 15 mins (set to secs for demo purpose),
        // we can assume that the DB cache is up to date and ignore going to the drive
        boolean shouldFetchFromDrive = cachedFiles.isEmpty() ||
                cachedFiles.stream().anyMatch(file -> file.getUpdatedAt() == null ||
                        file.getUpdatedAt().isBefore(LocalDateTime.now().minusSeconds(15)));

        if (shouldFetchFromDrive) {
            // Clear old cache
            fileRepository.deleteAllByUser_OauthId(userId);

            // Fetch fresh data from Google Drive
            Drive drive = driveService.getDriveService(userId);
            FileList result = drive.files().list()
                    .setQ("'me' in owners")
                    .setFields("files(id, name, mimeType, modifiedTime)")
                    .execute();

            User user = userRepository.findByOauthId(userId);
            cachedFiles = result.getFiles().stream().map(file -> {
                return getFileMetadata(file, user);
            }).collect(Collectors.toList());

            // update file records in the DB
            fileRepository.saveAll(cachedFiles);
        }

        return cachedFiles.stream()
                .map(FileMetadataDTO::fromEntity)
                .collect(Collectors.toList());
    }

    // Upload a file
    public FileMetadataDTO uploadFile(MultipartFile file, String userId) throws IOException {
        File fileData = new File();
        fileData.setName(file.getOriginalFilename());

        InputStream inputStream = file.getInputStream();
        Drive drive = driveService.getDriveService(userId);
        File uploadedFile = drive.files()
                .create(fileData, new InputStreamContent(file.getContentType(), inputStream))
                .setFields("id, name, mimeType")
                .execute();

        User user = userRepository.findByOauthId(userId);
        FileMetadata fileEntity = getFileMetadata(uploadedFile, user);
        fileRepository.save(fileEntity);

        return FileMetadataDTO.fromEntity(fileEntity);
    }

    // Download file content from Google Drive
    public void downloadFile(String fileId, String userId, HttpServletResponse response)
            throws IOException {

        Drive drive = driveService.getDriveService(userId);
        try {
            File file = drive.files().get(fileId).execute();
            String fileName = file.getName();
            String mimeType = file.getMimeType();

            response.setContentType(mimeType);
            response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");

            OutputStream outputStream = response.getOutputStream();
            drive.files().get(fileId).executeMediaAndDownloadTo(outputStream);
            outputStream.flush();
        } catch (HttpResponseException e) {
            throw new IOException("Error downloading file");
        }
    }

    // Delete a file by Drive File ID
    public void deleteFileByDriveFileId(String userId, String driveFileId) throws IOException {
        Drive drive = driveService.getDriveService(userId);
        drive.files().delete(driveFileId).execute();
        FileMetadata fileEntity = fileRepository.findByDriveFileId(driveFileId);
        if (fileEntity != null) {
            fileRepository.delete(fileEntity);
        }
    }

    private static FileMetadata getFileMetadata(File file, User user) {
        FileMetadata entity = new FileMetadata();
        entity.setDriveFileId(file.getId());
        entity.setFileName(file.getName());
        entity.setMimeType(file.getMimeType());
        entity.setUpdatedAt(LocalDateTime.now());
        entity.setUser(user);
        return entity;
    }
}
