package com.google.cloud.healthcare.imaging.dicomadapter.backupuploader;

import com.google.cloud.healthcare.IDicomWebClient;

import java.io.FileOutputStream;
import java.io.IOException;


public class LocalBackupUploadService extends AbstractBackupUploadService {
    public LocalBackupUploadService(IDicomWebClient dicomWebClient, String uploadStorageLocation, int uploadRetryAmount) {
        super(dicomWebClient, uploadStorageLocation, uploadRetryAmount);
    }

    @Override
    public byte[] doReadBackupFile(String downloadPath) {
        return new byte[0];
    }

    @Override
    public BackupState createBackup(byte[] backupData) throws BackupExeption {
        String backupPath = getUploadStorageLocation();
        BackupState backupState = new BackupState(backupPath, getUploadRetryAmount());
        try(FileOutputStream fos=new FileOutputStream(backupPath))
        {
            fos.write(backupData, 0, backupData.length);
            return backupState;
        }catch (IOException ex){
            // todo : throw
            throw new BackupExeption("Message", ex);
        }
    }
}
