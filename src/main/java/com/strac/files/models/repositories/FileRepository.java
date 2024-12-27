package com.strac.files.models.repositories;

import com.strac.files.models.FileMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FileRepository extends JpaRepository<FileMetadata, Long> {

    FileMetadata findByDriveFileId(String driveFileId);

    List<FileMetadata> findAllByUser_OauthId(String userId);

    void deleteAllByUser_OauthId(String userId);
}