package com.strac.files.services;

import com.google.api.services.drive.Drive;

import java.io.IOException;

/**
 * @author Charles on 25/12/2024
 */
public interface DriveService {

    Drive getDriveService(String userId) throws IOException;
}
