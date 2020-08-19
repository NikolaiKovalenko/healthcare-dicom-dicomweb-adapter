package com.google.cloud.healthcare.imaging.dicomadapter.backupuploader;

import com.google.auth.Credentials;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.ReadChannel;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.util.Arrays;
import org.apache.commons.lang3.StringUtils;

public class GcpBackupUploader extends AbstractBackupUploader {
  private String projectId;
  private String bucketName;
  private String uploadObject;
  private Storage storage;

  private static final String GCP_PATH_PREFIX = "gs://";

  public GcpBackupUploader(String uploadFilePath, String gcpProjectId, Storage storage) throws IOException {
    super(uploadFilePath);
    this.projectId = gcpProjectId;
    parseUploadFilePath(getUploadFilePath());
    this.storage = storage;
  }

  public GcpBackupUploader(String uploadFilePath, String gcpProjectId, String oauthScopes) throws IOException {
    super(uploadFilePath);
    this.projectId = gcpProjectId;
    parseUploadFilePath(getUploadFilePath());
    storage = getStorage(oauthScopes);
  }

  @Override
  public void doWriteBackup(InputStream backupData, String uniqueFileName) throws BackupException {
    try {
      if (backupData == null) {
        throw new BackupException("Backup data is null");
      }
      validatePathParameter(uniqueFileName, "unique file name");
      BlobId blobId = BlobId.of(bucketName, getFullUploadObject(uniqueFileName));
      BlobInfo blobInfo = BlobInfo.newBuilder(blobId).build();
      storage.createFrom(blobInfo, backupData);
    } catch (Exception e) {
      throw new BackupException("Error with writing backup file: " + e.getMessage(), e);
    }
  }

  @Override
  public InputStream doReadBackup(String uniqueFileName) throws BackupException {
    try {
      validatePathParameter(uniqueFileName, "unique file name");
      Blob blob = storage.get(BlobId.of(bucketName, getFullUploadObject(uniqueFileName)));
      ReadChannel channel = blob.reader();
      return Channels.newInputStream(channel);
    } catch (Exception e) {
      throw new BackupException("Error with reading backup file: " + e.getMessage(), e);
    }
  }

  @Override
  public void doRemoveBackup(String uniqueFileName) throws BackupException {
    try {
      validatePathParameter(uniqueFileName, "unique file name");
      storage.delete(bucketName, getFullUploadObject(uniqueFileName));
    } catch (Exception e) {
      throw new BackupException("Error with removing backup file: " + e.getMessage(), e);
    }
  }

  private void parseUploadFilePath(String uploadFilePath) throws GcpUriParseException {
    try {
      if (!uploadFilePath.startsWith(GCP_PATH_PREFIX)) {
        throw new GcpUriParseException("Not gcs link");
      }
      validatePathParameter(uploadFilePath, "upload file path");
      String route = uploadFilePath.replaceAll(GCP_PATH_PREFIX, "");
      String[] ar = route.split("/");
      bucketName = ar[0];
      uploadObject = String.join("/", Arrays.copyOfRange(ar, 1, ar.length));
      validatePathParameter(projectId, "project name");
      validatePathParameter(bucketName, "bucket name");
      validatePathParameter(uploadObject, "upload object");
    } catch (Exception e) {
      throw new GcpUriParseException("Invalid upload path: " + e.getMessage(), e);
    }
  }

  public String getProjectId() {
    return projectId;
  }

  public String getBucketName() {
    return bucketName;
  }

  public String getUploadObject() {
    return uploadObject;
  }

  public Credentials getCredential(String oauthScopes) throws IOException {
    validatePathParameter(oauthScopes, "oauthScopes");
    GoogleCredentials credentials = GoogleCredentials.getApplicationDefault();
    if (!StringUtils.isBlank(oauthScopes)) {
      credentials = credentials.createScoped(Arrays.asList(oauthScopes.split(",")));
    }
    return credentials;
  }

  private String getFullUploadObject(String uniqueFileName) {
    return uploadObject.concat("/").concat(uniqueFileName);
  }

  private Storage getStorage(String oauthScopes) throws IOException {
    return StorageOptions.newBuilder().setCredentials(getCredential(oauthScopes))
            .setProjectId(projectId).build().getService();
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
