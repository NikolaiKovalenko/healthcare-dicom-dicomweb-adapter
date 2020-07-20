package com.google.cloud.healthcare.imaging.dicomadapter.backupuploader;

import com.google.cloud.healthcare.IDicomWebClient;

import java.io.IOException;

public abstract class AbstractBackupUploadService implements IBackupUploadService {
    private IDicomWebClient dicomWebClient;
    private String uploadStorageLocation;
    private int uploadRetryAmount;

    public AbstractBackupUploadService(IDicomWebClient dicomWebClient, String uploadStorageLocation, int uploadRetryAmount) {
        this.dicomWebClient = dicomWebClient;
        this.uploadStorageLocation = uploadStorageLocation;
        this.uploadRetryAmount = uploadRetryAmount;
    }

    protected IDicomWebClient getDicomWebClient() {
        return dicomWebClient;
    }

    protected String getUploadStorageLocation() {
        return uploadStorageLocation;
    }

    protected int getUploadRetryAmount() {
        return uploadRetryAmount;
    }

    public void startUploading(IDicomWebClient dicomWebClient, BackupState backupState) {
        // todo: implement_me
    }

    public abstract byte[] doReadBackupFile(String downloadPath);

    static class BackupExeption extends IOException {
        public BackupExeption(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
