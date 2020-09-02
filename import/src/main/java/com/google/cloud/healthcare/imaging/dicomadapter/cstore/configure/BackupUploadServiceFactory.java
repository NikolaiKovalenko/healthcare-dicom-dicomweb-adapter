package com.google.cloud.healthcare.imaging.dicomadapter.cstore.configure;

import com.google.cloud.healthcare.imaging.dicomadapter.Flags;
import com.google.cloud.healthcare.imaging.dicomadapter.cstore.backup.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

public class BackupUploadServiceFactory implements IBackupUploadServiceFactory {
    private static String persistentFileStorageLocation;
    private static int persistentFileUploadRetryAmount;
    private static int minUploadDelay;
    private static int maxWaitingTimeBetweenUploads;
    private static List<Integer> httpErrorCodesToRetry;
    private static String gcpBackupPrefix;
    private static String gcsBackupProjectId;
    private static String oauthScopes;
    private static BackupUploadService backupUploadService;

    private Logger log = LoggerFactory.getLogger(this.getClass());

    private static BackupUploadService configureBackupUploadService() throws IOException {
        String uploadPath = persistentFileStorageLocation;
        BackupFlags backupFlags = new BackupFlags(
                persistentFileUploadRetryAmount,
                minUploadDelay,
                maxWaitingTimeBetweenUploads,
                httpErrorCodesToRetry);

        if (!uploadPath.isBlank()) {
            final IBackupUploader backupUploader;
            if (uploadPath.startsWith(gcpBackupPrefix)) {
                backupUploader = new GcpBackupUploader(uploadPath, gcsBackupProjectId, oauthScopes);
            } else {
                backupUploader = new LocalBackupUploader(uploadPath);
            }
            return new BackupUploadService(backupUploader, backupFlags, new DelayCalculator());
        }
        return null;
    }

    @Override
    public BackupUploadService create() throws IOException {
        return configureBackupUploadService();
    }

    public BackupUploadServiceFactory setPersistentFileStorageLocation(String persistentFileStorageLocation) {
        BackupUploadServiceFactory.persistentFileStorageLocation = persistentFileStorageLocation;
        return this;
    }

    public BackupUploadServiceFactory setPersistentFileUploadRetryAmount(int persistentFileUploadRetryAmount) {
        BackupUploadServiceFactory.persistentFileUploadRetryAmount = persistentFileUploadRetryAmount;
        return this;
    }

    public BackupUploadServiceFactory setMinUploadDelay(int minUploadDelay) {
        BackupUploadServiceFactory.minUploadDelay = minUploadDelay;
        return this;
    }

    public BackupUploadServiceFactory setMaxWaitingTimeBetweenUploads(int maxWaitingTimeBetweenUploads) {
        BackupUploadServiceFactory.maxWaitingTimeBetweenUploads = maxWaitingTimeBetweenUploads;
        return this;
    }

    public BackupUploadServiceFactory setHttpErrorCodesToRetry(List<Integer> httpErrorCodesToRetry) {
        BackupUploadServiceFactory.httpErrorCodesToRetry = httpErrorCodesToRetry;
        return this;
    }

    public BackupUploadServiceFactory setGcpBackupPrefix(String gcpBackupPrefix) {
        BackupUploadServiceFactory.gcpBackupPrefix = gcpBackupPrefix;
        return this;
    }

    public BackupUploadServiceFactory setGcsBackupProjectId(String gcsBackupProjectId) {
        BackupUploadServiceFactory.gcsBackupProjectId = gcsBackupProjectId;
        return this;
    }

    public BackupUploadServiceFactory setOauthScopes(String oauthScopes) {
        BackupUploadServiceFactory.oauthScopes = oauthScopes;
        return this;
    }
}
