package com.strac.files.services.impl;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.strac.files.models.FileMetadata;
import com.strac.files.models.User;
import com.strac.files.models.dto.FileMetadataDTO;
import com.strac.files.models.repositories.FileRepository;
import com.strac.files.models.repositories.UserRepository;
import com.strac.files.services.DriveService;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * @author Charles on 23/12/2024
 */

@ExtendWith(MockitoExtension.class)
class FileServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private FileRepository fileRepository;

    @Mock
    private DriveService driveService;

    @Mock
    private Drive drive;

    @Mock
    private Drive.Files driveFiles;

    @Mock
    private Drive.Files.List filesList;

    @Mock
    private Drive.Files.Get filesGet;

    @Mock
    private Drive.Files.Create filesCreate;

    @Mock
    private Drive.Files.Delete filesDelete;

    @Mock
    private MultipartFile multipartFile;

    @Mock
    private HttpServletResponse response;

    @InjectMocks
    private FileServiceImpl fileService;

    private static final String USER_ID = "test-user-id";
    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setOauthId(USER_ID);
    }

    @Test
    void listAllFilesByUserId_WhenCacheIsValid_ReturnsCachedFiles() throws IOException {
        // Arrange
        FileMetadata cachedFile = new FileMetadata();
        cachedFile.setUpdatedAt(LocalDateTime.now());
        cachedFile.setFileName("test.txt");
        List<FileMetadata> cachedFiles = Collections.singletonList(cachedFile);

        when(fileRepository.findAllByUser_OauthId(USER_ID)).thenReturn(cachedFiles);

        // Act
        List<FileMetadataDTO> result = fileService.listAllFilesByUserId(USER_ID);

        // Assert
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals("test.txt", result.get(0).getFileName());
        verify(drive, never()).files();
    }

    @Test
    void listAllFilesByUserId_WhenCacheIsStale_FetchesFromDrive() throws IOException {
        // Arrange
        FileMetadata staleFile = new FileMetadata();
        staleFile.setUpdatedAt(LocalDateTime.now().minusMinutes(30));
        List<FileMetadata> staleFiles = Collections.singletonList(staleFile);

        when(fileRepository.findAllByUser_OauthId(USER_ID)).thenReturn(staleFiles);
        when(driveService.getDriveService(anyString())).thenReturn(drive);
        when(drive.files()).thenReturn(driveFiles);
        when(driveFiles.list()).thenReturn(filesList);
        when(filesList.setQ(anyString())).thenReturn(filesList);
        when(filesList.setFields(anyString())).thenReturn(filesList);

        File driveFile = new File();
        driveFile.setId("drive-file-id");
        driveFile.setName("updated.txt");
        FileList fileList = new FileList();
        fileList.setFiles(Collections.singletonList(driveFile));

        when(filesList.execute()).thenReturn(fileList);
        when(userRepository.findByOauthId(USER_ID)).thenReturn(testUser);

        // Act
        List<FileMetadataDTO> result = fileService.listAllFilesByUserId(USER_ID);

        // Assert
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals("updated.txt", result.get(0).getFileName());
        verify(fileRepository).deleteAllByUser_OauthId(USER_ID);
        verify(fileRepository).saveAll(any());
    }

    @Test
    void uploadFile_Success() throws IOException {
        // Arrange
        when(driveService.getDriveService(anyString())).thenReturn(drive);
        when(drive.files()).thenReturn(driveFiles);
        when(driveFiles.create(any(File.class), any())).thenReturn(filesCreate);
        when(filesCreate.setFields(anyString())).thenReturn(filesCreate);

        File uploadedFile = new File();
        uploadedFile.setId("new-file-id");
        uploadedFile.setName("test-upload.txt");
        when(filesCreate.execute()).thenReturn(uploadedFile);

        when(multipartFile.getOriginalFilename()).thenReturn("test-upload.txt");
        when(multipartFile.getContentType()).thenReturn("text/plain");
        when(multipartFile.getInputStream()).thenReturn(new ByteArrayInputStream("test content".getBytes()));
        when(userRepository.findByOauthId(USER_ID)).thenReturn(testUser);

        // Act
        FileMetadataDTO result = fileService.uploadFile(multipartFile, USER_ID);

        // Assert
        assertNotNull(result);
        assertEquals("test-upload.txt", result.getFileName());
        verify(fileRepository).save(any(FileMetadata.class));
    }

    @Test
    void downloadFile_Success() throws IOException {
        // Arrange
        when(driveService.getDriveService(anyString())).thenReturn(drive);
        when(drive.files()).thenReturn(driveFiles);
        when(driveFiles.get(anyString())).thenReturn(filesGet);

        File driveFile = new File();
        driveFile.setName("download.txt");
        driveFile.setMimeType("text/plain");
        when(filesGet.execute()).thenReturn(driveFile);

        ServletOutputStream outputStream = mock(ServletOutputStream.class);
        when(response.getOutputStream()).thenReturn(outputStream);

        // Act
        fileService.downloadFile("file-id", USER_ID, response);

        // Assert
        verify(response).setContentType("text/plain");
        verify(response).setHeader(eq("Content-Disposition"), contains("download.txt"));
        verify(outputStream).flush();
    }

    @Test
    void deleteFileByDriveFileId_Success() throws IOException {
        // Arrange
        when(driveService.getDriveService(anyString())).thenReturn(drive);
        when(drive.files()).thenReturn(driveFiles);
        when(driveFiles.delete(anyString())).thenReturn(filesDelete);
        FileMetadata existingFile = new FileMetadata();
        when(fileRepository.findByDriveFileId("file-id")).thenReturn(existingFile);

        // Act
        fileService.deleteFileByDriveFileId(USER_ID, "file-id");

        // Assert
        verify(filesDelete).execute();
        verify(fileRepository).delete(existingFile);
    }

    @Test
    void deleteFileByDriveFileId_WhenFileNotInDatabase_OnlyDeletesFromDrive() throws IOException {
        // Arrange
        when(driveService.getDriveService(USER_ID)).thenReturn(drive);
        when(drive.files()).thenReturn(driveFiles);
        when(driveFiles.delete("file-id")).thenReturn(filesDelete);
        doNothing().when(filesDelete).execute();  // Changed this line

        when(fileRepository.findByDriveFileId("file-id")).thenReturn(null);

        // Act
        fileService.deleteFileByDriveFileId(USER_ID, "file-id");

        // Assert
        verify(filesDelete).execute();
        verify(fileRepository, never()).delete(any());
    }

    @Test
    void listAllFilesByUserId_WhenDriveThrowsException_PropagatesException() throws IOException {
        // Arrange
        when(fileRepository.findAllByUser_OauthId(USER_ID)).thenReturn(Collections.emptyList());
        when(driveService.getDriveService(USER_ID)).thenReturn(drive);  // Add this
        when(drive.files()).thenReturn(driveFiles);  // Add this
        when(driveFiles.list()).thenReturn(filesList);
        when(filesList.setQ(anyString())).thenReturn(filesList);
        when(filesList.setFields(anyString())).thenReturn(filesList);
        when(filesList.execute()).thenThrow(new IOException("Drive API error"));

        // Act & Assert
        assertThrows(IOException.class, () -> fileService.listAllFilesByUserId(USER_ID));
    }

    @Test
    void uploadFile_WhenInputStreamFails_PropagatesException() throws IOException {
        // Arrange
        when(multipartFile.getInputStream()).thenThrow(new IOException("Failed to read file"));

        // Act & Assert
        assertThrows(IOException.class, () -> fileService.uploadFile(multipartFile, USER_ID));
    }
}