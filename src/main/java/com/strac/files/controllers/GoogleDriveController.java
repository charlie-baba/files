package com.strac.files.controllers;

import com.strac.files.models.FileMetadata;
import com.strac.files.models.dto.FileMetadataDTO;
import com.strac.files.services.FileService;
import com.strac.files.services.OAuth2Service;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * @author Charles on 22/12/2024
 * Controller for Google Drive Operations
 */
@Slf4j
@RestController
@RequestMapping("/api/google-drive")
public class GoogleDriveController {

    @Autowired
    FileService fileService;

    @Autowired
    OAuth2Service auth;

    // List files in Google Drive
    @GetMapping("/files")
    public List<FileMetadataDTO> listFiles() throws IOException {
        return fileService.listAllFilesByUserId(auth.getOauthId());
    }

    // Upload a file to Google Drive
    @PostMapping("/upload")
    public FileMetadataDTO uploadFile(@RequestParam MultipartFile file) throws IOException {
        return fileService.uploadFile(file, auth.getOauthId());
    }

    // Download a file from Google Drive
    @GetMapping("/download/{fileId}")
    public void downloadFile( @PathVariable String fileId, HttpServletResponse response) throws IOException {
        fileService.downloadFile(fileId, auth.getOauthId(), response);
    }

    // Delete a file from Google Drive
    @DeleteMapping("/delete/{fileId}")
    public ResponseEntity<Void> deleteFile(@PathVariable String fileId) throws IOException {
        fileService.deleteFileByDriveFileId(auth.getOauthId(), fileId);
        return ResponseEntity.noContent().build();
    }
}
