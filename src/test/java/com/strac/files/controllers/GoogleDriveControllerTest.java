package com.strac.files.controllers;

import com.strac.files.models.dto.FileMetadataDTO;
import com.strac.files.services.FileService;
import com.strac.files.services.OAuth2Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * @author Charles on 27/12/2024
 */
public class GoogleDriveControllerTest {

    @Mock
    private FileService fileService;

    @Mock
    private OAuth2Service auth;

    @InjectMocks
    private GoogleDriveController googleDriveController;

    private String oauthId;
    private LocalDateTime now = LocalDateTime.now();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        oauthId = "test-oauth-id";
        when(auth.getOauthId()).thenReturn(oauthId);
    }

    @Test
    void testListFiles() throws IOException {
        List<FileMetadataDTO> mockFiles = Arrays.asList(
                new FileMetadataDTO("file1", "test-file-1", "application/pdf", now),
                new FileMetadataDTO("file2", "test-file-2", "image/png", now)
        );
        when(fileService.listAllFilesByUserId(oauthId)).thenReturn(mockFiles);

        List<FileMetadataDTO> result = googleDriveController.listFiles();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("test-file-1", result.get(0).getFileName());
        verify(fileService, times(1)).listAllFilesByUserId(oauthId);
    }

    @Test
    void testUploadFile() throws IOException {
        MultipartFile mockFile = mock(MultipartFile.class);
        FileMetadataDTO mockMetadata = new FileMetadataDTO("file1", "uploaded-file", "application/pdf", now);
        when(fileService.uploadFile(mockFile, oauthId)).thenReturn(mockMetadata);

        FileMetadataDTO result = googleDriveController.uploadFile(mockFile);

        assertNotNull(result);
        assertEquals("uploaded-file", result.getFileName());
        verify(fileService, times(1)).uploadFile(mockFile, oauthId);
    }

    @Test
    void testDownloadFile() throws IOException {
        String fileId = "test-file-id";
        MockHttpServletResponse mockResponse = new MockHttpServletResponse();

        doNothing().when(fileService).downloadFile(fileId, oauthId, mockResponse);

        googleDriveController.downloadFile(fileId, mockResponse);

        verify(fileService, times(1)).downloadFile(fileId, oauthId, mockResponse);
    }

    @Test
    void testDeleteFile() throws IOException {
        String fileId = "test-file-id";

        doNothing().when(fileService).deleteFileByDriveFileId(oauthId, fileId);

        ResponseEntity<Void> response = googleDriveController.deleteFile(fileId);

        assertNotNull(response);
        assertEquals(204, response.getStatusCodeValue());
        verify(fileService, times(1)).deleteFileByDriveFileId(oauthId, fileId);
    }
}
