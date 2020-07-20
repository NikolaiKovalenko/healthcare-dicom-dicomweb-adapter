package com.google.cloud.healthcare.imaging.dicomadapter.backupuploader;

import com.google.cloud.healthcare.IDicomWebClient;

public class GcpBackupUploadService extends AbstractBackupUploadService {
    public GcpBackupUploadService(IDicomWebClient dicomWebClient, String uploadStorageLocation, int uploadRetryAmount) {
        super(dicomWebClient, uploadStorageLocation, uploadRetryAmount);
    }

    @Override
    public byte[] doReadBackupFile(String downloadPath) {
        return new byte[0];
    }

    @Override
    public BackupState createBackup(byte[] backupData) throws BackupExeption {
        // todo: implement_me
        return null;
    }
}
