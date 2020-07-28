package com.google.cloud.healthcare.imaging.dicomadapter.backupuploader;


import com.google.auth.Credentials;
import com.google.auth.oauth2.GoogleCredentials;
import org.apache.http.client.utils.URIBuilder;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.cloud.storage.Blob;

public class GcpBackupUploader extends AbstractBackupUploader {
  private String projectName;
  private String bucketName;
  private String uploadObject;

  private static final String ENV_CREDS = "GOOGLE_APPLICATION_CREDENTIALS";

  public GcpBackupUploader(String uploadFilePath) throws GcpUriParseException {
    super(uploadFilePath);
    parse();
  }

  @Override
  public void doWriteBackup(byte[] backupData, String uniqueFileName)
      throws BackupException {
      try {
        if (backupData == null) {
          throw new BackupException("Backup data is null");
        }
        if (uniqueFileName.isEmpty() || uniqueFileName.isBlank()) {
          throw new BackupException("Invalid unique file name");
        }

        Storage storage = getStorage();
        BlobId blobId = BlobId.of(bucketName, getFullUploadObject(uniqueFileName));
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId).build();
        storage.create(blobInfo, backupData);
      } catch (Exception e) {
        throw new BackupException("Error with writing backup file: " + e.getMessage(), e);
      }
  }

  @Override
  public byte[] doReadBackup(String uniqueFileName) throws BackupException {
    try {
      if (uniqueFileName.isEmpty() || uniqueFileName.isBlank()) {
        throw new BackupException("Invalid unique file name");
      }

      Storage storage = getStorage();
      Blob blob = storage.get(BlobId.of(bucketName, getFullUploadObject(uniqueFileName)));
      blob.downloadTo(Paths.get(uniqueFileName));
      try (FileInputStream fin =
                   new FileInputStream(uniqueFileName)) {
        byte[] buffer = new byte[fin.available()];
        fin.read(buffer, 0, fin.available());
        if (buffer.length == 0) {
          throw new BackupException("No data in backup file.");
        }
        return buffer;
      } catch (Exception e) {
        throw new BackupException("Error with reading file : " + e.getMessage());
      } finally {
        Files.deleteIfExists(Paths.get(uniqueFileName));
      }
    } catch (Exception e) {
      throw new BackupException("Error with reading backup file: " + e.getMessage(), e);
    }
  }

  @Override
  public void removeBackup(String uniqueFileName) throws BackupException {
    try {
      if (uniqueFileName.isEmpty() || uniqueFileName.isBlank()) {
        throw new BackupException("Invalid unique file name");
      }
      Storage storage = getStorage();
      storage.delete(bucketName, getFullUploadObject(uniqueFileName));
    } catch (Exception e) {
      throw new BackupException("Error with removing backup file: " + e.getMessage(), e);
    }
  }

  private void parse() throws GcpUriParseException {
    try {
      if (getUploadFilePath().isBlank() || getUploadFilePath().isEmpty()) {
        throw new GcpUriParseException("Invalid upload path");
      }
      List<String> segments = new URIBuilder().setPath(getUploadFilePath()).getPathSegments();
      projectName = segments.get(2);
      bucketName = segments.get(3);

      uploadObject = new URIBuilder()
              .setPathSegments(segments.subList(4, segments.size()))
              .getPath().substring(1);
      if (projectName ==null || projectName.isBlank() || projectName.isEmpty()) {
        throw new GcpUriParseException("Invalid name for GCP project");
      }
      if (bucketName == null || bucketName.isBlank() || bucketName.isEmpty()) {
        throw new GcpUriParseException("Invalid name for GCS bucket");
      }
      if (uploadObject == null || uploadObject.isBlank() || uploadObject.isEmpty()) {
        throw new GcpUriParseException("Invalid name for GCS object");
      }
    } catch (Exception e) {
      throw new GcpUriParseException("Invalid upload path :" + e.getMessage(), e);
    }
  }

  public String getProjectName() {
    return projectName;
  }

  public String getBucketName() {
    return bucketName;
  }

  public String getUploadObject() {
    return uploadObject;
  }

  public Credentials getCredential(String env) throws IOException {
    return GoogleCredentials
            .fromStream(new FileInputStream(System.getenv(env)));
  }

  private String getFullUploadObject(String uniqueFileName) {
    return new URIBuilder().setPathSegments(uploadObject, uniqueFileName)
            .getPath().substring(1);
  }

  private Storage getStorage() throws IOException {
    return StorageOptions.newBuilder().setCredentials(getCredential(ENV_CREDS))
            .setProjectId(projectName).build().getService();
  }

  class GcpUriParseException extends IOException {
    public GcpUriParseException(String message, Throwable cause) {
      super(message, cause);
    }
    public GcpUriParseException(String message) {
      super(message);
    }
  }
}
