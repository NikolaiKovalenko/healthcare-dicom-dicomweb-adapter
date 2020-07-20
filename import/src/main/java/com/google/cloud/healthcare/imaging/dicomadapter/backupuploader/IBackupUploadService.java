package com.google.cloud.healthcare.imaging.dicomadapter.backupuploader;

import com.google.cloud.healthcare.IDicomWebClient;

public interface IBackupUploadService {
    AbstractBackupUploadService.BackupState createBackup(byte [] backupData) throws AbstractBackupUploadService.BackupExeption;
    void startUploading(IDicomWebClient dicomWebClient, AbstractBackupUploadService.BackupState backupState);

    public static class BackupState {
        private String downloadPath;
        private int attemptCountdown;

        public BackupState(String downloadPath, int attpemtCountdown) {
            this.downloadPath = downloadPath;
            this.attemptCountdown = attpemtCountdown;
        }

        public String getDownloadPath() {
            return downloadPath;
        }

        public int getAttemptCountdown() {
            return attemptCountdown;
        }
    }
}