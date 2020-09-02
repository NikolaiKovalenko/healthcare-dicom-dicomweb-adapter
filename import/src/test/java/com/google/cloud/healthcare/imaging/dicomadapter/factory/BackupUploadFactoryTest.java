package com.google.cloud.healthcare.imaging.dicomadapter.factory;

import com.google.cloud.healthcare.imaging.dicomadapter.cstore.backup.BackupUploadService;
import com.google.cloud.healthcare.imaging.dicomadapter.cstore.configure.BackupUploadServiceFactory;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;

public class BackupUploadFactoryTest {
  private BackupUploadService backupUploadService;
  private static BackupUploadServiceFactory backupUploadServiceFactory;

  private static final String PERSISTENT_FILE_STORAGE_LOCATION = "tmp/backupfile ";
  private static final String OAUTH_SCOPES = "https://www.googleapis.com/auth/cloud-platform";
  private static final String GCP_BACKUP_PROJECT_ID = "gcp-test-project";
  private static final String GCP_PATH_PREFIX = "gs://";
  private static final List<Integer> HTTP_ERROR_CODES_TO_RETRY = new ArrayList<>();
  private static final int PERSISTENT_FILE_UPLOAD_RETRY_AMOUNT = 3;
  private static final int MIN_UPLOAD_DELAY = 100;
  private static final int MAX_WAITING_TIME_BETWEEN_UPLOADS = 100;

  @Before
  public static void init(){
    backupUploadServiceFactory = new BackupUploadServiceFactory()
            .setPersistentFileStorageLocation(PERSISTENT_FILE_STORAGE_LOCATION)
            .setPersistentFileUploadRetryAmount(PERSISTENT_FILE_UPLOAD_RETRY_AMOUNT)
            .setMinUploadDelay(MIN_UPLOAD_DELAY)
            .setMaxWaitingTimeBetweenUploads(MAX_WAITING_TIME_BETWEEN_UPLOADS)
            .setGcpBackupPrefix(GCP_PATH_PREFIX)
            .setGcsBackupProjectId(GCP_BACKUP_PROJECT_ID)
            .setHttpErrorCodesToRetry(HTTP_ERROR_CODES_TO_RETRY)
            .setOauthScopes(OAUTH_SCOPES);
  }

  @Test
  public void createNewInstance() throws IOException {
   backupUploadService = backupUploadServiceFactory.create();

   assertThat(backupUploadService).isNotNull();
   assertThat(backupUploadService).isInstanceOf(BackupUploadService.class);
  }
}