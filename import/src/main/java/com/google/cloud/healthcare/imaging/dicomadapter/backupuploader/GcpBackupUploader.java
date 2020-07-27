package com.google.cloud.healthcare.imaging.dicomadapter.backupuploader;

import com.google.auth.Credentials;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class GcpBackupUploader implements IBackupUploader {
  private GcpUtil gcpUtil;

  private static final String ENV_CREDS = "GOOGLE_APPLICATION_CREDENTIALS";

  @Override
  public void doWriteBackup(byte[] backupData, String uploadFilePath, String uniqueFileName)
      throws BackupException {
    try {
      if (backupData == null) {
        throw new BackupException("Backup data is null");
      }
      gcpUtil = getGcpUtil(uploadFilePath, uniqueFileName);
      gcpUtil.parse();
      Storage storage = StorageOptions.newBuilder().setCredentials(getCredential(ENV_CREDS))
              .setProjectId(gcpUtil.getProject()).build().getService();
      BlobId blobId = BlobId.of(gcpUtil.getBucket(), gcpUtil.getObject());
      BlobInfo blobInfo = BlobInfo.newBuilder(blobId).build();
      storage.create(blobInfo, backupData);
    } catch (Throwable e) {
      throw new BackupException("Error with writing backup file: " + e.getMessage(), e);
    }
  }

  @Override
  public byte[] doReadBackup(String uploadFilePath, String uniqueFileName) throws BackupException {
    try {
      gcpUtil = getGcpUtil(uploadFilePath, uniqueFileName);
      gcpUtil.parse();
      Storage storage = StorageOptions.newBuilder().setProjectId(gcpUtil.getProject()).build().getService();
      Blob blob = storage.get(BlobId.of(gcpUtil.getBucket(), gcpUtil.getObject()));
      blob.downloadTo(Paths.get(uniqueFileName));
      try (FileInputStream fin =
                   new FileInputStream(uniqueFileName)) {
        byte[] buffer = new byte[fin.available()];
        fin.read(buffer, 0, fin.available());
        if (buffer.length == 0) {
          throw new BackupException("No data in backup file.");
        }
        return buffer;
      }catch (Throwable e) {
        throw new BackupException("Error with reading file : " + e.getMessage());
      }finally {
        Files.deleteIfExists(Paths.get(uniqueFileName));
      }
    }catch (Throwable e) {
      throw new BackupException("Error with reading backup file: " + e.getMessage(), e);
    }
  }

  @Override
  public void removeBackup(String uploadFilePath, String uniqueFileName) throws BackupException {
    try {
      gcpUtil = getGcpUtil(uploadFilePath, uniqueFileName);
      gcpUtil.parse();
      Storage storage = StorageOptions.newBuilder().setCredentials(getCredential(ENV_CREDS))
              .setProjectId(gcpUtil.getProject()).build().getService();
      storage.delete(gcpUtil.getBucket(), gcpUtil.getObject());
    }catch (Throwable e) {
      throw new BackupException("Error with removing backup file: " + e.getMessage(), e);
    }
  }

  public GcpUtil getGcpUtil(String uploadFilePath, String uniqueFileName) throws BackupException {
    if (uploadFilePath.isBlank() || uploadFilePath.isEmpty()){
      throw new BackupException("Invalid upload path");
    }
    if (uniqueFileName.isBlank() || uniqueFileName.isEmpty()){
      throw new BackupException("Invalid unique file name");
    }
    return new GcpUtil(uploadFilePath.concat("/").concat(uniqueFileName));
  }

  public Credentials getCredential(String env) throws IOException {
    return GoogleCredentials
            .fromStream(new FileInputStream(System.getenv(env)));
  }
}
